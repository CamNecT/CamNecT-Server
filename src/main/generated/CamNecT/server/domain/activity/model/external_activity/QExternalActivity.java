package CamNecT.server.domain.activity.model.external_activity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExternalActivity is a Querydsl query type for ExternalActivity
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExternalActivity extends EntityPathBase<ExternalActivity> {

    private static final long serialVersionUID = -652140845L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExternalActivity externalActivity = new QExternalActivity("externalActivity");

    public final NumberPath<Long> activityId = createNumber("activityId", Long.class);

    public final DatePath<java.time.LocalDate> applyEndDate = createDate("applyEndDate", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> applyStartDate = createDate("applyStartDate", java.time.LocalDate.class);

    public final EnumPath<CamNecT.server.domain.activity.model.enums.ActivityCategory> category = createEnum("category", CamNecT.server.domain.activity.model.enums.ActivityCategory.class);

    public final StringPath context = createString("context");

    public final StringPath contextTitle = createString("contextTitle");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath officialUrl = createString("officialUrl");

    public final StringPath organizer = createString("organizer");

    public final StringPath region = createString("region");

    public final DatePath<java.time.LocalDate> resultAnnounceDate = createDate("resultAnnounceDate", java.time.LocalDate.class);

    public final EnumPath<CamNecT.server.domain.activity.model.enums.ActivityStatus> status = createEnum("status", CamNecT.server.domain.activity.model.enums.ActivityStatus.class);

    public final StringPath targetDescription = createString("targetDescription");

    public final StringPath thumbnailKey = createString("thumbnailKey");

    public final StringPath title = createString("title");

    public final CamNecT.server.domain.users.model.QUsers user;

    public QExternalActivity(String variable) {
        this(ExternalActivity.class, forVariable(variable), INITS);
    }

    public QExternalActivity(Path<? extends ExternalActivity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExternalActivity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExternalActivity(PathMetadata metadata, PathInits inits) {
        this(ExternalActivity.class, metadata, inits);
    }

    public QExternalActivity(Class<? extends ExternalActivity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

