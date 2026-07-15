# Chat 안정화 프론트엔드 작업 전달 문서

## 1. 프론트에서 반드시 적용할 작업

이번 백엔드 변경은 기존 채팅 주소를 유지하면서 다음 계약을 추가했다. 프론트 핵심 작업은 두 가지다.

1. `/user/queue/chat-acks`, `/user/queue/chat-errors`를 구독하고 각각 처리한다.
2. 메시지의 한 번의 사용자 전송 동작마다 `clientMessageId`를 한 번 생성하고, 같은 메시지를 재시도할 때 같은 ID를 재사용한다.

6단계의 테스트 보강은 프론트 코드 변경이 필요 없다.

## 2. 연결 후 권장 구독 순서

STOMP CONNECT 성공 직후 다음 순서로 구독한다.

1. `/user/queue/chat-acks`
2. `/user/queue/chat-errors`
3. `/sub/user/{로그인사용자ID}/rooms`
4. 채팅방 진입 시 `/sub/chat/room/{roomId}`

ACK/error 구독을 먼저 하는 이유는 방 구독이나 첫 메시지 전송 직후 발생한 응답을 놓치지 않기 위해서다.

허용되지 않는 목적지, 타인의 `/sub/user/{userId}/rooms`, 참여하지 않은 방을 구독하면 서버가 STOMP `ERROR` frame을 보내고 연결을 종료할 수 있다.

## 3. 메시지 전송 구현

전송 목적지:

```text
/pub/chat/message
```

요청 예시:

```json
{
  "roomId": 12,
  "content": "안녕하세요.",
  "clientMessageId": "0e9e31aa-99e7-4c58-90d8-f939b56fd234"
}
```

TypeScript 예시:

```ts
type ChatMessageSendRequest = {
  roomId: number;
  content: string;
  clientMessageId: string;
};

function createPendingMessage(roomId: number, content: string) {
  return {
    roomId,
    content,
    clientMessageId: crypto.randomUUID(),
    state: "pending" as const,
  };
}

function sendChatMessage(message: ChatMessageSendRequest) {
  stompClient.publish({
    destination: "/pub/chat/message",
    body: JSON.stringify(message),
  });
}
```

규칙:

- 사용자가 전송 버튼을 한 번 눌러 만든 **논리 메시지 1개당 UUID 1개**를 생성한다.
- 네트워크 지연, ACK 유실, 재연결 등으로 같은 메시지를 재전송할 때는 기존 `clientMessageId`를 재사용한다.
- 내용을 편집해서 새 메시지로 보내는 경우에는 새 UUID를 생성한다.
- `crypto.randomUUID()`가 만드는 UUID v4를 그대로 사용하면 된다.
- `content`는 공백 문자열이면 안 되며 최대 16,000자다.
- 재시도할 때 `content` 문자열도 원 요청과 정확히 같아야 한다. 같은 ID에 다른 내용을 보내면 `48903`이다.
- 전송 버튼 이중 클릭은 UI에서 먼저 막되, 두 호출이 발생해도 반드시 같은 pending 객체/ID를 재사용하도록 구현하면 서버에서도 한 건으로 수렴한다.

## 4. ACK 처리

구독 목적지:

```text
/user/queue/chat-acks
```

응답 타입:

```ts
type ChatMessageAck = {
  type: "ACK";
  messageId: number;
  roomId: number;
  clientMessageId: string;
  duplicate: boolean;
};
```

처리 방법:

1. `clientMessageId`로 로컬 pending 메시지를 찾는다.
2. 서버의 `messageId`를 pending 메시지에 연결한다.
3. `duplicate`가 `false`든 `true`든 전송 성공 상태로 변경한다.
4. `duplicate=true`일 때 새 말풍선을 추가하지 않는다. 이전 요청이 이미 저장되었다는 뜻이다.
5. ACK 타임아웃만으로 곧바로 실패 확정하지 말고, 연결 상태를 확인한 뒤 같은 ID로 제한적으로 재시도한다.

