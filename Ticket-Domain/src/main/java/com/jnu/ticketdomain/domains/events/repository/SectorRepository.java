package com.jnu.ticketdomain.domains.events.repository;


import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findById(Long sectorId);

    @Query("select s from Sector s where s.event.id = :eventId")
    List<Sector> findByEventId(@Param("eventId") Long eventId);

    @Query(
            "select s from Sector s join fetch s.event where s.event.eventStatus = 'OPEN' or s.event.eventStatus = 'READY'")
    List<Sector> findAllByEventStatus();

    @Query("select s from Sector s where s.id = :sectorId and s.event.publish = false")
    Optional<Sector> findByIdWhereEventPublishIdFalse(Long sectorId);

    @Query("UPDATE Sector s SET s.isDeleted = true WHERE s.event.id = ?1")
    @Modifying(clearAutomatically = true)
    void deleteByEventId(Long eventId);

    @Query("update Sector s SET s.isDeleted = true where s.id = :sectorId")
    @Modifying(clearAutomatically = true)
    void delete(Long sectorId);
}
