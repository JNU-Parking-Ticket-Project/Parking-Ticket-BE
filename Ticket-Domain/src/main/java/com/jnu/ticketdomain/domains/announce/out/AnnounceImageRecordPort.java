package com.jnu.ticketdomain.domains.announce.out;


import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;

public interface AnnounceImageRecordPort {

    List<AnnounceImage> saveAll(List<AnnounceImage> announceImage);

    List<AnnounceImage> updateAll(List<AnnounceImage> announceImages);
}
