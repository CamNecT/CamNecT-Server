package CamNecT.server.domain.profile.components.certificate.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCertificate is a Querydsl query type for Certificate
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCertificate extends EntityPathBase<Certificate> {

    private static final long serialVersionUID = -879619618L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCertificate certificate = new QCertificate("certificate");

    public final DatePath<java.time.LocalDate> acquiredDate = createDate("acquiredDate", java.time.LocalDate.class);

    public final NumberPath<Long> certificateId = createNumber("certificateId", Long.class);

    public final StringPath certificateName = createString("certificateName");

    public final StringPath credentialUrl = createString("credentialUrl");

    public final StringPath description = createString("description");

    public final DatePath<java.time.LocalDate> expireDate = createDate("expireDate", java.time.LocalDate.class);

    public final StringPath issuerName = createString("issuerName");

    public final CamNecT.server.domain.users.model.QUsers user;

    public QCertificate(String variable) {
        this(Certificate.class, forVariable(variable), INITS);
    }

    public QCertificate(Path<? extends Certificate> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCertificate(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCertificate(PathMetadata metadata, PathInits inits) {
        this(Certificate.class, metadata, inits);
    }

    public QCertificate(Class<? extends Certificate> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user")) : null;
    }

}

