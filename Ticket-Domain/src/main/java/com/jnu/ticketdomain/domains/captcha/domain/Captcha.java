package com.jnu.ticketdomain.domains.captcha.domain;


import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "captcha_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Captcha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer", nullable = false)
    private String answer;

    @Column(name = "image_name", nullable = false)
    private String imageName;

    @Builder
    public Captcha(String answer, String imageName) {
        this.answer = answer;
        this.imageName = imageName;
    }
}
