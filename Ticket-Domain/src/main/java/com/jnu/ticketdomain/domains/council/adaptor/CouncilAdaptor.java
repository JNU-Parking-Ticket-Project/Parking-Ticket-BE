package com.jnu.ticketdomain.domains.council.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.council.out.CouncilLoadPort;
import com.jnu.ticketdomain.domains.council.out.CouncilRecordPort;
import com.jnu.ticketdomain.domains.council.repository.CouncilRepository;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CouncilAdaptor implements CouncilLoadPort, CouncilRecordPort {
    private final UserRepository userRepository;
    private final CouncilRepository councilRepository;

    @Override
    public Council save(Council council) {
        return councilRepository.save(council);
    }

    @Override
    public List<Council> findAll() {
        return councilRepository.findAll();
    }
}
