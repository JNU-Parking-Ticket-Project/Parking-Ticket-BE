package com.jnu.ticketdomain.domains.registration.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import com.querydsl.core.types.dsl.PathInits;
import javax.annotation.processing.Generated;

/** QRegistration is a Querydsl query type for Registration */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegistration extends EntityPathBase<Registration> {

    private static final long serialVersionUID = 1138249633L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistration registration = new QRegistration("registration");

    public final StringPath affiliation = createString("affiliation");

    public final StringPath carNum = createString("carNum");

    public final DateTimePath<java.time.LocalDateTime> createdAt =
            createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final BooleanPath isLight = createBoolean("isLight");

    public final BooleanPath isSaved = createBoolean("isSaved");

    public final StringPath name = createString("name");

    public final StringPath phoneNum = createString("phoneNum");

    public final com.jnu.ticketdomain.domains.events.domain.QSector sector;

    public final StringPath studentNum = createString("studentNum");

    public final com.jnu.ticketdomain.domains.user.domain.QUser user;

    public QRegistration(String variable) {
        this(Registration.class, forVariable(variable), INITS);
    }

    public QRegistration(Path<? extends Registration> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistration(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistration(PathMetadata metadata, PathInits inits) {
        this(Registration.class, metadata, inits);
    }

    public QRegistration(
            Class<? extends Registration> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.sector =
                inits.isInitialized("sector")
                        ? new com.jnu.ticketdomain.domains.events.domain.QSector(
                                forProperty("sector"), inits.get("sector"))
                        : null;
        this.user =
                inits.isInitialized("user")
                        ? new com.jnu.ticketdomain.domains.user.domain.QUser(
                                forProperty("user"), inits.get("user"))
                        : null;
    }
}
