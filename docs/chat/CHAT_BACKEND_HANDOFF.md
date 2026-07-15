# Chat 도메인 전체 변경 백엔드 전달 문서

## 1. 문서 목적과 전체 범위

이 문서는 이번 작업 기간에 Chat 도메인과 WebSocket 경로에서 변경한 내용을 다른 백엔드 팀원이 코드만 역추적하지 않고도 인수할 수 있도록 정리한 전체 전달 문서다. 안정화 1~6단계뿐 아니라 같은 기간 `ChatService`와 Chat REST API에 반영된 Home 요청함 연계, Swagger 오류 명세, 입력 검증도 포함한다.

| 구분 | 핵심 변경 | 목적 |
|---|---|---|
| 1단계 | presence 세션·구독 단위 추적 | 다중 탭/기기에서 한 연결 종료가 다른 연결의 접속 상태를 지우는 문제 방지 |
| 2단계 | read/unread 계산 일관화 | READ 기준 메시지와 방 목록·전체 배지의 unread 수 일치 |
| 3단계 | 실시간 전송 AFTER_COMMIT 분리 | DB 롤백 전에 유령 메시지·READ 이벤트가 전송되는 문제 방지 |
| 4단계 | 구조화된 STOMP 오류와 구독 권한 검증 | 프론트가 실패 원인과 연관 요청을 안정적으로 식별 |
| 5단계 | `clientMessageId` 기반 메시지 멱등성·ACK | 이중 클릭, 재연결, ACK 유실 재시도의 중복 저장·푸시 방지 |
| 6단계 | 단위·동시성·트랜잭션·실제 STOMP 통합 테스트 | 과거 오류가 많았던 WebSocket 경로의 회귀 방지 |
| REST 계약 | ChatRequest/ChatRoom Swagger·validation | REST 오류 상황 문서화와 잘못된 입력의 조기 차단 |
| Home 연계 | 커피챗·팀원모집 대기 요청 미리보기 | Home이 ChatRequest 데이터를 타입별로 안전하게 재사용 |

### 관련 커밋

| commit | 내용 |
|---|---|
| `9f8eef6` | Home에 커피챗·팀원모집 대기 요청 미리보기 연계 |
| `505c1e9` | Home inbox·ChatService·STOMP 접근 테스트 보강 |
| `771a596` | 1단계: session-aware WebSocket presence |
| `cb2f607` | 2단계: read/unread 일관성 |
| `8b88b99` | 3단계: AFTER_COMMIT 실시간 전송 |
| `46a179a` | 4단계: 구조화된 STOMP 오류 |
| `730b686` | 5단계: 메시지 전송 멱등성 |
| `34b3a34` | 6단계: WebSocket 안정성 테스트와 최초 전달 문서 |
| `84436a6` | Chat REST Swagger 오류 명세와 입력 validation |

REST 성공 응답의 `ApiResponse` 및 기존 `ErrorResponse(status, code, message)` 형태는 변경하지 않았다. STOMP에는 별도의 ACK/error 메시지 계약이 추가되었다.

## 2. 1단계 — 세션·구독 기반 presence

### 기존 문제

기존 presence는 `roomId -> userId 집합`만 저장했다. 한 사용자가 같은 방을 여러 탭이나 기기에서 구독해도 사용자 한 명으로만 기록되었고, 어느 한 연결이 끊기면 `leaveAll(userId)`가 다른 정상 연결의 presence까지 지웠다.

그 결과 다음 문제가 가능했다.

- 노트북 탭 하나를 닫았을 뿐인데 모바일/다른 탭까지 방에서 나간 것으로 판단
- 실제 수신자가 방을 보고 있는데도 부재중으로 판단하여 불필요한 채팅 푸시 발송
- `UNSUBSCRIBE`와 명시적 방 퇴장이 구분되지 않아 stale presence가 남거나 과하게 제거됨

### 변경 구조

`ChatPresenceServiceImpl`은 현재 다음 두 방향의 인메모리 인덱스를 함께 관리한다.

```text
(roomId, userId) -> Set<(sessionId, subscriptionId)>
sessionId -> Map<subscriptionId, (roomId, userId)>
```

