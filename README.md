# Merchant Sales Analysis Platform

지도 기반으로 프랜차이즈 가맹점의 매출 현황을 시각화하고, 전월 대비 성장률·거래 건수·객단가·업종/지역 평균 비교를 통해 본사 관리자와 영업 담당자의 의사결정을 지원하는 매출 분석 플랫폼입니다.

여러 가맹점의 상승/보합/하락 상태를 지도에서 모니터링하고, Gemini API 기반 AI 운영 인사이트를 통해 우선 확인이 필요한 매장과 핵심 지표를 파악할 수 있도록 설계했습니다.

## 주요 기능

- JWT 기반 로그인/로그아웃
- 관리자/영업사원 역할 기반 접근 제어
- 영업사원별 담당 가맹점 조회 범위 제한
- Kakao Maps 기반 가맹점 위치 시각화
- 최근 월 매출 규모에 따른 마커 크기 차등 표시
- 전월 대비 매출 변화율 기준 상승/보합/하락 마커 색상 구분
- 지역/업종 필터링 및 가맹점 목록 선택
- 월별 매출 추이 차트 및 업종 평균 비교
- 관리자 페이지에서 가맹점 담당자 배정
- 사용자별 AI 분석 권한 ON/OFF 설정
- Gemini 기반 AI 운영 인사이트 생성

## 서비스 대상

이 프로젝트는 개별 가맹점 점주가 아니라 프랜차이즈 본사 관리자, 영업 담당자, 내부 의사결정자를 주요 사용자로 가정합니다.

목표는 여러 가맹점 중 어떤 매장을 우선적으로 확인해야 하는지, 매출 변화의 원인 후보가 무엇인지, 어떤 지표를 지속적으로 모니터링해야 하는지 판단할 수 있도록 돕는 것입니다.

## 기술 스택

### Frontend

- React 18
- Vite
- Chart.js
- react-chartjs-2
- Kakao Maps JavaScript SDK
- Google Generative AI SDK
- ESLint

### Backend

- Java 17
- Spring Boot 3.2.5
- Gradle
- JWT 인증
- PBKDF2 비밀번호 해시
- In-memory mock data
- GCP Cloud SQL(MySQL) 연동 프로필

## 프로젝트 구조

```text
.
├── backend
│   ├── src/main/java/com/example/franchise
│   │   ├── config
│   │   ├── controller
│   │   ├── domain
│   │   └── service
│   └── src/main/resources
├── frontend
│   ├── src/components
│   ├── src/data
│   └── src/utils
└── requirements
```

## 실행 방법

### 1. Backend 실행

Java 17과 Gradle이 필요합니다.

Windows 환경에서 Java 17 경로를 명시해 실행하려면 프로젝트에 포함된 helper script를 사용할 수 있습니다.

```bash
cd backend
.\gradle-java17.cmd bootRun --no-problems-report
```

일반 Gradle 환경에서는 아래처럼 실행할 수 있습니다.

```bash
cd backend
gradle bootRun --no-problems-report
```

Backend 기본 주소:

```text
http://localhost:8080
```

### 2. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

Frontend 기본 주소:

```text
http://localhost:5173
```

Windows PowerShell에서 `npm` 실행 정책 문제가 발생하면 아래처럼 실행할 수 있습니다.

```bash
npm.cmd run dev
```

## 환경 변수

