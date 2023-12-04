package com.jnu.ticketdomain.domains.announce.out;

import com.jnu.ticketdomain.domains.announce.domain.Announce;

public interface AnnounceRecordPort {

    Announce save(Announce announce);
}
