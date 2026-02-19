# CamNecT Server

**CamNecT** 프로젝트의 백엔드 서버 레포지토리입니다.  
동문/재학생 네트워킹, 커뮤니티, 채팅, 알림(푸시/실시간) 등 핵심 기능을 Spring Boot 기반으로 제공합니다.

---

## 🧱 Tech Stack

[![My Skills](https://skillicons.dev/icons?i=java,spring,hibernate,gradle,mysql,aws,docker,nginx,firebase,github,githubactions&theme=light)](https://skillicons.dev)

| Category | Technology | Version |
| --- | --- | --- |
| Language | Java | 21 (권장) |
| Framework | Spring Boot | 3.5.x |
| ORM | Spring Data JPA / Hibernate | - |
| Build Tool | Gradle | 8.x |
| Database | MySQL | 8.0 |
| Infra | AWS (EC2/RDS/S3/CloudFront), Docker, Nginx | - |
| Realtime/Push | WebSocket(STOMP), Firebase Cloud Messaging | - |
| CI/CD | GitHub Actions | - |


## Server Architecture
<img width="1187" height="502" alt="image" src="https://github.com/user-attachments/assets/a5a8c94d-c62e-46ec-a8ea-36c9fb8ecfac" />


---

## ✅ Prerequisites

- JDK 21 (최소 17 이상)
- Docker Desktop (Compose 포함)
- IntelliJ IDEA (권장)

---

## 📁 Project Modules (패키지 기준)

서버는 도메인별로 패키지를 분리해 관리합니다.

- `domain/auth` : 로그인/회원가입/인증 플로우
- `domain/profile`, `domain/users` : 유저/프로필/온보딩
- `domain/community` : 게시글/댓글/좋아요/북마크
- `domain/chat` : 채팅/채팅요청/소켓 연결
- `global/notification` : 알림, FCM 푸시, 디바이스 토큰
- `global/storage` : S3/Presign/CDN URL 발급, 업로드 티켓
- `domain/verification` : 이메일/문서 인증
- 기타 `activity`, `portfolio`, `gifticon`, `point`, `home` 등  
  (실제 구조는 레포 트리 기준)

---

## ⚙️ Configuration

### 1) 환경변수 (.env)

레포 **루트(root)** 에 `.env` 파일을 두고 로컬 개발용 값을 채웁니다.

- DB 접속 정보
- JWT/암호화 키
- AWS 자격증명/버킷/리전
- Firebase Admin SDK 관련 값 등


### 2) Spring Profile

리소스에 `application.yml`, `application-local.yml`이 있으므로, 기본적으로는 application.yml을 사용하며,
s3를 쓰지 못하는 local환경에서 application-local을 사용하시면 됩니다.(application-local.yml은 현재 업데이트되지 않았습니다.)

- IntelliJ Run Config에서 `SPRING_PROFILES_ACTIVE=local`
- 또는 터미널에서 `-Dspring.profiles.active=local`

---

## 🚀 Local Quick Start

### 1) 프로젝트 클론

```bash
git clone https://github.com/Konkuk-KUIT/CamNecT-Server.git
cd CamNecT-Server
```

## 📚 API Docs

Swagger 설정이 포함되어 있으므로, 서버 실행 후 아래 경로로 확인합니다.

- `http://localhost:8080/swagger-ui/index.html`
- 'https://api.camnect.site'
로 오시면 swagger 리다이렉트 되어 있습니다.

---

## 🌿 Branch & Collaboration

- **`main`** : 운영 배포 브랜치 (푸시/머지 시 배포 파이프라인 트리거)
- **`develop`** : 통합 개발 브랜치

### 개인 개발 브랜치(계정 기반)

팀에서 **개인 작업을 먼저 쌓는 브랜치**를 따로 두는 경우, 아래 방식으로 운영합니다.

- **`develop.<githubId>`** : 개인 개발 브랜치 (예: `develop.mo-seung`, `develop.jisoo`)
  - 개인 작업은 우선 여기로 푸시
  - 기능 단위로 정리되면 `develop`로 PR

> 운영 방식 예시  
> `develop.<githubId>`에서 작업하다가, PR 올린 뒤 `develop`로 머지

---

### Workflow

1. Issue 생성
2. `develop.<githubId>` 브랜치에서 작업 후 push
3. 작업 완료 후 `develop`으로 Pull Request (PR)
4. 코드 리뷰 및 Merge (1개 이상의 Approve 후 Merge)
5. 배포가 필요할 때 `develop` -> `main`으로 Merge (자동 배포)

---

## 🚢 Deployment (EC2 + Docker)

운영 서버에서는 아래 파일 조합으로 올리는 방식을 권장합니다.

- `.env.prod`
- `docker-compose.prod.yml`

예시:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build


