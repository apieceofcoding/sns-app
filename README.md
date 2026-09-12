# sns-app

> 현재 브랜치는 04강 완료본입니다. 직접 따라 만들려면 `main`에서 시작하세요.
> 완료본의 실행 방법은 [scripts/README.md](scripts/README.md)를 참고해요. 아래 기본 앱과 시작 골격 안내는 `main` 기준입니다.

배포와 관측 가능성 강의에서 사용하는 Spring Boot SNS 애플리케이션입니다.
`main`의 기본 앱에서 바로 01강을 시작해요. 강의를 따라 컨테이너화, CI, 메트릭, 로그와 트레이스를 직접 추가합니다.

## 실습 방식

이 저장소와 [sns-devops](https://github.com/apieceofcoding/sns-devops)를 자신의 GitHub 계정으로 Fork하고 나란히 clone하세요.
수강생은 두 저장소의 `main`에서 변경을 커밋하며 끝까지 진행합니다.

| 브랜치 | 용도 |
| --- | --- |
| `main` | 실습 시작 코드. 자신의 Fork에서 학습 내용을 누적해요. |
| `part-1-containerization` ~ `part-9-ai-agent-analysis` | 각 강의까지 완료한 코드 |
| `backup/main` | 실습 시작 상태로 바꾸기 전의 기존 `main` 백업 |

완료본은 강사 저장소의 `part-*`에서 확인할 수 있습니다. 예를 들어 05강을 직접 따라 만들 때의 시작점은 04강 완료본이고, 비교할 정답은 05강 완료본이에요.
처음부터 실습한다면 매 강의마다 브랜치를 바꿀 필요가 없습니다.

## 준비

- JDK 25
- Docker Desktop 또는 Docker Engine과 Compose
- Git과 `curl`
- Windows에서는 Git Bash

```bash
java -version
docker compose version
git branch --show-current
```

현재 브랜치가 `main`인지 확인하세요. Gradle은 저장소의 wrapper를 사용합니다.

## 기본 앱 실행

아래 명령은 `sns-app` 디렉터리에서 실행해요.
현재 Compose에는 PostgreSQL, Redis, RustFS가 있고, 앱은 로컬 JVM에서 실행합니다.

```bash
docker compose up -d
docker compose ps
./gradlew bootRun
```

앱이 시작되면 다른 터미널에서 회원가입을 실행하세요. 같은 사용자로 다시 확인할 때는 로그인부터 진행합니다.

```bash
curl -i -X POST http://localhost:8080/api/v1/users/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"password123"}'
```

로그인한 뒤 내 정보를 조회해요. 응답이 HTTP 200이고 `username`이 `student`이면 기본 동작을 확인한 것입니다.

```bash
curl -i -X POST http://localhost:8080/api/v1/login \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'username=student&password=password123' \
  -c /tmp/sns-app-cookies.txt

curl -i http://localhost:8080/api/v1/users/me \
  -b /tmp/sns-app-cookies.txt
```

`/actuator/health`는 01강에서 추가합니다. 기본 앱에서는 위 API로 실행 상태를 확인하세요.
한 번에 실행하려면 `./scripts/start.sh`를 사용할 수 있어요.

종료할 때는 앱 터미널에서 `Ctrl+C`를 누른 뒤 다음 명령을 실행합니다. DB 데이터는 볼륨에 유지돼요.

```bash
docker compose down
```

## 제공된 실행 스크립트

`main`에는 01강부터 09강까지의 `scripts/`가 모두 들어 있습니다.
해당 단원의 코드를 작성한 뒤 폴더로 들어가 실행하세요. 스크립트는 직접 작성하지 않아도 돼요.

```bash
# sns-app 폴더에서 실행
cd scripts/part-1
./run.sh
```

단원별 명령은 [scripts/README.md](scripts/README.md)에 정리했습니다.

## 다음 단계

01강에서는 현재 코드에 Actuator, Jib, Compose의 앱 서비스를 추가합니다.
02강부터 Kubernetes 설정은 `sns-devops/main`에 작성해요.

03강과 04강에서 구성할 배포 흐름은 다음과 같습니다.

```text
내 sns-app/main에 push
  → GitHub Actions에서 테스트와 이미지 발행
  → 내 sns-devops/main의 이미지 태그 갱신
  → Argo CD가 main의 변경을 감지해 Kind에 배포
```

CI의 이미지 주소와 대상 저장소는 자신의 계정으로 설정하세요. `sns-devops` checkout은 `ref: main`, Argo CD는 `targetRevision: main`으로 맞춥니다.
