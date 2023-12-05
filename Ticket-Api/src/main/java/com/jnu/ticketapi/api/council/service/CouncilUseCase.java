package com.jnu.ticketapi.api.council.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CouncilUseCase {
    private final CouncilAdaptor councilAdaptor;

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return councilAdaptor.findByEmail(email);
    }
}
