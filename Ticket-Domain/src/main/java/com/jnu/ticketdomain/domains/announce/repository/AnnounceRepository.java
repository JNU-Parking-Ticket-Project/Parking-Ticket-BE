package com.jnu.ticketdomain.domains.announce.repository;

import com.jnu.ticketdomain.domains.announce.domain.Announce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnounceRepository extends JpaRepository<Announce, Long> {

    Page<Announce> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
