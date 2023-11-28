package com.jnu.ticketdomain.domains.user.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.out.UserLoadPort;
import com.jnu.ticketdomain.domains.user.out.UserRecordPort;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Adaptor
@RequiredArgsConstructor
@Component
public class UserAdaptor implements UserLoadPort, UserRecordPort {
    private final UserRepository userRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
