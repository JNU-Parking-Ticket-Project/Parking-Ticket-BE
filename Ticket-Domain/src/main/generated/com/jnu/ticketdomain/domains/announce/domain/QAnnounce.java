package com.jnu.ticketdomain.domains.announce.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import javax.annotation.processing.Generated;

/** QAnnounce is a Querydsl query type for Announce */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAnnounce extends EntityPathBase<Announce> {

    private static final long serialVersionUID = -1199637375L;

    public static final QAnnounce announce = new QAnnounce("announce");

    public final StringPath announceContent = createString("announceContent");

    public final NumberPath<Long> announceId = createNumber("announceId", Long.class);

    public final StringPath announceTitle = createString("announceTitle");

    public final DateTimePath<java.time.LocalDateTime> createdAt =
            createDateTime("createdAt", java.time.LocalDateTime.class);

    public QAnnounce(String variable) {
        super(Announce.class, forVariable(variable));
    }

    public QAnnounce(Path<? extends Announce> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAnnounce(PathMetadata metadata) {
        super(Announce.class, metadata);
    }
}
