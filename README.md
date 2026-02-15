# springboot-sns-sample

## 1) 사전 준비

- Docker Desktop (또는 Docker Engine + Compose v2)
- Java 25 (앱 로컬 실행 시)
- `curl`

확인 명령:

```bash
docker --version
docker compose version
java -version
```

## 2) 가장 빠른 실행 (권장)

`docker-compose`로 앱 + PostgreSQL + Redis + RustFS를 한 번에 띄웁니다.

```bash
cd sns-app
docker compose up -d --build
docker compose ps
```

정상 기준:

- `app`, `postgres`, `redis`, `rustfs` 컨테이너가 `Up` 상태
- app healthcheck 통과

## 3) 동작 확인 (검증 체크리스트)

### 3-1. Health 확인

```bash
curl -s http://localhost:8080/actuator/health
```

기대 결과: `"status":"UP"` 포함

### 3-2. 관측 데모 API 호출 (인증 불필요)

```bash
curl -s "http://localhost:8080/api/v1/demo/trace?message=hello"
curl -s "http://localhost:8080/api/v1/demo/error"
```

기대 결과:

- `/trace`: `"status":"ok"`
- `/error`: HTTP 500 + `"status":"error"`

### 3-3. 회원가입 + 로그인 (기본 인증 흐름)

```bash
# signup
curl -s -X POST http://localhost:8080/api/v1/users/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# login (session cookie 저장)
curl -s -X POST http://localhost:8080/api/v1/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser&password=password123" \
  -c cookies.txt -i

# 내 정보 조회
curl -s http://localhost:8080/api/v1/users/me -b cookies.txt
```

## 4) 종료/초기화

```bash
# 컨테이너만 종료
docker compose down

# 볼륨까지 삭제(데이터 초기화)
docker compose down -v
```

## 5) 로컬 JVM 실행 (선택)

DB/Redis/RustFS만 compose로 띄우고 앱은 로컬 Java로 실행할 수 있습니다.

```bash
cd sns-app
docker compose up -d postgres redis rustfs
./gradlew bootRun
```

앱 설정 기본값(`src/main/resources/application.yaml`)은 localhost 의존성을 사용합니다.

## 6) 자주 막히는 문제

- `actuator/health`가 `DOWN`
  - `docker compose ps`로 postgres/redis 상태 확인
  - postgres가 늦게 떠서 초기 실패할 수 있으니 20~40초 뒤 재확인
- `401 Unauthorized`
  - 로그인 쿠키(`-c cookies.txt`) 저장 후 요청에 `-b cookies.txt` 사용
- 포트 충돌 (`8080`, `5432`, `6379`, `9000`, `9001`)
  - 기존 프로세스/컨테이너 종료 후 재시도

# 
