package com.jnu.ticketbatch.config;


import com.jnu.ticketbatch.job.EventRegisterJob;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationResultEmailAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailDto;
import com.jnu.ticketinfrastructure.service.MailService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

@Slf4j
@DisallowConcurrentExecution
@Component
@RequiredArgsConstructor
public class RegistrationResultEmailJob implements Job {

    private final RegistrationResultEmailAdaptor registrationResultEmailAdaptor;
    private final MailService mailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap jobDataMap = context.getMergedJobDataMap();
            Long eventId = jobDataMap.getLong(EventRegisterJob.EVENT_ID);

            List<RegistrationResultEmailDto> registrationResultEmailDtos =
                    registrationResultEmailAdaptor.findOutboxEmailsByEventIdWithThreshold(eventId);

            for (RegistrationResultEmailDto registrationResultEmailDto : registrationResultEmailDtos) {
                boolean emailTransferResult = mailService.sendRegistrationResultMail(
                                                registrationResultEmailDto.receiverEmail(),
                                                registrationResultEmailDto.receiverName(),
                                                registrationResultEmailDto.registrationResult(),
                                                registrationResultEmailDto.registrationSequence());

                registrationResultEmailAdaptor.updateEmailTransferResult(
                        registrationResultEmailDto.id(), emailTransferResult);
                log.info(
                        "신청 결과 메일 발송. 사용자 이메일 : {}, 결과 : {}",
                        registrationResultEmailDto.receiverEmail(),
                        emailTransferResult);
            }

        } catch (Exception e) {
            log.error("RegistrationResultEmailJob Exception: {}", e.getMessage());
        }
    }
}