ACK는 요청을 보낸 WebSocket 세션에만 전달된다. 같은 계정으로 여러 탭을 열었을 때 다른 탭에는 ACK가 오지 않지만, 방을 구독 중이면 기존 방 메시지 브로드캐스트는 받을 수 있다.

## 5. 방 메시지 이벤트 처리

기존 방 메시지 응답에 `clientMessageId`가 추가된다.

```ts
type ChatMessageResponse = {
  messageId: number;
  roomId: number;
  clientMessageId: string | null;
  senderId: number;
  sender: string;
  receiverId: number;
  receiver: string;
  message: string;
  read: boolean;
  sendDate: string;
  readAt: string | null;
};
```

권장 중복 제거 우선순위:

1. `messageId`가 이미 렌더링되어 있으면 무시
2. 내가 보낸 메시지이고 `clientMessageId`가 로컬 pending과 같으면 새 말풍선을 만들지 말고 해당 pending을 서버 응답으로 교체
3. 둘 다 일치하지 않을 때만 새 메시지 추가

이 처리가 필요한 이유는 전송한 탭이 ACK와 방 브로드캐스트를 모두 받을 수 있고, 같은 계정의 다른 탭도 방 브로드캐스트를 받을 수 있기 때문이다.

과거 DB 메시지 또는 점진 배포 중 생성된 메시지는 `clientMessageId=null`일 수 있으므로 `messageId` 기반 처리를 항상 유지한다.

## 6. 애플리케이션 오류 큐 처리

구독 목적지:

```text
/user/queue/chat-errors
```

응답 타입:

```ts
type ChatSocketOperation =
  | "CONNECT"
  | "SUBSCRIBE"
  | "SEND_MESSAGE"
  | "LEAVE_ROOM"
  | "UNKNOWN";

type ChatSocketError = {
  type: "ERROR";
  status: number;
  code: number;
  message: string;
  operation: ChatSocketOperation;
  roomId: number | null;
  clientMessageId: string | null;
};
```

처리 원칙:

- `clientMessageId`가 있으면 해당 pending 메시지를 찾아 실패 상태와 재시도 가능 여부를 표시한다.
- 서버 `message`는 사용자 안내에 사용할 수 있지만, UI 분기 기준은 문자열이 아니라 `code`를 사용한다.
- `roomId`가 있으면 해당 방의 상태/접근 권한을 다시 확인한다.
- `50000`은 내부 상세가 숨겨진 오류다. 자동 무한 재시도하지 말고 일반 오류 안내와 수동 재시도를 제공한다.

주요 코드별 처리 권장안:

| code | 권장 프론트 동작 |
|---:|---|
| `40000` | 잘못된 payload 확인, 자동 재시도 금지 |
| `48003` | 공백/길이 검증 안내, 입력 화면 유지 |
| `48005` | 닫힌 방 안내 후 방 목록·이력 재조회 |
| `48006` | 클라이언트 ID 생성 로직 점검, 새 논리 메시지라면 새 UUID로 재작성 |
| `48302` | 접근 불가 안내 후 해당 구독/방 화면 정리 |
| `48402` | 삭제·종료된 방으로 처리하고 방 목록 재조회 |
| `48903` | 같은 ID에 내용이 달라진 프론트 버그이므로 자동 재시도 금지 |
| `50000` | 일반 서버 오류 안내, 제한된 수동 재시도 허용 |

401 계열 인증 오류에서는 소켓을 폐기하고 기존 로그인 만료 흐름으로 이동한다. 현재 refresh token은 사용하지 않으므로 WebSocket 코드에서 임의의 refresh 재발급 흐름을 추가하지 않는다.

## 7. CONNECT/SUBSCRIBE 오류는 별도 처리

CONNECT와 SUBSCRIBE는 컨트롤러에 도달하기 전에 실패할 수 있으므로 `/user/queue/chat-errors`가 아니라 STOMP protocol `ERROR` frame으로 온다.

`@stomp/stompjs` 예시:

