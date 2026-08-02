package com.jnu.ticketdomain.domains.email.repository;


import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
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
                    + "and e.failedAt is null "
                    + "and e.retryCount < :maxRetryCount "
                    + "and (e.processingAt is null or e.processingAt < :staleBefore) "
                    + "order by e.id asc")
    List<EmailOutbox> findPending(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e set e.processingAt = :now "
                    + "where e.id = :id "
                    + "and e.sentAt is null "
                    + "and e.failedAt is null "
                    + "and e.retryCount < :maxRetryCount "
                    + "and (e.processingAt is null or e.processingAt < :staleBefore)")
    int claim(
            @Param("id") Long id,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxRetryCount") int maxRetryCount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e "
                    + "set e.sentAt = :sentAt, e.processingAt = null, e.lastError = null "
                    + "where e.id = :id and e.sentAt is null and e.failedAt is null")
    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e "
                    + "set e.failedAt = :failedAt, e.processingAt = null, "
                    + "e.lastError = :lastError, e.retryCount = e.retryCount + 1 "
                    + "where e.id = :id and e.sentAt is null and e.failedAt is null "
                    + "and e.retryCount >= :terminalRetryCount "
                    + "and e.retryCount < :maxRetryCount")
    int markTerminalFailure(
            @Param("id") Long id,
            @Param("failedAt") LocalDateTime failedAt,
            @Param("lastError") String lastError,
            @Param("terminalRetryCount") int terminalRetryCount,
            @Param("maxRetryCount") int maxRetryCount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e "
                    + "set e.processingAt = :failedAt, e.lastError = :lastError, "
                    + "e.retryCount = e.retryCount + 1 "
                    + "where e.id = :id and e.sentAt is null and e.failedAt is null "
                    + "and e.retryCount < :terminalRetryCount")
    int markRetryFailure(
            @Param("id") Long id,
            @Param("failedAt") LocalDateTime failedAt,
            @Param("lastError") String lastError,
            @Param("terminalRetryCount") int terminalRetryCount);

    @Query(
            "select e from EmailOutbox e "
                    + "where e.eventId = :eventId and e.sentAt is null and e.failedAt is not null "
                    + "order by e.failedAt desc, e.id desc")
    Page<EmailOutbox> findFailedByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update EmailOutbox e "
                    + "set e.failedAt = null, e.lastError = null, e.retryCount = 0, "
                    + "e.processingAt = null "
                    + "where e.id = :id and e.eventId = :eventId "
                    + "and e.sentAt is null and e.failedAt is not null")
    int requeueFailed(@Param("eventId") Long eventId, @Param("id") Long id);
}
