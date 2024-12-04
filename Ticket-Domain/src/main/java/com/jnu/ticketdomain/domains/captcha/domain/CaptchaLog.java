package com.jnu.ticketdomain.domains.captcha.domain;


import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "captcha_log_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class CaptchaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "captcha_id", nullable = false)
    private Long captchaId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "salt", nullable = false)
    private String salt;

    @Column(name = "is_success", nullable = false)
    @Builder.Default
    private Boolean isSuccess = Boolean.FALSE;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;

    public void markUse() {
        this.isSuccess = Boolean.TRUE;
    }
}
