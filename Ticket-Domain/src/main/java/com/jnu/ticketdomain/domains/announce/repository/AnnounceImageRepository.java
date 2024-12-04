package com.jnu.ticketdomain.domains.announce.repository;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnounceImageRepository extends JpaRepository<AnnounceImage, Long> {

    List<AnnounceImage> findByAnnounce(Announce announce);
}
