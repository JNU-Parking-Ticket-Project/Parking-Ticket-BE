package com.jnu.ticketdomain.domains.announce.adaptor;

import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.out.AnnounceLoadPort;
import com.jnu.ticketdomain.domains.announce.out.AnnounceRecordPort;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Adaptor
@RequiredArgsConstructor
public class AnnounceAdaptor implements AnnounceRecordPort, AnnounceLoadPort {

    private final AnnounceRepository announceRepository;

    @Override
    public Announce save(Announce announce) {
        return announceRepository.save(announce);
    }

    @Override
    public Page<Announce> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return announceRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
