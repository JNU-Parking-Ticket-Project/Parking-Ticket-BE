package com.jnu.ticketdomain.domains.events.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import com.querydsl.core.types.dsl.PathInits;
import javax.annotation.processing.Generated;

/** QSector is a Querydsl query type for Sector */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSector extends EntityPathBase<Sector> {

    private static final long serialVersionUID = -694634770L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSector sector = new QSector("sector");

    public final QEvent event;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> initReserve = createNumber("initReserve", Integer.class);

    public final NumberPath<Integer> initSectorCapacity =
            createNumber("initSectorCapacity", Integer.class);

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final NumberPath<Integer> issueAmount = createNumber("issueAmount", Integer.class);

    public final StringPath name = createString("name");

    public final ListPath<
                    com.jnu.ticketdomain.domains.registration.domain.Registration,
                    com.jnu.ticketdomain.domains.registration.domain.QRegistration>
            registrations =
                    this
                            .<com.jnu.ticketdomain.domains.registration.domain.Registration,
                                    com.jnu.ticketdomain.domains.registration.domain.QRegistration>
                                    createList(
                                            "registrations",
                                            com.jnu.ticketdomain.domains.registration.domain
                                                    .Registration.class,
                                            com.jnu.ticketdomain.domains.registration.domain
                                                    .QRegistration.class,
                                            PathInits.DIRECT2);

    public final NumberPath<Integer> remainingAmount =
            createNumber("remainingAmount", Integer.class);

    public final NumberPath<Integer> reserve = createNumber("reserve", Integer.class);

    public final NumberPath<Integer> sectorCapacity = createNumber("sectorCapacity", Integer.class);

    public final StringPath sectorNumber = createString("sectorNumber");

    public QSector(String variable) {
        this(Sector.class, forVariable(variable), INITS);
    }

    public QSector(Path<? extends Sector> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSector(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSector(PathMetadata metadata, PathInits inits) {
        this(Sector.class, metadata, inits);
    }

    public QSector(Class<? extends Sector> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event =
                inits.isInitialized("event")
                        ? new QEvent(forProperty("event"), inits.get("event"))
                        : null;
    }
}
