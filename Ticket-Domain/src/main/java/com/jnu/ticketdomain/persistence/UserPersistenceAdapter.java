package com.jnu.ticketdomain.persistence;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domain.user.User;
import com.jnu.ticketdomain.domain.user.UserRepository;
import com.jnu.ticketdomain.out.UserLoadPort;
import com.jnu.ticketdomain.out.UserRecordPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Adaptor
@RequiredArgsConstructor
@Component
public class UserPersistenceAdapter implements UserLoadPort, UserRecordPort {
    private final UserRepository userRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
