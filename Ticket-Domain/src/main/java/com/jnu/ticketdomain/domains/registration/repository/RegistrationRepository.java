package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
            "select r from Registration r join fetch r.sector join fetch r.user where r.isSaved = true and r.sector.event.id = :eventId")
    List<Registration> findByIsDeletedFalseAndIsSavedTrue(@Param("eventId") Long eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "update registration_tb set saved_at = (UNIX_TIMESTAMP(NOW(6))*1000000) where id = :id",
            nativeQuery = true
    )
    void updateSavedAt(@Param("id") Long registrationId);

    @Query(
            "select r from Registration r where r.isDeleted = false and r.isSaved = true and r.sector.event.id = :eventId")
    Page<Registration> findByIsDeletedFalseAndIsSavedTrueByPage(
            @Param("eventId") Long eventId, Pageable pageable);

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

    @Query("select r from Registration r where r.user.id = :userId")
    List<Registration> findByUserId(@Param("userId") Long userId);

    List<Registration> findByUser(User user);

    @Query(
            value =
                    "SELECT row_num FROM ( "
                            + "  SELECT r.id, ROW_NUMBER() OVER (ORDER BY r.saved_at) AS row_num "
                            + "  FROM registration_tb r "
                            + "  WHERE r.is_saved = true AND r.sector_id = :sectorId "
                            + ") AS numbered_registrations "
                            + "WHERE numbered_registrations.id = :id",
            nativeQuery = true)
    Integer findPositionBySavedAt(@Param("id") Long id, @Param("sectorId") Long sectorId);

    Boolean existsByIdAndIsSavedTrue(Long id);
}
