# 가맹점 등록 및 위치 자동화 기획안

## 1. 현재 구현 상태

현재 프로젝트에는 관리자가 기존 가맹점의 담당 영업사원을 변경하는 기능은 구현되어 있다.

- 관리자 페이지에서 가맹점별 담당 영업사원 선택 가능
- `POST /api/admin/assign-manager` API로 담당자 배정 변경
- `user_franchise_assignments` 테이블로 영업사원과 가맹점 관계 저장

하지만 관리자가 신규 가맹점을 직접 등록하는 기능은 아직 없다. 현재 가맹점 데이터는 `data.sql` 또는 DB 직접 입력을 전제로 한다.

지도 표시 방식은 다음 순서로 단순화한다.

1. `franchises.latitude`, `franchises.longitude` 값이 있으면 해당 좌표로 마커 표시
2. 좌표가 없으면 지도에서 자동 지오코딩하지 않음
3. 좌표가 없는 가맹점은 지도에 표시하지 않고 미확인 매장 수로 집계
4. 좌표 생성과 검증은 관리자 등록·수정 화면에서 처리

따라서 가맹점 등록 기능을 추가할 때 관리자가 위도/경도를 직접 입력하게 만들 필요는 없다.

## 2. 문제 정의

관리자가 신규 가맹점을 등록하려면 다음 문제가 해결되어야 한다.

- 관리자는 일반적으로 위도/경도 값을 알지 못한다.
- 주소만 입력해도 지도에 표시되어야 한다.
- 주소가 부정확하거나 지오코딩에 실패한 가맹점은 운영자가 확인할 수 있어야 한다.
- 신규 가맹점 등록과 동시에 담당 영업사원을 배정할 수 있어야 한다.
- 좌표를 프론트 브라우저 캐시에만 보관하면 다른 사용자나 다른 브라우저에서는 다시 지오코딩해야 한다.

## 3. 목표

관리자가 가맹점을 등록할 때 주소 기반으로 위치를 자동 확인하고, 필요한 경우 담당 영업사원을 함께 배정할 수 있도록 한다.

핵심 목표는 다음과 같다.

- 관리자는 가맹점명, 업종, 지역, 주소만 입력해도 가맹점을 등록할 수 있다.
- 위도/경도는 시스템이 주소를 기반으로 자동 산출한다.
- 좌표 산출 결과를 DB에 저장해 지도 로딩 시 반복 지오코딩을 줄인다.
- 좌표를 찾지 못한 가맹점은 별도 상태로 표시해 관리자가 수정할 수 있게 한다.
- 신규 등록 시 담당 영업사원 배정을 함께 처리할 수 있다.

## 3.1 지도 마커 표시 권장 방식

가맹점 마커 표시는 `등록/수정 시 좌표 확정 + 지도 화면은 표시 전용` 방식을 기준으로 한다.

권장 순서:

1. 관리자가 가맹점을 등록하거나 주소를 수정할 때 주소를 입력한다.
2. 관리자 화면에서 Kakao Geocoder로 주소를 좌표로 변환한다.
3. 지도 미리보기에서 위치를 확인한다.
4. 저장 시 `latitude`, `longitude`, `location_status`를 DB에 저장한다.
5. 대시보드 지도는 DB에 저장된 좌표만 사용해 마커를 표시한다.
6. 좌표가 없는 가맹점은 지도에서 자동 보정하지 않고 `좌표 미확인`으로 집계한다.

이 방식이 현재 프로젝트에 가장 적합한 이유:

- 지도 화면이 조회와 표시만 담당하므로 구조가 단순하다.
- 대시보드를 여는 행위가 DB 변경을 일으키지 않는다.
- 좌표 생성/검증 책임이 관리자 등록·수정 화면으로 모인다.
- 좌표가 없는 가맹점을 운영자가 명시적으로 확인하고 보정할 수 있다.
- 지도 렌더링 중 외부 지오코딩 호출이 발생하지 않아 화면 동작이 예측 가능하다.

## 4. 권장 UX

### 4.1 관리자 페이지 탭 구조

관리자 페이지를 다음 탭 구조로 확장한다.

- 가맹점 관리
- 담당자 배정
- 권한 관리
- 조치 현황
- 데이터 업로드

가맹점 등록 기능은 `가맹점 관리` 탭에 배치한다.

### 4.2 가맹점 등록 폼

입력 항목:

- 가맹점명
- 업종
- 지역
- 주소
- 담당 영업사원
- 초기 월 매출 입력 여부

위도/경도는 기본 입력 항목에서 제외한다.

