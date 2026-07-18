package CamNecT.server.domain.activity.model.external_activity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExternalActivityBookmark is a Querydsl query type for ExternalActivityBookmark
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExternalActivityBookmark extends EntityPathBase<ExternalActivityBookmark> {

    private static final long serialVersionUID = -588470999L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExternalActivityBookmark externalActivityBookmark = new QExternalActivityBookmark("externalActivityBookmark");

    public final QExternalActivity activity;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final CamNecT.server.domain.users.model.QUsers user;

    public QExternalActivityBookmark(String variable) {
        this(ExternalActivityBookmark.class, forVariable(variable), INITS);
    }

    public QExternalActivityBookmark(Path<? extends ExternalActivityBookmark> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExternalActivityBookmark(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExternalActivityBookmark(PathMetadata metadata, PathInits inits) {
        this(ExternalActivityBookmark.class, metadata, inits);
    }

    public QExternalActivityBookmark(Class<? extends ExternalActivityBookmark> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.activity = inits.isInitialized("activity") ? new QExternalActivity(forProperty("activity"), inits.get("activity")) : null;
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user")) : null;
    }

}

