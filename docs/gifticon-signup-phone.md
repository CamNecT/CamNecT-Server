# 가입 전화번호를 기프티콘 기본 수신 번호로 사용

구매별 입력 방식과의 **대안 PR**이다. 두 방식을 동시에 머지하지 않는다. [공통 관리자 메일·엑셀 계약](gifticon-contact-export.md)을 포함한다.

## API

- `POST /api/auth/signup/email/verify`: `phoneNum` 필수. 국내 휴대전화번호를 공백·하이픈 제거 후 검증·저장한다. 중복 번호는 `409 / 41903`, 잘못된 요청은 `400 / 40000`이다. 이메일 인증 흐름은 유지한다.
- `GET /api/profile/me/settings`, `GET /api/gifticons/home`: 기존 `email`에 `phoneNum`을 추가한다. 기존 회원은 `null`일 수 있다.
- `POST /api/gifticons/purchases/confirm`: 선택값 `recipientPhone`을 추가한다. 생략하면 계정 전화번호, `recipientEmail` 생략 시 가입 이메일을 사용한다. 유효한 수신 번호를 정할 수 없으면 `400 / 47004`이며 포인트를 차감하지 않는다.
- `recipientPhone`은 이번 주문의 수신 번호만 바꾸며 계정 번호를 수정하지 않는다. 가입 번호가 없는 기존 회원은 구매 화면에서 번호를 입력해야 한다.

```json
{"productId":10,"quantity":1,"spendPoints":1000,"clientRequestId":"order-unique-id","recipientPhone":"010-1234-5678","recipientEmail":"recipient@example.com"}
```

구매 시 `buyerPhone`과 `buyerEmail`, `recipientPhone`과 `recipientEmail`을 각각 저장한다. 재시도는 현재 계정 대신 최초 주문 스냅샷으로 비교하며 번호·이메일·상품 등 주문 내용이 다르면 `409 / 47901`로 거절한다. 동일 주문은 재차감하지 않는다.

## 배포

V18은 주문 스냅샷 컬럼, V19는 nullable/unique `users.phone_num`을 추가한다. 과거 삭제된 번호를 임의 복구하지 않는다. 기존 계정은 로그인 가능하며 새 가입에는 전화번호가 필수다. 회원 탈퇴 시 계정 전화번호를 비워 재가입을 막지 않는다. 프론트엔드는 가입 입력, 기본 번호 표시, 번호가 없는 기존 회원의 구매 입력 처리를 함께 준비해야 한다. 기존 마이그레이션은 변경하지 않는다.
