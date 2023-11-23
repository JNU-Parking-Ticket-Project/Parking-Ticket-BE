package com.jnu.ticketapi.application.service;


import com.jnu.ticketdomain.domain.user.User;
import com.jnu.ticketdomain.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user =
                userRepository
                        .findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPwd(),
                AuthorityUtils.createAuthorityList(user.getUserRole().getValue()));
    }
}
