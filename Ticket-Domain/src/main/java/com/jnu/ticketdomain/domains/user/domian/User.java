package com.jnu.ticketdomain.domains.user.domian;

import com.jnu.ticketdomain.domains.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
@NoArgsConstructor
@Entity
@Getter
public class User extends BaseTimeEntity {
    @Column(name = "pwd", nullable = false)
    private String pwd;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @ColumnDefault("'USER'")
    private UserRole userRole;
    @Column(name = "nickname", nullable = false)
    @ColumnDefault("''")
    private String status;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "email_confirmed", nullable = false)
    @ColumnDefault("false")
    private boolean emailConfirmed;
    @Builder
    public User(String pwd, String email) {
        this.pwd = pwd;
        this.email = email;
    }
}
