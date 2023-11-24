package com.jnu.ticketdomain.domains.coupon.repository;


import com.jnu.ticketdomain.domains.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {}
