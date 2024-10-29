package com.jnu.ticketdomain.domains.announce.repository;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnounceImageRepository extends JpaRepository<AnnounceImage, Long> {

    String INSERT_DUPLICATE_ON =
            "INSERT INTO ANNOUNCE_IMAGE_TB(URL, ANNOUNCE_ID) VALUES (?,?)"
                    + "ON DUPLICATE KEY UPDATE URL = VALUES(URL)";

    List<AnnounceImage> findByAnnounce(Announce announce);
}
