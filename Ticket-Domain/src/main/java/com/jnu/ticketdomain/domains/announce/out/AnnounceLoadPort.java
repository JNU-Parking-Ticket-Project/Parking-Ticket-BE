package com.jnu.ticketdomain.domains.announce.out;

import com.jnu.ticketdomain.domains.announce.domain.Announce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnounceLoadPort {

    Page<Announce> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Announce findAnnounceByLastOne();
}
