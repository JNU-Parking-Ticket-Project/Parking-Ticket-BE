package com.jnu.ticketdomain.domains.events.repository;


import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventStatus(EventStatus eventStatus);

    @Query(
            "select e from Event e where e.dateTimePeriod.endAt < :time and e.eventStatus = com.jnu.ticketdomain.domains.events.domain.EventStatus.OPEN")
    List<Event> findByEndAtBeforeAndStatusOpen(LocalDateTime time);
}
