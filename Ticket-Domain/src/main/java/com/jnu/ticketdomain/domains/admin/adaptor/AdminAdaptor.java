package com.jnu.ticketdomain.domains.admin.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.admin.out.AdminLoadPort;
import com.jnu.ticketdomain.domains.admin.out.AdminRecordPort;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class AdminAdaptor implements AdminLoadPort, AdminRecordPort {
    private final UserRepository userRepository;
}
