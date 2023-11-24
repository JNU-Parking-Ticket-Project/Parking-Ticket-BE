package com.jnu.ticketdomain.persistence;

import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domain.system.System;
import com.jnu.ticketdomain.domain.system.SystemRepository;
import com.jnu.ticketdomain.out.SystemLoadPort;
import com.jnu.ticketdomain.out.SystemRecordPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Adaptor
@Component
@RequiredArgsConstructor
public class SystemPersistenceAdapter implements SystemRecordPort, SystemLoadPort {

    private final SystemRepository systemRepository;
    @Override
    public System save(System system) {
        return systemRepository.save(system);
    }
}
