package com.jnu.ticketdomain.domains.registration.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.common.util.UlidGenerator;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmail;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailDto;
import com.jnu.ticketdomain.domains.registration.exception.NotFoundRegistrationResultEmail;
import com.jnu.ticketdomain.domains.registration.out.RegistrationResultEmailLoadPort;
import com.jnu.ticketdomain.domains.registration.out.RegistrationResultEmailRecordPort;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationResultEmailRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class RegistrationResultEmailAdaptor implements RegistrationResultEmailLoadPort, RegistrationResultEmailRecordPort {
    private static final int EMAIL_TRANSFER_THRESHOLD = 14;
    private final RegistrationResultEmailRepository registrationResultEmailRepository;


    public String save(long eventId, String receiverEmail, String receiverName, String registrationResult, int registrationSequence) {
        return registrationResultEmailRepository.save(
                RegistrationResultEmail.builder()
                        .emailId(UlidGenerator.generateUlid())
                        .eventId(eventId)
                        .receiverEmail(receiverEmail)
                        .receiverName(receiverName)
                        .registrationResult(registrationResult)
                        .registrationSequence(registrationSequence)
                        .build()
        ).getEmailId();
    }

    public RegistrationResultEmail getById(String id) {
        return registrationResultEmailRepository
                .findById(id)
                .orElseThrow(() -> NotFoundRegistrationResultEmail.EXCEPTION);
    }

    @Override
    public List<RegistrationResultEmailDto> findOutboxEmailsByEventIdWithThreshold(Long eventId) {
        return registrationResultEmailRepository
                .findOutboxEmailsByEventId(eventId, EMAIL_TRANSFER_THRESHOLD)
                .stream()
                .map(RegistrationResultEmailDto::of)
                .toList();
    }

    @Override
    public void updateEmailTransferResult(String id, boolean emailTransferResult) {
        RegistrationResultEmail email = getById(id);
        email.updateEmailTransferResult(emailTransferResult);
        registrationResultEmailRepository.save(email);
    }
}
