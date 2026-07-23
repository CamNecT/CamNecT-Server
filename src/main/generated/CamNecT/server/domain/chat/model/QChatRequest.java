package CamNecT.server.domain.chat.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRequest is a Querydsl query type for ChatRequest
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRequest extends EntityPathBase<ChatRequest> {

    private static final long serialVersionUID = 51776914L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatRequest chatRequest = new QChatRequest("chatRequest");

    public final NumberPath<Long> activityId = createNumber("activityId", Long.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final CamNecT.server.domain.users.model.QUsers receiver;

    public final NumberPath<Long> recruitmentId = createNumber("recruitmentId", Long.class);

    public final CamNecT.server.domain.users.model.QUsers requester;

    public final ListPath<CamNecT.server.global.tag.model.Tag, CamNecT.server.global.tag.model.QTag> requestInterests = this.<CamNecT.server.global.tag.model.Tag, CamNecT.server.global.tag.model.QTag>createList("requestInterests", CamNecT.server.global.tag.model.Tag.class, CamNecT.server.global.tag.model.QTag.class, PathInits.DIRECT2);

    public final EnumPath<ChatRequest.RequestStatus> status = createEnum("status", ChatRequest.RequestStatus.class);

    public final EnumPath<ChatRequest.RequestType> type = createEnum("type", ChatRequest.RequestType.class);

    public QChatRequest(String variable) {
        this(ChatRequest.class, forVariable(variable), INITS);
    }

    public QChatRequest(Path<? extends ChatRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatRequest(PathMetadata metadata, PathInits inits) {
        this(ChatRequest.class, metadata, inits);
    }

    public QChatRequest(Class<? extends ChatRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.receiver = inits.isInitialized("receiver") ? new CamNecT.server.domain.users.model.QUsers(forProperty("receiver"), inits.get("receiver")) : null;
        this.requester = inits.isInitialized("requester") ? new CamNecT.server.domain.users.model.QUsers(forProperty("requester"), inits.get("requester")) : null;
    }

}

