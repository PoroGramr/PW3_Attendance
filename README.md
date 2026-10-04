# 🎓 온누리교회 중고등부 PW3 출석 관리 체계 

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)

> **교회 주일학교 출석 관리 시스템** - QR 코드 기반 실시간 출석 체크 및 AI 기반 데이터 분석을 제공하는 엔터프라이즈급 백엔드 시스템

---

## 📋 Table of Contents

- [프로젝트 개요](#-프로젝트-개요)
- [핵심 기능](#-핵심-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [API 문서](#-api-문서)
- [주요 도메인 모델](#-주요-도메인-모델)

---

## 🎯 프로젝트 개요

**PW3 Attendance System**은 교회 주일학교의 효율적인 학생 출석 관리를 위해 설계된 RESTful API 서버입니다.

### 핵심 가치

- **📱 모바일 최적화**: QR 코드 기반 비접촉 출석 체크
- **🤖 AI 기반 인사이트**: Gemini AI를 활용한 자연어 질의응답 및 출석 데이터 분석
- **📊 실시간 대시보드**: 반별/교사별/학년별 출석 현황 실시간 집계
- **🔔 자동 알림**: SMS를 통한 QR 코드 자동 발송 및 생일 알림
- **🎂 생일 관리**: 월별 학생/교사 생일자 자동 조회 및 알림

### 주요 사용 사례

- 주일 예배 출석 체크 및 통계 관리
- 학생 및 교사 정보 통합 관리
- 반별/학년별 출석률 분석 및 리포트 생성
- 생일자 자동 조회 및 축하 메시지 발송
- AI 챗봇을 통한 출석 데이터 질의응답

---

## ✨ 핵심 기능

### 1. 🎫 QR 코드 기반 출석 시스템

- **개인별 고유 QR 코드 생성**: 각 학생에게 고유한 QR 코드 발급
- **SMS 자동 발송**: CoolSMS API를 통한 QR 코드 링크 자동 전송
- **실시간 스캔 처리**: 교사용 스캔 기능으로 즉시 출석 처리
- **출석 상태 관리**: PRESENT(출석), ABSENT(결석), LATE(지각), EXCUSED(사유결석) 상태 관리

### 2. 📊 출석 데이터 분석 및 리포트

- **일별 출석 현황**: 특정 날짜의 전체/반별 출석 통계
- **주간 출석 요약**: 일요일별 출석률 집계 및 트렌드 분석
- **학년별 통계**: 학년별 최근 1개월 일요일 출석 요약
- **반별 리포트**: 각 반의 출석 현황 및 결석자 목록
- **텍스트 리포트 생성**: 출석 현황을 텍스트 형식으로 자동 생성

### 3. 🤖 AI 기반 질의응답 시스템

- **자연어 처리**: "오늘 출석률이 어때?" 같은 자연어 질문 지원
- **Gemini AI 통합**: Google Gemini 1.5 Flash 모델 활용
- **컨텍스트 인식**: 출석 데이터 기반 지능형 응답 생성
- **실시간 데이터 조회**: 최신 출석 정보 기반 답변 제공

### 4. 👥 학생 및 교사 관리

- **학생 정보 관리**: 이름, 생년월일, 성별, 연락처, 학교, 메모 등
- **반 배정 시스템**: 학년도별 학생-반 매핑 관리
- **졸업생 관리**: 졸업 처리 및 졸업생 제외 조회 기능
- **교사 정보 관리**: 교사 정보 및 담당 반 관리
- **연도별 등록 통계**: 월별 신규 학생 등록 현황 조회

### 5. 🎂 생일 관리 시스템

- **월별 생일자 조회**: 학생 및 교사의 월별 생일자 자동 조회
- **데이터 정제**: 잘못된 생일 데이터(1/1) 자동 필터링
- **생일순 정렬**: 일자별 정렬로 생일자 목록 제공
- **통합 조회**: 학생과 교사 생일자 통합 조회 API

### 6. 📱 메시지 발송 시스템

- **SMS 발송**: CoolSMS API를 통한 대량 문자 발송
- **QR 코드 링크 전송**: 개인별 QR 코드 페이지 링크 자동 발송
- **발송 이력 관리**: 메시지 발송 로그 저장 및 조회

---

## 🛠 기술 스택

### Backend Framework

- **Spring Boot 3.4.5**: 최신 스프링 부트 프레임워크
- **Spring Data JPA**: ORM 기반 데이터베이스 접근
- **Spring Web**: RESTful API 구현
- **Spring Validation**: 입력 데이터 검증

### Database

- **MySQL 8.0**: 메인 데이터베이스
- **Hibernate**: JPA 구현체
- **Soft Delete**: 논리 삭제 패턴 적용

### AI & External APIs

- **Spring AI**: AI 통합 프레임워크
- **Google Gemini 1.5 Flash**: 자연어 처리 및 데이터 분석
- **CoolSMS API**: SMS 발송 서비스
- **ImgBB API**: 이미지 호스팅 (QR 코드 저장)

### Documentation & Monitoring

- **SpringDoc OpenAPI 3**: Swagger UI 기반 API 문서 자동 생성
- **Spring Boot Actuator**: 애플리케이션 모니터링 및 헬스 체크

### DevOps & Deployment

- **Docker**: 컨테이너화
- **Docker Compose**: 멀티 컨테이너 오케스트레이션
- **Jenkins**: CI/CD 파이프라인
- **Gradle**: 빌드 자동화

### Development Tools

- **Lombok**: 보일러플레이트 코드 자동 생성
- **Java 17**: LTS 버전 사용

---

## 🏗 시스템 아키텍처

```
┌─────────────────┐
│   Client App    │
│  (Mobile/Web)   │
└────────┬────────┘
         │ HTTP/REST
         ▼
┌─────────────────────────────────────┐
│      Spring Boot Application        │
│  ┌───────────────────────────────┐  │
│  │    Controller Layer           │  │
│  │  - StudentController          │  │
│  │  - AttendanceController       │  │
│  │  - BirthdayController         │  │
│  │  - AiController               │  │
│  └──────────┬────────────────────┘  │
│             │                        │
│  ┌──────────▼────────────────────┐  │
│  │    Service Layer              │  │
│  │  - StudentService             │  │
│  │  - AttendanceService          │  │
│  │  - BirthdayService            │  │
│  │  - AiChatService              │  │
│  │  - QrService                  │  │
│  └──────────┬────────────────────┘  │
│             │                        │
│  ┌──────────▼────────────────────┐  │
│  │    Repository Layer           │  │
│  │  - JPA Repositories           │  │
│  └──────────┬────────────────────┘  │
└─────────────┼────────────────────────┘
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌────────┐ ┌─────┐ ┌──────────┐
│ MySQL  │ │ AI  │ │ External │
│   DB   │ │ API │ │   APIs   │
└────────┘ └─────┘ └──────────┘
                    - CoolSMS
                    - ImgBB
```

### 계층별 책임

#### Controller Layer
- HTTP 요청/응답 처리
- 입력 데이터 검증
- API 엔드포인트 정의
- Swagger 문서화

#### Service Layer
- 비즈니스 로직 구현
- 트랜잭션 관리
- 외부 API 통합
- 데이터 변환 및 집계

#### Repository Layer
- 데이터베이스 접근
- JPQL/Native Query 실행
- 엔티티 영속성 관리

---

## 📚 API 문서

### Swagger UI 접근

애플리케이션 실행 후 다음 URL에서 전체 API 문서를 확인할 수 있습니다:

```
http://localhost:8080/swagger-ui/index.html
```

### 주요 API 엔드포인트

#### 👤 학생 관리 (`/api/students`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/students` | 전체 학생 조회 (졸업생 제외) |
| `GET` | `/api/students/with-graduated` | 졸업생 포함 전체 학생 조회 |
| `GET` | `/api/students/{id}` | 특정 학생 조회 |
| `POST` | `/api/students` | 신규 학생 등록 |
| `PUT` | `/api/students/{id}` | 학생 정보 수정 |
| `DELETE` | `/api/students/{id}` | 학생 삭제 (Soft Delete) |
| `DELETE` | `/api/students/graduated/{id}` | 학생 졸업 처리 |
| `POST` | `/api/students/{studentId}/send-qr` | QR 코드 SMS 발송 |
| `GET` | `/api/students/registrations/by-year/{year}` | 연도별 월별 등록 현황 |

#### 📋 출석 관리 (`/api/attendances`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/attendances/summary-by-date` | 일별 출석 현황 요약 |
| `GET` | `/api/attendances/report-by-date` | 일별 출석 리포트 텍스트 |
| `GET` | `/api/attendances/summary/sundays` | 일요일별 전체 출석 요약 |
| `GET` | `/api/attendances/summary/grades/sundays` | 학년별 최근 1달 일요일 출석 요약 |
| `PUT` | `/api/attendances/{studentClassId}/{date}` | 출석 데이터 생성/수정 |
| `POST` | `/api/attendances/scan` | QR 코드 스캔 출석 처리 |
| `GET` | `/api/attendances/year/{schoolYear}/date/{date}` | 특정 학년도/일자 전체 출석 조회 |
| `GET` | `/api/attendances/classrooms/{classRoomId}/date/{date}` | 특정 반/일자 출석 조회 |

#### 🔄 Acts29 출석 동기화 (`/api/integrations/acts29`)

관리자 또는 슈퍼어드민 JWT가 필요합니다. 요청 본문을 생략하거나 `dryRun`을 `true`로 보내면 원격 데이터는 변경하지 않고 학생 매칭 및 변경 예정 내역만 반환합니다.

```http
POST /api/integrations/acts29/attendances/{date}/sync
```

Dry-run:

```json
{
  "dryRun": true,
  "className": "고1-2"
}
```

실제 반영 시에는 경로 날짜와 같은 `confirmDate`가 필요합니다. 학생 매칭 실패가 있으면 기본적으로 `409 Conflict`로 중단하며, 불일치 학생을 제외하고 반영하려는 경우에만 `allowPartial`을 명시적으로 활성화합니다.

```json
{
  "dryRun": false,
  "allowPartial": false,
  "confirmDate": "2026-10-04",
  "className": "고1-2"
}
```

`className`을 생략하면 해당 학년도 전체, 지정하면 해당 반 학생만 동기화합니다. 상태 변환은 `ATTEND`, `LATE`를 출석(`Y`)으로, `ABSENT`, `OTHER`, `UNCHECKED`를 결석(`N`)으로 처리합니다. 학생은 이름과 생년월일로 매칭하며, 동명이인이나 불일치가 있으면 dry-run 응답의 `matchIssues`에 포함됩니다.

필수 환경 변수:

```bash
ACTS29_USER_ID=<통합인증 이메일>
ACTS29_PASSWORD=<통합인증 비밀번호>
```

로컬에 설치된 Chrome을 사용해야 한다면 `ACTS29_BROWSER_CHANNEL=chrome`을 추가할 수 있습니다. Docker 이미지는 `build.gradle`의 Playwright 버전과 일치하는 브라우저 런타임을 포함합니다.

#### 🎂 생일 관리 (`/api/birthday`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/birthday/students/{month}` | 특정 월 학생 생일자 조회 |
| `GET` | `/api/birthday/teachers/{month}` | 특정 월 교사 생일자 조회 |
| `GET` | `/api/birthday/{month}` | 특정 월 전체 생일자 조회 |

#### 🤖 AI 챗봇 (`/api/ai`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/ai/chat` | AI 기반 자연어 질의응답 |

**요청 예시:**
```json
{
  "question": "오늘 출석률이 어때?"
}
```

**응답 예시:**
```json
{
  "answer": "오늘 전체 출석률은 85%입니다. 총 100명 중 85명이 출석했습니다."
}
```

---



## 🗂 주요 도메인 모델

### Student (학생)

```java
@Entity
public class Student extends BaseEntity {
    private Long id;
    private String name;           // 이름
    private LocalDate birth;       // 생년월일
    private Sex sex;               // 성별 (MAN, WOMAN)
    private String phone;          // 연락처
    private String parentPhone;    // 부모 연락처
    private String school;         // 학교
    private String memo;           // 메모
    private Boolean isGraduated;   // 졸업 여부
    private LocalDateTime deletedAt; // 삭제 일시 (Soft Delete)
}
```

### Attendance (출석)

```java
@Entity
public class Attendance extends BaseEntity {
    private Long id;
    private StudentClass studentClass; // 학생-반 매핑
    private LocalDate date;            // 출석 날짜
    private AttendanceStatus status;   // 출석 상태
    
    public enum AttendanceStatus {
        PRESENT,  // 출석
        ABSENT,   // 결석
        LATE,     // 지각
        EXCUSED   // 사유결석
    }
}
```

### ClassRoom (반)

```java
@Entity
public class ClassRoom extends BaseEntity {
    private Long id;
    private String name;        // 반 이름
    private Integer schoolYear; // 학년도
    private Integer grade;      // 학년
}
```

### StudentClass (학생-반 매핑)

```java
@Entity
public class StudentClass extends BaseEntity {
    private Long id;
    private Student student;    // 학생
    private ClassRoom classRoom; // 반
    private Integer schoolYear;  // 학년도
}
```

---

## 🔍 주요 기능 구현 상세

### 1. Soft Delete 패턴

모든 엔티티는 물리적 삭제 대신 논리적 삭제를 사용합니다:

```java
@SQLDelete(sql = "UPDATE student SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Student extends BaseEntity {
    private LocalDateTime deletedAt;
}
```

### 2. 생일 데이터 정제

잘못된 생일 데이터(1/1)를 자동으로 필터링합니다:

```java
@Query("SELECT s FROM Student s WHERE MONTH(s.birth) = :month " +
       "AND s.deletedAt IS NULL " +
       "AND NOT (MONTH(s.birth) = 1 AND DAY(s.birth) = 1) " +
       "ORDER BY DAY(s.birth)")
List<Student> findByBirthMonth(@Param("month") int month);
```

### 3. AI 기반 질의응답

Spring AI를 사용하여 자연어 질문을 처리합니다:

```java
@Service
public class AiChatService {
    private final ChatClient chatClient;
    
    public String chatWithAgent(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

### 4. QR 코드 생성 및 발송

각 학생에게 고유한 QR 코드를 생성하고 SMS로 발송합니다:

```java
@Service
public class QrService {
    public boolean sendPersonalQrCodeSms(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
        
        String qrUrl = generateQrCodeUrl(studentId);
        return smsService.sendSms(student.getPhone(), qrUrl);
    }
}
```

---

## 📊 데이터베이스 스키마

### ERD 주요 관계

```
Student (1) ──────< (N) StudentClass (N) >────── (1) ClassRoom
                         │
                         │ (1)
                         │
                         v
                    Attendance (N)

Teacher (1) ──────< (N) TeacherClass (N) >────── (1) ClassRoom

Student (1) ──────< (1) StudentQr
```

---


---

## 📈 성능 최적화

### 적용된 최적화 기법

1. **N+1 문제 해결**: Fetch Join 사용
2. **인덱싱**: 자주 조회되는 컬럼에 인덱스 적용
3. **캐싱**: Spring Cache 적용 (추후 Redis 도입 예정)
4. **페이지네이션**: 대량 데이터 조회 시 페이징 처리
5. **DTO 변환**: 엔티티 직접 노출 방지

---

## 🔒 보안

### 관리자 인증

관리자 가입 계정은 슈퍼어드민 승인 후 로그인할 수 있습니다. Access Token은 API 요청의
`Authorization: Bearer <token>` 헤더에 전달하고, Refresh Token은 재발급 및 로그아웃 API에서만 사용합니다.

필수 환경 변수:

```bash
JWT_SECRET=<32바이트 이상의 일반 문자열 또는 Base64 문자열>
JWT_ACCESS_TOKEN_VALIDITY=15m
JWT_REFRESH_TOKEN_VALIDITY=14d
JWT_ISSUER=pw3-attendant
```

아이디 찾기와 비밀번호 초기화 이메일 발송을 위한 SMTP 환경 변수:

```bash
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=no-reply@example.com
MAIL_PASSWORD=<SMTP password>
MAIL_FROM=no-reply@example.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

# 선택값
ACCOUNT_RECOVERY_CODE_VALIDITY=10m
ACCOUNT_RECOVERY_RESEND_INTERVAL=1m
ACCOUNT_RECOVERY_MAX_ATTEMPTS=5
```

문자 발송은 기존 `COOLSMS_API_KEY`, `COOLSMS_SECRET_KEY`, `COOLSMS_SENDER_NUMBER` 설정을 사용합니다.

주요 엔드포인트:

| 메서드 | 경로 | 권한 |
|---|---|---|
| POST | `/api/auth/signup` | 공개 |
| PUT | `/api/auth/resubmit` | 공개, 거절 계정의 기존 비밀번호 확인 |
| POST | `/api/auth/login` | 공개 |
| POST | `/api/auth/refresh` | 공개, Refresh Token 필요 |
| POST | `/api/auth/logout` | 공개, Refresh Token 필요 |
| POST | `/api/auth/logout-all` | 로그인 관리자 |
| GET | `/api/auth/me` | 로그인 관리자 |
| PATCH | `/api/auth/me` | 로그인 관리자, 회원정보 및 선택적 비밀번호 변경 |
| POST | `/api/auth/find-username` | 공개, 등록 이메일·전화번호로 아이디 발송 |
| POST | `/api/auth/password-reset/request` | 공개, 초기화 인증번호 발송 |
| POST | `/api/auth/password-reset/confirm` | 공개, 인증번호 확인 후 비밀번호 변경 |
| GET | `/api/super-admin/admins?status=PENDING` | 슈퍼어드민 |
| PATCH | `/api/super-admin/admins/{id}/approve` | 슈퍼어드민 |
| PATCH | `/api/super-admin/admins/{id}/reject` | 슈퍼어드민 |

### 학생·교사 얼굴 사진

얼굴 사진은 비공개 MinIO 버킷에 저장하고, 데이터베이스에는 객체 키만 보관합니다. 학생과 교사는 각각 대표 사진 1장만 가질 수 있으며 새 사진 업로드 시 기존 사진을 교체합니다.

```bash
MINIO_ENDPOINT=https://minio.example.com
MINIO_ACCESS_KEY=<MinIO access key>
MINIO_SECRET_KEY=<MinIO secret key>
MINIO_BUCKET=pw3-face-images
MINIO_PRESIGNED_URL_VALIDITY=10m
```

버킷이 없으면 최초 업로드 때 생성하므로 MinIO 계정에는 해당 버킷의 조회·생성·업로드·삭제 권한이 필요합니다. 기존 버킷을 사용할 경우 비공개 정책을 유지해야 합니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| PUT | `/api/students/{id}/face-image` | 학생 사진 등록·교체 (`multipart/form-data`, `file`) |
| GET | `/api/students/{id}/face-image` | 학생 사진의 임시 조회 URL 발급 |
| DELETE | `/api/students/{id}/face-image` | 학생 사진 삭제 |
| PUT | `/api/teacher/{id}/face-image` | 교사 사진 등록·교체 (`multipart/form-data`, `file`) |
| GET | `/api/teacher/{id}/face-image` | 교사 사진의 임시 조회 URL 발급 |
| DELETE | `/api/teacher/{id}/face-image` | 교사 사진 삭제 |

업로드는 JPEG 또는 PNG만 허용하며 파일은 5MB, 가로·세로는 각각 4096px 이하여야 합니다. 모든 얼굴 사진 API는 관리자 Access Token 인증이 필요합니다.

최초 슈퍼어드민은 운영 DB에 수동 등록합니다. 먼저 비밀번호 해시를 생성합니다.

```bash
read -s ADMIN_PASSWORD
export ADMIN_PASSWORD
./gradlew generateAdminPasswordHash
unset ADMIN_PASSWORD
```

출력된 해시를 아래 `password_hash`에 넣습니다.

```sql
INSERT INTO admin_account (
    username, password_hash, name, email, phone, role, approval_status,
    approved_at, created_at, updated_at
) VALUES (
    'superadmin', '<BCrypt hash>', '슈퍼 관리자', 'superadmin@example.com', '01000000000',
    'SUPER_ADMIN', 'APPROVED', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);
```

### 적용된 보안 조치

- **SQL Injection 방지**: JPA Parameterized Query 사용
- **XSS 방지**: 입력 데이터 검증 및 이스케이핑
- **환경 변수 분리**: 민감 정보 외부화
- **Soft Delete**: 데이터 복구 가능성 확보

### 향후 개선 계획

- [x] Spring Security 적용
- [x] JWT 기반 인증/인가
- [x] Role-based Access Control (RBAC)
- [ ] API Rate Limiting

---

## 🤝 기여 방법

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 라이선스

This project is licensed under the MIT License.

---

## 👨‍💻 개발자

**PoroGramr** - Backend Developer

- GitHub: [@PoroGramr](https://github.com/PoroGramr)

---

## 🙏 감사의 말

이 프로젝트는 교회 주일학교의 효율적인 운영을 위해 개발되었습니다.

---
