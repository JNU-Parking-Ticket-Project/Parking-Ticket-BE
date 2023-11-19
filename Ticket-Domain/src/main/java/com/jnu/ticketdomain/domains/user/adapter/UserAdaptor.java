package com.jnu.ticketdomain.domains.user.adapter;


import com.jnu.ticketdomain.domains.out.UserLoadPort;
import com.jnu.ticketdomain.domains.out.UserRecordPort;
import com.jnu.ticketdomain.domains.user.domian.User;
import com.jnu.ticketdomain.domains.user.domian.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
