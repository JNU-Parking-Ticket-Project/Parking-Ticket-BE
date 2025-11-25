package com.jnu.ticketapi.api.flow;

import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserStatusProcessor {

    private final SectorAdaptor sectorAdaptor;
    private final UserAdaptor userAdaptor;

    @Transactional
    public void applyStatus(Long userId, Long sectorId, int position) {
        Sector sector = sectorAdaptor.findById(sectorId);
        User user = userAdaptor.findById(userId);
        Integer sectorCapacity = sector.getInitSectorCapacity();
        Integer issueAmount = sector.getIssueAmount();
        if (position <= sectorCapacity) {
            user.success();
        } else if (position <= issueAmount) {
            user.prepare(position - sectorCapacity);
        } else {
            user.fail();
        }
        userAdaptor.save(user);
    }
}
