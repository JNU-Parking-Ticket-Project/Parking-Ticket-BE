package com.jnu.ticketdomain.domains.council.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.council.out.CouncilLoadPort;
import com.jnu.ticketdomain.domains.council.out.CouncilRecordPort;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CouncilAdaptor implements CouncilLoadPort, CouncilRecordPort {
    private final UserRepository userRepository;

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> NotFoundUserException.EXCEPTION);
    }
}
