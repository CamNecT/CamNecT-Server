# 구매할 때마다 입력하는 기프티콘 수신 전화번호

가입 번호 복구 방식과의 **대안 PR**이다. 두 PR을 동시에 머지하지 않는다. [공통 관리자 메일·엑셀 계약](gifticon-contact-export.md)을 포함한다. 회원가입 및 사용자 테이블은 이메일 기반 그대로이며 전화번호를 추가로 수집하지 않는다.

## 새 구매 API

`POST /api/gifticons/purchases/confirm-with-phone` (기존 구매 API와 동일한 Access Token 사용)

```json
{
  "productId": 10,
  "quantity": 1,
  "spendPoints": 1000,
  "clientRequestId": "order-unique-id",
  "recipientPhone": "010-1234-5678",
  "recipientEmail": "recipient@example.com"
}
```

- `recipientPhone`은 매 주문 필수다. 국내 휴대전화번호의 공백·하이픈을 제거하여 저장한다. 형식 오류·누락은 `400 / 40000`, 서비스에서 수신 번호를 확정할 수 없으면 `400 / 47004`다. 실패 시 저장·차감하지 않는다.
- `recipientEmail`은 기존처럼 선택이며 생략하면 가입 이메일을 사용한다. 선택값 `recipientName`, `giftMessage`도 유지한다.
- 응답은 기존과 동일한 `purchaseId`, `requestedAt`이다. 새 수신 번호는 구매 레코드에만 저장하며 다음 주문의 기본값으로 사용하지 않는다.
- `buyerEmail`은 가입 이메일 스냅샷, `recipientPhone`은 입력한 번호, `recipientEmail`은 확정된 이메일이다. 구매자 번호를 따로 수집하지 않아 `buyerPhone`은 비워 둔다.
- 같은 `clientRequestId` 재시도는 번호의 표시 형식만 달라도 정규화 후 같으면 같은 주문을 반환한다. 번호나 이메일 등 주문 내용이 달라지면 `409 / 47901`, 추가 차감은 없다.

이전 `/purchases/confirm` 경로는 deprecated로 남긴다. 전화번호 없는 기존 주문의 동일 재시도는 허용하되, 전화번호 없는 **새 주문은 차감 전에 거절**한다. 이전 경로에 전화번호를 보내도 같은 구매 서비스와 멱등 키를 사용하므로 두 경로 사이에서도 중복 차감하지 않는다. 신규 프론트엔드는 반드시 새 API와 매 구매 번호 입력 화면을 사용해야 한다.

Flyway V18만 추가한다. 기존 회원 가입 계약은 바꾸지 않는다. 과거 주문의 삭제된 번호는 채우지 않고 엑셀에서 확인 대상으로 표시한다. 실제 Gmail 발송과 운영 DB 적용은 배포 시 수행한다.
