# 자동부여봇 v1.0

건국대학교 취업팀 4084 게시판을 읽고 새 공고에 대한 태그 후보와 판정 근거를 로컬에 저장한다. Windows 작업 이름은 `CamNecT-AutoTagBot-v1`이다. 현재 사용자로 로그인한 PC에서 30분 간격과 로그인 시점에 실행된다. 실행에는 해당 계정의 Codex 로그인과 HTTPS 접근이 필요하다.

## 수집과 판정

처음 설치할 때 전체 게시판 목록을 읽어 기등록 ID를 기록한다. 이후 전체 목록에 새로 나타난 ID만 큐에 넣는다. 중복 게시와 재시작은 게시물 ID를 기준으로 처리한다. 같은 ID의 본문 수정은 재판정 대상에서 제외한다. 포스터나 첨부파일을 읽는 기능은 없으며, 추출된 본문이 비면 제목만 사용한다.

각 공고의 제목·본문을 고정한 후 `gpt-6-astra` 세 세션에 각각 동결된 프롬프트를 보낸다. 세 판정이 모두 유효해야 후보를 공개한다. 공고별 태그마다 3표는 `auto_candidates`, 정확히 2표는 `review_candidates`에 남긴다. 1표는 원본 투표 원장에서만 확인할 수 있다. 동결 판독기의 공개 태그 최대 4개와 실제 모집 직무 인덱스는 별도 필드로 보존한다. 실패한 표는 이후 실행에서 다시 요청하며, 완료된 표와 원문은 재사용한다.

## 설치 및 확인

저장소에 포함된 `install.ps1 -Python <Windows-python.exe> -Codex <codex.exe>`를 실행한다. Python은 Windows 인증서 저장소를 사용하는 3.11 이상과 같은 폴더의 pythonw.exe가 필요하다. 설치 프로그램은 실행 파일을 현재 사용자의 `%LOCALAPPDATA%\CamNecT\AutoTagBot\1.0`에 복사한다. 같은 버전의 실행 파일이 달라지면 설치를 중단한다. 운영 서버 연결 설정은 사용하지 않는다.

- 예약 실행 결과는 `Get-ScheduledTaskInfo -TaskName CamNecT-AutoTagBot-v1`으로 확인한다.
- `%LOCALAPPDATA%\CamNecT\AutoTagBot\state-4084-v1\candidates.json`에 원문 URL, 공고별 후보, 인용 근거 및 미완료 사유가 있다. `worker.log`에 수집 실패와 실행 시각을 남긴다. 같은 폴더의 `candidates.sqlite3`가 원문·판정자별 응답·세션 ID를 보관한다.
- 신규 공고 성능을 측정할 때 원문만 보고 독립 정답을 먼저 확정한다. 정답 JSON의 키에는 공고 ID, 값에는 태그 ID 배열을 넣고 태그가 없는 공고도 빈 배열로 포함한다. `python score.py candidates.json gold.json`은 3표 후보와 2표 이상 후보 각각의 TP·FP·FN, 정밀도·재현율, 누락·과잉 태그를 출력한다.
- 특정 기존 공고의 설치 점검은 `python worker.py smoke --state <별도폴더> --id <4084게시물번호> --codex <codex.exe>`로 실행한다. 이 결과는 신규 공고 성능 집계에서 제외한다.
- 중지는 `Disable-ScheduledTask -TaskName CamNecT-AutoTagBot-v1`, 재개는 `Enable-ScheduledTask -TaskName CamNecT-AutoTagBot-v1`로 한다. 원장은 다음 실행에 이어 쓴다.

## 회귀 검사

`python -B -m unittest discover -s tools/auto-tagbot-v1 -v`로 동결 규칙, 투표 분리, 기준점, 실패 후 재시도, 지표 계산을 검사한다. `frozen/final-reader-lock.json`과 판독기 파일들의 SHA-256을 실행 시 검증한다. `source-libs.zip`은 공식 게시판을 읽는 파서와 의존성을 포함한다. `build_source_libs.py`가 그 빌드 입력과 버전을 기록한다.