```ts
stompClient.onStompError = (frame) => {
  let error: ChatSocketError | null = null;

  try {
    error = JSON.parse(frame.body) as ChatSocketError;
  } catch {
    // JSON 파싱 불가 시 일반 연결 오류로 처리
  }

  if (error?.operation === "CONNECT") {
    // 인증 만료/정지/토큰 유형 오류 처리
  } else if (error?.operation === "SUBSCRIBE") {
    // 권한 없는 방 또는 잘못된 구독 목적지 처리
  }
};
```

주의:

- STOMP `ERROR` frame 이후 연결이 종료될 수 있다.
- 401/403 성격의 실패에는 무한 자동 재연결하지 않는다.
- CONNECT 인증 오류는 로그인 상태를 정리한다.
- SUBSCRIBE `48302`는 해당 방/목적지를 다시 구독하지 말고 방 목록을 갱신한다.
- 네트워크 단절일 때만 기존 백오프 재연결 정책을 적용한다.

## 8. pending 상태 권장 모델

```ts
type PendingState = "pending" | "sent" | "failed";

type PendingChatMessage = {
  roomId: number;
  content: string;
  clientMessageId: string;
  serverMessageId?: number;
  state: PendingState;
  retryCount: number;
  errorCode?: number;
};
```

권장 흐름:

```text
사용자 전송
  → UUID 생성 및 pending 저장
  → STOMP SEND
  → ACK 또는 방 메시지 수신
      → 같은 clientMessageId의 pending을 sent로 확정
  → 전송 불확실/연결 단절
      → 같은 ID·같은 content로 재전송
  → ERROR 수신
      → code에 따라 failed 처리 또는 로그인/방 상태 복구
```

페이지 새로고침 후에도 미확정 메시지를 자동 재전송할 계획이라면 pending 데이터에 `clientMessageId`와 원문을 함께 보존해야 한다. 보존하지 않는다면 새로고침 후에는 이력 API를 다시 조회해 서버 상태를 기준으로 화면을 복구한다.

## 9. 하위 호환성과 배포 순서

- 기존 SEND 목적지와 기존 방/방 목록 구독 주소는 바뀌지 않았다.
- `clientMessageId`는 현재 서버에서 선택 입력이라 백엔드 선배포가 가능하다.
- 구버전 프론트도 메시지는 보낼 수 있지만 ACK 기반 상태 확정과 재시도 중복 방지는 받지 못한다.
- 기존 메시지 DTO에는 필드가 추가됐을 뿐 기존 필드는 유지된다.

권장 순서:

1. 백엔드 배포 완료 확인
2. ACK/error 구독 코드 배포
3. `clientMessageId` 생성·pending 매칭·재시도 코드 배포
4. 다중 탭과 네트워크 단절 QA

## 10. 프론트 QA 체크리스트

- [ ] 정상 전송 시 말풍선이 한 번만 나타나고 pending이 sent로 바뀐다.
- [ ] 전송 버튼을 빠르게 두 번 눌러도 동일 논리 메시지가 한 건만 표시된다.
- [ ] 같은 `clientMessageId`·같은 내용으로 재전송하면 `duplicate=true` ACK를 성공으로 처리한다.
- [ ] 같은 `clientMessageId`·다른 내용은 `48903`으로 실패하며 자동 재시도하지 않는다.
- [ ] ACK가 늦고 방 메시지가 먼저 도착해도 pending과 정상 결합된다.
- [ ] ACK가 먼저 도착하고 방 메시지가 나중에 와도 말풍선이 중복되지 않는다.
- [ ] 같은 계정의 두 탭에서 요청 탭만 ACK를 받고, 두 탭 모두 방 이벤트를 받아도 중복 UI가 생기지 않는다.
- [ ] 공백 메시지, 16,000자 초과, 잘못된 UUID 오류가 해당 pending 메시지에 표시된다.
- [ ] 참여하지 않은 방 구독 시 STOMP `ERROR`를 처리하고 무한 재연결하지 않는다.
- [ ] 만료/잘못된 ACCESS 토큰 CONNECT 실패 시 로그인 만료 흐름으로 이동한다.
- [ ] 네트워크 단절 후 재전송은 새 UUID가 아니라 기존 UUID를 사용한다.
- [ ] 과거 `clientMessageId=null` 메시지도 `messageId` 기준으로 정상 렌더링한다.
