package com.jnu.ticketdomain.domains.captcha.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;
import com.jnu.ticketdomain.domains.captcha.exception.NotFoundCaptchaPendingException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaPendingLoadPort;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaPendingRecordPort;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaPendingRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class CaptchaPendingAdaptor implements CaptchaPendingLoadPort, CaptchaPendingRecordPort {
    private final CaptchaPendingRepository captchaPendingRepository;

    @Override
    public CaptchaPending save(CaptchaPending captchaPending) {
        return captchaPendingRepository.save(captchaPending);
    }

    @Override
    public CaptchaPending findById(Long id) {
        return captchaPendingRepository
                .findById(id)
                .orElseThrow(() -> NotFoundCaptchaPendingException.EXCEPTION);
    }
}
