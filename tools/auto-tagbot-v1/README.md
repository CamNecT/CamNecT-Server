# 자동부여봇 v1.0

운영 서버가 크롤러의 4084 등록 영수증과 연결된 공고를 5분마다 조회한다. 독립적인 규칙 판독기가 각 공고의 제목·본문에서 태그와 실제 모집 직무의 근거를 찾는다. 이미지를 읽는 기능은 없다. 최초 처리한 기존 공고는 `bootstrap`, 이후 새로 등록된 공고는 `live`로 표시한다. 수정 수집본도 revision과 content hash로 구분해 재처리한다.

## 서버에서 읽고 쓰는 데이터

- `camnect-tag-imports.service`가 서버의 기존 연결 설정을 사용해 운영 DB에서 읽기 전용 트랜잭션을 연다. 대상은 `activity_imports`의 `KONKUK_JOB_4084#` 영수증과 대응하는 `external_activities`뿐이다. 고정 SELECT로 /var/lib/camnect-tag-input에 입력 스냅샷을 만들고 성공 시 판독기를 호출한다.
- `camnect-auto-tagbot.service`는 전용 비권한 계정과 격리된 네트워크에서 작동한다. 모델·API 키·Docker 접근 권한이 필요 없다. `/var/lib/camnect-auto-tagbot/candidates.sqlite3`에 공고별 버전과 부여 태그를 기록하고 같은 폴더의 `candidates.json`으로 현재 결과를 내보낸다. 운영 앱에서 사용자에게 보이는 태그는 이 버전에서 갱신하지 않는다.
- `assigned_tag_ids`에는 판독기의 제안 중 로컬 개발 판정자 3명이 승인한 조건에 일치한 태그만 들어간다. 정확히 2명이 승인한 조건은 `review_candidates`에 담는다. 나머지 제안은 `other_suggestions`, 명시된 직무는 `role_index`에 원문 인용과 함께 보존한다. 승인 수는 **규칙 개발 단계**의 표다. 신규 공고의 런타임 판정 횟수를 뜻하지 않는다.

## 개발과 배포

로컬에서 `python -B -m unittest discover -s tools/auto-tagbot-v1 -v`로 검사한다. `offline_reader.py`는 근거 추출과 태그 제안을 담당하고 동결된 `frozen/model2/select_tags.py`와 사전을 사용한다. `server/policy.json`을 변경하면 개발 PC에서 `validate_policy.py --codex <실행파일> --out server/policy-review.json`으로 규칙을 다시 검토한다. 정밀도와 재현율은 별도 정답 표본으로 측정한다. `score.py candidates.json gold.json`은 독립 라벨을 준 신규 공고의 자동 후보와 검수 포함 후보를 각각 채점한다. gold.json은 공고 ID에서 태그 ID 배열로의 매핑이다.

배포 패키지에는 `server_worker.py`, `offline_reader.py`, 동결 사전·선택기, `server/`만 담는다. 서버의 /opt/camnect-auto-tagbot/releases 아래 커밋별 경로에 풀고 그 경로에서 `sudo sh server/install.sh`를 실행한다. `camnect-auto-tagbot.timer`의 다음 실행 시각과 두 서비스의 `Result`, `ExecMainStatus`를 확인한다. 처리를 멈추려면 `sudo systemctl disable --now camnect-auto-tagbot.timer`를 실행한다. 앱이나 기존 크롤러를 재시작할 필요가 없다.
