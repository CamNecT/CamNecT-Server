package CamNecT.server.global.tag.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTagStats is a Querydsl query type for TagStats
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTagStats extends EntityPathBase<TagStats> {

    private static final long serialVersionUID = 1515708149L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTagStats tagStats = new QTagStats("tagStats");

    public final NumberPath<Integer> docCount = createNumber("docCount", Integer.class);

    public final QTagStats_TagStatsId id;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QTagStats(String variable) {
        this(TagStats.class, forVariable(variable), INITS);
    }

    public QTagStats(Path<? extends TagStats> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTagStats(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTagStats(PathMetadata metadata, PathInits inits) {
        this(TagStats.class, metadata, inits);
    }

    public QTagStats(Class<? extends TagStats> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QTagStats_TagStatsId(forProperty("id")) : null;
    }

}

