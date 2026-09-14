# 회원가입 흐름 및 Redis/RTR 점검

## 확인 범위와 원인

- 서버 기준: `main`의 `57fcc0a`, 작업 브랜치 `fix/signup-onboarding-flow`.
- FE 참고: [CamNecT-Web PR #167](https://github.com/CamNecT/CamNecT-Web/pull/167), `fix/146-RTR`의 `e061f85577c9364bc1c7060c87c6e6763bf5a35a`. 확인 당시 PR은 open이며 main/develop에는 RTR 코드가 아직 없었다. 실제 배포 버전은 별도 확인해야 한다.
- `a0b8cec`에서 태그/프로필의 tempToken 허용이 제거되고 온보딩이 승인 후로 제한됐다. 이는 Redis 세션 및 RTR 도입보다 앞선 변경이다. Redis 자체가 41106의 원인은 아니다.
- FE는 서류 제출 → 프로필/태그 선택 또는 건너뛰기 → 심사 대기 → 승인 후 완료 화면 흐름을 구현한다. 서버는 승인 전 온보딩을 거절했고, 승인 시 기존 온보딩 완료 플래그를 false로 재설정했다.
- 기존 서버는 `initial_setup_completed` 하나로 온보딩과 완료 안내를 함께 처리했다. FE 완료 화면의 시작하기 버튼은 로컬 nextStep만 HOME으로 바꾸므로 이후 로그인에서 완료 안내가 반복됐다.

## 서버에서 복구한 계약

| 상황 | 로그인 nextStep | 로그인 응답 토큰 |
| --- | --- | --- |
| 서류 없음 / 반려 / 취소 | DOCUMENT_REQUIRED | 가입용 VERIFICATION, refreshToken=null |
| ADMIN_PENDING + 서류 제출 + 온보딩 미완료 | ONBOARDING_REQUIRED | 가입용 VERIFICATION, refreshToken=null |
| ADMIN_PENDING + 온보딩 완료 | DOCUMENT_REVIEW_WAITING | 가입용 VERIFICATION, refreshToken=null |
| ACTIVE + 온보딩 미완료 | ONBOARDING_REQUIRED | ACCESS + REFRESH, Redis 세션 |
| 승인 전 온보딩 완료 → 승인됨 + 완료 안내 미발급 | VERIFICATION_COMPLETE | ACCESS + REFRESH, Redis 세션 |
| ACTIVE 상태에서 온보딩 성공 응답을 받음 | HOME | ACCESS + REFRESH, Redis 세션 |
| ACTIVE + 온보딩 완료 + 완료 안내 발급됨 | HOME | ACCESS + REFRESH, Redis 세션 |

관리자는 기존 ADMIN_DASHBOARD 분기를 유지한다. 정지/탈퇴는 기존 접근 제한을 적용한다. 비정상적으로 서류 APPROVED와 사용자 ADMIN_PENDING이 동시에 존재해도 ACCESS 세션을 발급하지 않는다.

`POST /api/auth/onboarding`은 `{}` 또는 선택값이 빈 요청도 완료로 기록한다. ADMIN_PENDING/ACTIVE 모두 가능하며 승인 상태를 변경하지 않는다. ACTIVE 성공 응답에서는 FE가 인증 완료 화면을 바로 표시하므로 `verification_complete_notified`도 같은 트랜잭션에서 true로 기록하고 다음 로그인부터 HOME을 반환한다. ADMIN_PENDING 완료는 안내 상태를 소비하지 않는다. 이미 완료된 요청의 재시도는 태그 삭제, 프로필 덮어쓰기, 업로드 티켓 재소비 없이 성공하며, ACTIVE로 응답할 때는 안내 완료를 기록한다. 이후 프로필 수정은 기존 프로필 수정 API를 사용한다.

관리자 승인은 인증 정보 및 사용자 상태를 갱신하며 기존 온보딩 완료 상태를 초기화하지 않는다. 로그인은 사용자 행 잠금 안에서 `verification_complete_notified`를 기록하므로 동시 로그인에도 완료 안내를 한 번만 발급한다. Redis 세션 생성이 실패하면 안내 발급 상태를 바꾸지 않는다.

완료 안내 처리 기준은 **ACTIVE 온보딩 성공 또는 로그인 응답의 VERIFICATION_COMPLETE 발급 시점**이다. 두 경로 모두 사용자 행 잠금 안에서 처리한다. 브라우저 렌더링 완료를 보장하는 확인 프로토콜은 아니므로 응답 유실 시 다음 로그인은 HOME일 수 있다. 실제 화면 확인까지 보장하려면 별도 FE 확인 API가 필요하다. 온보딩 미완료/실패는 안내 완료로 처리하지 않으며 ONBOARDING_REQUIRED를 유지한다.

`GET /api/auth/verification-complete`는 승인·온보딩을 마친 사용자가 조회/재시도할 수 있다. 안내 발급 플래그 때문에 GET이 409가 되지 않으며 GET에서 상태를 변경하지 않는다.

## 토큰 권한과 수명

| 토큰 | 허용 범위 | 갱신/폐기 |
| --- | --- | --- |
| tempToken (JWT type=VERIFICATION) | 가입용 문서 API, GET /api/tags, POST /api/profile/uploads/presign, POST /api/auth/onboarding | RTR 불가. 만료 시 재로그인. 비밀번호 변경, 정지/탈퇴 시 사용 불가 |
| accessToken (JWT type=ACCESS) | 기존 인증 API | 서명·만료와 Redis 세션 내 토큰 해시를 모두 검증 |
| refreshToken (JWT type=REFRESH) | POST /api/auth/refresh의 JSON 본문 | RTR 성공 시 새 ACCESS/REFRESH 쌍으로 교체. 구 REFRESH 재사용 시 해당 세션 폐기 |
| PASSWORD_RESET | 기존 비밀번호 재설정 절차 | 가입/일반 API의 Bearer 토큰으로 사용 불가 |

회원가입 이메일 인증 응답은 `tempToken` 필드에 VERIFICATION 토큰을 반환한다. **승인 대기 중 로그인 응답은 호환성을 위해 `accessToken`이라는 기존 필드에 VERIFICATION 토큰을 담는다.** 이름만 보고 정식 ACCESS로 판단하면 안 된다. 이 경우 `status=ADMIN_PENDING`, `refreshToken=null`이다.

tempToken의 신규 권한은 정확한 HTTP 메서드와 경로 세 개에만 추가했다. `/api/profile/**` 전체를 열지 않는다. 온보딩 중 관리자가 먼저 승인하더라도 가입용 세 API는 원래 만료 시점까지 사용 가능하다. 승인 후 문서 API, 일반 프로필/커뮤니티/관리자 API, logout/withdraw/refresh 권한으로 확대되지 않는다. 이 토큰으로 재로그인 없이 정식 ACCESS/REFRESH를 발급하는 교환 기능도 추가하지 않는다.

tempToken의 비밀번호 fingerprint 검증은 인터셉터와 `/api/auth` 인자 해석기에서 같은 로직을 사용한다. 정식 ACCESS/REFRESH의 Redis 해시 저장, 만료 처리, 원자적 회전, 이전 ACCESS의 남은 수명, 세션별 로그아웃·재사용 차단은 유지한다. Redis 장애를 인증 실패로 간주해 우회하거나 새 토큰을 발급하지 않는다.

JWT 만료·서명 오류는 일반 API와 `/api/auth` 모두 40100으로 통일했다. 폐기된/없는 Redis 세션은 41103, 타입 오용은 41106, REFRESH 재사용은 41107, REFRESH 만료는 41108, Redis 일시 장애는 50310을 유지한다.

## FE 실제 코드와 대조 — 2026-09-14

Web develop `bb9b49980e5a7b56fc7b78b94938464647aaddad`, main `72c3f655d38e938d64f23c38de1f87e7eb418976`을 확인했다. 두 브랜치의 온보딩 성공 분기는 같다. RTR PR #167도 병합된 상태로 확인했으며, 과거 검토의 미병합 상태와 구분한다. 실제 운영 FE 빌드 SHA까지 확인한 것은 아니다.

1. [InterestsStep](https://github.com/CamNecT/CamNecT-Web/blob/bb9b49980e5a7b56fc7b78b94938464647aaddad/src/pages/auth/InterestsStep.tsx#L104)은 저장 성공 후 bare 응답의 `result.status`를 부모에게 넘긴다.
2. [SignUpPage.handleOnboardingComplete](https://github.com/CamNecT/CamNecT-Web/blob/bb9b49980e5a7b56fc7b78b94938464647aaddad/src/pages/auth/SignUpPage.tsx#L44)는 ADMIN_PENDING이면 심사 대기(step 7), ACTIVE이며 정식 ACCESS/REFRESH 세션이 있으면 SchoolCompletion(step 8)으로 이동한다.
3. TEMP만 가진 ACTIVE 사용자는 인증 상세 완료 화면 대신 “승인이 완료되었습니다 / 다시 로그인해 주세요” 팝업을 본다. 요청한 ACTIVE 기준에 따라 이 경로도 다음 로그인은 HOME이다. 온보딩 성공 응답에서 새 정식 토큰을 발급하거나 TEMP의 권한을 확대하지 않는다.
4. SchoolCompletion은 `/api/auth/verification-complete`로 인증 정보를 읽고, 시작하기에서 로컬 nextStep을 HOME으로 변경한다. 서버의 안내 플래그가 true여도 인증 정보 GET은 가능하므로 완료 화면 조회를 막지 않는다.
5. LoginPage는 HOME 응답이면 스플래시 후 `/home`으로 이동한다. 승인 전 온보딩을 완료한 사용자는 기존처럼 승인 후 첫 로그인에서 VERIFICATION_COMPLETE를 받고, 그 이후 HOME으로 이동한다.

FE 코드의 경로·응답 타입·성공 콜백을 대조한 결과이며 브라우저 E2E 실행 결과는 아니다. 이 PR은 서버를 수정한다. 여러 탭의 refreshToken 재사용 시 해당 sid의 최신 ACCESS/REFRESH가 함께 폐기되는 기존 정책도 유지한다.

## 엄격 재검토에서 추가 확인한 사항

- **서류 취소와 승인 경합 수정:** 취소가 잠금 없이 PENDING을 읽으면 관리자가 승인한 뒤에도 오래된 엔티티로 CANCELED를 덮어쓰고 승인 서류 파일을 삭제할 수 있었다. 취소도 승인과 동일한 서류 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 소유자와 PENDING 여부를 확인하도록 수정했다. 취소 트랜잭션을 커밋 직전에 멈추고 승인을 실행하는 테스트에서 기존 코드의 실패와 수정 후 통과를 확인했다. 취소가 먼저 잠그면 승인은 42910으로 거절되고 계정은 ADMIN_PENDING을 유지한다.
- **실제 커밋 경계 검증:** 테스트 전체를 하나의 롤백 트랜잭션에 넣는 방식 외에 요청마다 독립적으로 커밋하는 테스트를 추가했다. 이미지 티켓 USED 처리, 태그·소개 저장, 승인 후 정보 보존, 재시도 시 이미지 중복 소비 방지, 업로드 실패 시 태그 교체 롤백, 동시 승인·온보딩, 동시 로그인 완료 안내 1회를 확인했다. 외부 S3·메일·파일 삭제는 테스트 대역을 사용한다.
- **토큰 경계 유지:** TEMP를 일반 ACCESS로 승격하거나 REFRESH 대신 사용하지 않는다. 승인 직후에도 원래 3일 유효기간 내 가입용 세 API만 허용하며, 온보딩 재시도는 완료된 프로필을 덮어쓰지 않는다. Redis 회전·재사용 차단과 ACCESS 만료 기본값은 변경하지 않았다. ACCESS 기본값은 현재 2일이며 RTR이 실제 FE 배포에 포함된 뒤 만료 정책을 별도 조정해야 한다.
- **기존 데이터의 한계:** 구버전 승인 과정에서 이미 `initial_setup_completed=false`로 되돌아간 계정은 이 플래그만으로 실제 미완료 계정과 구분할 수 없다. V14에서 선택 프로필이나 태그 유무로 추측해 true로 바꾸지 않는다. 해당 계정은 온보딩을 다시 완료하거나, 확인 가능한 기록을 근거로 별도 데이터 보정이 필요하다.
- **완료 안내의 의미:** 2026-09-14 수정으로 ACTIVE 온보딩 성공 시에도 안내 완료를 기록한다. 해당 FE 화면과 다음 로그인에서 완료 안내가 중복되지 않는다. 응답 유실까지 포함한 실제 화면 표시 1회를 보장하려면 FE 확인 API 계약이 추가로 필요하다.

## 배포 및 검증

- 엄격 재검토와 추가 수정 후 로컬 전체 회귀 테스트 **496개(123개 클래스)**가 실패/오류/스킵 없이 통과했다. 로그는 `build/signup-strict-full-test.log`, 집계는 `build/signup-strict-full-test-summary.json`에 있다.
- 통합 테스트는 실제 MVC 인증, JWT 발급/검증, DB 상태 전이, 관리자 승인, 동시 로그인을 포함한다. Redis 세션은 테스트 프로필의 in-memory 구현과 기존 Redis 스크립트 단위 테스트로 검증했다. 실제 Redis 서버에서 Lua를 실행하거나 실제 S3/MySQL/운영 서버에 요청한 검증은 아니다. presign은 테스트 전용 자격값으로 로컬에서 서명한다.

- V14는 새 컬럼을 추가하고 기존 `initial_setup_completed`와 사용자 상태를 보존한다. 기존 ACTIVE/SUSPENDED + 온보딩 완료 계정은 안내 발급됨으로 보정하여 HOME 동작을 유지한다. 선택 프로필이 비었는지로 온보딩 완료를 추정하지 않는다.
- 테스트 H2는 JPA 스키마를 사용한다. V14 SQL 자체도 별도 H2 MySQL 모드 테스트로 검증한다. 실제 MySQL 적용 및 운영 Redis 연결 상태는 스테이징에서 확인해야 한다.
- 구버전 서버가 승인 시 초기 설정을 다시 false로 만들 수 있으므로 신구 버전 혼재 상태에서 가입/승인 트래픽을 처리하지 않는다.
- UT 전에는 온보딩 먼저 → 승인 → 첫 로그인 COMPLETE → 재로그인 HOME과, 승인 먼저 → 온보딩 성공/FE 완료 화면 → 첫 로그인부터 HOME을 각각 확인한다. ACTIVE 온보딩 실패 시 ONBOARDING_REQUIRED 유지와 성공 응답 재시도의 프로필 보존도 확인한다.
- ACCESS 만료 후 RTR 재시도, 구 REFRESH 재사용 차단, deviceId를 포함한 로그아웃, tempToken의 일반 API 차단도 확인한다.
