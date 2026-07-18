package CamNecT.server.domain.verification.document.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDocumentVerificationSubmission is a Querydsl query type for DocumentVerificationSubmission
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDocumentVerificationSubmission extends EntityPathBase<DocumentVerificationSubmission> {

    private static final long serialVersionUID = -506687587L;

    public static final QDocumentVerificationSubmission documentVerificationSubmission = new QDocumentVerificationSubmission("documentVerificationSubmission");

    public final StringPath contentType = createString("contentType");

    public final EnumPath<DocumentType> docType = createEnum("docType", DocumentType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath originalFilename = createString("originalFilename");

    public final StringPath rejectReason = createString("rejectReason");

    public final DateTimePath<java.time.LocalDateTime> reviewedAt = createDateTime("reviewedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> reviewerAdminId = createNumber("reviewerAdminId", Long.class);

    public final NumberPath<Long> size = createNumber("size", Long.class);

    public final EnumPath<VerificationStatus> status = createEnum("status", VerificationStatus.class);

    public final StringPath storageKey = createString("storageKey");

    public final DateTimePath<java.time.LocalDateTime> submittedAt = createDateTime("submittedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> uploadedAt = createDateTime("uploadedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QDocumentVerificationSubmission(String variable) {
        super(DocumentVerificationSubmission.class, forVariable(variable));
    }

    public QDocumentVerificationSubmission(Path<? extends DocumentVerificationSubmission> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDocumentVerificationSubmission(PathMetadata metadata) {
        super(DocumentVerificationSubmission.class, metadata);
    }

}

