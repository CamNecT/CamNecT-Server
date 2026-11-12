# CamNecT 자동 업로드

## 구현된 흐름

취업팀 4084 공고는 수집본을 저장한 직후 오프라인 판독기로 제목·본문을 분류한다. 기본값 `JOB_TAG_PUBLICATION_MODE=shadow`에서는 원문 근거와 부여 태그를 S3의 `tag-decisions/4084/<판독기 버전>/<공고 ID>/<수집 버전>-<해시>.json`에 보관한다. 활동 API 호출 횟수는 0이다. 학생공지 등록은 기존 경로를 사용한다.

표준 SAM 배포에서는 판독기와 `READER_VERSION`, `classify(row)`를 내보내는 `camnect_tagger.py`를 함수 패키지에 넣거나, `python/` 아래에 같은 파일들을 담은 Lambda 레이어의 버전 ARN을 `JobTagReaderLayerArn`에 전달한다. 판독기가 누락되거나 판정이 실패하면 게시가 차단된다.

향후 운영 쓰기를 켜는 경우에만 새 서버 경로와 `JOB_TAG_PUBLICATION_MODE=tagged`가 함께 필요하다. 승인 태그가 0개면 `AWAITING_TAGS`에 머문다. 태그가 있으면 서명한 요청 한 번으로 활동·태그 관계·수집 영수증을 한 트랜잭션에 저장한다. 아래의 업로드 설명은 이 모드에 해당한다.

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

관리자가 봇 관리 필드나 취업팀 태그를 수정하면 MANUAL_OVERRIDE를 반환한다. 삭제된 글은 원문 매핑을 남기고 DELETED로 응답한다. 북마크는 보존한다.

원문 버전과 매핑 코드 버전을 결합한 `sourceVersion * 1000 + mappingVersion`을 서버 revision으로 전송한다. 학생공지 매핑은 1이다. 취업팀 판독기 1.0은 2, 1.1은 3이므로 같은 수집본도 새 판독기 버전으로 다시 게시할 수 있다.

## 서버 설정

새 마이그레이션 `V20__Add_activity_imports.sql`과 서버 코드를 함께 배포해야 한다. 배포 전 최신 migration 번호 충돌을 확인한다. main의 V19 다음에 V20으로 적용한다. 별도 Draft PR #311의 V20/V21은 이 변경 이후 병합 시 V21/V22로 조정해야 하며, 이 배포에는 포함하지 않는다.

서버 환경 변수:

```text
APP_ACTIVITYIMPORT_ENABLED=true
APP_ACTIVITYIMPORT_AUTHORUSERNAME=camnect
APP_ACTIVITYIMPORT_SHAREDSECRET=<새로 생성한 충분히 긴 무작위 서명키>
```

`app.activity-import.*`를 환경 변수로 바꿀 때 하이픈을 제거하는 것은 [Spring Boot 바인딩 규칙](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)에 따른다. 설정/키가 없으면 새 API는 닫힌 상태다. 실제 키를 Git·명령 출력·문서에 기록하지 않는다.

취업팀은 `POST /api/internal/activity-imports/konkuk-job/tagged`에서 전용 HMAC 인증을 사용한다. `X-Crawler-Timestamp`는 Unix 초이며 허용 시차는 5분이다. `X-Crawler-Signature`는 다음 문자열의 HMAC-SHA256 소문자 hex다.

```text
{timestamp}\nPOST\n/api/internal/activity-imports/konkuk-job/tagged\n{SHA256(rawBody)}
```

## 봇 설정과 배포

새 AWS 계정에 `{"sharedSecret":"<서버와 같은 키>"}` 형식의 Secrets Manager Secret을 보관한다. `template.yaml` 배포 인자는 다음과 같다.

- `ExpectedAccountId=117088935250`
- `CamnectApiBaseUrl=https://api.camnect.site`
- `CamnectImportSecretArn=<위 Secret의 ARN>`
- 최초 `ScheduleState=DISABLED`, 수동 검증 완료 후 `ENABLED`

서버 API와 migration을 먼저 배포하고, 봇을 배포한 뒤 수동 실행에서 `publicationResults`와 실제 활동 목록을 확인한다. 이후 6시간 스케줄을 활성화한다. 현재 커밋/PR/운영 배포·서명키 생성·운영 계정 조회는 이 검증 기록 기준으로 수행하지 않았다.

로컬 테스트 API에 취업팀 공고를 실제 전송하는 설정 예시는 다음과 같다. 판독기 모듈은 Python 경로에서 접근 가능해야 한다.

```text
JOB_TAG_PUBLICATION_MODE=tagged
CAMNECT_API_BASE_URL=http://localhost:8080
CAMNECT_IMPORT_SECRET=<로컬 테스트 서명 키>
```

이미 수집한 데이터만 전송할 때:

```powershell
$env:PYTHONPATH = (Resolve-Path src).Path
.venv/Scripts/python -m konkuk_job --data-dir data/local --publish-only --max-articles 49 --allow-local-api
```

HTTP는 명시적인 `--allow-local-api`와 loopback 주소를 함께 사용한 테스트에서만 허용한다. 리다이렉트로 서명 요청을 다른 서버에 넘기지 않는다.

## 테스트

`ActivityImportIntegrationTest`는 실제 Spring HTTP 서버, H2 DB, 로컬 파일 저장소를 사용한다. 서명 검증, 활성 관리자 확인, 생성/변경/중복 재시도, 동시 업로드, 삭제/수동 편집 보호, 일반 사용자 인증 유지, 기존 목록·상세 API 노출을 검증한다.

수집본 49개가 로컬 `data/smoke`에 있을 때 `CRAWLER_LIVE_CAPTURE=true`, `CAMNECT_TEST_READER_PATH=<배포용 판독기 모듈 경로>`로 통합 테스트를 실행할 수 있다. 태그가 있는 공고 수와 실제 H2 목록 노출 수가 일치하는지, 재전송할 때 같은 수만큼 UNCHANGED가 나오는지 확인한다.
