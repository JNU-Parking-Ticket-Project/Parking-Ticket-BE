package com.jnu.ticketapi;

import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.api.event.model.request.EventRegisterRequest;
import com.jnu.ticketapi.api.event.model.request.UpdateEventPublishRequest;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketapi.registration.FinalSaveRequestTestDataBuilder;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketbatch.config.ProcessQueueDataJob;
import com.jnu.ticketbatch.config.QuartzJobLauncher;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaRepository;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jnu.ticketdomain.domains.user.domain.UserStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class FlowTest {

    private static final int ASYNC_CORE_POOL_SIZE = 1000;
    private static final int ASYNC_MAX_POOL_SIZE = 1000;
    private static final int ASYNC_QUEUE_CAPACITY = 50000;
    private static final int HIKARI_MAXIMUM_POOL_SIZE = 2000;

    private static final int REDIS_PORT = 6379;
    private static final int MYSQL_PORT = 3306;
    private static final Long EVENT_VALUE = 1L;
    private static final String BEARER_PREFIX = "Bearer ";

    @Container
    static MySQLContainer mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.40"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
        registry.add("spring.datasource.url", () -> String.format(
                "jdbc:mysql://%s:%d/%s", mysqlContainer.getHost(), mysqlContainer.getMappedPort(MYSQL_PORT), mysqlContainer.getDatabaseName())
        );
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

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

    private record Setting(int capacity, int reserve, int requestCount) {
    }

    private Long USER_IDENTIFIER = 1L;

    @Test
    void flowTest() throws Exception {
        // given
        List<Setting> settings = List.of(
                new Setting(10, 30, 40),
                new Setting(10, 30, 40),
                new Setting(10, 30, 40)
        );

        int resultWaitingSecond = 20;

        Integer capacityCountSum = settings.stream().map(Setting::capacity).reduce(0, Integer::sum);
        Integer reserveCountSum = settings.stream().map(Setting::reserve).reduce(0, Integer::sum);
        Integer userCountSum = settings.stream().map(Setting::requestCount).reduce(0, Integer::sum);

        List<List<String>> userAccessTokens = setUpAccessTokensPerSector(settings);

        String tempAccessToken = userAccessTokens.get(0).get(0);
        createEvent(tempAccessToken);
        createCaptcha();
        createSectors(settings);
        setEventPublic();

        rescheduleJob();
        Thread.sleep(1000);

        // when
        ExecutorService executorServiceForSector = Executors.newFixedThreadPool(settings.size());
        ExecutorService executorServiceInSector = Executors.newCachedThreadPool();

        for (int i = 0; i < settings.size(); i++) {
            int sectorId = i + 1;
            List<String> accessTokensPerSector = userAccessTokens.get(i);
            executorServiceForSector.submit(() -> finalSaveRequestToSector(sectorId, accessTokensPerSector, executorServiceInSector));
        }

        Thread.sleep(resultWaitingSecond * 1000);

        // then
        List<User> usersWithResult = userRepository.findAll(Sort.by("id"));
        List<Registration> registrations = registrationRepository.findAll();

        Map<UserStatus, List<User>> resultByGroup = usersWithResult.stream()
                .collect(Collectors.groupingBy(User::getStatus, Collectors.toList()));

        assertSoftly(softly -> {
            softly.assertThat(registrations).hasSize(userCountSum);
            softly.assertThat(resultByGroup.getOrDefault(SUCCESS, Collections.emptyList())).hasSize(capacityCountSum);
            softly.assertThat(resultByGroup.getOrDefault(PREPARE, Collections.emptyList())).hasSize(reserveCountSum);
            softly.assertThat(resultByGroup.getOrDefault(FAIL, Collections.emptyList())).hasSize(userCountSum - (capacityCountSum + reserveCountSum));
        });

        assertPerSector(usersWithResult, settings);


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

        JobDetail processJob = createRegistrationProcessingJob();
        Trigger processTrigger = createRegistrationProcessingTrigger(processJob);

        scheduler.scheduleJob(processJob, processTrigger);
        scheduler.scheduleJob(openJob, openTrigger);
        scheduler.start();
    }

    private SimpleTrigger createRegistrationProcessingTrigger(JobDetail processJob) {
        return newTrigger()
                .withIdentity("REGISTRATION_PROCESSING_TRIGGER" + EVENT_VALUE, "testGroup")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(400)
                                .repeatForever())
                .forJob(processJob)
                .build();
    }

    private JobDetail createRegistrationProcessingJob() {
        return newJob(ProcessQueueDataJob.class)
                .withIdentity("REGISTRATION_PROCESSING_JOB" + EVENT_VALUE, "testGroup")
                .usingJobData("eventId", EVENT_VALUE)
                .build();
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

}