- `enter(roomId, userId, sessionId, subscriptionId)`: 실제 방 SUBSCRIBE 단위 등록
- `leaveSubscription(sessionId, subscriptionId)`: 해당 구독만 제거
- `leaveRoom(roomId, userId, sessionId)`: 명시적 LEAVE를 보낸 세션의 해당 방 구독만 제거
- `leaveSession(sessionId)`: DISCONNECT된 세션이 보유한 모든 구독 제거
- `isPresent(roomId, userId)`: 하나 이상의 살아 있는 구독이 있을 때만 `true`

두 인덱스의 원자성을 유지하기 위해 public 연산을 `synchronized`로 보호한다. 동일 SUBSCRIBE/정리 이벤트가 중복되어도 결과가 깨지지 않도록 멱등적으로 처리한다.

### WebSocket 이벤트 연결

- `SessionSubscribeEvent`: `/sub/chat/room/{roomId}`일 때 session/subscription을 등록하고 읽음 처리를 호출
- `SessionUnsubscribeEvent`: 해당 subscription만 제거
- `SessionDisconnectEvent`: 해당 session만 제거
- `/pub/chat/room/{roomId}/leave`: 현재 session의 해당 방 presence만 제거

따라서 같은 사용자의 다른 탭이나 기기가 방을 계속 보고 있다면 한 세션이 종료돼도 presence는 유지된다.

### 제약

presence 저장소는 현재 JVM 메모리다. 서버 인스턴스가 여러 대이고 WebSocket 연결이 서로 다른 인스턴스로 분산되면 전역 presence는 정확하지 않다. 다중 인스턴스 운영으로 전환할 때는 sticky session만으로 끝낼지, Redis 같은 공유 저장소로 presence를 옮길지 별도 결정이 필요하다.

## 3. 2단계 — read/unread 상태 일관화

### 기존 문제

- 읽지 않은 메시지 조회 순서가 명시되지 않아 `getLast()`가 실제 최신 메시지라는 보장이 없었다.
- 전체 unread 수가 사용자가 이미 나간 방의 메시지까지 포함할 수 있었다.
- READ 이벤트의 `lastReadMessageId`, 방별 unread, 전체 배지가 서로 다른 기준으로 계산될 여지가 있었다.

### 변경 내용

- `findUnreadMessages(roomId, receiverId)`를 `message id ASC`로 정렬한다.
- 읽음 처리 후 가장 마지막 원소의 ID를 `lastReadMessageId`로 사용하므로 실제 가장 큰 메시지 ID와 일치한다.
- `countVisibleUnreadByUserId(userId)`를 추가해 사용자가 나가지 않은 방만 전체 unread에 포함한다.
- presence 때문에 수신자가 이미 방에 있으면 새 메시지를 저장하기 전에 읽음 상태로 표시한다.
- 방을 나간 사용자의 unread는 방 목록과 전체 배지에서 제외한다.
- `markAllAsRead`는 호출 사용자가 현재 해당 방에 접근 가능한지 확인한 후 처리한다.

### 결과

다음 세 화면/이벤트가 같은 기준을 사용한다.

- 방 안의 READ 이벤트
- `/sub/user/{userId}/rooms` 방 목록 갱신
- 채팅 목록 전체 unread 배지

## 4. 3단계 — 실시간 이벤트를 커밋 이후 전송

### 기존 문제

기존 `ChatService`는 DB 트랜잭션 안에서 `SimpMessagingTemplate.convertAndSend`를 직접 호출했다. 소켓 전송 후 DB 커밋이 실패하면 프론트에는 메시지 또는 READ 이벤트가 보이지만 REST 이력에는 존재하지 않는 유령 상태가 생길 수 있었다.

### 변경 구조

서비스는 트랜잭션 안에서 다음 도메인 이벤트만 발행한다.

- `ChatMessageCommittedEvent`
- `ChatReadCommittedEvent`

`ChatRealtimeEventListener`가 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 이벤트를 받아 `ChatRealtimeDeliveryService`에 전달한다.

`ChatRealtimeDeliveryService`는 `REQUIRES_NEW`, read-only 트랜잭션에서 커밋된 DB 상태를 다시 조회하고 다음 목적지로 전송한다.

