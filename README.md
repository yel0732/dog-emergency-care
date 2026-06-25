# 🐶 구해줘 멍즈

> 반려견 응급 상황, 위험 음식 섭취, 병원 탐색을 하나의 흐름으로 연결하는 반려견 케어 서비스

구해줘 멍즈는 보호자가 반려견의 이상 증상이나 위험 음식 섭취 상황에서 빠르게 응급도를 확인하고, 필요한 대처 방법과 주변 동물병원 정보를 찾을 수 있도록 돕는 웹 서비스입니다.

---

## 🧭 서비스 소개

반려견이 갑자기 아프거나 위험한 음식을 먹었을 때 보호자는 병원에 가야 하는지, 당장 어떤 조치를 해야 하는지 판단하기 어렵습니다.
구해줘 멍즈는 증상 입력부터 응급도 확인, 수의사 전달 리포트, 병원 지도, 음식 안전 정보까지 한 번에 연결해 보호자의 초기 대응을 돕습니다.

---

## ✨ 주요 기능

| 아이콘 | 기능 | 설명 |
|:---:|---|---|
| 🚨 | 응급 체크 | 증상, 반복 횟수, 섭취 음식 등을 입력해 응급도를 확인합니다. |
| 🧾 | 수의사 리포트 | 응급 체크 결과를 수의사에게 전달하기 좋은 형태로 정리하고 PDF로 확인합니다. |
| 🏥 | 병원·지도 | Kakao 지도에서 주변 동물병원을 확인하고 길찾기로 연결합니다. |
| 🕒 | 영업 정보 | 병원 영업시간, 야간, 주말, 24시 여부를 확인합니다. |
| 🍫 | 음식 안전 | 음식별 위험 이유, 관찰 증상, 대처 방법, 참고 자료를 제공합니다. |
| 🎥 | 응급 영상 | 응급 처치 영상을 검색하고 북마크할 수 있습니다. |
| 🐕 | 반려견 관리 | 반려견 정보와 관리 기록을 등록하고 확인합니다. |
| 💬 | 응급 사례 | 보호자들의 사례를 공유하고 댓글로 소통할 수 있습니다. |

---

## 🔄 사용자 흐름

```text
1. 반려견 정보 등록
2. 증상 또는 위험 음식 입력
3. 응급도 및 대처 방법 확인
4. 리포트로 상황 정리
5. 지도에서 주변 동물병원 확인
6. 길찾기 또는 전화 연결
```

---

## 🛠️ 기술 스택

### Frontend

| 기술 | 용도 |
|---|---|
| Vue 3 | 화면 구성 |
| Vite | 프론트엔드 빌드 |
| Vue Router | 라우팅 |
| Pinia | 인증 및 상태 관리 |
| Axios | API 통신 |
| CSS | 반응형 UI 및 디자인 |

### Backend

| 기술 | 용도 |
|---|---|
| Java 17 | 백엔드 개발 언어 |
| Spring Boot 3.5 | API 서버 |
| Spring Security | 인증/인가 |
| JWT | 로그인 세션 관리 |
| MyBatis | DB 매핑 |
| Openhtmltopdf | 리포트 PDF 생성 |

### Database & External API

| 기술 | 용도 |
|---|---|
| MySQL | 사용자, 반려견, 병원, 음식, 기록 데이터 저장 |
| Kakao Maps SDK | 지도 표시 및 마커 렌더링 |
| Kakao Local API | 병원 좌표 보정 |
| Google Places API | 병원 전화번호/영업시간 보강 |
| GMS/OpenAI API | 응급 체크 결과 요약 |

---

## 🧱 아키텍처

```text
Vue 3 Frontend
   ↓ REST API
Spring Boot Backend
   ├─ Auth / JWT
   ├─ Emergency Check
   ├─ Food Safety
   ├─ Hospital Map
   ├─ Pet Care Records
   └─ Case Board
   ↓
MySQL

External APIs
   ├─ Kakao Maps / Local
   ├─ Google Places
   └─ GMS/OpenAI
```

---

## 📁 프로젝트 구조

```text
07_FINAL
├─ frontend
│  ├─ src
│  │  ├─ api
│  │  ├─ components
│  │  ├─ router
│  │  ├─ stores
│  │  ├─ styles
│  │  └─ views
│  ├─ package.json
│  └─ vite.config.js
├─ src/main/java/com/ssafy/rescuemungz
│  ├─ auth
│  ├─ user
│  ├─ pet
│  ├─ emergencycheck
│  ├─ emergencyvideo
│  ├─ foodsafety
│  ├─ hospital
│  ├─ caseboard
│  └─ videoreview
├─ src/main/resources
│  ├─ application.properties
│  ├─ schema.sql
│  ├─ data.sql
│  └─ static
└─ docs
```

---

## 🔐 환경변수 설정

`.env.example`을 복사해 `.env` 파일을 생성합니다.

```powershell
Copy-Item .env.example .env
```

