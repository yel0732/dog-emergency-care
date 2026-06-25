# 구해줘 멍즈

반려견 보호자를 위한 AI 응급 대처 안내 및 케어 기록 서비스입니다. 보호자는 반려견 정보를 등록하고, 증상과 의심 음식/물질을 입력해 응급도를 확인할 수 있습니다. 결과는 수의사에게 전달하기 좋은 PDF 리포트로 저장할 수 있으며, 음식 안전 검색, 주변 동물병원 검색, 응급처치 영상, 응급 사례 게시판, 관리 기록 기능을 함께 제공합니다.

## 주요 기능

- 회원 가입, 로그인, JWT 기반 인증
- 반려견 등록, 수정, 삭제 및 다중 반려견 관리
- 증상 기반 AI 응급 체크와 위험도 안내
- 수의사 전달용 PDF 리포트 다운로드
- 음식 안전 검색과 위험도/급여 방법 안내
- 응급처치 영상 검색, 리뷰, 찜 기능
- 찜한 영상과 프로필을 관리하는 마이페이지
- 응급 사례 게시판, 댓글, 답글, 팔로우 기능
- 관리 기록 달력과 반려견별 케어 일정 관리
- 동물병원 검색, 야간/응급 필터, Kakao 길찾기 연결

## 기술 스택

### Backend

- Java 17
- Spring Boot 3.5.0
- Spring Security
- JWT
- MyBatis
- MySQL
- Spring AI
- Openhtmltopdf
- H2 for tests

### Frontend

- Vue 3
- Vue Router
- Pinia
- Axios
- Vite

## 프로젝트 구조

```text
.
├── src/main/java/com/ssafy/rescuemungz
│   ├── auth
│   ├── user
│   ├── pet
│   ├── emergencycheck
│   ├── emergencyvideo
│   ├── foodsafety
│   ├── hospital
│   └── caseboard
├── src/main/resources
│   ├── application.properties
│   ├── schema.sql
│   ├── data.sql
│   └── static
├── frontend
│   ├── src
│   ├── package.json
│   └── vite.config.js
└── docs
```

## 환경 변수

로컬 실행 전 `.env.example`을 복사해 프로젝트 루트에 `.env`를 만듭니다.

```powershell
Copy-Item .env.example .env
```

`.env`는 `.gitignore`에 포함되어 있으므로 GitHub에 올라가지 않습니다. 실제 DB 계정, 비밀번호, API 키는 `.env`에만 작성하세요.

필수 MySQL 설정:

```properties
DB_URL=jdbc:mysql://localhost:3306/rescue_mungz?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
```

주요 선택 설정:

```properties
JWT_SECRET=put-your-long-random-jwt-secret-here
JWT_EXPIRATION_SECONDS=7200

KAKAO_REST_API_KEY=put-your-kakao-rest-api-key-here
VITE_KAKAO_JS_KEY=put-your-kakao-javascript-key-here
GOOGLE_PLACES_API_KEY=put-your-google-places-api-key-here

GMS_API_KEY=put-your-gms-api-key-here
GMS_BASE_URL=https://gms.ssafy.io/gmsapi
GMS_CHAT_COMPLETIONS_PATH=/api.openai.com/v1/chat/completions
GMS_MODEL=gpt-4o-mini
GMS_TIMEOUT_SECONDS=8
GMS_TEMPERATURE=0.2
GMS_RETRY_MAX_ATTEMPTS=1
```

Spring Boot는 `src/main/resources/application.properties`에서 다음처럼 `.env` 값을 읽습니다.

```properties
spring.config.import=optional:file:.env[.properties]
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

즉, MySQL URL, username, password는 코드에 고정하지 않고 `.env` 또는 실행 환경 변수에서만 가져옵니다.

## DB 준비

MySQL에서 사용할 데이터베이스와 사용자를 준비합니다. 사용자명과 비밀번호는 본인 환경에 맞게 바꾸고, 같은 값을 `.env`에 입력합니다.

```sql
CREATE DATABASE IF NOT EXISTS rescue_mungz
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'your_mysql_username'@'localhost'
IDENTIFIED BY 'your_mysql_password';

GRANT ALL PRIVILEGES ON rescue_mungz.* TO 'your_mysql_username'@'localhost';
FLUSH PRIVILEGES;
```

초기 데이터 입력이 필요하면 `.env`에서 다음 값을 설정한 뒤 백엔드를 실행합니다.

```properties
SQL_INIT_MODE=always
```

초기 데이터 입력 후에는 중복 삽입을 피하기 위해 다시 `never`로 두는 것을 권장합니다.

```properties
SQL_INIT_MODE=never
```

## 실행 방법

### Backend

```powershell
.\mvnw.cmd spring-boot:run
```

- API 서버: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

- Vite 개발 서버: `http://localhost:5173`
- 개발 서버는 `/api` 요청을 Spring Boot 서버로 프록시합니다.

### Frontend Build

```powershell
cd frontend
npm run build
```

빌드 결과는 `src/main/resources/static`에 생성되어 Spring Boot 단독 실행에서도 최신 화면을 제공합니다.

## 테스트

### Backend

```powershell
.\mvnw.cmd test
```

테스트 환경은 `src/test/resources/application.properties`의 H2 설정을 사용합니다. 이 설정은 테스트 전용이므로 실제 MySQL 접속 정보와 분리되어 있습니다.

### Frontend

```powershell
cd frontend
npm run build
```

## 주요 API

### Auth

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Users

- `POST /api/users`
- `GET /api/users/me`
- `PUT /api/users/me`
- `DELETE /api/users/me`
- `POST /api/users/{targetId}/follow`
- `DELETE /api/users/{targetId}/follow`
- `GET /api/users/me/followers`
- `GET /api/users/me/following`

### Pets

- `GET /api/pets`
- `POST /api/pets`
- `GET /api/pets/{id}`
- `PUT /api/pets/{id}`
- `DELETE /api/pets/{id}`

### Emergency Checks

- `POST /api/emergency-checks`
- `GET /api/emergency-checks`
- `GET /api/emergency-checks/{id}`
- `GET /api/emergency-checks/{id}/vet-report`
- `GET /api/emergency-checks/{id}/vet-report/pdf`

### Food Safety

- `GET /api/food-safety?keyword=`
- `GET /api/food-safety/{id}`

### Hospitals

- `GET /api/hospitals`
- `GET /api/hospitals/{id}`
- `POST /api/hospitals/geocode`
- `POST /api/hospitals/sync-hours`

### Emergency Videos

- `GET /api/emergency-videos`
- `POST /api/emergency-videos`
- `PUT /api/emergency-videos/{id}`
- `DELETE /api/emergency-videos/{id}`
- `GET /api/emergency-videos/bookmarks`
- `POST /api/emergency-videos/{id}/bookmark`
- `DELETE /api/emergency-videos/{id}/bookmark`

### Case Board

- `GET /api/case-posts`
- `POST /api/case-posts`
- `GET /api/case-posts/{id}`
- `PUT /api/case-posts/{id}`
- `DELETE /api/case-posts/{id}`
- `GET /api/case-posts/{id}/comments`
- `POST /api/case-posts/{id}/comments`
- `PUT /api/case-comments/{id}`
- `DELETE /api/case-comments/{id}`

## 보안 메모

- `.env`는 Git 추적 대상이 아닙니다.
- 공개 저장소에는 `.env.example`처럼 placeholder 값만 올립니다.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, 외부 API 키는 실제 값이 노출되지 않도록 `.env` 또는 배포 환경 변수로 관리합니다.
- 운영 환경에서는 충분히 긴 `JWT_SECRET`을 반드시 설정하세요.