- 메시지/READ: `/sub/chat/room/{roomId}`
- 수신자 방 목록: `/sub/user/{receiverId}/rooms`
- 발신자 방 목록: `/sub/user/{senderId}/rooms`
- READ한 사용자 방 목록: `/sub/user/{readerId}/rooms`

각 브로커 전송과 방 목록 payload 조회는 실패를 격리한다. WebSocket 브로커 장애가 이미 완료된 메시지 저장 트랜잭션을 실패로 되돌리지 않는다.

### 보장과 한계

- 롤백된 트랜잭션에서는 메시지/READ 실시간 이벤트가 전송되지 않는다.
- 커밋 후의 unread 조회 결과를 사용한다.
- 커밋 직후 프로세스가 종료되는 상황을 재처리하는 outbox는 아니다.
- 브로커 전송 실패 시 REST 채팅 이력 재조회로 최종 상태를 복구해야 한다.

## 5. 현재 메시지 처리 전체 흐름

```text
CONNECT
  -> ACCESS JWT / 사용자 존재 / 정지 상태 검증
  -> session userId와 StompPrincipal 등록

SUBSCRIBE /sub/chat/room/{roomId}
  -> 실제 참여·미퇴장 사용자 검증
  -> session/subscription presence 등록
  -> unread DB 읽음 처리
  -> 커밋 후 READ 및 방 목록 이벤트 전송

SEND /pub/chat/message
  -> payload 검증
  -> 채팅방 행 잠금
  -> 방 상태·참여자 검증
  -> clientMessageId 중복 확인
  -> receiver presence 확인
  -> 메시지 1회 저장
  -> 부재중이면 채팅 푸시 이벤트 발행
  -> 커밋 후 방 메시지·방 목록 이벤트 전송
  -> 요청 세션에 ACK 반환

UNSUBSCRIBE / LEAVE / DISCONNECT
  -> 해당 subscription / 해당 session의 방 / 해당 session 전체 순으로 정리
```

수신자가 방에 없을 때만 `NewChatMessageEvent`가 발행된다. 채팅 메시지는 알림함 DB에는 저장하지 않고, 커밋 후 WebSocket 알림과 FCM push 전용으로 처리한다. FCM data에는 `roomId`가 포함되며 링크도 채팅방 기준으로 생성된다.

## 6. STOMP 목적지 계약

| 방향 | 목적지 | 용도 | 전달 범위 |
|---|---|---|---|
| Client → Server | `/pub/chat/message` | 메시지 전송 | 해당 세션의 요청 |
| Client → Server | `/pub/chat/room/{roomId}/leave` | 명시적 방 퇴장 | 해당 세션의 요청 |
| Server → Client | `/user/queue/chat-acks` | 메시지 저장 성공/중복 처리 ACK | 요청을 보낸 세션만 |
| Server → Client | `/user/queue/chat-errors` | SEND/LEAVE/구독 후처리 오류 | 오류가 발생한 세션만 |
| Server → Client | STOMP `ERROR` frame | CONNECT/SUBSCRIBE 인터셉터 오류 | 해당 연결, 일반적으로 프레임 이후 연결 종료 |
| Server → Client | `/sub/chat/room/{roomId}` | 기존 메시지·READ 실시간 이벤트 | 해당 방 구독자 |
| Server → Client | `/sub/user/{userId}/rooms` | 기존 방 목록 갱신 이벤트 | 해당 사용자 |

`/user/queue/chat-acks`와 `/user/queue/chat-errors`는 Spring user destination이다. 서버 내부 발송 주소는 `/queue/...`이지만 클라이언트 구독 주소에는 반드시 `/user`가 붙는다.

### 구독 허용 목록

`ChatStompInterceptor`가 다음 목적지만 허용한다.

- `/user/queue/chat-acks`
- `/user/queue/chat-errors`
- 참여 중인 방의 `/sub/chat/room/{roomId}`
- 자기 ID와 일치하는 `/sub/user/{userId}/rooms`

그 외 목적지, 타인의 방 목록, 참여하지 않은 채팅방 구독은 `48302 CHATROOM_ACCESS_DENIED`로 거부된다.

## 7. 메시지 요청·응답 계약

### 전송 요청

