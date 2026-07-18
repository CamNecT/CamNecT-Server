package CamNecT.server.global.point.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPointTransaction is a Querydsl query type for PointTransaction
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPointTransaction extends EntityPathBase<PointTransaction> {

    private static final long serialVersionUID = 1732188296L;

    public static final QPointTransaction pointTransaction = new QPointTransaction("pointTransaction");

    public final NumberPath<Integer> balanceAfter = createNumber("balanceAfter", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath eventKey = createString("eventKey");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> pointChange = createNumber("pointChange", Integer.class);

    public final NumberPath<Long> postId = createNumber("postId", Long.class);

    public final NumberPath<Long> requestId = createNumber("requestId", Long.class);

    public final EnumPath<PointSource> sourceType = createEnum("sourceType", PointSource.class);

    public final EnumPath<TransactionType> transactionType = createEnum("transactionType", TransactionType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPointTransaction(String variable) {
        super(PointTransaction.class, forVariable(variable));
    }

    public QPointTransaction(Path<? extends PointTransaction> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPointTransaction(PathMetadata metadata) {
        super(PointTransaction.class, metadata);
    }

}

