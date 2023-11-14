package com.jnu.ticketdomain.domains.user.domian;

import com.slack.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
