package CamNecT.server.global.point.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPointWallet is a Querydsl query type for PointWallet
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPointWallet extends EntityPathBase<PointWallet> {

    private static final long serialVersionUID = 719170575L;

    public static final QPointWallet pointWallet = new QPointWallet("pointWallet");

    public final NumberPath<Integer> balance = createNumber("balance", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QPointWallet(String variable) {
        super(PointWallet.class, forVariable(variable));
    }

    public QPointWallet(Path<? extends PointWallet> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPointWallet(PathMetadata metadata) {
        super(PointWallet.class, metadata);
    }

}

