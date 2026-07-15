# Chat 안정화 4~6단계 백엔드 전달 문서

## 1. 작업 목적과 완료 범위

이번 작업은 기존 1~3단계의 세션 기반 presence, unread/read 일관성, 트랜잭션 커밋 이후 실시간 전송을 전제로 다음 세 항목을 구현한 것이다.

1. **4단계 — 구조화된 STOMP 오류 계약**
   - WebSocket/STOMP 오류를 REST 오류 응답과 유사한 JSON 구조로 통일했다.
   - 연결·구독 단계 오류와 애플리케이션 메시지 처리 오류를 각각 실제 전달 가능한 경로로 분리했다.
   - 인증되지 않은 구독, 타인의 채팅방/방 목록 구독, 알 수 없는 목적지 구독을 차단한다.
2. **5단계 — 메시지 전송 멱등성**
   - 프론트가 생성한 `clientMessageId`를 메시지와 함께 저장한다.
   - 같은 사용자가 같은 방에서 같은 ID와 같은 내용을 재전송하면 새 메시지를 만들지 않고 기존 메시지 ACK를 돌려준다.
   - 같은 ID를 다른 내용에 재사용하면 `48903`으로 거부한다.
3. **6단계 — 안정성 테스트 보강**
   - 오류 매핑, STOMP ERROR 프레임, 세션 한정 오류 전달, ACK 계약, 중복 전송 동시성, 커밋/롤백 전송, 실제 WebSocket 연결 흐름을 검증한다.

HTTP REST 응답 DTO의 공통 형식은 변경하지 않았다. 이번 계약 변경은 채팅 STOMP 메시지에 한정된다.

## 2. STOMP 목적지 계약

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

## 3. 메시지 요청·응답 계약

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

## 4. 구조화된 오류 계약

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

## 5. 멱등성 처리 상세

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

## 6. 트랜잭션과 전달 보장 수준

- 새 메시지와 READ 실시간 이벤트는 기존 3단계 구현대로 `AFTER_COMMIT`에서만 브로드캐스트된다.
- 롤백된 트랜잭션의 이벤트는 프론트에 전달되지 않는다.
- 메시지 ACK는 `@Transactional` 서비스 메서드가 정상 반환하고 트랜잭션 커밋이 끝난 뒤 컨트롤러 반환값으로 전송된다.
- 커밋 후 브로커 전송 실패는 DB 트랜잭션을 되돌리지 않도록 격리된다.
- 현재 구현은 DB outbox가 아니므로 서버가 커밋 직후 종료되는 극단적 상황까지 실시간 전송을 재처리하는 exactly-once delivery는 보장하지 않는다. REST 이력 재조회로 최종 상태를 복구하는 기존 구조는 유지한다.

## 7. DB 배포 시 확인 사항

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

## 8. 테스트 범위

| 테스트 | 검증 내용 |
|---|---|
| `ChatSocketErrorMapperTest` | 도메인/변환/예상 밖 예외 매핑과 민감 정보 비노출 |
| `ChatStompErrorHandlerTest` | CONNECT/SUBSCRIBE 실패의 JSON STOMP ERROR 프레임 |
| `ChatControllerContractTest` | 세션 한정 ACK와 인증 누락 실패 계약 |
| `ChatSocketErrorPublisherTest` | 특정 세션 오류 전달과 브로커 실패 격리 |
| `WebSocketEventListenerTest` | 구독 후 read 실패를 발생 세션에 전달 |
| `ChatServiceTest` | 동일 키 재요청, 다른 내용 키 재사용, 신규 저장, 최대 길이 방어 |
| `ChatMessageIdempotencyIntegrationTest` | 동일 키 동시 요청 2건이 DB 메시지 1건으로 수렴 |
| `ChatRealtimeAfterCommitIntegrationTest` | 커밋 후 전송 및 롤백 시 전송 억제 |
| `ChatWebSocketContractIntegrationTest` | 실제 STOMP 연결·구독·전송·ACK·1회 브로드캐스트·중복 ACK·오류 채널 |

검증 명령:

```powershell
.\gradlew.bat test
```

최종 검증 결과: **55 tests, 0 failures, 0 errors, 0 skipped**. 추가로 `clean test`로 생성물 제거 후 전체 재컴파일까지 확인했다.

## 9. 주요 변경 파일

- `domain/chat/controller/ChatController.java`
- `domain/chat/dto/message/ChatMessageSendRequestDto.java`
- `domain/chat/dto/message/ChatMessageResponseDto.java`
- `domain/chat/dto/message/ChatMessageAckResponseDto.java`
- `domain/chat/dto/message/ChatSocketErrorResponse.java`
- `domain/chat/model/Chat.java`
- `domain/chat/repository/ChatRepository.java`
- `domain/chat/service/ChatService.java`
- `global/websocket/ChatStompErrorHandler.java`
- `global/websocket/ChatSocketErrorMapper.java`
- `global/websocket/ChatSocketErrorPublisher.java`
- `global/websocket/ChatStompInterceptor.java`
- `global/websocket/WebSocketConfig.java`
- `global/websocket/WebSocketEventListener.java`

## 10. 리뷰 시 특히 볼 부분

- 운영 DB의 컬럼·유일 인덱스가 실제로 생성되는지
- 프론트가 재시도 시 새 ID가 아니라 같은 `clientMessageId`를 재사용하는지
- 프론트가 CONNECT/SUBSCRIBE의 STOMP `ERROR`와 `/user/queue/chat-errors`를 서로 다른 경로로 모두 처리하는지
- 다중 탭에서 ACK는 요청 탭만, 방 브로드캐스트는 구독 중인 탭 모두가 받는 동작이 의도와 맞는지
- `50000` 비율이 상승할 때 서버 로그의 operation/roomId를 기준으로 원인을 추적하는지
