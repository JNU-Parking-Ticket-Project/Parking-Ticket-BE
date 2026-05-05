package com.jnu.ticketapi.api.user;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResultAssignment {

    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;

    @Transactional
    public void assign(Long eventId) {
        List<Registration> registrations = registrationAdaptor.findSavedByEventIdOrderBySavedAt(eventId);

        Map<Sector, List<Registration>> registrationsBySector = registrations.stream().collect(Collectors.groupingBy(Registration::getSector));
        registrationsBySector.forEach(this::assignBySector);
    }

    private void assignBySector(Sector sector, List<Registration> registrations) {
        int sectorCapacity = sector.getInitSectorCapacity();
        int issueAmount = sector.getIssueAmount();

        for (int i = 0; i < registrations.size(); i++) {
            int position = i + 1;
            User user = registrations.get(i).getUser();

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
}
