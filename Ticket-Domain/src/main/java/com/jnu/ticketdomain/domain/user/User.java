package com.jnu.ticketdomain.domain.user;


import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "user_tb")
@DynamicInsert
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pwd", nullable = false)
    private String pwd;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @ColumnDefault("'USER'")
    private UserRole userRole;

    @Column(name = "status", nullable = false)
    @ColumnDefault("''")
    private String status;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_confirmed", nullable = false)
    @ColumnDefault("false")
    private boolean emailConfirmed;

    @Builder
    public User(String pwd, String email, UserRole userRole) {
        this.pwd = pwd;
        this.email = email;
        this.userRole = userRole;
    }
}
