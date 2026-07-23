package CamNecT.server.domain.gifticon.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGifticonPurchase is a Querydsl query type for GifticonPurchase
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGifticonPurchase extends EntityPathBase<GifticonPurchase> {

    private static final long serialVersionUID = 522514110L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGifticonPurchase gifticonPurchase = new QGifticonPurchase("gifticonPurchase");

    public final StringPath adminMemo = createString("adminMemo");

    public final DateTimePath<java.time.LocalDateTime> adminProcessedAt = createDateTime("adminProcessedAt", java.time.LocalDateTime.class);

    public final BooleanPath adminSuccess = createBoolean("adminSuccess");

    public final StringPath buyerEmail = createString("buyerEmail");

    public final StringPath buyerName = createString("buyerName");

    public final StringPath buyerPhone = createString("buyerPhone");

    public final StringPath clientRequestId = createString("clientRequestId");

    public final QGifticonExportBatch exportBatch;

    public final DateTimePath<java.time.LocalDateTime> exportedAt = createDateTime("exportedAt", java.time.LocalDateTime.class);

    public final StringPath giftMessage = createString("giftMessage");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QGifticonProduct product;

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final StringPath recipientName = createString("recipientName");

    public final StringPath recipientPhone = createString("recipientPhone");

    public final DateTimePath<java.time.LocalDateTime> requestedAt = createDateTime("requestedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> totalPricePoints = createNumber("totalPricePoints", Integer.class);

    public final NumberPath<Integer> unitPricePoints = createNumber("unitPricePoints", Integer.class);

    public final CamNecT.server.domain.users.model.QUsers user;

    public QGifticonPurchase(String variable) {
        this(GifticonPurchase.class, forVariable(variable), INITS);
    }

    public QGifticonPurchase(Path<? extends GifticonPurchase> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGifticonPurchase(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGifticonPurchase(PathMetadata metadata, PathInits inits) {
        this(GifticonPurchase.class, metadata, inits);
    }

    public QGifticonPurchase(Class<? extends GifticonPurchase> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.exportBatch = inits.isInitialized("exportBatch") ? new QGifticonExportBatch(forProperty("exportBatch")) : null;
        this.product = inits.isInitialized("product") ? new QGifticonProduct(forProperty("product")) : null;
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

