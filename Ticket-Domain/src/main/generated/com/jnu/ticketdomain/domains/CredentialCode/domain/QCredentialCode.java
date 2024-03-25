package com.jnu.ticketdomain.domains.CredentialCode.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import javax.annotation.processing.Generated;

/** QCredentialCode is a Querydsl query type for CredentialCode */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCredentialCode extends EntityPathBase<CredentialCode> {

    private static final long serialVersionUID = -394953961L;

    public static final QCredentialCode credentialCode = new QCredentialCode("credentialCode");

    public final StringPath code = createString("code");

    public final NumberPath<Long> credentialCodeId = createNumber("credentialCodeId", Long.class);

    public final StringPath email = createString("email");

    public QCredentialCode(String variable) {
        super(CredentialCode.class, forVariable(variable));
    }

    public QCredentialCode(Path<? extends CredentialCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCredentialCode(PathMetadata metadata) {
        super(CredentialCode.class, metadata);
    }
}
