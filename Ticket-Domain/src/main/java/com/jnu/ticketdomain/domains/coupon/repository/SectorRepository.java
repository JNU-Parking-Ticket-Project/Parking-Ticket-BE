package com.jnu.ticketdomain.domains.coupon.repository;


import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {}
