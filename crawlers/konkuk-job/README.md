# 건국대 진로취업센터 수집기

`job.konkuk.ac.kr`의 외부행사 게시판(4084)을 수집하고 **CamNecT 대외활동(EXTERNAL)에 자동 등록/갱신**하는 모듈이다. 목록·상세·미디어를 보관한 뒤 서명된 서버 전용 API로 전송한다. 작성자는 사용자가 지정한 `camnect` 계정이며 서버가 활성 관리자 여부를 확인한다. API 설정 없이 로컬에서 실행할 때만 원문 보관 모드로 동작한다. AWS 함수는 자동 업로드 설정이 필수다.

**구현과 테스트 서버 검증은 완료했지만 운영 AWS 배포/활성화는 아직 하지 않았다.** 운영 반영 절차는 [AUTO_UPLOAD.md](AUTO_UPLOAD.md)에 있다.

## 실행

Python 3.12 또는 3.13. 아래 명령은 이 디렉터리에서 실행한다.

```powershell
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements-dev.txt
$env:PYTHONPATH = (Resolve-Path src).Path
.venv/Scripts/python -m unittest discover -s tests -v
.venv/Scripts/python -m konkuk_job --data-dir data/local --max-articles 20
```

Linux/macOS에서는 `.venv/bin/python`을 사용하고 `PYTHONPATH=src`를 지정한다.

```bash
PYTHONPATH=src .venv/bin/python -m konkuk_job --data-dir data/local --max-articles 20
```

다시 실행하면 기존 글도 재확인한다. 실행당 최대 20개이며, 미수집/가장 오래 확인하지 않은 글부터 처리한다. 전체 목록이 49개일 때 세 번 실행하면 정상 응답하는 모든 글을 확인할 수 있다. 새 글이 계속 추가되는 경우에는 전체 재확인까지 더 걸릴 수 있다.

`--max-pages` 기본 20, `--seconds` 기본 480. 시간·페이지·파일 크기 제한을 초과하면 실행 보고서에서 원인을 확인한다. 게시판 차단/429에서는 실행을 중단하며, 일반 게시글 구조 오류에서는 해당 글의 기존 데이터를 유지하고 다른 글을 처리한다. 오류가 있는 실행은 종료 코드 1을 반환한다.

네트워크 요청 없이 현재 데이터를 내보내려면:

```powershell
.venv/Scripts/python -m konkuk_job --data-dir data/local --export-only
```

## 결과

- `state.sqlite3`: 게시글별 최신 상태 및 버전 인덱스.
- `review-queue.json`: 검수용 최신 데이터. `record`에 제목·본문·신청기간 후보·링크·미디어 정보가 있다.
- `revisions/{articleId}/`: 변경 전후의 불변 JSON 기록.
- `blobs/`: 원문 HTML, 이미지 및 첨부파일. SHA-256 이름으로 중복 저장을 피한다.
- `runs/`: 성공/실패, 발견·처리 개수, 개별 실패 원인, 시작·종료 시각.
- `publications` 테이블 / 검수 큐의 `publication`: 업로드 목적지, 활동 ID, 전달한 버전, 서버 응답. 테스트 서버 영수증을 운영 업로드 완료로 취급하지 않는다.

원본 HTML은 보관 전용이다. 웹에서 그대로 렌더링하지 않는다. `bodyText`는 스크립트 등을 제거한 텍스트이며, 소비자는 텍스트로 표시해야 한다. 외부 사이트 링크는 주소만 기록하며 자동 방문하지 않는다. ZIP/HWP/PDF 등 첨부파일도 실행하거나 압축 해제하지 않는다.

## 데이터 처리

