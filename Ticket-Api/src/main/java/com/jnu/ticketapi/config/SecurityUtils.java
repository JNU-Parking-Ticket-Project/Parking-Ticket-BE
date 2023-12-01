package com.jnu.ticketapi.config;


import com.jnu.ticketcommon.exception.SecurityContextNotFoundException;
import com.jnu.ticketdomain.out.UserLoadPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

@RequiredArgsConstructor
public class SecurityUtils {
    private final UserLoadPort userLoadPort;
    private static SimpleGrantedAuthority anonymous = new SimpleGrantedAuthority("ROLE_USER");
    private static SimpleGrantedAuthority swagger = new SimpleGrantedAuthority("ROLE_SWAGGER");

    private static List<SimpleGrantedAuthority> notUserAuthority = List.of(anonymous, swagger);

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw SecurityContextNotFoundException.EXCEPTION;
        }
        // 스웨거 유저일시 익명 유저 취급

        if (authentication.isAuthenticated()
                && !CollectionUtils.containsAny(
                        authentication.getAuthorities(), notUserAuthority)) {
            return Long.valueOf(authentication.getName());
        }
        // 익명유저시 userId 0 반환
        return 0L;
    }
}
