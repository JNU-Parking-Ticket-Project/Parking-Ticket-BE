package com.jnu.ticketapi;
import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.api.event.model.request.EventRegisterRequest;
import com.jnu.ticketapi.api.event.model.request.UpdateEventPublishRequest;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketapi.api.user.ResultAssignment;
import com.jnu.ticketapi.registration.FinalSaveRequestTestDataBuilder;
import com.jnu.ticketapi.registration.TemporarySaveRequestTestDataBuilder;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketbatch.config.QuartzJobLauncher;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaRepository;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.repository.EventRepository;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jnu.ticketdomain.domains.user.domain.UserStatus.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FlowTest implements UsingContainers {

    // production 환경변수 값
    private static final int ASYNC_CORE_POOL_SIZE = 1000;
    private static final int ASYNC_MAX_POOL_SIZE = 1000;
    private static final int ASYNC_QUEUE_CAPACITY = 50000;
    private static final int HIKARI_MAXIMUM_POOL_SIZE = 2000;

    private static final Long EVENT_VALUE = 1L;
    private static final String BEARER_PREFIX = "Bearer ";

    @DynamicPropertySource
    static void asyncTheadProperties(DynamicPropertyRegistry registry) {
        registry.add("thread.core-pool-size", () -> ASYNC_CORE_POOL_SIZE);
        registry.add("thread.max-pool-size", () -> ASYNC_MAX_POOL_SIZE);
        registry.add("thread.queue-capacity", () -> ASYNC_QUEUE_CAPACITY);
    }

    @DynamicPropertySource
    static void hikariProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> HIKARI_MAXIMUM_POOL_SIZE);
    }

    @Autowired
    WebTestClient client;

    @Autowired
    Scheduler scheduler;

    @Autowired
    JwtGenerator jwtGenerator;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CaptchaRepository captchaRepository;

    @Autowired
    RegistrationRepository registrationRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    RedisRepository redisRepository;

    @Autowired
    ResultAssignment resultAssignment;

    private record Setting(int capacity, int reserve, int requestCount) {
    }

    private Long USER_IDENTIFIER = 1L;


    /**
     * Setting record 통해서 구간별 여석, 예비, 요청 수 설정 가능
     * user id 순으로 각 sector에 요청보냄.
     * sector 랑 event는 새로 만들어져야함 id = 1부터 시작해야함.
     */
    @Test
    @DisplayName("여러 사용자의 최종 저장 요청 신청시, 여석에 맞게 합격, 예비, 불합격 수와 각 구간별 예비번호를 검증한다.")
    void flowTest() throws Exception {
        // given
        List<Setting> settings = List.of(
                new Setting(50, 10, 80),
                new Setting(25, 10, 80),
                new Setting(55, 10, 80),
                new Setting(80, 10, 100),
                new Setting(40, 10, 80)
        );


        Integer capacityCountSum = settings.stream().map(Setting::capacity).reduce(0, Integer::sum);
        Integer reserveCountSum = settings.stream().map(Setting::reserve).reduce(0, Integer::sum);
        Integer userCountSum = settings.stream().map(Setting::requestCount).reduce(0, Integer::sum);

        List<List<String>> userAccessTokens = setUpAccessTokensPerSector(settings);

        String tempAccessToken = userAccessTokens.get(0).get(0);
        createEvent(tempAccessToken);
        createCaptcha();
        createSectors(settings);
        setEventPublic();

        temporalSaveRequest(userAccessTokens);

        rescheduleJob();
        awaitEventOpen();

        // when
        ExecutorService executorServiceForSector = Executors.newFixedThreadPool(settings.size());
        ExecutorService executorServiceInSector = Executors.newCachedThreadPool();

        for (int i = 0; i < settings.size(); i++) {
            int sectorId = i + 1;
            List<String> accessTokensPerSector = userAccessTokens.get(i);
            executorServiceForSector.submit(() -> finalSaveRequestToSector(sectorId, accessTokensPerSector, executorServiceInSector));
        }
        executorServiceForSector.shutdown();
        executorServiceForSector.awaitTermination(60, SECONDS);
        executorServiceInSector.shutdown();
        executorServiceInSector.awaitTermination(60, SECONDS);

        awaitAllRegistrationsSaved(userCountSum);

        resultAssignment.assign(EVENT_VALUE);

        // then
        List<User> usersWithResult = userRepository.findAll(Sort.by("id"));
        List<Registration> resultRegistration = registrationRepository.findSortedRegistrationsByEventId(EVENT_VALUE);
        List<Registration> registrations = registrationRepository.findAll();

        Map<UserStatus, List<User>> resultByGroup = usersWithResult.stream()
                .collect(Collectors.groupingBy(User::getStatus, Collectors.toList()));

        printRegistrationsWithUserStatus(resultRegistration, usersWithResult);

        assertSoftly(softly -> {
            softly.assertThat(registrations).hasSize(userCountSum);
            softly.assertThat(resultByGroup.getOrDefault(SUCCESS, Collections.emptyList())).hasSize(capacityCountSum);
            softly.assertThat(resultByGroup.getOrDefault(PREPARE, Collections.emptyList())).hasSize(reserveCountSum);
            softly.assertThat(resultByGroup.getOrDefault(FAIL, Collections.emptyList())).hasSize(userCountSum - (capacityCountSum + reserveCountSum));
        });

        assertPerSector(usersWithResult, settings);
    }

    private void printRegistrationsWithUserStatus(List<Registration> registrations, List<User> usersWithResult) {
        Map<String, User> usersByEmail = usersWithResult.stream()
                .collect(Collectors.toMap(User::getEmail, u -> u, (a, b) -> a));

        System.out.println("============ 신청서 결과 (savedAt 오름차순) ============");
        System.out.printf("%-6s | %-30s | %-23s | %-8s | %-8s%n",
                "regId", "email", "savedAt", "status", "sequence");
        System.out.println("-".repeat(95));

        int i = 1;
        for (Registration r : registrations) {
            Long savedAt = r.getSavedAt();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(savedAt),
                    ZoneId.systemDefault()
            );

            User user = usersByEmail.get(r.getEmail());
            String status = user != null ? String.valueOf(user.getStatus()) : "N/A";
            String sequence = user != null ? String.valueOf(user.getSequence()) : "N/A";

            // 날짜 형식이 너무 길면 칸이 깨지므로, 길이에 맞춰 너비를 조절했습니다.
            System.out.printf("%-6d | %-35s | %-25s | %-10s | %-8s | %-10s| %-2d%n",
                    r.getId(),
                    r.getEmail(),
                    localDateTime,
                    status,
                    sequence,
                    r.getSector().getId(),
                    i);
            i++;
        }
        System.out.println("=".repeat(95));
    }

    private void temporalSaveRequest(List<List<String>> userAccessTokens) {
        List<String> flatTokens = userAccessTokens.stream()
                .flatMap(List::stream)
                .toList();
        int count = (int) (flatTokens.size() * (0.9));
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            int groupIndex = random.nextInt(userAccessTokens.size());
            int sectorId = groupIndex + 1;
            List<String> accessTokens = userAccessTokens.get(groupIndex);
            int userIndex = random.nextInt(accessTokens.size());

            String accessToken = accessTokens.get(userIndex);

            WebTestClient newClient = client.mutate()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .build();

            TemporarySaveRequest request = TemporarySaveRequestTestDataBuilder
                    .builder()
                    .withSelectSectorId((long) sectorId)
                    .build();

            newClient.post().uri("/v1/registration/temporary/{event-id}", EVENT_VALUE)
                    .bodyValue(request)
                    .exchange().expectStatus().isOk();
        }

    }

    private List<List<String>> setUpAccessTokensPerSector(List<Setting> settings) {
        return settings.stream()
                .map(Setting::requestCount)
                .map(this::setUpUserData)
                .toList();
    }

    private void finalSaveRequestToSector(long sectorId, List<String> accessTokens, ExecutorService executorService) {
        for (String accessToken : accessTokens) {
            executorService.execute(() -> {
                WebTestClient newClient = client.mutate()
                        .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                        .build();

                String captchaCode = newClient.get().uri("/v1/captcha")
                        .exchange().expectStatus().isOk()
                        .expectBody(CaptchaResponse.class)
                        .returnResult().getResponseBody().captchaCode();

                FinalSaveRequest request2 = FinalSaveRequestTestDataBuilder
                        .builder()
                        .withSelectSectorId(sectorId)
                        .withCaptchaCode(captchaCode)
                        .withCaptchaAnswer("1")
                        .build();

                newClient.post().uri("/v1/registration/{event-id}", EVENT_VALUE)
                        .bodyValue(request2)
                        .exchange().expectStatus().isOk();
            });
        }
    }

    private void assertPerSector(List<User> usersWithResult, List<Setting> settings) {
        List<List<User>> usersGroupBySector = groupBySector(usersWithResult, settings);
        int i = 0;
        for (List<User> users : usersGroupBySector) {
            Map<UserStatus, List<User>> resultByGroup = users.stream()
                    .collect(Collectors.groupingBy(User::getStatus, Collectors.toList()));

            int capacity = settings.get(i).capacity();
            int reserve = settings.get(i).reserve();

            List<Integer> preparedNumbers = resultByGroup.get(PREPARE).stream().map(User::getSequence).toList();
            List<Integer> expectedPreparedNumbers = IntStream.rangeClosed(1, reserve).boxed().toList();

            assertSoftly(softly -> {
                softly.assertThat(resultByGroup.getOrDefault(SUCCESS, Collections.emptyList())).hasSize(capacity);
                softly.assertThat(resultByGroup.getOrDefault(PREPARE, Collections.emptyList())).hasSize(reserve);
                softly.assertThat(resultByGroup.getOrDefault(FAIL, Collections.emptyList())).hasSize(users.size() - (capacity + reserve));
                softly.assertThat(preparedNumbers).containsExactlyInAnyOrderElementsOf(expectedPreparedNumbers);
            });
            i++;
        }
    }

    private List<List<User>> groupBySector(List<User> usersWithResult, List<Setting> settings) {
        List<Integer> count = settings.stream()
                .map(Setting::requestCount)
                .toList();

        int from = 0;
        List<List<User>> usersGroupBySector = new ArrayList<>();
        for (int i = 0; i < count.size(); i++) {
            Integer to = count.get(i);
            List<User> users = usersWithResult.subList(from, from + to);
            usersGroupBySector.add(users);
            from = from + to;
        }
        return usersGroupBySector;
    }

    private void rescheduleJob() throws SchedulerException {
        scheduler.clear();

        JobDetail openJob = createEventOpenJob();
        Trigger openTrigger = createEventOpenTrigger(openJob);

        scheduler.scheduleJob(openJob, openTrigger);
        scheduler.start();
    }

    private Trigger createEventOpenTrigger(JobDetail eventOpenJob) {
        return newTrigger()
                .withIdentity("EVENT_OPEN_TRIGGER" + EVENT_VALUE, "testGroup")
                .startNow()
                .forJob(eventOpenJob)
                .build();
    }

    private JobDetail createEventOpenJob() {
        return newJob(QuartzJobLauncher.class)
                .withIdentity("EVENT_OPEN_JOB" + EVENT_VALUE, "testGroup")
                .usingJobData("eventId", EVENT_VALUE)
                .build();
    }

    private List<String> setUpUserData(int userSize) {
        List<User> users = saveUsers(userSize);
        return generateToken(users);
    }

    private List<User> saveUsers(int size) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            User user = User.builder()
                    .email("user" + USER_IDENTIFIER + "@test.ac.kr")
                    .pwd("password" + USER_IDENTIFIER)
                    .userRole(UserRole.ADMIN)
                    .build();
            users.add(userRepository.save(user));
            USER_IDENTIFIER++;
        }
        return users;
    }

    private List<String> generateToken(List<User> users) {
        return users.stream()
                .map(user -> jwtGenerator.generateAccessToken(user.getEmail(), user.getUserRole().name()))
                .toList();
    }

    private void setEventPublic() {
        client.put().uri("/v1/events/publish/{event-id}", EVENT_VALUE)
                .bodyValue(new UpdateEventPublishRequest(true))
                .exchange().expectStatus().isOk();
    }

    private void createEvent(String accessToken) {
        client = client.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                .build();

        LocalDateTime now = LocalDateTime.now();
        DateTimePeriod dateTimePeriod = new DateTimePeriod(now.plusSeconds(5), now.plusMinutes(10));

        EventRegisterRequest registerRequest = new EventRegisterRequest(dateTimePeriod, "주차권 이벤트");
        client.post().uri("/v1/events")
                .bodyValue(registerRequest)
                .exchange().expectStatus().isOk();
    }

    private void createCaptcha() {
        String captchaAnswer = "1";
        captchaRepository.save(new Captcha(captchaAnswer, "imageUrl"));
    }

    private void createSectors(List<Setting> settings) {
        int sectorIdentifier = 1;
        for (Setting setting : settings) {
            Integer capacity = setting.capacity();
            Integer reserve = setting.reserve();
            List<SectorRegisterRequest> request = List.of(
                    new SectorRegisterRequest(sectorIdentifier + "구간", "테스트" + sectorIdentifier, capacity, reserve)
            );
            client.post().uri("/v1/sectors")
                    .bodyValue(request)
                    .exchange().expectStatus().isOk();
            sectorIdentifier++;
        }
    }

    private void awaitAllRegistrationsSaved(int expectedSavedCount) {
        await().atMost(60, SECONDS)
                .pollInterval(200, MILLISECONDS)
                .until(() -> registrationRepository.findAll().stream()
                        .filter(Registration::isSaved)
                        .count() == expectedSavedCount);
        System.out.println("============데이터 처리 완료===============");
    }

    private void awaitEventOpen() {
        await().atMost(60, SECONDS)
                .pollInterval(1, SECONDS)
                .until(
                        () -> {
                            // 매번 findById를 통해 DB를 조회 시도
                            Event event = eventRepository.findFirstByOrderByIdDesc().orElseThrow();
                            log.info("현재 이벤트 상태: {}", event.getEventStatus());
                            return event.getEventStatus() == EventStatus.OPEN;
                        });
        System.out.println("============이벤트 오픈 완료===============");
    }

}

