package com.jnu.ticketdomain.domains.captcha.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import javax.annotation.processing.Generated;

/** QCaptcha is a Querydsl query type for Captcha */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCaptcha extends EntityPathBase<Captcha> {

    private static final long serialVersionUID = -704274569L;

    public static final QCaptcha captcha = new QCaptcha("captcha");

    public final StringPath answer = createString("answer");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageName = createString("imageName");

    public QCaptcha(String variable) {
        super(Captcha.class, forVariable(variable));
    }

    public QCaptcha(Path<? extends Captcha> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCaptcha(PathMetadata metadata) {
        super(Captcha.class, metadata);
    }
}