Frontend에서 Kakao Maps와 Gemini API를 사용하려면 `frontend/.env` 파일을 생성하고 아래 값을 설정합니다.

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_KAKAO_MAP_API_KEY=your_kakao_javascript_key
VITE_GEMINI_API_KEY=your_gemini_api_key
```

Backend JWT secret은 환경변수로 설정할 수 있습니다.

```bash
FRANCHISE_JWT_SECRET=your-secret-key
```

설정하지 않으면 개발용 기본값이 사용됩니다. 운영 환경에서는 반드시 별도 secret을 설정해야 합니다.

### GCP Cloud SQL(MySQL) 연동

기본 실행은 기존처럼 in-memory mock data를 사용합니다. GCP Cloud SQL에 연결하려면 `gcp` 프로필과 DB 환경변수를 함께 설정합니다.

```bash
cd backend
SPRING_PROFILES_ACTIVE=gcp \
DB_HOST=127.0.0.1 \
DB_PORT=3306 \
DB_NAME=franchise_sales \
DB_USERNAME=franchise_app \
DB_PASSWORD=your-db-password \
FRANCHISE_JWT_SECRET=your-secret-key \
gradle bootRun --no-problems-report
```

Windows PowerShell에서는 아래처럼 설정할 수 있습니다.

```powershell
$env:SPRING_PROFILES_ACTIVE="gcp"
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="franchise_sales"
$env:DB_USERNAME="franchise_app"
$env:DB_PASSWORD="your-db-password"
$env:FRANCHISE_JWT_SECRET="your-secret-key"
gradle bootRun --no-problems-report
```

Cloud SQL Auth Proxy를 사용할 경우 `DB_HOST=127.0.0.1`, `DB_PORT=3306`으로 두면 됩니다. Cloud SQL public/private IP로 직접 연결할 경우 `DB_HOST`에 해당 IP를 넣습니다.

초기 테이블과 샘플 데이터를 생성하려면 최초 1회만 `DB_INIT_MODE=always`를 추가합니다. SQL 파일은 `backend/src/main/resources/db/mysql/schema.sql`, `backend/src/main/resources/db/mysql/data.sql`에 있습니다. 초기 계정 비밀번호는 mock data와 동일하게 `1234`입니다.

## 테스트 계정

Mock data 기준 테스트 계정입니다.

| 역할 | 아이디 | 비밀번호 | 접근 범위 |
|---|---|---|---|
| 관리자 | `admin` | `1234` | 전체 가맹점, 관리자 페이지 |
| 영업사원 | `sales_user` | `1234` | 담당 가맹점 |
| 영업사원 | `sales_user2` | `1234` | 담당 가맹점 |

## 주요 API

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/auth/test-users` | 테스트 계정 목록 조회 |
| `POST` | `/api/auth/login` | 로그인 및 JWT 발급 |
| `GET` | `/api/users` | 사용자 목록 조회 |
| `GET` | `/api/franchises` | 가맹점 목록 조회 |
| `GET` | `/api/averages` | 업종/지역 평균 매출 조회 |
| `POST` | `/api/admin/assign-manager` | 가맹점 담당자 변경 |
| `POST` | `/api/admin/toggle-ai` | AI 분석 권한 변경 |

인증이 필요한 API는 아래 형식의 Authorization 헤더를 사용합니다.

```http
Authorization: Bearer <token>
```

## 검증 명령어

Frontend 정적 검사:

```bash
cd frontend
npm run lint
```

Frontend production build:

```bash
cd frontend
npm run build
```

Backend test:

```bash
cd backend
gradle test --no-problems-report
```

Windows PowerShell에서 `npm` 실행 정책 문제가 있으면 `npm.cmd run lint`, `npm.cmd run build`처럼 실행할 수 있습니다.

## 구현 메모

- 기본 실행의 데이터는 DB 연동 전 흐름 검증을 위한 in-memory mock data입니다.
- `gcp` 프로필에서는 Cloud SQL MySQL 데이터를 JDBC로 조회/변경합니다.
- 가맹점 데이터는 상승/보합/하락 상태를 모두 확인할 수 있도록 구성했습니다.
- 마커 크기는 최근 월 매출 규모를 기준으로 계산합니다.
- 마커 색상은 전월 대비 매출 성장률 기준으로 상승, 보합, 하락을 구분합니다.
- AI 인사이트는 점주 컨설팅이 아니라 본사/영업 담당자의 운영 모니터링 관점으로 작성되도록 프롬프트를 설계했습니다.
