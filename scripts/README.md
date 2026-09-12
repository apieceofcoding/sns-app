# 실습 스크립트

`main`에서 단원의 코드를 작성한 뒤, 해당 폴더에서 `./run.sh`를 실행하세요.
스크립트는 이미 제공되어 있으므로 직접 작성하지 않아도 됩니다.

```bash
# sns-app 폴더에서 01강 실행
cd scripts/part-1
./run.sh

# 다음 단원으로 이동
cd ../part-2
./run.sh
```

| 폴더 | 실행 명령 | 하는 일 |
| --- | --- | --- |
| `part-1` | `./run.sh` | 컨테이너 이미지 빌드와 Compose 실행 |
| `part-2` | `./run.sh` | Kind에 넣을 이미지 빌드 |
| `part-3` | `./run.sh` | CI 상태 확인 |
| `part-3` | `./run.sh --push` | 커밋을 마친 main에서 CI 트리거 |
| `part-4` | `./run.sh` | DevOps 실습 위치 안내 |
| `part-5` | `./run.sh` | 배포된 앱의 메트릭 확인 |
| `part-6` | `./run.sh` | 배포된 앱의 로그 확인 |
| `part-7` | `./run.sh` | 트레이스 요청 |
| `part-8` | `./run.sh` | 장애 트래픽 발생 |
| `part-9` | `./run.sh 60` | 장애 분석용 요청 60회 |

기본 앱만 실행하려면 `sns-app` 폴더에서 `./scripts/start.sh`를 사용해요.
05강 이후에는 변경한 앱을 배포하고 필요한 관측 스택을 준비한 뒤 실행합니다.
