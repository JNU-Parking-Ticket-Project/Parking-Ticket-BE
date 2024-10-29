package com.jnu.ticketdomain.domains.announce.out;


import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;

public interface AnnounceImageRecordPort {
    String DELETE_NOT_FOUND_IMAGE = "DELETE FROM ANNOUNCE_IMAGE_TB i WHERE i.URL NOT IN :url";

    List<AnnounceImage> saveAll(List<AnnounceImage> announceImage);

    List<AnnounceImage> updateAll(List<AnnounceImage> announceImages);
}