```json
{
  "roomId": 12,
  "content": "안녕하세요.",
  "clientMessageId": "0e9e31aa-99e7-4c58-90d8-f939b56fd234"
}
```

- `roomId`: 필수, 양수
- `content`: 필수, 공백 문자열 불가, 최대 16,000자
- `clientMessageId`: UUID 정규 형식 권장
  - 현재는 구버전 프론트와의 점진 전환을 위해 누락을 허용한다.
  - 누락하면 서버가 UUID를 생성하므로 저장은 가능하지만, 클라이언트 재시도 멱등성은 보장할 수 없다.
  - 신규 프론트는 항상 생성해서 보내야 한다.

### 성공 ACK

구독: `/user/queue/chat-acks`

```json
{
  "type": "ACK",
  "messageId": 321,
  "roomId": 12,
  "clientMessageId": "0e9e31aa-99e7-4c58-90d8-f939b56fd234",
  "duplicate": false
}
```

- `duplicate=false`: 이번 요청에서 새 DB 행이 저장되었다.
- `duplicate=true`: 같은 논리 메시지가 이미 저장되어 기존 `messageId`를 반환했다.
- 두 경우 모두 프론트에서는 전송 성공으로 처리한다.
- ACK는 `broadcast=false`이므로 같은 계정의 다른 탭이 아니라 요청을 보낸 WebSocket 세션에만 간다.

### 방 메시지 이벤트의 추가 필드

기존 `ChatMessageResponseDto`에 다음 필드만 추가했다.

```json
{
  "messageId": 321,
  "roomId": 12,
  "clientMessageId": "0e9e31aa-99e7-4c58-90d8-f939b56fd234",
  "senderId": 1,
  "message": "안녕하세요."
}
```

기존 필드는 제거하거나 이름을 바꾸지 않았다.

## 8. 4단계 — 구조화된 오류 계약

```json
{
  "type": "ERROR",
  "status": 403,
  "code": 48302,
  "message": "해당 채팅방에 접근할 권한이 없습니다.",
  "operation": "SUBSCRIBE",
  "roomId": 12,
  "clientMessageId": null
}
```

필드 의미:

- `status`: HTTP 의미를 재사용한 상태값
- `code`: 기존 `ErrorCode`/도메인 ErrorCode 숫자
- `operation`: `CONNECT`, `SUBSCRIBE`, `SEND_MESSAGE`, `LEAVE_ROOM`, `UNKNOWN`
- `roomId`, `clientMessageId`: 원 요청에서 복원 가능할 때만 포함

### 오류 전달 경로를 두 개로 나눈 이유

1. **CONNECT/SUBSCRIBE 인터셉터 오류**
   - 컨트롤러에 도달하기 전에 실패한다.
   - `ChatStompErrorHandler`가 JSON body를 가진 STOMP `ERROR` frame으로 변환한다.
   - STOMP 클라이언트의 protocol error callback에서 처리해야 한다.
2. **SEND/LEAVE 및 메시지 검증·도메인 오류**
   - `ChatController`의 `@MessageExceptionHandler`가 `/user/queue/chat-errors`로 응답한다.
   - 해당 세션에만 전달되므로 같은 계정의 다른 탭 상태를 잘못 변경하지 않는다.
3. **구독 성공 후 presence/read 후처리 오류**
   - `WebSocketEventListener`가 기존처럼 로그만 남기지 않고 해당 세션의 `/user/queue/chat-errors`로 보낸다.
   - 브로커 자체 전송 실패는 원래 처리 흐름에 재전파하지 않고 로그로 격리한다.

예상하지 못한 예외는 내부 예외 메시지나 DB 정보 대신 `50000 INTERNAL_ERROR`만 노출한다.

### 이번에 직접 관련된 오류 코드

| code | status | 의미 |
|---:|---:|---|
| `40000` | 400 | JSON 변환 실패, roomId 형식 등 일반 잘못된 요청 |
| `48003` | 400 | 내용 공백 또는 16,000자 초과 |
| `48005` | 400 | 닫힌 채팅방 |
| `48006` | 400 | 잘못된 `clientMessageId` |
| `48302` | 403 | 방 또는 구독 목적지 접근 권한 없음 |
| `48402` | 404 | 채팅방 없음 |
| `48903` | 409 | 동일한 `clientMessageId`를 다른 내용에 재사용 |
| `50000` | 500 | 예상하지 못한 서버 내부 오류 |