- `(source, articleId)` 기준으로 공지/일반 목록의 중복을 통합한다.
- 조회수·HTML 스타일 변화는 버전을 만들지 않는다. 정제 본문·추출 데이터·미디어 해시 변화는 새 버전을 만든다.
- 이미지/첨부 URL이 그대로여도 파일이 바뀌면 변경으로 기록한다. 동일 이미지 내용은 다시 저장하지 않는다.
- 목록에서 빠졌거나 읽기에 실패한 글을 삭제하거나 모집 종료로 처리하지 않는다.
- 신청 날짜는 `모집기간/접수기간/신청기간/마감` 문맥에서만 보수적으로 추출한다. 근거 문장을 함께 남긴다.
- 명확한 날짜와 시각이 있으면 `endAt`에 KST 오프셋을 보존한다. 시각이 없으면 `endAt=null`; 자정 마감을 추정하지 않는다.
- 연도 생략 범위·충돌·미확정 날짜는 검수 대상으로 남긴다. 등록일을 모집 시작일로 대입하지 않는다.
- 원문 추출의 `reviewStatus=PENDING` 및 `MEDIA_REVIEW`, `IMAGE_DOMINANT`, `APPLICATION_DATE_REVIEW`는 정보 정확도 검수 표식이다. 자동 등록을 막지 않는다. 날짜가 없으면 null과 원문 확인 안내로 게시한다. 실제 게시 결과는 별도 `publication`에 있다.
- 미디어는 글당 최대 8개, 파일당 8MB, 실행당 64MB이다. 8MB 초과 파일은 `TOO_LARGE`로 남기고 본문을 저장한다. 이 파일의 내용 변경 감지는 수행하지 못하므로 검수가 필요하다. 글당 8개를 넘으면 `MEDIA_LIMIT`로 표시한다. 실행 전체 한도에 도달하면 중단하고 다음 실행에서 미수집/오래된 글부터 재개한다.
- 외부 호스트의 미디어는 다운로드하지 않는다. 원문 호스트도 HTTPS만 허용하고 리다이렉트 대상에 같은 제한을 적용한다.

**OCR/LLM 이미지 분석은 아직 구현하지 않았다.** 현재는 본문과 원문/관련 자료 링크를 게시하고, 다운로드한 첫 JPG/PNG/WebP 이미지를 서비스 썸네일로 저장한다. 날짜만 확인되면 `applyEndDate`, 시각까지 확인되면 상세 응답의 `applyEndAt`에도 UTC 시각을 보존한다. 기존 필드는 유지된다. HTML을 그대로 게시하지 않는다.

## AWS 배포 구성

`template.yaml`은 새 계정 `117088935250`용 SAM 템플릿이다.

- Lambda Python 3.13/arm64, 동시 실행 1개, 최대 600초.
- EventBridge Scheduler 6시간 주기, **기본 DISABLED**.
- 비공개 S3에 원문/버전/실행 기록, DynamoDB에 최신 상태 저장.
- 수집기 권한은 해당 테이블 Get/Put, 해당 버킷 Get/Put, 지정된 서명키 Secret 읽기로 제한한다. 서비스 DB 자격 증명을 사용하지 않는다.
- S3/DynamoDB는 스택 삭제 시 보존. 실행 기록은 90일, CloudWatch 로그는 30일 보관한다.
- 오류 Alarm의 `AlarmTopicArn`에 구독 확인된 기존 SNS 토픽을 연결할 수 있다. 빈 값이면 실제 알림은 전송되지 않는다. 오류와 정상 복귀를 모두 통지한다.
- Discord 연결은 별도 `notifications.yaml`과 [알림 운영 문서](DISCORD_ALERTS.md)를 사용한다. 웹후크 URL은 Secrets Manager에만 저장하고 전용 Lambda가 SNS 경보를 변환한다.
- 동시 변경은 조건부 쓰기로 거절하고, 네트워크/수집 실패를 성공으로 기록하지 않는다.

AWS CLI와 SAM CLI를 준비하고 **새 계정 프로파일**로 확인한 후 실행한다. 이 문서는 배포 완료를 의미하지 않는다.

```bash
aws sts get-caller-identity --profile <new-account-profile>
sam validate --lint
sam build
sam deploy --guided --profile <new-account-profile> --region ap-northeast-2
```

최초 배포에서 `ScheduleState=DISABLED`를 유지하고 출력된 함수 이름으로 1회 수동 호출한다.

```bash
aws lambda invoke --function-name <FunctionName> --payload '{}' \
  --cli-binary-format raw-in-base64-out --profile <new-account-profile> \
  --region ap-northeast-2 invocation.json
```

호출 응답의 `FunctionError`가 없고 `invocation.json`의 `status`가 `SUCCESS`인지 확인한다. S3 실행 기록과 검수 데이터를 확인한 뒤 `sam deploy --parameter-overrides ExpectedAccountId=117088935250 ScheduleState=ENABLED`로 정기 실행을 켠다. 프로파일/리전은 최초 배포 때 저장한 값 또는 명시적인 인자로 지정한다.

템플릿은 계정 확인 규칙과 런타임 계정 확인을 모두 포함한다. 실제 배포 환경에서의 네트워크 접근과 IAM 동작은 수동 실행으로 추가 검증해야 한다.

