package com.jnu.ticketdomain.domains.events.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEvent is a Querydsl query type for Event
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEvent extends EntityPathBase<Event> {

    private static final long serialVersionUID = 935002642L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEvent event = new QEvent("event");

    public final com.jnu.ticketdomain.common.vo.QDateTimePeriod dateTimePeriod;

    public final StringPath eventCode = createString("eventCode");

    public final EnumPath<EventStatus> eventStatus = createEnum("eventStatus", EventStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final BooleanPath publish = createBoolean("publish");

    public final ListPath<Sector, QSector> sector = this.<Sector, QSector>createList("sector", Sector.class, QSector.class, PathInits.DIRECT2);

    public final StringPath title = createString("title");

    public QEvent(String variable) {
        this(Event.class, forVariable(variable), INITS);
    }

    public QEvent(Path<? extends Event> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEvent(PathMetadata metadata, PathInits inits) {
        this(Event.class, metadata, inits);
    }

    public QEvent(Class<? extends Event> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.dateTimePeriod = inits.isInitialized("dateTimePeriod") ? new com.jnu.ticketdomain.common.vo.QDateTimePeriod(forProperty("dateTimePeriod")) : null;
    }

}