인증 관련 오류는 기존 `AuthErrorCode`를 그대로 사용한다. 없는 사용자, 유효하지 않은 토큰, ACCESS가 아닌 토큰, 정지 사용자는 CONNECT 단계에서 차단된다.

## 9. 5단계 — 메시지 멱등성 처리 상세

멱등성 키의 유효 범위는 다음 조합이다.

```text
(roomId, senderId, clientMessageId)
```

동작 순서:

1. 채팅방 행을 비관적 잠금으로 조회한다.
2. 방 상태와 실제 참여자 여부를 검증한다.
3. `clientMessageId`를 소문자 UUID 정규 형식으로 정규화한다.
4. 같은 범위의 기존 메시지를 조회한다.
5. 기존 메시지의 내용까지 같으면 `duplicate=true` ACK만 반환한다.
6. 내용이 다르면 `48903`으로 거부한다.
7. 기존 메시지가 없으면 1회 저장하고 기존 AFTER_COMMIT 이벤트를 발행한다.

중복 요청에서는 다음 부수 효과를 다시 실행하지 않는다.

- 새 메시지 DB 저장
- 방 메시지 재브로드캐스트
- unread 증가
- 푸시 알림/알림 이벤트 재발행
- 마지막 메시지 시각 재갱신

DB에도 다음 유일성 제약을 선언해 애플리케이션 로직 밖의 중복을 방어한다.

```text
uk_chat_message_room_sender_client_id
(cc_thread_id, sender_id, client_message_id)
```

## 10. 트랜잭션과 전달 보장 수준 종합

- 새 메시지와 READ 실시간 이벤트는 기존 3단계 구현대로 `AFTER_COMMIT`에서만 브로드캐스트된다.
- 롤백된 트랜잭션의 이벤트는 프론트에 전달되지 않는다.
- 메시지 ACK는 `@Transactional` 서비스 메서드가 정상 반환하고 트랜잭션 커밋이 끝난 뒤 컨트롤러 반환값으로 전송된다.
- 커밋 후 브로커 전송 실패는 DB 트랜잭션을 되돌리지 않도록 격리된다.
- 현재 구현은 DB outbox가 아니므로 서버가 커밋 직후 종료되는 극단적 상황까지 실시간 전송을 재처리하는 exactly-once delivery는 보장하지 않는다. REST 이력 재조회로 최종 상태를 복구하는 기존 구조는 유지한다.

## 11. DB 배포 시 확인 사항

운영 설정의 Hibernate `ddl-auto=update`가 컬럼과 인덱스를 생성할 수 있으나, 운영 배포에서는 자동 반영만 믿지 말고 실제 스키마를 확인하는 것을 권장한다.

권장 DDL 예시:

```sql
ALTER TABLE coffee_chat_message
    ADD COLUMN client_message_id VARCHAR(36) NULL;

CREATE UNIQUE INDEX uk_chat_message_room_sender_client_id
    ON coffee_chat_message (cc_thread_id, sender_id, client_message_id);
```

- 컬럼은 기존 메시지와 구버전 서버가 존재할 수 있어 의도적으로 nullable이다.
- MySQL unique index는 기존 `NULL` 행들을 허용하며, 신규 서버가 저장하는 메시지에는 항상 값이 들어간다.
- 이미 컬럼/인덱스가 생성되어 있다면 DDL을 중복 실행하지 않는다.
- 배포 후 `SHOW INDEX FROM coffee_chat_message`로 유일 인덱스를 확인한다.

권장 배포 순서:

1. DB 컬럼과 유일 인덱스 존재 여부 확인 및 필요 시 적용
2. 백엔드 배포
3. 프론트에서 ACK/error 구독과 `clientMessageId` 전송 배포
4. 로그와 `48903`, `50000`, STOMP 재연결률 모니터링

백엔드를 먼저 배포해도 구버전 프론트 요청은 허용된다. 다만 구버전은 `clientMessageId`를 보내지 않으므로 재시도 중복 방지 혜택은 받지 못한다.

## 12. Chat REST Swagger·validation 변경

