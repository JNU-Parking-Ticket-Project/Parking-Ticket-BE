package com.jnu.ticketapi.api.flow;

import com.jnu.ticketbatch.flow.RegistrationProcessor;
import com.jnu.ticketinfrastructure.model.RegistrationInfo;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class RegistrationProcessorImp implements RegistrationProcessor {

    private final RegistrationSavingProcessor registrationSavingProcessor;
    private final UserStatusProcessor userStatusProcessor;
    private final RedisRepository redisRepository;


    public boolean process(RegistrationInfo registrationInfo) {
        try {
            registrationSavingProcessor.process(registrationInfo);
        } catch (DataIntegrityViolationException e) {
            log.error("중복 키 에러", e);
            return true;
        } catch (Exception e) {
            log.error("알수 없는 에러", e);
            return false;
        }

        Long sectorId = registrationInfo.getSectorId();
        int position = Math.toIntExact(redisRepository.increment("구간-" + sectorId));

        Long userId = registrationInfo.getUserId();
        userStatusProcessor.applyStatus(userId, sectorId, position);

        return true;
    }

}
