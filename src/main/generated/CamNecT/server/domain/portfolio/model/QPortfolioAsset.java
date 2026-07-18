package CamNecT.server.domain.portfolio.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPortfolioAsset is a Querydsl query type for PortfolioAsset
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPortfolioAsset extends EntityPathBase<PortfolioAsset> {

    private static final long serialVersionUID = -211908375L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPortfolioAsset portfolioAsset = new QPortfolioAsset("portfolioAsset");

    public final NumberPath<Long> assetId = createNumber("assetId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath fileKey = createString("fileKey");

    public final QPortfolioProject portfolioProject;

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final StringPath type = createString("type");

    public QPortfolioAsset(String variable) {
        this(PortfolioAsset.class, forVariable(variable), INITS);
    }

    public QPortfolioAsset(Path<? extends PortfolioAsset> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPortfolioAsset(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPortfolioAsset(PathMetadata metadata, PathInits inits) {
        this(PortfolioAsset.class, metadata, inits);
    }

    public QPortfolioAsset(Class<? extends PortfolioAsset> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.portfolioProject = inits.isInitialized("portfolioProject") ? new QPortfolioProject(forProperty("portfolioProject")) : null;
    }

}