`ChatRequestController`, `ChatRoomController`의 모든 API에 실제 서비스에서 발생 가능한 오류 상태와 `ErrorResponse` schema를 명시했다. Swagger 설명만 추가한 것이 아니라 잘못된 입력이 서비스/DB까지 내려가지 않도록 validation도 연결했다.

### 컨트롤러 변경

- 두 컨트롤러에 `@Validated` 적용
- request body에 `@Valid` 적용
- `requestId`, `recruitmentId`, `roomId` path variable에 `@Positive` 적용
- 각 API의 400/401/403/404/409/415/500 가능 상황과 도메인 오류 코드 명시

### DTO 변경

`ChatRequestSendDto`:

- `receiverId`: `@NotNull`, `@Positive`
- 제공된 `tagIds` 원소: 각각 `@NotNull`, `@Positive`
- `content`: `@NotBlank`, 최대 16,000자
- `tagIds` 목록 자체의 null/empty는 기존 서비스 정책대로 태그 없음으로 허용

`ChatRequestAcceptDto`:

- `requestId`: `@NotNull`, `@Positive`
- `isAccepted`: 기존 primitive `boolean` 유지

validation, JSON 파싱, enum/path variable 변환 실패는 `GlobalExceptionHandler`에서 기존 `ErrorResponse(400, 40000, message)`로 통일된다. 응답 DTO 형식은 변경하지 않았다.

### 알려진 입력 계약 주의점

`isAccepted`는 primitive이므로 JSON에서 필드를 생략해도 Java 기본값 `false`가 되어 거절로 처리된다. 현재 프론트 호환성을 유지하기 위해 그대로 두었지만, 필드 누락을 잘못된 요청으로 강제하려면 추후 `Boolean + @NotNull`로 별도 계약 변경이 필요하다.

## 13. Home 요청함과 ChatRequest 연계

이번 작업 기간에는 Home 화면의 커피챗 요청뿐 아니라 팀원모집 요청도 동일한 ChatRequest 기반으로 노출하도록 `ChatService`와 `ChatRequestRepository`가 함께 변경되었다.

### 현재 동작

- `getHomeInbox(userId, limit)`: `COFFEE_CHAT`, `WAITING` 요청의 총 개수와 최신 미리보기
- `getHomeRecruitmentInbox(userId, limit)`: `TEAM_RECRUIT`, `WAITING` 요청의 총 개수와 최신 미리보기
- 최신 요청은 `createdAt DESC` 및 pageable limit으로 조회
- requester를 fetch join하고, 관련 프로필은 `findGlobalsByUserIdIn`으로 일괄 조회해 반복 조회를 줄임
- 미리보기는 `requestId`, 보낸 사용자 ID/이름, 전공, 학번을 반환
- pendingCount가 0이면 각 Home section의 empty 응답 반환

이 변경은 WebSocket 1~6단계의 프로토콜과 직접 연결되지는 않지만, 같은 `ChatService`/`ChatRequestRepository`를 수정하므로 병합·회귀 검토 시 함께 봐야 한다.

## 14. 6단계 — 테스트 범위

| 테스트 | 검증 내용 |
|---|---|
| `ChatPresenceServiceImplTest` | 다중 세션 disconnect, unsubscribe, 명시적 leave, 중복 정리의 멱등성 |
| `ChatReadStateTest` | READ 이벤트의 최신 메시지 ID와 접근 가능한 방 기준 처리 |
| `ChatRealtimeEventListenerTest` | AFTER_COMMIT listener가 delivery service에 위임 |
| `ChatRealtimeDeliveryServiceTest` | 메시지·READ 목적지와 unread payload, 전송 실패 격리 |
| `ChatSocketErrorMapperTest` | 도메인/변환/예상 밖 예외 매핑과 민감 정보 비노출 |
| `ChatStompErrorHandlerTest` | CONNECT/SUBSCRIBE 실패의 JSON STOMP ERROR 프레임 |
| `ChatControllerContractTest` | 세션 한정 ACK와 인증 누락 실패 계약 |
| `ChatSocketErrorPublisherTest` | 특정 세션 오류 전달과 브로커 실패 격리 |
| `WebSocketEventListenerTest` | 구독 후 read 실패를 발생 세션에 전달 |
| `ChatServiceTest` | 동일 키 재요청, 다른 내용 키 재사용, 신규 저장, 최대 길이 방어 |
| `ChatMessageIdempotencyIntegrationTest` | 동일 키 동시 요청 2건이 DB 메시지 1건으로 수렴 |
| `ChatRealtimeAfterCommitIntegrationTest` | 커밋 후 전송 및 롤백 시 전송 억제 |
| `ChatWebSocketContractIntegrationTest` | 실제 STOMP 연결·구독·전송·ACK·1회 브로드캐스트·중복 ACK·오류 채널 |
| `ChatHomeInboxTest` | 커피챗·팀원모집 Home inbox 타입 분리, 개수, 미리보기 |

