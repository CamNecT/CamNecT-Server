package CamNecT.server.global.storage.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUploadTicket is a Querydsl query type for UploadTicket
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUploadTicket extends EntityPathBase<UploadTicket> {

    private static final long serialVersionUID = -591362500L;

    public static final QUploadTicket uploadTicket = new QUploadTicket("uploadTicket");

    public final StringPath contentType = createString("contentType");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath originalFilename = createString("originalFilename");

    public final EnumPath<UploadPurpose> purpose = createEnum("purpose", UploadPurpose.class);

    public final NumberPath<Long> size = createNumber("size", Long.class);

    public final EnumPath<UploadTicket.Status> status = createEnum("status", UploadTicket.Status.class);

    public final StringPath storageKey = createString("storageKey");

    public final DateTimePath<java.time.LocalDateTime> usedAt = createDateTime("usedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> usedRefId = createNumber("usedRefId", Long.class);

    public final EnumPath<UploadRefType> usedRefType = createEnum("usedRefType", UploadRefType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUploadTicket(String variable) {
        super(UploadTicket.class, forVariable(variable));
    }

    public QUploadTicket(Path<? extends UploadTicket> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUploadTicket(PathMetadata metadata) {
        super(UploadTicket.class, metadata);
    }

}

