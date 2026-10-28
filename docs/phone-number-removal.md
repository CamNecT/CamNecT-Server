## SOLAPI 운영 전환 (2026-10-29)

개인 계정에서 사용할 수 있는 SOLAPI를 기본 SMS 공급자로 사용한다. SENS는 `PHONE_SMS_PROVIDER=sens`를 명시한 기존 사업자 환경에서만 선택한다. 이번 전환은 V21/V22 스키마를 그대로 사용하며 새 Flyway migration이나 기존 migration 수정이 없다.

- `PHONE_SMS_PROVIDER=solapi`, `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, `PHONE_SMS_SENDER_NUMBER`, 독립적인 `PHONE_VERIFICATION_CODE_SECRET`을 운영 비밀 환경파일에 설정한다. 키와 OTP는 저장소·로그에 기록하지 않는다.
- 키는 운영 서버의 고정 출발 IP에 제한하고 만료 전에 교체한다. 발신번호는 SOLAPI에서 소유·사용 권한을 확인한 실제 번호여야 한다. 수신번호는 각 회원이 저장한 번호다.
- SMS는 건당 18원(VAT 별도), 월 50회면 약 990원(VAT 포함)이다. 가입 지급 포인트는 매월 무료 제공량이 아니다. 현재 가격은 [SOLAPI 공식 요금](https://solapi.dev/guides/pricing)을 확인한다.
- 앱은 월 50회, 일 20회, 계정/번호별 일 5회를 제한한다. 재발송·실패·타임아웃도 예약한 예산을 소비하고 자동 재시도하지 않는다. 월 상한을 50보다 크게 설정하면 앱 설정 검증 및 활성화 배포 검사가 실패한다. 공급자를 변경해도 기존 DB 예산을 초기화하지 않는다.
- 콘솔 직접 발송·다른 앱의 발송은 이 서버의 월 상한에 포함되지 않는다. 공유 키를 사용하지 않고 자동 충전을 끈다. SOLAPI 콘솔의 일 한도는 앱의 월 한도를 대체하지 않는다.
- SMS 타입을 고정하고 자동 LMS 전환을 끈다. HTTP 200이라도 개별 실패·불완전한 접수 응답은 실패로 처리한다. 접수 성공은 통신사 최종 전달 성공과 별개다.
- `legacy` 모드에서는 기존 이메일 교환을 유지한다. 가입 전화번호 수집, 설정 번호 변경, 샵 OTP 화면을 실제 배포·확인한 뒤에만 `PHONE_FRONTEND_READY=true`, `PHONE_VERIFICATION_ENABLED=true`, `PHONE_ROLLOUT_MODE=verified-phone`으로 전환한다. 키 저장이나 서버 코드 배포만으로 프론트 준비 완료를 선언하지 않는다.
- 가입 시 SMS를 보내지 않는다. 첫 교환 전 한 번 인증하고 번호가 바뀌면 재인증한다. 기프티콘은 인증된 본인 번호로만 수신한다.

2026-10-29 운영자는 프론트가 곧 배포될 것을 전제로 서버 인증 API와 교환 인증 필수 정책을 먼저 활성화하도록 승인했다. 이 경우에만 `PHONE_FRONTEND_READY=false`를 사실대로 유지하고 `PHONE_ALLOW_PENDING_FRONTEND=true`로 배포 검사 예외를 명시한다. 이 예외는 SMS 키·활성화·월 예산 검증을 생략하지 않는다. 구형 프론트의 번호 없는 가입과 미인증 교환은 서버가 4xx로 거절하며, 프론트 배포 후 READY=true/ALLOW_PENDING=false로 정리한다.

아래 SENS 관련 기록은 이전 공급자 도입 당시의 이력이다. 현재 기본 설정과 비용은 이 절 및 `deploy/phone-verification.env.example`을 기준으로 한다.

# 전화번호 인증 및 기프티콘 수신 연락처

> 아래 V13 내용은 과거 전환 이력입니다. V21/V22은 전화번호 인증과 주문 연락처 저장을 준비합니다. 기본 `legacy` 모드에서는 기존 이메일 교환을 유지하고 SMS를 보내지 않습니다. SENS와 프론트 준비 후 `verified-phone` 모드로 전환하면 인증된 본인 번호로만 교환합니다.

### 단계적 활성화

- 이번 배포는 `PHONE_ROLLOUT_MODE=legacy`, `PHONE_VERIFICATION_ENABLED=false`로 운영합니다. 두 값 모두 기본값이므로 기존 환경 파일에 추가하지 않아도 이메일 교환이 유지됩니다.
- 가입 요청에 `phoneNum`이 있으면 정규화하여 미인증 번호로 저장합니다. 기존 프론트 호환을 위해 legacy에서는 생략할 수 있습니다. 가입 과정에서 SMS는 발송하지 않습니다.
- legacy 구매는 인증 여부와 무관하게 기존 이메일 정책을 사용하며 주문 전화번호는 NULL로 저장합니다. 관리자 XLSX의 `EMAIL_ONLY` 주문은 저장된 수신 이메일로 처리합니다. 기존 주문도 같은 규칙을 적용하며 현재 계정 번호로 덮어쓰지 않습니다.
- 아래 인증 필수 정책은 `PHONE_ROLLOUT_MODE=verified-phone` 활성화 이후 적용합니다. 전환하려면 SMS 설정과 프론트 준비가 모두 필요합니다.

## V21: 첫 기프티콘 교환 시 전화번호 인증

### 정책과 기존 회원

- 회원가입은 이메일 인증으로 완료한다. `POST /api/auth/signup/email/verify`의 `phoneNum`은 verified-phone 모드에서 필수다. 가입 단계에는 SMS를 보내지 않는다.
- 국내 `010` 번호만 받는다. `01012345678`, `010-1234-5678`, `+82 10-1234-5678`은 `01012345678`로 저장한다.
- 새 기프티콘 교환은 현재 계정 전화번호가 인증된 경우만 허용한다. 미인증이면 `403 / 47301`이며 구매 저장과 포인트 차감 전에 차단한다.
- 한 번 인증하면 만료 없이 유지한다. 번호를 바꾸면 즉시 미인증 상태로 돌아가며, 새 번호 인증 후 다시 교환할 수 있다. 같은 정규화 번호를 다시 저장하면 인증을 유지한다.
- 변경 전 인증번호는 번호를 원래대로 되돌려도 사용할 수 없다. 인증번호의 재사용과 다른 계정에서의 사용도 거절한다.
- 기존 회원은 전화번호가 NULL이고 미인증으로 시작한다. 재가입하지 않고 전화번호 등록과 인증을 진행한다. 과거 교환 이력이 있어도 현재 번호를 인증해야 새 교환이 가능하다.
- 이미 저장된 구매의 동일 `clientRequestId` 재요청은 기존 멱등 응답을 반환한다. 이 경우 새 교환이나 추가 포인트 차감은 발생하지 않는다.
- 전화번호 인증은 구매자 계정에 적용한다. 기프티콘 수신 번호는 **구매 당시 인증된 본인 번호로 고정**한다. 구매 요청의 `recipientPhone`은 400으로 거절하며 다른 사람 번호 입력 API는 추가하지 않는다. 기존 `recipientEmail`은 참고 연락처로 함께 전달한다.
- 주문에는 `buyerPhone`, `recipientPhone`을 같은 값으로 저장한다. 계정 번호를 바꾸거나 탈퇴해도 이미 접수한 주문의 연락처 스냅샷을 현재 계정 값으로 덮어쓰지 않는다. 기존 구매 재시도 역시 원래 주문을 반환한다.
- 동일 전화번호의 여러 계정 등록을 금지하는 정책은 추가하지 않았다. SMS 소유 확인은 통신사 실명 본인확인이나 1인 1계정 보장이 아니다.
- 탈퇴 시 사용자 전화번호와 인증 시각을 삭제한다. 만료된 인증 요청은 5분 간격으로 정리하며, 일별 제한용 HMAC 식별자는 일별 창 종료 후 하루가 지난 시점부터 정리한다.

### 프론트 API 계약

아래 API는 기존 ACCESS 토큰을 사용한다. 번호 저장·발송·코드 확인은 ACTIVE 계정에서만 가능하다. 가입 진행용 VERIFICATION 토큰으로는 사용할 수 없다.

| 메서드 / 경로 | 요청 | 응답의 `data` |
| --- | --- | --- |
| `GET /api/profile/me/phone` | 없음 | `phoneNum`, `phoneVerified`, `phoneVerifiedAt`, `phoneVerificationRequired`, `smsAvailable` |
| `PUT /api/profile/me/phone` | `{"phoneNum":"01012345678"}` | 저장 후 전화번호·인증 상태 |
| `POST /api/profile/me/phone/verification/send` | 본문 없음 | `challengeId`, `expiresInSeconds:180`, `resendAfterSeconds:60` |
| `POST /api/profile/me/phone/verification/confirm` | `{"challengeId":"발송 응답의 UUID","code":"123456"}` | 인증된 전화번호·인증 상태 |

`GET /api/profile/me/settings`, `GET /api/gifticons/home`에도 `phoneNum`, `phoneVerified`가 추가된다. 샵 응답의 `phoneVerificationRequired=true`이면서 `phoneVerified=false`일 때만 교환 전에 인증 화면을 연다. legacy에서는 기존 이메일 교환 화면을 유지한다. 전화번호가 없거나 변경할 때 PUT → send → confirm 순서로 처리한다. 번호가 이미 저장되어 있으면 send부터 시작한다. 코드 인증 성공 후 기존 구매 확정 API를 호출한다. 서버가 최종 인증 상태를 검사한다.

| HTTP / 코드 | 의미 |
| --- | --- |
| 400 / 42040 | 국내 휴대전화 형식 오류 |
| 400 / 42041 | 저장된 전화번호 없음 |
| 400 / 42042 | 만료·변경·이미 소비된 인증 요청 또는 다른 계정의 요청 |
| 400 / 42043 | 인증번호 불일치 |
| 409 / 42940 | 이미 인증되어 재발송 불필요 |
| 429 / 42941 | 재발송 또는 발송량 제한 |
| 429 / 42942 | 코드 입력 5회 실패 |
| 503 / 42540 | SMS 비활성화 또는 발송 예산 잠금 준비 안 됨 |
| 502 / 42541 | 공급자 발송 접수를 확인하지 못함 |
| 403 / 47301 | 미인증 번호로 새 기프티콘 교환 시도 |

발송 응답은 단말 수신 완료가 아닌 SENS의 접수 성공이다. 실패·시간 초과 때 자동 재시도하지 않는다. 이미 접수된 문자가 있을 수 있으므로 해당 요청의 발송 예산은 반환하지 않고 60초 후 사용자가 재요청하도록 한다.

### 서버와 저장소

`domain.verification.phone`이 번호 변경, OTP 발급·검증, SENS 서명 요청을 담당한다. 사용자 전화번호 및 인증 시각, 인증 요청, 발송 한도를 기존 MySQL에 저장한다. 새로운 서버·Redis 키·Valkey ACL 변경은 필요 없다.

번호 변경·코드 확인·교환은 같은 사용자 행 잠금을 사용한다. 발송 예산은 영구 `phone_sms_quotas.lock` 행을 잠가 여러 앱 인스턴스의 동시 요청도 직렬화한다. 짧은 DB 트랜잭션에서 예산과 인증 요청을 먼저 저장하고 SENS 호출은 트랜잭션 밖에서 실행한다. 호출 중 번호가 바뀌면 이전 발송 결과로 인증 요청을 되살리지 않는다. 코드 오입력 횟수는 오류 응답 전에 커밋한다.

OTP 원문은 저장하지 않는다. 독립 비밀키를 사용한 HMAC-SHA256으로 계정·요청 ID·번호·코드를 함께 묶어 검증한다. 번호별 발송 제한 키도 HMAC을 사용한다. 공급자 오류 본문·API 키·OTP를 로그에 기록하지 않는다.

MySQL REPEATABLE READ에서 사용자 접근 검사로 생성된 이전 스냅샷을 재사용하지 않도록, 전역 예산 잠금 이후 개별 카운터도 `SELECT ... FOR UPDATE`로 읽는다. 이를 통해 잠금을 기다리는 동안 다른 요청이 소비한 한도를 확인한다.

### 기존 PR 통합과 관리자 발송

- #304의 가입 시 번호 수집은 현재 이메일 가입 + 미인증 번호 저장 정책으로 대체한다. 인증되지 않은 번호를 다른 회원이 먼저 입력해 가입을 막는 일을 피하려고 과거 PR의 가입 번호 UNIQUE 제약은 가져오지 않는다.
- #305의 구매마다 전화번호 입력 및 `confirm-with-phone` API는 채택하지 않는다. 기존 `/purchases/confirm`에서 인증된 본인 번호를 사용한다.
- 두 PR의 공통인 관리자 XLSX 전화번호·이메일 전달을 통합한다. `buyerPhone`과 `recipientPhone`을 문자열로 내보내 앞자리 0을 유지한다. `READY` 주문은 수신 번호로, `EMAIL_ONLY` 주문은 수신 이메일로 발송한다. `PHONE_REQUIRED` / `EMAIL_REQUIRED` / `CONTACT_REQUIRED`는 관리자 확인 전 발송 보류다.
- 이미 제출한 배치는 다시 발송하지 않는다. 재시도 대기 중인 구형 XLSX만 새로운 연락처 열로 재생성한다. 이전 주문의 삭제된 전화번호는 추정하여 채우지 않는다.
- main의 V18(댓글), V19(학과), V20(공지 수집)을 보존하고 전화번호 인증은 V21, 주문 연락처는 V22로 추가한다. 이전 PR의 V18/V19는 적용하지 않는다.

초기 제한은 번호당/계정당 1일 5회, 전체 1일 20회, 월 50회, 재발송 60초, 유효기간 180초, 입력 실패 5회다. 날짜·월 경계는 Asia/Seoul을 사용한다. 앱 전체 상한은 해당 데이터베이스 기준으로 공급자 호출 시도 횟수를 센다. 번호 변경으로 계정 제한이 초기화되지 않는다. IP 제한은 이 구현에 포함하지 않는다.

### SENS 설정과 배포

1. SENS 프로젝트의 SMS를 활성화하고 발신번호 서류 승인을 완료한다. 공식 가이드상 영업일 3~4일이 소요될 수 있다.
2. 서버 환경 파일에 아래 값을 주입한다. 실제 키는 저장소나 프론트에 넣지 않는다. OTP 비밀키는 JWT/SENS 키와 별도로 충분히 무작위인 32자 이상 값을 생성한다.

```dotenv
PHONE_ROLLOUT_MODE=verified-phone
PHONE_VERIFICATION_ENABLED=true
PHONE_FRONTEND_READY=true
SENS_ACCESS_KEY=<server-only>
SENS_SECRET_KEY=<server-only>
SENS_SERVICE_ID=<sms-service-id>
SENS_SENDER_NUMBER=<approved-sender-digits>
PHONE_VERIFICATION_CODE_SECRET=<independent-random-secret-at-least-32-characters>
PHONE_SMS_DAILY_LIMIT=20
PHONE_SMS_MONTHLY_LIMIT=50
```

3. V21/V22을 운영 복제본의 MySQL에서 검증한 뒤 배포한다. 이미 적용된 V13을 수정하거나 삭제된 전화번호를 복원하지 않는다. `phone_sms_quotas`의 `lock` 행을 수동 삭제하지 않는다.
4. 가입 화면의 필수 전화번호 입력, 설정의 번호 변경, 샵의 인증 상태 표시·인증 화면을 서버와 함께 배포한다. 프론트 소스 변경은 이 서버 저장소 작업에 포함하지 않는다.
5. 승인된 테스트 번호로 실발송·확인 후 새 교환 차단/허용, 번호 변경 후 재차단, 기존 주문 재시도, 탈퇴를 확인한다.

기본 `legacy` 모드와 `PHONE_VERIFICATION_ENABLED=false`에서는 SENS 없이 배포할 수 있고 기존 이메일 교환을 유지한다. `verified-phone` 모드에서 SMS가 꺼져 있으면 배포 검사와 앱 시작 검증이 실패하여 잘못된 활성화를 차단한다. 비밀키를 교체하면 기존 OTP가 무효화되고 번호별 HMAC 제한 키도 바뀌므로, 창을 고려해 교체한다. 계정·전역 상한은 유지된다.

환경변수 샘플은 `deploy/phone-verification.env.example`에 있다. 전체 `.env.prod`를 교체하지 않고 기존 파일에 필요한 키만 추가한다. prod/stage 배포 워크플로는 컨테이너 교체 전에 `scripts/check-phone-deployment.py`를 실행한다. Compose가 해석한 실제 환경에서 rollout 모드를 확인하고, verified-phone에서는 SMS 활성화·필수 키·발신번호 형식·예산·`PHONE_FRONTEND_READY=true`를 검사한다. 비밀 값은 출력하지 않는다. `--check-network`는 SMS가 켜진 경우 SENS TLS 연결만 확인하며 문자를 보내거나 키의 유효성·발신번호 승인을 확인하지 않는다.

```bash
# 운영: 기존 배포 사용자 및 DOCKER_USERNAME 환경에서 실행
sudo -E python3 scripts/check-phone-deployment.py --compose docker-compose.prod.yml --check-network
```

2026-10-28 운영 앱 서버에서 SSM으로 읽기 전용 점검을 수행했다. prod/stage 환경 파일은 존재하지만 SENS 및 전화번호 인증 키는 모두 미설정이었다. 앱 서버 → `sens.apigw.ntruss.com:443`은 인증서 검증을 포함한 TLS 1.3 연결에 성공했다. 운영 컨테이너 재시작, DB 마이그레이션, 실제 SMS 발송은 하지 않았다. 네이버 콘솔은 로그아웃 상태였고 사용자는 계정 연동을 나중에 진행하기로 했다.

네이버 요금표는 국내 SMS 월 50건 무료, 초과분 9원/건(VAT 별도)이다. 무료량은 인증 성공 수가 아닌 문자량이며 재발송도 포함한다. 앱의 월 50회 제한은 다른 프로젝트·콘솔·스테이징 발송을 계산하지 못하므로 **네이버 콘솔 한도도 별도로 설정**하고 계정 전체 사용량을 합산해야 한다. 같은 SENS 계정으로 stage/prod를 쓰면 각 환경 예산의 합을 무료 범위 안으로 나누거나 별도 비용을 허용한다.

### 검증

전체 Java 테스트와 함께 번호 정규화, 가입 시 미인증 저장, OTP 만료·재발송·5회 실패·재사용·계정 바인딩, 번호 변경 중 발송 경쟁, 동시 발송 예산, 월 경계, 교환 차단·반복 교환, 서명·공급자 오류·타임아웃을 검증한다. 테스트에서는 SENS를 모의 처리하여 실제 문자를 발송하지 않는다.

`Phone verification tests` PR CI는 운영과 같은 MySQL 8.0.45의 일회용 데이터베이스를 사용한다. 동일한 인증·HTTP·동시성 테스트를 MySQL에서도 실행하고, 별도 DB에서 전체 Flyway V0~V20 적용 → 기존 계정/주문 생성 → V21/V22 적용 → 데이터 보존·제약·재실행 검증을 한다. 운영 데이터는 사용하지 않는다. MySQL URL 환경변수가 없는 로컬 실행에서는 이 추가 테스트가 명시적으로 건너뛰어진다. 배포 검사 Python 테스트도 실행한다.

공식 자료: [요금](https://www.ncloud.com/charge/price/ko), [발신번호 등록](https://guide.ncloud-docs.com/docs/sens-callingno), [API 인증](https://api.ncloud-docs.com/docs/sens-overview), [SMS 발송](https://api.ncloud-docs.com/docs/sens-sms-send).

---

## V13 전환 이력

## API 변경

| 기능 | 변경 |
| --- | --- |
| `POST /api/auth/signup/email/verify` | `phoneNum` 입력 및 중복 검사 제거. 이메일 인증·아이디/이메일 중복 검사는 유지. |
| `GET /api/profile/me/settings` | 응답의 `phoneNum` 제거. 기존 `email` 사용. |
| `GET /api/admin/verification/documents`, `GET /api/admin/verification/documents/{submissionId}` | `phoneNum`을 `email`로 교체. |
| `GET /api/gifticons/home` | 응답의 `phoneNum`을 `email`로 교체. |
| `POST /api/gifticons/purchases/confirm` | `recipientPhone` 대신 선택적 `recipientEmail` 사용. 미입력/공백이면 구매자 가입 이메일을 사용. 앞뒤 공백 제거, 이메일 형식 및 최대 255자 검증. 수신 주소가 유효하지 않으면 `400 / 47003`, 요청 필드 형식 오류는 `400 / 40000`. |

구매 요청은 알 수 없는 필드를 거부한다. 구버전 클라이언트가 `recipientPhone`을 보내면 주문을 거절하여 수신 정보가 무시된 채 구매자에게 발송되는 일을 막는다. 회원가입 화면과 관리자 화면을 포함한 클라이언트도 새 계약에 맞춰 전화번호 입력·표시·전송을 제거해야 한다.

```json
{
  "productId": 10,
  "quantity": 1,
  "spendPoints": 1000,
  "clientRequestId": "unique-order-id",
  "recipientEmail": "recipient@example.com"
}
```

자기 구매는 `recipientEmail`을 생략할 수 있다. 실제 수신 이메일은 구매 시점에 저장한다. 같은 `clientRequestId`의 재시도는 기존 구매의 이메일 스냅샷과 비교하며 수신 이메일이 달라지면 `409 / 47901`로 거절한다.

## 관리자 엑셀 처리

서버는 기프티콘을 직접 구매하거나 사용자에게 발송하지 않는다. 기존처럼 구매요청을 엑셀로 내보내 관리자에게 메일로 전달한다. 상품 공급사 클라이언트는 상품 목록 동기화만 수행한다.

- `buyerPhone` 제거, `recipientPhone`을 `recipientEmail`로 교체.
- `buyerEmail`: 구매자 이메일 스냅샷. 문의 시 사용.
- `recipientEmail`: 실제 외부 구매·발송에 사용할 이메일.
- `deliveryStatus=READY`: 유효한 수신 이메일이 있는 주문.
- `deliveryStatus=EMAIL_REQUIRED`: 기존 선물 주문 등 수신 이메일을 확정할 수 없는 주문. 발송을 보류하고 구매자에게 수신 이메일을 확인한다. 구매자 이메일로 임의 대체하지 않는다.

이메일 확인이 필요한 주문도 엑셀에 포함하므로 관리자에게 누락되지 않는다. 확인한 주소는 관리자 작업용 엑셀에 기입한 뒤 처리한다. 확인되지 않은 주문을 발송 완료로 취급하지 않는다. `export_batch_id`/`exported_at`은 엑셀 추출 여부이며 발송 완료 여부가 아니다.

관리자 메일 발송 재시도와 배치 상태 추적은 유지한다. 재시도 대기 중인 기존 배치의 서버 엑셀에 옛 컬럼이 있으면, 전환된 DB 데이터를 사용해 이메일 형식으로 파일을 재생성한 뒤 발송한다. 엑셀 행의 `deliveryStatus`는 수신 이메일 확인 여부이며, DB 배치의 `delivery_status`는 관리자 메일 전송 상태로 서로 다른 값이다.

## 기존 DB 전환과 배포

1. 클라이언트의 전화번호 입력·표시·전송을 제거하고 새 이메일 필드에 맞춘 버전을 준비한다. 구버전 구매 요청은 서버가 거절하므로 해당 버전의 구매 기능을 차단하거나 업데이트를 유도한다.
2. 구버전 서버와 기프티콘 스케줄러를 중지해 마이그레이션 중 주문·엑셀 생성이 발생하지 않게 한다. 운영 복제본에서 V13을 먼저 검증하고 기존 주문의 처리 상태와 필요한 수신 이메일을 확인한다.
3. 새 서버 배포 시 Flyway가 V13을 실행한다. 이미 배포된 V0~V12는 수정하지 않는다. 사용자 계정·비밀번호·상태·포인트·구매 내역은 유지하고 전화번호 3개 컬럼과 저장값을 삭제한다. 기존 전화번호가 있는 사용자에게 재가입을 요구하지 않는다.
4. V13은 기존 구매자 이메일 스냅샷을 우선 사용한다. 스냅샷이 없으면 계정 이메일을 사용한다. 본인 수신을 확인할 수 있는 주문만 수신 이메일을 채우며, 다른 사람의 전화번호로 지정한 선물 주문과 이메일이 없는 주문은 수신 이메일을 비워 둔다.
5. 아래 조회로 이메일 확인 대상과 기존 추출 여부를 확인한다. 기존 배치와 전송 상태는 유지한다. `READY` 배치는 정해진 메일 재시도를 계속하고, `SUBMITTED`/`LEGACY_UNKNOWN` 배치는 다시 대기열에 넣지 않는다. 이미 관리자에게 보낸 과거 엑셀의 미처리 주문은 최신 이메일 정보를 기준으로 관리자가 별도로 정리한다.
6. 가입·로그인·환경설정·서류심사·구매·관리자 엑셀을 확인하고 스케줄러를 재개한다. 컬럼 삭제 후 구버전 서버를 다시 실행하면 호환되지 않는다. 장애 시 새 스키마에 맞는 코드로 수정 배포한다. 전화번호는 마이그레이션만 되돌려 복구할 수 없다.

```sql
SELECT purchase_id, user_id, buyer_email, recipient_email,
       export_batch_id, exported_at, admin_processed_at, admin_success
