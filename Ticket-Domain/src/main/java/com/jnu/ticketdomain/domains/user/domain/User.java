package com.jnu.ticketdomain.domains.user.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "user_tb")
@DynamicInsert
@DynamicUpdate
@JsonIgnoreProperties("registrations")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "pwd", nullable = false)
    private String pwd;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @ColumnDefault("'USER'")
    private UserRole userRole;

    @Column(name = "status", nullable = false)
    @ColumnDefault("'불합격'")
    @Convert(converter = UserStatusConverter.class)
    private UserStatus status;

    @Column(name = "sequence", nullable = false)
    /*
    User 엔티티의 인스턴스가 생성되고 sequence 필드에 별도의 값이 할당되지 않으면,
    자바의 기본값 규칙에 따라 0으로 초기화 되기 때문에 @ColumnDefault('-2')를 하면 안된다.
    합격일 경우 -2, 불합격일 경우 -1, 예비일경우는 그냥 숫자
    */
    private Integer sequence = -1;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_confirmed", nullable = false)
    @ColumnDefault("false")
    private boolean emailConfirmed;

    @JsonManagedReference(value = "user-registration")
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();

    public void updateRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public void updatePassword(String password) {
        this.pwd = password;
    }

    public void success() {
        this.status = UserStatus.SUCCESS;
        this.sequence = -2;
    }

    public void fail() {
        this.status = UserStatus.FAIL;
        this.sequence = -1;
    }

    public void prepare(Integer sequence) {
        this.status = UserStatus.PREPARE;
        this.sequence = sequence;
    }

    @Builder
    public User(String pwd, String email, UserRole userRole) {
        this.pwd = pwd;
        this.email = email;
        this.userRole = userRole;
    }
}
