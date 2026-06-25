package com.jnu.ticketdomain.domains.email.repository;


import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    boolean existsByRegistrationId(Long registrationId);

    @Query(
            "select e from EmailOutbox e "
                    + "where e.sentAt is null "
                    + "and (e.processingAt is null or e.processingAt < :staleBefore) "
                    + "order by e.id asc")
    List<EmailOutbox> findPending(
            @Param("staleBefore") LocalDateTime staleBefore, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e set e.processingAt = :now "
                    + "where e.id = :id "
                    + "and e.sentAt is null "
                    + "and (e.processingAt is null or e.processingAt < :staleBefore)")
    int claim(
            @Param("id") Long id,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailOutbox e set e.sentAt = :sentAt, e.processingAt = null where e.id = :id")
    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e set e.processingAt = null, e.retryCount = e.retryCount + 1 "
                    + "where e.id = :id")
    int releaseAfterFailure(@Param("id") Long id);
}