등록 흐름:

1. 관리자가 가맹점 기본 정보와 주소를 입력한다.
2. 주소 확인 버튼을 누르면 Kakao 지오코딩으로 후보 좌표를 조회한다.
3. 지도 미리보기에서 마커 위치를 확인한다.
4. 위치가 맞으면 등록한다.
5. 등록 시 좌표와 지오코딩 상태를 함께 저장한다.

### 4.3 주소 검증 상태

주소 검증 결과는 다음 상태로 관리한다.

- `VERIFIED`: 주소 기반 좌표 확인 완료
- `GEOCODED`: 시스템이 주소 기반으로 자동 산출한 좌표
- `UNVERIFIED`: 주소는 있으나 좌표 확인 전
- `FAILED`: 지오코딩 실패
- `MANUAL`: 관리자가 지도에서 직접 위치 보정

지도 화면에서는 `FAILED`, `UNVERIFIED` 상태의 가맹점을 별도 목록으로 보여준다.

## 5. 위치 처리 방식

### 5.1 1안: 프론트엔드 지오코딩 후 백엔드 저장

관리자 등록 화면에서 Kakao Maps SDK로 주소를 지오코딩하고, 결과 좌표를 백엔드에 함께 전송한다.

장점:

- 관리자 등록·수정 화면에서 Kakao Maps Geocoder를 재사용하기 쉽다.
- 구현 난이도가 낮다.
- 관리자가 지도 미리보기로 즉시 위치를 확인할 수 있다.

단점:

- Kakao JavaScript 키가 브라우저에서 사용된다.
- 좌표 산출 신뢰성을 프론트 흐름에 의존한다.

초기 구현에는 이 방식을 권장한다.

### 5.2 2안: 백엔드 지오코딩 후 저장

백엔드에서 Kakao Local API 또는 별도 지오코딩 API를 호출해 좌표를 산출한다.

장점:

- API 키를 백엔드에서 관리할 수 있다.
- 좌표 산출 로직과 실패 로그를 서버에서 일관되게 관리할 수 있다.
- 배치 재시도나 주소 재검증 작업을 만들기 쉽다.

단점:

- 백엔드 외부 API 연동과 키 관리가 추가된다.
- 지도 미리보기 UX를 위해 프론트와 별도 연동이 필요하다.

운영 단계에서는 이 방식으로 확장하는 것을 권장한다.

### 5.3 권장 단계

초기에는 관리자 등록·수정 화면에서 프론트엔드 지오코딩 후 백엔드 저장 방식으로 구현한다. 이때 주소 검색 결과를 관리자가 확인하지 않은 상태라면 `GEOCODED`, 관리자가 지도 미리보기에서 위치를 확인하고 저장하면 `VERIFIED`로 저장한다. 이후 운영 안정화 단계에서 백엔드 지오코딩과 재검증 배치로 확장한다.

## 6. DB 변경안

기존 `franchises` 테이블에 위치 검증 상태를 추가한다.

```sql
ALTER TABLE franchises
    ADD COLUMN location_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '위치 좌표 검증 상태',
    ADD COLUMN geocoded_at TIMESTAMP NULL COMMENT '주소 기반 좌표 산출 시각',
    ADD COLUMN geocode_source VARCHAR(50) COMMENT '좌표 산출 출처',
    ADD COLUMN location_note VARCHAR(255) COMMENT '위치 검증 또는 보정 메모';
```

신규 등록과 배정 이력을 추적하려면 담당자 변경 이력 테이블을 추가한다.

```sql
CREATE TABLE assignment_histories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '담당자 배정 이력 ID',
    franchise_id VARCHAR(20) NOT NULL COMMENT '가맹점 ID',
    previous_user_id VARCHAR(64) COMMENT '변경 전 담당 사용자 ID',
    new_user_id VARCHAR(64) COMMENT '변경 후 담당 사용자 ID',
    changed_by VARCHAR(64) NOT NULL COMMENT '변경을 수행한 관리자 ID',
    change_reason VARCHAR(255) COMMENT '담당자 변경 사유',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담당자 변경 시각',
    PRIMARY KEY (id),
    KEY idx_assignment_history_franchise (franchise_id, created_at),
    KEY idx_assignment_history_user (new_user_id, created_at),
    CONSTRAINT fk_assignment_history_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assignment_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id)
        ON DELETE CASCADE
);
```

## 7. API 변경안

### 7.1 가맹점 등록

```http
POST /api/admin/franchises
```

요청 예시:

