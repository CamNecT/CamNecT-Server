# CamNecT 자동 업로드

## 구현된 흐름

정기 수집 → 원문/미디어와 버전 저장 → HMAC 서명 요청 → 서버의 `external_activities` 등록/갱신 → 기존 대외활동 목록·상세 API 노출 → 활동 ID/전달 결과 저장.

사용자 선택에 따라 작성자 계정은 **username `camnect`**가 기본이다. 활성 ADMIN 계정이어야 하며 일반 계정을 임의 승격하거나 새 계정을 자동 생성하지 않는다. `author-user-id`를 명시하면 해당 ID가 우선한다. 계정 이름을 지정하는 것은 표시 이름 문자열만 바꾸는 것이 아니라 실제 작성자 관계를 연결하는 작업이다.

## 게시 데이터

- 카테고리: EXTERNAL. 이 게시판의 글을 대외활동에 모은다.
- 제목, 본문 텍스트, 제목의 `[기관명]`이 있으면 기관명, 원문 주소.
- 본문 끝에 원문·신청/안내 링크·이미지/첨부 링크 보존.
- 다운로드한 첫 JPG/PNG/WebP를 기존 FileStorage로 저장하여 썸네일 연결. 운영 S3/CDN 경로가 적용된다.
- 기간 미확정 글도 자동 게시한다. 시작일/마감일은 추정하지 않으며 원문 확인 문구를 넣는다.
- `applyEndAt`은 UTC Instant로 상세 API에 추가한다. 기존 `applyEndDate`도 유지한다.
- 명확하게 마감된 글은 CLOSED로 등록한다. 이미 올라간 글도 다음 전송 시 마감 여부를 재계산한다. 실시간 정각 마감 스케줄러는 별도 기능이며 현재는 수집 주기에 따른 반영이다.
- 이미지 OCR은 미구현이다. 긴 본문은 MySQL TEXT 용량에 맞게 자르고 원문 안내를 남긴다.

## 중복·재시도·수동 편집

`activity_imports.source_id = KONKUK_JOB_4084#{articleId}`가 원문과 서비스 활동을 연결한다. 작성자 행 잠금과 DB 트랜잭션으로 동시 최초 등록을 직렬화한다.

같은 데이터의 재전송은 UNCHANGED, 새 버전은 UPDATED다. 오래된 버전은 STALE로 거절한다. 서버 처리 후 응답이 유실되어도 동일 요청 재전송으로 중복 생성하지 않는다. 업로드가 실패하면 원문/버전은 남으며 영수증은 기록하지 않아 다음 수집에서 우선 재시도한다.

관리자가 봇 관리 필드를 수정하면 MANUAL_OVERRIDE를 반환해 해당 글을 덮어쓰지 않는다. 삭제된 글은 원문 매핑을 남기고 DELETED로 응답하여 다시 생성하지 않는다. 태그와 북마크는 봇이 변경하지 않는다. MANUAL_OVERRIDE/DELETED는 실패 재시도로 덮어쓸 대상이 아니다.

원문 버전과 매핑 코드 버전을 결합한 `sourceVersion * 1000 + MAPPING_VERSION`을 서버 revision으로 전송한다. 매핑 계약이 바뀌면 MAPPING_VERSION을 증가시키며 1000 미만을 유지한다.

## 서버 설정

새 마이그레이션 `V20__Add_activity_imports.sql`과 서버 코드를 함께 배포해야 한다. 배포 전 최신 migration 번호 충돌을 확인한다. main의 V19 다음에 V20으로 적용한다. 별도 Draft PR #311의 V20/V21은 이 변경 이후 병합 시 V21/V22로 조정해야 하며, 이 배포에는 포함하지 않는다.

서버 환경 변수:

```text
APP_ACTIVITYIMPORT_ENABLED=true
APP_ACTIVITYIMPORT_AUTHORUSERNAME=camnect
APP_ACTIVITYIMPORT_SHAREDSECRET=<새로 생성한 충분히 긴 무작위 서명키>
```

`app.activity-import.*`를 환경 변수로 바꿀 때 하이픈을 제거하는 것은 [Spring Boot 바인딩 규칙](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)에 따른다. 설정/키가 없으면 새 API는 닫힌 상태다. 실제 키를 Git·명령 출력·문서에 기록하지 않는다.

별도의 `POST /api/internal/activity-imports/konkuk-job` 경로만 사용자 JWT 대신 전용 HMAC 인증을 사용한다. 나머지 사용자 API의 인증은 유지한다. `X-Crawler-Timestamp`는 Unix 초이며 허용 시차는 5분이다. `X-Crawler-Signature`는 다음 문자열의 HMAC-SHA256 소문자 hex다.

```text
{timestamp}\nPOST\n/api/internal/activity-imports/konkuk-job\n{SHA256(rawBody)}
```

## 봇 설정과 배포

새 AWS 계정에 `{"sharedSecret":"<서버와 같은 키>"}` 형식의 Secrets Manager Secret을 보관한다. `template.yaml` 배포 인자는 다음과 같다.

- `ExpectedAccountId=117088935250`
- `CamnectApiBaseUrl=https://api.camnect.site`
- `CamnectImportSecretArn=<위 Secret의 ARN>`
- 최초 `ScheduleState=DISABLED`, 수동 검증 완료 후 `ENABLED`

서버 API와 migration을 먼저 배포하고, 봇을 배포한 뒤 수동 실행에서 `publicationResults`와 실제 활동 목록을 확인한다. 이후 6시간 스케줄을 활성화한다. 현재 커밋/PR/운영 배포·서명키 생성·운영 계정 조회는 이 검증 기록 기준으로 수행하지 않았다.

로컬 CLI에서도 API 주소와 키를 환경에 주입하면 수집 후 자동 업로드한다.

```text
CAMNECT_API_BASE_URL=https://api.camnect.site
CAMNECT_IMPORT_SECRET=<서버와 같은 키>
```

이미 수집한 데이터만 전송할 때:

```powershell
$env:PYTHONPATH = (Resolve-Path src).Path
.venv/Scripts/python -m konkuk_job --data-dir data/local --publish-only --max-articles 49
```

HTTP는 명시적인 `--allow-local-api`와 loopback 주소를 함께 사용한 테스트에서만 허용한다. 리다이렉트로 서명 요청을 다른 서버에 넘기지 않는다.

## 테스트

`ActivityImportIntegrationTest`는 실제 Spring HTTP 서버, H2 DB, 로컬 파일 저장소를 사용한다. 서명 검증, 활성 관리자 확인, 생성/변경/중복 재시도, 동시 업로드, 삭제/수동 편집 보호, 일반 사용자 인증 유지, 기존 목록·상세 API 노출을 검증한다.

수집한 49개가 로컬 `data/smoke`에 있을 때 `CRAWLER_LIVE_CAPTURE=true`로 같은 테스트를 실행하면 실제 Python 게시기를 호출한다. 49개 전송 → 인증된 대외활동 목록 조회 → 49개 재전송 → UNCHANGED 49개를 확인한다. **테스트 DB 대상이며 운영 게시를 뜻하지 않는다.** 테스트 데이터가 없는 CI에서는 이 한 사례만 건너뛰고 나머지 통합 테스트를 실행한다.
