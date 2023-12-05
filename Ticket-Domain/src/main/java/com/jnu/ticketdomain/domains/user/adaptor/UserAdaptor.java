package com.jnu.ticketdomain.domains.user.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
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
    /*
    로그인할 때 사용
    findByEmail로 User를 불러오고 유저가 null일 때 분기처리를 해야함으로 Optional 반환
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> NotFoundUserException.EXCEPTION
                );
    }

    @Override
    public User updateRole(Long userId, String role) {
        User user = findById(userId);
        user.updateRole(UserRole.valueOf(role));
        return user;
    }
}