FROM gifticon_purchases
WHERE recipient_email IS NULL OR TRIM(recipient_email) = ''
ORDER BY requested_at;
```

DB 마이그레이션은 이미 생성된 서버 엑셀 파일, 관리자 메일의 첨부파일·다운로드본, DB 백업·로그의 전화번호까지 삭제하지 않는다. 기존 주문의 이메일 전환 및 중복 발송 여부를 대조한 후 해당 보관본을 운영 정책에 따라 정리해야 한다. 이 저장소 변경은 운영 DB·메일함·외부 발송 서비스에 직접 적용하지 않는다.

## 검증 범위

`gradlew.bat --offline test`로 전체 테스트를 실행한다. 회귀 테스트는 전화번호 없는 회원가입, 기본·선물 수신 이메일 저장, 주소 오류 시 포인트 미차감, 중복 요청 처리, 구버전 수신 필드 거절, 실제 XLSX 셀과 보류 상태를 검증한다.

`PhoneNumberRemovalMigrationTest`는 빈 스키마와 기존 사용자·주문 데이터에 V13 SQL 원문을 실행해 전화번호 컬럼 삭제, 이메일 전환, 계정·포인트·추출 상태 보존을 확인한다. 이 테스트는 H2 MySQL 모드 기준이며 실제 MySQL의 DDL·잠금 동작 및 V0~V13 전체 Flyway 실행 검증을 대신하지 않는다.
