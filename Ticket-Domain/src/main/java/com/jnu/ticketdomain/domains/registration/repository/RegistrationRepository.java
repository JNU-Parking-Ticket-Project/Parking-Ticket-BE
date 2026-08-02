package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository
        extends JpaRepository<Registration, Long>, RegistrationRepositoryCustom {
    // 신청, 구간 한꺼번에 조회
    //    @Query(
    //            "SELECT r FROM Registration r  join fetch r.sector s join fetch s.event e WHERE
    // r.user.id = :userId AND r.sector.event.id = :eventId")
    //    Optional<Registration> findByUserIdAndEventId(
    //            @Param("userId") Long userId, @Param("eventId") Long eventId);
    @Query(
            "SELECT r FROM Registration r "
                    + "INNER JOIN r.sector s "
                    + "INNER JOIN s.event e "
                    + "WHERE r.user.id = :userId "
                    + "AND e.id = :eventId")
    Optional<Registration> findByUserIdAndEventId(
            @Param("userId") Long userId, @Param("eventId") Long eventId);

    Optional<Registration> findById(Long id);

    Optional<Registration> findByEmail(String email);

    @Query(
            "select r from Registration r "
                    + "where r.isDeleted = false and r.isSaved = true "
                    + "and r.sector.event.id = :eventId order by r.id asc")
    Page<Registration> findByIsDeletedFalseAndIsSavedTrueByPage(
            @Param("eventId") Long eventId, Pageable pageable);

    @Query(
            "select r from Registration r join fetch r.sector "
                    + "where r.isDeleted = false and r.isSaved = true "
                    + "and r.sector.event.id = :eventId order by r.id asc")
    List<Registration> findSavedForAdmissionRecovery(@Param("eventId") Long eventId);

    @Query("UPDATE Registration r SET r.isDeleted = true WHERE r.sector.id = :sectorId")
    @Modifying(clearAutomatically = true)
    void deleteBySectorId(@Param("sectorId") Long sectorId);

    @Query(
            value =
                    "update registration_tb r join sector s on r.sector_id = s.sector_id set r.is_deleted = 1"
                            + " where s.event_id = :eventId",
            nativeQuery = true)
    @Modifying()
    void deleteByEventId(@Param("eventId") Long eventId);

    @Query(
            "select r from Registration r where r.isSaved = :flag and r.email = :email order by r.id desc")
    List<Registration> findByEmailAndIsSaved(
            @Param("email") String email, @Param("flag") boolean flag);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select r from Registration r "
                    + "where r.isDeleted = false and r.isSaved = false "
                    + "and r.email = :email and r.eventId = :eventId order by r.id desc")
    List<Registration> findTemporaryByEmailAndEventIdForUpdate(
            @Param("email") String email, @Param("eventId") Long eventId);

    @Query("select r from Registration r where r.user.id = :userId")
    List<Registration> findByUserId(@Param("userId") Long userId);

    List<Registration> findByUser(User user);

    Boolean existsByIdAndIsSavedTrue(Long id);

    @Query("select count(r) from Registration r where r.isSaved = true and r.sector.id = :sectorId")
    Long countSavedBySectorId(@Param("sectorId") Long sectorId);

    @Query(
            "select r from Registration r "
                    + "join fetch r.sector "
                    + "join fetch r.user "
                    + "where r.isDeleted = false and r.isSaved = true "
                    + "and r.email = :email and r.eventId = :eventId")
    Optional<Registration> findSavedByEmailAndEventId(
            @Param("email") String email, @Param("eventId") Long eventId);

    @Query(
            "select r from Registration r "
                    + "where r.isDeleted = false and r.isSaved = true "
                    + "and r.sector.id = :sectorId and r.position = :position")
    Optional<Registration> findSavedBySectorIdAndPosition(
            @Param("sectorId") Long sectorId, @Param("position") Integer position);

    @Query(
            "select r.position from Registration r "
                    + "where r.isDeleted = false and r.isSaved = true "
                    + "and r.sector.id = :sectorId and r.position is not null")
    List<Integer> findSavedPositionsBySectorId(@Param("sectorId") Long sectorId);
}
