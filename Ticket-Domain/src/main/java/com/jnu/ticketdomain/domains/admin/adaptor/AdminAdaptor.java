package com.jnu.ticketdomain.domains.admin.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.admin.out.AdminLoadPort;
import com.jnu.ticketdomain.domains.admin.out.AdminRecordPort;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class AdminAdaptor implements AdminLoadPort, AdminRecordPort {
    private final UserRepository userRepository;
}
