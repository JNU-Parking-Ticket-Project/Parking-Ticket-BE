package com.jnu.ticketdomain.domains.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import com.querydsl.core.types.dsl.PathInits;
import javax.annotation.processing.Generated;

/** QUser is a Querydsl query type for User */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -424544827L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final StringPath email = createString("email");

    public final BooleanPath emailConfirmed = createBoolean("emailConfirmed");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath pwd = createString("pwd");

    public final com.jnu.ticketdomain.domains.registration.domain.QRegistration registration;

    public final NumberPath<Integer> sequence = createNumber("sequence", Integer.class);

    public final StringPath status = createString("status");

    public final EnumPath<UserRole> userRole = createEnum("userRole", UserRole.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registration =
                inits.isInitialized("registration")
                        ? new com.jnu.ticketdomain.domains.registration.domain.QRegistration(
                                forProperty("registration"), inits.get("registration"))
                        : null;
    }
}
