package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionState;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationAdmissionJournalRepository
        extends JpaRepository<RegistrationAdmissionJournal, Long> {
    Optional<RegistrationAdmissionJournal> findByEventIdAndEmail(Long eventId, String email);

    boolean existsByEventIdAndEmail(Long eventId, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from RegistrationAdmissionJournal j where j.id = :journalId")
    Optional<RegistrationAdmissionJournal> findByIdForUpdate(@Param("journalId") Long journalId);

    List<RegistrationAdmissionJournal>
            findAllByEventIdAndStateAndDecidedAtLessThanEqualOrderByIdAsc(
                    Long eventId, RegistrationAdmissionState state, Long decidedAt);

    List<RegistrationAdmissionJournal> findAllByEventIdAndStateAndIdLessThanEqualOrderByIdAsc(
            Long eventId, RegistrationAdmissionState state, Long id);

    @Query(
            "select j from RegistrationAdmissionJournal j "
                    + "where j.state = :state and j.decidedAt <= :decidedAt "
                    + "and j.id > :afterId and j.id <= :throughId order by j.id asc")
    List<RegistrationAdmissionJournal> findBatchForMaterialization(
            @Param("state") RegistrationAdmissionState state,
            @Param("decidedAt") Long decidedAt,
            @Param("afterId") Long afterId,
            @Param("throughId") Long throughId,
            Pageable pageable);

    @Query(
            "select max(j.id) from RegistrationAdmissionJournal j "
                    + "where j.state = :state and j.decidedAt <= :decidedAt")
    Long findMaxIdForMaterialization(
            @Param("state") RegistrationAdmissionState state, @Param("decidedAt") Long decidedAt);

    @Query(
            "select max(j.position) from RegistrationAdmissionJournal j "
                    + "where j.sectorId = :sectorId and j.state in :states")
    Integer findMaxPositionBySectorIdAndStates(
            @Param("sectorId") Long sectorId,
            @Param("states") List<RegistrationAdmissionState> states);

    @Query(
            "select j.position from RegistrationAdmissionJournal j "
                    + "where j.sectorId = :sectorId and j.state in :states and j.position is not null")
    List<Integer> findPositionsBySectorIdAndStates(
            @Param("sectorId") Long sectorId,
            @Param("states") List<RegistrationAdmissionState> states);
}