검증 명령:

```powershell
.\gradlew.bat test
```

최종 검증 결과: **55 tests, 0 failures, 0 errors, 0 skipped**. 추가로 `clean test`로 생성물 제거 후 전체 재컴파일까지 확인했다.

## 15. 주요 변경 파일

- `domain/chat/controller/ChatController.java`
- `domain/chat/controller/ChatRequestController.java`
- `domain/chat/controller/ChatRoomController.java`
- `domain/chat/dto/message/ChatMessageSendRequestDto.java`
- `domain/chat/dto/message/ChatMessageResponseDto.java`
- `domain/chat/dto/message/ChatMessageAckResponseDto.java`
- `domain/chat/dto/message/ChatSocketErrorResponse.java`
- `domain/chat/dto/message/ChatSocketOperation.java`
- `domain/chat/dto/request/request/ChatRequestAcceptDto.java`
- `domain/chat/dto/request/request/ChatRequestSendDto.java`
- `domain/chat/event/ChatMessageCommittedEvent.java`
- `domain/chat/event/ChatReadCommittedEvent.java`
- `domain/chat/event/ChatRealtimeEventListener.java`
- `domain/chat/model/Chat.java`
- `domain/chat/repository/ChatRepository.java`
- `domain/chat/repository/ChatRequestRepository.java`
- `domain/chat/repository/ChatRoomRepository.java`
- `domain/chat/service/ChatPresenceService.java`
- `domain/chat/service/ChatPresenceServiceImpl.java`
- `domain/chat/service/ChatRealtimeDeliveryService.java`
- `domain/chat/service/ChatService.java`
- `global/websocket/ChatStompErrorHandler.java`
- `global/websocket/ChatSocketErrorMapper.java`
- `global/websocket/ChatSocketErrorPublisher.java`
- `global/websocket/ChatStompInterceptor.java`
- `global/websocket/WebSocketConfig.java`
- `global/websocket/WebSocketEventListener.java`
- `domain/home/dto/HomeResponse.java`
- `domain/home/service/HomeService.java`

## 16. 배포·리뷰 시 특히 볼 부분

- 서버가 여러 인스턴스라면 인메모리 presence가 인스턴스 간 공유되지 않는다는 점
- UNSUBSCRIBE/DISCONNECT/명시적 LEAVE가 서로 다른 정리 범위를 유지하는지
- 나간 방 unread가 전체 배지에 다시 포함되지 않는지
- 메시지·READ 소켓 이벤트가 DB 커밋 전에 발송되지 않는지
- 커밋 후 브로커 전송 실패 시 프론트가 REST 이력 재조회로 복구 가능한지
- 운영 DB의 컬럼·유일 인덱스가 실제로 생성되는지
- 프론트가 재시도 시 새 ID가 아니라 같은 `clientMessageId`를 재사용하는지
- 프론트가 CONNECT/SUBSCRIBE의 STOMP `ERROR`와 `/user/queue/chat-errors`를 서로 다른 경로로 모두 처리하는지
- 다중 탭에서 ACK는 요청 탭만, 방 브로드캐스트는 구독 중인 탭 모두가 받는 동작이 의도와 맞는지
- `ChatRequestAcceptDto.isAccepted` 누락을 현재처럼 reject로 볼지 향후 400으로 강화할지
- Home inbox의 COFFEE_CHAT/TEAM_RECRUIT 타입 분리가 유지되는지
- `50000` 비율이 상승할 때 서버 로그의 operation/roomId를 기준으로 원인을 추적하는지
