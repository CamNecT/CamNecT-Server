# 크롤러 Discord 장애 알림

대상: `KUIT_6th_캠넥트`의 `📄be-백엔드` (채널 ID `1426565454343831572`).
이 문서와 템플릿은 배포 완료를 뜻하지 않는다. 별도 알림 스택을 만든 후 기존 크롤러 경보에 연결해야 한다.

## 구성

CloudWatch 크롤러 ErrorAlarm → SNS → 알림 전용 Lambda → Discord 전용 웹후크.

- `notifications.yaml`은 크롤러와 분리된 SAM 스택이다. 크롤러 실행 코드/권한/스케줄을 바꾸지 않는다.
- `camnect/konkuk-job/discord-webhook` Secrets Manager secret에 `{"webhookUrl":"<전용 웹후크 URL>"}`을 저장한다. URL을 Git, 셸 명령줄/이력, 로그, CloudFormation 매개변수에 넣지 않는다. 비밀 입력은 `getpass` 또는 보안 입력 UI를 사용한다.
- 알림 함수는 지정한 secret만 읽는다. 원문 저장소, 활동 DB, 등록 서명키 접근 권한은 없다.
- SNS 발행 정책은 지정한 계정·크롤러 경보만 허용하고, Lambda 호출은 전용 SNS 토픽으로 제한한다. 런타임에서도 계정·토픽·경보 식별자를 검증한다.
- URL은 Discord HTTPS 웹후크만 허용하고 리다이렉트를 거부한다. `wait=true`로 메시지 저장 응답과 채널 ID를 확인한다.
- 알림에는 경보 상태·시각·AWS 콘솔 링크만 넣는다. 원문, 사용자 정보, 비밀값, 원시 오류 메시지는 전달하지 않는다. `@everyone` 등 모든 멘션은 차단한다.
- ALARM 진입 및 ALARM→OK 전환만 통지한다. 초기 OK와 일반 정상 실행은 통지하지 않는다.
- 기존 오류 경보는 6시간 오류 합계와 `TreatMissingData=notBreaching`을 사용한다. **OK는 오류 지표 해제이며 수집 재성공을 보장하지 않는다.** 메시지에도 이를 명시한다. 이번 연결은 이 판정 기준을 바꾸지 않는다.
- Lambda는 전송 오류를 최대 2회 재시도한다. 최종 실패는 SQS에 14일 보관한다. SNS→Lambda 전달 실패도 같은 큐에 보관한다. 큐와 CloudWatch 로그(30일)를 운영 점검에 포함한다. 웹후크 자체가 고장 나면 같은 Discord 경로로 장애를 통지할 수 없다.
- AWS의 최소 1회 전달 특성 및 전송 후 응답 유실로 드물게 같은 알림이 중복될 수 있다. 큐 재처리는 실제 Discord 수신 상태를 먼저 확인한다.

## 배포 및 확인

1. 계정 `117088935250`, 리전 `ap-northeast-2`와 대상 채널을 확인한다. `Camnect 크롤러 알림` 전용 웹후크와 secret을 만든다.
2. `camnect-konkuk-job` 스택의 `ErrorAlarm` 실제 ARN을 조회한다.
3. `sam build --template-file notifications.yaml` 후 별도 스택 `camnect-konkuk-discord-alerts`로 배포한다. `CrawlerAlarmArn`, `DiscordWebhookSecretArn`에는 ARN만 전달한다. IAM 권한 생성이 포함된다.
4. 알림 스택 출력의 `AlarmTopicArn`을 기존 크롤러 템플릿의 동일 매개변수에 연결한다. 기존 패키지 및 다른 스택 매개변수를 유지하고 변경 세트가 경보 동작만 바꾸는지 검토한다. 긴급 Lambda 코드 패치가 있었다면 이전 패키지로 덮어쓰지 않는다.
5. 전용 SNS 토픽에 실제 경보 형식의 테스트 메시지를 1회 발행한다. `ConnectionTest=true`를 넣어 `[연결 테스트 / 실제 장애 아님]`으로 표시한다. 테스트 때문에 운영 경보 상태를 강제로 변경하지 않는다.
6. Lambda `DELIVERED` 결과와 대상 Discord 채널의 실제 수신 메시지를 함께 확인한다. 경보 AlarmActions/OKActions와 SNS 구독 상태도 읽어서 확인한다.

메시지 필드는 테스트 코드 `tests/test_discord_alert.py`의 `event()`를 참고한다. 테스트 전송도 채널에 공개되는 메시지이므로 지정된 채널 외에는 발행하지 않는다.

공식 문서: [CloudWatch와 SNS/Lambda](https://repost.aws/knowledge-center/sns-lambda-function-cloudwatch-alarm), [Discord 웹후크](https://discord.com/developers/docs/resources/webhook).
