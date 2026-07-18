package CamNecT.server.domain.report.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReport is a Querydsl query type for Report
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReport extends EntityPathBase<Report> {

    private static final long serialVersionUID = -101694467L;

    public static final QReport report = new QReport("report");

    public final StringPath context = createString("context");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final EnumPath<TargetType> postType = createEnum("postType", TargetType.class);

    public final StringPath reportCategory = createString("reportCategory");

    public final NumberPath<Long> reportedPostId = createNumber("reportedPostId", Long.class);

    public final NumberPath<Long> reportedUserId = createNumber("reportedUserId", Long.class);

    public final NumberPath<Long> reporterId = createNumber("reporterId", Long.class);

    public final NumberPath<Long> reportId = createNumber("reportId", Long.class);

    public final EnumPath<ReportStatus> status = createEnum("status", ReportStatus.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QReport(String variable) {
        super(Report.class, forVariable(variable));
    }

    public QReport(Path<? extends Report> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReport(PathMetadata metadata) {
        super(Report.class, metadata);
    }

}

