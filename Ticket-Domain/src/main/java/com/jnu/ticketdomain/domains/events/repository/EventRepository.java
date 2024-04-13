package com.jnu.ticketdomain.domains.events.repository;


import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, CustomEventRepository {

    Optional<Event> findByEventStatus(EventStatus eventStatus);

    @Query(
            "select e from Event e where e.dateTimePeriod.endAt < :time and e.eventStatus = com.jnu.ticketdomain.domains.events.domain.EventStatus.OPEN")
    List<Event> findByEndAtBeforeAndStatusOpen(@Param("time") LocalDateTime time);

    @Query(
            "SELECT e FROM Event e "
                    + "WHERE e.eventStatus = 'CLOSED' AND e.dateTimePeriod.endAt < :time "
                    + "ORDER BY e.dateTimePeriod.endAt DESC, e.dateTimePeriod.startAt DESC, e.id DESC ")
    List<Event> findClosestClosedEvent(@Param("time") LocalDateTime time, Pageable pageable);
    //    @Query("SELECT e FROM Event e " +
    //        "WHERE e.eventStatus = 'CLOSED' AND e.dateTimePeriod.endAt < :time " +
    //        "ORDER BY e.dateTimePeriod.endAt ASC ")
    //    Optional<Event> findClosestClosedEvent(@Param("time") LocalDateTime time);

    Page<Event> findAllByOrderByIdDesc(Pageable pageable);

    Boolean existsByPublishTrue();
}
