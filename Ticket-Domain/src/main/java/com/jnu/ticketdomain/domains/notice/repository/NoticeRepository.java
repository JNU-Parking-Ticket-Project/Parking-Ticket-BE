package com.jnu.ticketdomain.domains.notice.repository;


import com.jnu.ticketdomain.domains.notice.domain.Notice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Optional<Notice> findFirstByOrderByNoticeIdDesc();
}
