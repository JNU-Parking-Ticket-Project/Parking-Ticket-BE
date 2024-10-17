package com.jnu.ticketbatch.config;

import com.jnu.ticketbatch.job.EventRegisterJob;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationResultEmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailDto;
import com.jnu.ticketinfrastructure.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.quartz.*;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@DisallowConcurrentExecution
@Component
@RequiredArgsConstructor
public class RegistrationResultEmailJob implements Job {

    private final RegistrationResultEmailOutboxAdaptor registrationResultEmailOutboxAdaptor;
    private final MailService mailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap jobDataMap = context.getMergedJobDataMap();
            Long eventId = jobDataMap.getLong(EventRegisterJob.EVENT_ID);

            List<RegistrationResultEmailDto> registrationResultEmailDtos = registrationResultEmailOutboxAdaptor.findWaitingRegistrationResultEmailsByEventIdWithThreshold(eventId);
            for (RegistrationResultEmailDto registrationResultEmailDto : registrationResultEmailDtos) {
                boolean emailTransferResult = mailService.sendRegistrationResultMail(
                        registrationResultEmailDto.receiverEmail(),
                        registrationResultEmailDto.receiverName(),
                        registrationResultEmailDto.registrationResult(),
                        registrationResultEmailDto.registrationSequence()
                );

                registrationResultEmailOutboxAdaptor.updateRegistrationResultEmailTransferResult(registrationResultEmailDto.id(),  emailTransferResult);
                log.info("신청 결과 메일 발송. 사용자 이메일 : {}, 결과 : {}", registrationResultEmailDto.receiverEmail(), emailTransferResult);
            }

        } catch (Exception e) {
            log.error("RegistrationResultEmailJob Exception: {}", e.getMessage());
        }
    }
}
