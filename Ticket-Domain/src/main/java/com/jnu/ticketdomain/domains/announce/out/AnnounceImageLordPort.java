package com.jnu.ticketdomain.domains.announce.out;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;

public interface AnnounceImageLordPort {

    List<AnnounceImage> findByAnnounce(Announce announce);
}
