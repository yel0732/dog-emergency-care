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


## 🛡️ 보안 메모

- `.env`는 Git에 올리지 않습니다.
- `.env.example`에는 placeholder 값만 유지합니다.
- DB 계정, JWT secret, Kakao/Google/GMS API 키는 로컬 환경변수 또는 배포 환경변수로 관리합니다.
- 공개 저장소에 실제 키가 포함되지 않도록 커밋 전 반드시 확인합니다.

---

## 📚 문서

- [`docs`](./docs): 기획, 산출물, ERD, 화면 설계 자료
- [`docs/ERD`](./docs/ERD): 도메인별 ERD 이미지