```json
{
  "name": "강남 신규점",
  "industry": "카페",
  "region": "서울 강남구",
  "address": "서울 강남구 테헤란로 123",
  "latitude": 37.501,
  "longitude": 127.039,
  "locationStatus": "VERIFIED",
  "managerId": "sales_user"
}
```

처리:

- 관리자 권한 검증
- 필수값 검증
- 가맹점 ID 자동 생성 또는 중복 검증
- `franchises` 저장
- `managerId`가 있으면 `user_franchise_assignments` 저장
- 배정 이력 저장

### 7.2 가맹점 수정

```http
PUT /api/admin/franchises/{franchiseId}
```

수정 가능 항목:

- 가맹점명
- 업종
- 지역
- 주소
- 좌표
- 위치 검증 상태
- 위치 메모

주소가 변경되면 `locationStatus`를 `UNVERIFIED`로 되돌리거나, 즉시 재지오코딩한다.

### 7.3 위치 재검증

```http
POST /api/admin/franchises/{franchiseId}/geocode
```

처리:

- 현재 주소로 좌표 재조회
- 성공 시 `latitude`, `longitude`, `locationStatus`, `geocodedAt` 갱신
- 실패 시 `locationStatus=FAILED` 저장

초기 구현에서는 이 API를 생략하고 프론트에서 좌표를 전달해도 된다.

## 8. 프론트엔드 변경안

### 8.1 AdminPage

현재 관리자 페이지는 한 화면에 담당자 배정과 AI 권한 설정을 나란히 보여준다. 가맹점 등록까지 추가되면 화면이 복잡해지므로 탭 구조로 정리한다.

추천 탭:

- 가맹점 관리
- 담당자 배정
- 권한 관리

`가맹점 관리` 탭 기능:

- 가맹점 목록
- 신규 등록 버튼
- 주소/위치 상태 배지
- 좌표 미확인 가맹점 필터
- 수정 버튼

### 8.2 등록 모달

등록 모달 구성:

- 기본 정보 입력 영역
- 주소 검색/확인 영역
- 지도 미리보기 영역
- 담당자 선택 영역
- 저장 버튼

상태 처리:

- 주소 미입력
- 지오코딩 중
- 지오코딩 성공
- 후보 결과 없음
- 저장 실패

### 8.3 MapArea

`MapArea.jsx`는 지도 표시 전용 컴포넌트로 유지한다.

- DB에 저장된 `latitude`, `longitude`가 있는 가맹점만 마커로 표시한다.
- DB 좌표가 없는 가맹점은 지도에서 자동 지오코딩하지 않는다.
- 좌표가 없는 가맹점 수를 `좌표 미확인`으로 표시한다.
- 위치 보정은 관리자 페이지의 가맹점 등록·수정 화면에서 수행한다.
- 지도 컴포넌트는 DB 저장 API를 직접 호출하지 않는다.

## 9. 검증 규칙

필수 검증:

- 가맹점명은 필수
- 업종은 필수
- 지역은 필수
- 주소는 필수
- 동일 주소와 동일 가맹점명 조합 중복 경고
- 담당 영업사원은 `SALES` 역할 사용자만 선택 가능

좌표 검증:

- 위도 범위: -90 ~ 90
- 경도 범위: -180 ~ 180
- 좌표가 없으면 등록은 허용하되 `locationStatus=UNVERIFIED`
- 지오코딩 실패 시 `locationStatus=FAILED`

## 10. 우선순위

### MVP

1. 관리자 가맹점 등록 API
2. 관리자 가맹점 등록 모달
3. 주소 기반 프론트 지오코딩
4. 좌표/위치 상태 DB 저장
5. 등록과 동시에 담당 영업사원 배정

### 다음 단계

1. 가맹점 정보 수정
2. 좌표 미확인 가맹점 목록
3. 지도에서 위치 수동 보정
4. 담당자 배정 이력 저장
5. 백엔드 지오코딩 API와 재검증 기능

## 11. 완료 기준

- 관리자는 DB를 직접 수정하지 않고 신규 가맹점을 등록할 수 있다.
- 관리자는 위도/경도를 몰라도 주소 기반으로 지도 위치를 확인할 수 있다.
- 등록된 가맹점은 지도에 표시된다.
- 주소로 좌표를 찾지 못한 가맹점은 누락되지 않고 별도 상태로 확인된다.
- 신규 가맹점 등록 시 담당 영업사원을 함께 배정할 수 있다.
- 영업사원은 배정된 신규 가맹점을 본인 대시보드에서 조회할 수 있다.
