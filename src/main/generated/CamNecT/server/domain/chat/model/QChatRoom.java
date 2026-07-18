package CamNecT.server.domain.chat.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRoom is a Querydsl query type for ChatRoom
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoom extends EntityPathBase<ChatRoom> {

    private static final long serialVersionUID = -1856176968L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatRoom chatRoom = new QChatRoom("chatRoom");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastMessageAt = createDateTime("lastMessageAt", java.time.LocalDateTime.class);

    public final CamNecT.server.domain.users.model.QUsers receiver;

    public final BooleanPath receiverExited = createBoolean("receiverExited");

    public final QChatRequest request;

    public final CamNecT.server.domain.users.model.QUsers requester;

    public final BooleanPath requesterExited = createBoolean("requesterExited");

    public final EnumPath<ChatRoom.RoomStatus> status = createEnum("status", ChatRoom.RoomStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QChatRoom(String variable) {
        this(ChatRoom.class, forVariable(variable), INITS);
    }

    public QChatRoom(Path<? extends ChatRoom> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatRoom(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatRoom(PathMetadata metadata, PathInits inits) {
        this(ChatRoom.class, metadata, inits);
    }

    public QChatRoom(Class<? extends ChatRoom> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.receiver = inits.isInitialized("receiver") ? new CamNecT.server.domain.users.model.QUsers(forProperty("receiver")) : null;
        this.request = inits.isInitialized("request") ? new QChatRequest(forProperty("request"), inits.get("request")) : null;
        this.requester = inits.isInitialized("requester") ? new CamNecT.server.domain.users.model.QUsers(forProperty("requester")) : null;
    }

}

