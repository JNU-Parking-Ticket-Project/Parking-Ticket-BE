package com.jnu.ticketapi.application.service;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketdomain.domain.user.User;
import com.jnu.ticketdomain.persistence.UserPersistenceAdapter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserUseCase {
    private final UserPersistenceAdapter userPersistenceAdapter;

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmail(String email) {
        return userPersistenceAdapter.findByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        return userPersistenceAdapter.save(user);
    }
}
