package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    // 신청, 구간 한꺼번에 조회
    @Query("SELECT r FROM Registration r  join fetch r.sector WHERE r.user.id = :userId")
    Optional<Registration> findByUserId(@Param("userId") Long userId);
}