<<<<<<< HEAD
필수 설정:

```properties
DB_URL=jdbc:mysql://localhost:3306/rescue_mungz?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password

JWT_SECRET=put-your-long-random-jwt-secret-here
JWT_EXPIRATION_SECONDS=7200

KAKAO_REST_API_KEY=put-your-kakao-rest-api-key-here
VITE_KAKAO_JS_KEY=put-your-kakao-javascript-key-here
GOOGLE_PLACES_API_KEY=put-your-google-places-api-key-here

GMS_API_KEY=put-your-gms-api-key-here
GMS_BASE_URL=https://gms.ssafy.io/gmsapi
GMS_CHAT_COMPLETIONS_PATH=/api.openai.com/v1/chat/completions
GMS_MODEL=gpt-4o-mini
```

> `.env`는 Git 추적 대상이 아닙니다. 실제 키와 비밀번호는 `.env`에만 보관하세요.

---

## 🗄️ DB 준비
=======
`.env`는 `.gitignore`에 포함되어 있으므로 GitHub에 올라가지 않습니다. 실제 DB 계정, 비밀번호, API 키는 `.env`에만 작성하세요.

즉, MySQL URL, username, password는 코드에 고정하지 않고 `.env` 또는 실행 환경 변수에서만 가져옵니다.
>>>>>>> 43c43326f500d02e45e270e2d0d0b42810b6b87a

MySQL에서 데이터베이스와 사용자를 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS rescue_mungz
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'your_mysql_username'@'localhost'
IDENTIFIED BY 'your_mysql_password';

GRANT ALL PRIVILEGES ON rescue_mungz.* TO 'your_mysql_username'@'localhost';
FLUSH PRIVILEGES;
```

초기 데이터를 넣어야 할 경우 `.env`에서 아래 값을 사용합니다.

```properties
SQL_INIT_MODE=always
```

초기 데이터 입력 후에는 중복 삽입을 막기 위해 다시 변경하는 것을 권장합니다.

```properties
SQL_INIT_MODE=never
```

---

## 🚀 실행 방법

### Backend + 정적 Frontend 실행

```powershell
.\mvnw.cmd spring-boot:run
```

접속:

```text
http://localhost:8080
```

### Frontend 개발 서버

```powershell
cd frontend
npm install
npm run dev
```

접속:

```text
http://localhost:5173
```

### Frontend Build

```powershell
cd frontend
npm run build
```

빌드 결과는 Spring Boot가 제공하는 정적 리소스 경로로 생성됩니다.

```text
src/main/resources/static
```

---

## ✅ 테스트

### Backend

```powershell
.\mvnw.cmd test
```

### Frontend

```powershell
cd frontend
npm run build
```

---

## 🔌 주요 API

| 영역 | API |
|---|---|
| Auth | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Users | `POST /api/users`, `GET /api/users/me`, `PUT /api/users/me` |
| Pets | `GET /api/pets`, `POST /api/pets`, `PUT /api/pets/{id}` |
| Emergency Checks | `POST /api/emergency-checks`, `GET /api/emergency-checks/{id}` |
| Reports | `GET /api/emergency-checks/{id}/vet-report/pdf` |
| Food Safety | `GET /api/food-safety?keyword=`, `GET /api/food-safety/{id}` |
| Hospitals | `GET /api/hospitals`, `GET /api/hospitals/{id}` |
| Hospital Sync | `POST /api/hospitals/geocode`, `POST /api/hospitals/sync-hours` |
| Videos | `GET /api/emergency-videos`, `POST /api/emergency-videos/{id}/bookmark` |
| Case Board | `GET /api/case-posts`, `POST /api/case-posts`, `POST /api/case-posts/{id}/comments` |

---

## 🏥 병원 지도 데이터 안내

지도 마커는 병원 DB의 `lat`, `lng` 값이 있는 데이터만 표시합니다.
로컬과 다른 PC에서 마커 개수가 다르게 보인다면 코드 문제가 아니라 DB 좌표 보정 상태가 다른 경우가 많습니다.

확인 쿼리:

```sql
SELECT COUNT(*) AS total FROM hospitals;

SELECT COUNT(*) AS located
FROM hospitals
WHERE lat IS NOT NULL AND lng IS NOT NULL;

SELECT COUNT(*) AS missing
FROM hospitals
WHERE lat IS NULL OR lng IS NULL;
```

---

## 🛡️ 보안 메모

- `.env`는 Git에 올리지 않습니다.
- `.env.example`에는 placeholder 값만 유지합니다.
- DB 계정, JWT secret, Kakao/Google/GMS API 키는 로컬 환경변수 또는 배포 환경변수로 관리합니다.
- 공개 저장소에 실제 키가 포함되지 않도록 커밋 전 반드시 확인합니다.

---

## 📚 문서

- [`docs`](./docs): 기획, 산출물, ERD, 화면 설계 자료
- [`docs/ERD`](./docs/ERD): 도메인별 ERD 이미지
