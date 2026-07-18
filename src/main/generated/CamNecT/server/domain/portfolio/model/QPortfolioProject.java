package CamNecT.server.domain.portfolio.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPortfolioProject is a Querydsl query type for PortfolioProject
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPortfolioProject extends EntityPathBase<PortfolioProject> {

    private static final long serialVersionUID = -1385017646L;

    public static final QPortfolioProject portfolioProject = new QPortfolioProject("portfolioProject");

    public final ListPath<PortfolioAsset, QPortfolioAsset> assets = this.<PortfolioAsset, QPortfolioAsset>createList("assets", PortfolioAsset.class, QPortfolioAsset.class, PathInits.DIRECT2);

    public final ListPath<String, StringPath> assignedRole = this.<String, StringPath>createList("assignedRole", String.class, StringPath.class, PathInits.DIRECT2);

    public final DatePath<java.time.LocalDate> createdAt = createDate("createdAt", java.time.LocalDate.class);

    public final StringPath description = createString("description");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final BooleanPath isFavorite = createBoolean("isFavorite");

    public final BooleanPath isPublic = createBoolean("isPublic");

    public final NumberPath<Long> portfolioId = createNumber("portfolioId", Long.class);

    public final StringPath review = createString("review");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final ListPath<String, StringPath> techStack = this.<String, StringPath>createList("techStack", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final StringPath title = createString("title");

    public final DatePath<java.time.LocalDate> updatedAt = createDate("updatedAt", java.time.LocalDate.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPortfolioProject(String variable) {
        super(PortfolioProject.class, forVariable(variable));
    }

    public QPortfolioProject(Path<? extends PortfolioProject> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPortfolioProject(PathMetadata metadata) {
        super(PortfolioProject.class, metadata);
    }

}

