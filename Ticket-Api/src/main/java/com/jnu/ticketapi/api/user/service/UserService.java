package com.jnu.ticketapi.api.user.service;


import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserUseCase {
    private final UserAdaptor userAdaptor;

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmail(String email) {
        return userAdaptor.findByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        return userAdaptor.save(user);
    }
}
