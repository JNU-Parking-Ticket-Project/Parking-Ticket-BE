package com.jnu.ticketdomain.domains.announce.out;


import com.jnu.ticketdomain.domains.announce.domain.Announce;

public interface AnnounceRecordPort {

    Announce save(Announce announce);

    Announce update(Long announceId, String title, String content);

    void delete(Long announceId);
}
