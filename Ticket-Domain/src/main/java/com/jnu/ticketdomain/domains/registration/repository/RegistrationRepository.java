package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository
        extends JpaRepository<Registration, Long>, RegistrationRepositoryCustom {
    // 신청, 구간 한꺼번에 조회
    @Query(
            "SELECT r FROM Registration r  join fetch r.sector s join fetch s.event e WHERE r.user.id = :userId AND r.sector.event.id = :eventId")
    Optional<Registration> findByUserIdAndEventId(
            @Param("userId") Long userId, @Param("eventId") Long eventId);

    Optional<Registration> findById(Long id);

    Optional<Registration> findByEmail(String email);

    @Query(
            "select r from Registration r where r.isDeleted = false and r.isSaved = true and r.sector.event.id = :eventId")
    List<Registration> findByIsDeletedFalseAndIsSavedTrue(@Param("eventId") Long eventId);

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

    @Query("select r from Registration r where r.isSaved = :flag and r.email = :email")
    Optional<Registration> findByEmailAndIsSaved(
            @Param("email") String email, @Param("flag") boolean flag);
}
