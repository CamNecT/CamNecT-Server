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

> 팁: 협업 편의상 `.env.example` 파일을 만들어 키 목록만 공유하는 방식을 추천드립니다.

### 2) Spring Profile

리소스에 `application.yml`, `application-local.yml`이 있으므로, 로컬에서는 아래 중 하나로 실행합니다.

- IntelliJ Run Config에서 `SPRING_PROFILES_ACTIVE=local`
- 또는 터미널에서 `-Dspring.profiles.active=local`

---

## 🚀 Local Quick Start

### 1) 프로젝트 클론

```bash
git clone <YOUR_REPO_URL>
cd CamNecT-Server
