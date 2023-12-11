package com.jnu.ticketdomain.domains.events.repository;


import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findById(Long sectorId);
}