참조: [Lambda Python 런타임](https://docs.aws.amazon.com/lambda/latest/dg/lambda-python.html), [SAM ScheduleV2](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-schedulev2.html), [Beautiful Soup](https://www.crummy.com/software/BeautifulSoup/bs4/doc/).

## 날짜·마감 및 실패 처리

- `부터/시작/개시`만 있는 날짜는 시작일로 보존하고 마감일을 만들지 않는다. 마감 시각은 한국 시간으로 파싱하며 오전/오후를 구분한다. 역전된 기간, 불완전한 시간, 연도 생략, 서로 충돌하는 날짜는 검토 대상으로 남긴다.
- 서버의 `ActivityImportExpiryScheduler`가 기본 60초 간격으로 확정된 마감을 처리한다. 원문 목록에서 빠진 글도 대상이다. 날짜만 있으면 해당 날짜가 지난 한국 시간 자정부터, 시각이 있으면 그 시각부터 마감한다. 실행 주기에 따른 최대 약 1분의 지연이 있다.
- 서버 배포가 필요하다. `app.activity-import.expiry-enabled=false`로 작업만 끌 수 있다. `app.activity-import.enabled=false`이면 수신과 마감 모두 중지된다.
- 관리자 수정·삭제를 보호하며, 정상 자동 마감 시 관리 해시도 함께 갱신한다. 일정 연장 수신과 마감이 겹쳐도 작성자→매핑→활동 순서의 잠금으로 최신 일정을 보존한다.
- 글별 원문 오류와 비정상 게시 요청은 격리하고 정상 글을 계속 처리한 뒤 실행을 실패로 보고한다. 재시도는 6→12→24시간 대기하며, 정상 글 우선 처리와 회차당 재시도 1자리 확보로 양쪽의 영구 대기를 방지한다. 보류 상태는 DynamoDB `FAILURE#...` 또는 로컬 `failures` 테이블에 남는다.
- 인증·서버 장애·요청 제한·게시 시간 예산 소진은 전체 실행을 중단한다. 잘못된 응답 형식·ID·버전 또는 STALE 응답을 전달 성공으로 기록하지 않는다. STALE은 원문 상태 복구가 필요하다.
- 실행 요약의 `deferred`, `articleErrors`, `publicationErrors`도 확인한다. 성공 실행만 확인하면 대기 중인 글의 문제를 놓칠 수 있다.

### 2026-11-01 사고 복구 도구

`python -m konkuk_job.repair --function-name <함수명> --expected-code-sha256 <배포 코드 해시>`는 기본 읽기 전용 미리보기다. `--apply`를 붙이면 검증된 원문 ID 1210375→활동 38, 1210911→활동 23만 복구한다. 계정·배포 해시·등록 대상·원문 해시·날짜 근거를 확인하고, 기존 S3/DynamoDB 버전 보관과 서명된 게시 API를 사용한다. 직접 SQL로 status만 바꾸면 관리자 수정으로 오인될 수 있으므로 이 절차를 사용한다.

이미 적용된 복구도 재실행할 수 있으며, 게시 중 오류가 발생하면 신규 원문 버전은 미전달 상태로 보존된다. 관리자가 수정·삭제한 글은 강제 복구하지 않는다. 저장된 원문으로 복구하므로 실행 전 현재 원문과 사고 원인이 동일한지 확인한다.

## 학생공지 RSS (238)

기존 취업팀 HTML 수집과 함께 6시간마다 처리한다. 학생공지는 [공식 피드](https://www.konkuk.ac.kr/bbs/konkuk/238/rssList.do?row=50) 50건에서 최근 30일 활동 제목을 선별하고 원문을 확인한다. 발견한 글은 게시일로부터 90일까지 재조회한다. 한 실행에 최대 8건을 순환하며, RSS에서 빠져도 삭제하거나 마감하지 않는다. 제목 필터가 있으므로 전수 등록이나 과거 전체 목록의 복원은 보장하지 않는다.

학생공지는 제목·출처·원문 링크와 확인된 신청기한을 활동으로 게시한다. 본문·첨부·포스터는 재게시하지 않는다. RSS 게시일과 고정 종료일은 마감 근거가 아니다. 같은 신청 범위에 시작 연도가 명시되고 축약 종료일의 요일이 일치할 때만 같은 해로 인정한다. 날짜만 있으면 `applyEndAt=null`, 근거가 없으면 날짜도 null이다. 원문 근거는 아카이브에 남긴다. 외부 OCR·모델 API는 사용하지 않는다.

로컬 확인: 게시용 환경 변수 없이 `PYTHONPATH=src python -m konkuk_job --board student --data-dir data/local`. 수신 경로는 `/api/internal/activity-imports/konkuk-student`로, 취업팀 서명을 재사용할 수 없다. 서버를 먼저 배포하고 기존 Lambda에 같은 버전의 수집기를 배포한다. `{"board":"student"}`로 호출해 `boards.student.publicationResults`와 활동 ID를 확인한다. 기본 이벤트는 두 게시판을 실행한다.
