package CamNecT.server.global.notification.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotification is a Querydsl query type for Notification
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotification extends EntityPathBase<Notification> {

    private static final long serialVersionUID = 1018706748L;

    public static final QNotification notification = new QNotification("notification");

    public final NumberPath<Long> actorUserId = createNumber("actorUserId", Long.class);

    public final NumberPath<Long> commentId = createNumber("commentId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath link = createString("link");

    public final StringPath message = createString("message");

    public final NumberPath<Long> postId = createNumber("postId", Long.class);

    public final BooleanPath read = createBoolean("read");

    public final NumberPath<Long> receiverUserId = createNumber("receiverUserId", Long.class);

    public final NumberPath<Long> requestId = createNumber("requestId", Long.class);

    public final EnumPath<NotificationType> type = createEnum("type", NotificationType.class);

    public QNotification(String variable) {
        super(Notification.class, forVariable(variable));
    }

    public QNotification(Path<? extends Notification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotification(PathMetadata metadata) {
        super(Notification.class, metadata);
    }

}

