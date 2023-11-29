package com.jnu.ticketdomain.domains.registration.domain;


import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "registration_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 신청자 이메일
    @Column(name = "email", nullable = false)
    private String email;
    // 신청자 이름
    @Column(name = "name", nullable = false)
    private String name;
    // 신청자 학번
    @Column(name = "student_num", nullable = false)
    private int studentNum;
    // 신청자 단과대학
    @Column(name = "affiliation")
    private String affiliation;
    // 신청자 차량번호
    @Column(name = "car_num", nullable = false)
    private String carNum;
    // 차량 경차여부
    @Column(name = "is_light", nullable = false)
    private boolean isLight;
    // 신청자 연락처
    @Column(name = "phone_num", nullable = false)
    private String phoneNum;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    // 임시저장 여부
    @Column(name = "is_saved", nullable = false)
    private boolean isSaved;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sectorId", nullable = false)
    private Sector sector;

    @Builder
    public Registration(
            String email,
            String name,
            int studentNum,
            String affiliation,
            String carNum,
            boolean isLight,
            String phoneNum,
            LocalDateTime createdAt,
            boolean isSaved,
            User user,
            Sector sector) {
        this.email = email;
        this.name = name;
        this.studentNum = studentNum;
        this.affiliation = affiliation;
        this.carNum = carNum;
        this.isLight = isLight;
        this.phoneNum = phoneNum;
        this.createdAt = createdAt;
        this.isSaved = isSaved;
        this.user = user;
        this.sector = sector;
    }

    public void updateIsSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }
}
