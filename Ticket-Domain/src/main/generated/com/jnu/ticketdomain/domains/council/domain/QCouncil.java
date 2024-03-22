package com.jnu.ticketdomain.domains.council.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCouncil is a Querydsl query type for Council
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCouncil extends EntityPathBase<Council> {

    private static final long serialVersionUID = 1420029079L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCouncil council = new QCouncil("council");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath phoneNum = createString("phoneNum");

    public final StringPath studentNum = createString("studentNum");

    public final com.jnu.ticketdomain.domains.user.domain.QUser user;

    public QCouncil(String variable) {
        this(Council.class, forVariable(variable), INITS);
    }

    public QCouncil(Path<? extends Council> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCouncil(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCouncil(PathMetadata metadata, PathInits inits) {
        this(Council.class, metadata, inits);
    }

    public QCouncil(Class<? extends Council> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.jnu.ticketdomain.domains.user.domain.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

