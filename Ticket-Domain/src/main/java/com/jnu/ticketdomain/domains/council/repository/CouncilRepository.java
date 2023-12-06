package com.jnu.ticketdomain.domains.council.repository;


import com.jnu.ticketdomain.domains.council.domain.Council;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouncilRepository extends JpaRepository<Council, Long> {}
