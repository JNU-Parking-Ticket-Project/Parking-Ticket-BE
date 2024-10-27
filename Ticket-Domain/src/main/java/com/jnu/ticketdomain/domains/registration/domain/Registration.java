package com.jnu.ticketdomain.domains.registration.domain;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.time.Instant;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "registration_tb")
@Getter
@EntityListeners(AuditingEntityListener.class)
@DynamicUpdate
@AllArgsConstructor(access = AccessLevel.PUBLIC) // 생성자를 public으로 변경
@Where(clause = "is_deleted = false")
@ToString
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
    private String studentNum;
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

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private boolean isDeleted;

    // 최종 저장 시간(nano sec)
    @Column(name = "saved_at")
    private Long savedAt;

    @JsonBackReference(value = "user-registration")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonBackReference(value = "sector-registration")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sectorId", nullable = false)
    private Sector sector;

    @Builder
    private Registration(
            String email,
            String name,
            String studentNum,
            String affiliation,
            String carNum,
            boolean isLight,
            String phoneNum,
            LocalDateTime createdAt,
            boolean isSaved,
            Long savedAt,
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
        this.savedAt = savedAt;
        this.user = user;
        this.sector = sector;
    }

    @JsonCreator
    public Registration(
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("studentNum") String studentNum,
            @JsonProperty("affiliation") String affiliation,
            @JsonProperty("carNum") String carNum,
            @JsonProperty("isLight") boolean isLight,
            @JsonProperty("phoneNum") String phoneNum,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("isSaved") boolean isSaved,
            @JsonProperty("isDeleted") boolean isDeleted,
            @JsonProperty("savedAt") Long savedAt,
            @JsonProperty("user") User user,
            @JsonProperty("sector") Sector sector,
            @JsonProperty("id") Long id) {
        this.email = email;
        this.name = name;
        this.studentNum = studentNum;
        this.affiliation = affiliation;
        this.carNum = carNum;
        this.isLight = isLight;
        this.phoneNum = phoneNum;
        this.createdAt = createdAt;
        this.isSaved = isSaved;
        this.isDeleted = isDeleted;
        this.savedAt = savedAt;
        this.user = user;
        this.sector = sector;
        this.id = id;
    }

    public Registration() {}

    public void finalSave() {
        this.isSaved = true;
        Instant now = Instant.now();
        this.savedAt =
                now.getEpochSecond() * 1_000_000_000L + now.getNano(); // 현재 시간을 나노초 단위 정수로 변환
    }

    public void updateIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void update(Registration registration) {
        this.email = registration.getEmail();
        this.name = registration.getName();
        this.studentNum = registration.getStudentNum();
        this.affiliation = registration.getAffiliation();
        this.carNum = registration.getCarNum();
        this.isLight = registration.isLight();
        this.phoneNum = registration.getPhoneNum();
        this.sector = registration.getSector();
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
