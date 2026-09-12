#!/usr/bin/env bash
# Phase 7. 트레이스
# scripts/part-7 폴더에서 실행: ./run.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/common.sh
require_files src/main/java/com/apiece/springboot_sns_sample/api/ObservabilityDemoController.java src/main/java/com/apiece/springboot_sns_sample/domain/recommendation/RecommenderClient.java

# 윈도우는 *.localhost 를 자동으로 127.0.0.1 로 풀지 않습니다. 안 될 때 안내할 위치를 고릅니다.
hosts_hint() {
    case "$(uname -s)" in
        MINGW* | MSYS* | CYGWIN*) file='C:\Windows\System32\drivers\etc\hosts (관리자 권한)' ;;
        *) file='/etc/hosts (sudo)' ;;
    esac
    echo "  이름이 풀리지 않으면 $file 에 아래 줄을 추가하세요." >&2
    echo "  127.0.0.1 sns.localhost grafana.localhost prometheus.localhost loki.localhost tempo.localhost argocd.localhost" >&2
}

echo "07강의 샘플링, 데모 컨트롤러와 추천 서비스 연동을 작성하고 앱을 다시 배포하세요."
echo "Tempo 설치와 추천 서비스 배포는 sns-devops 에서 진행합니다."
echo
echo "==> 트레이스 생성용 호출 (ga 세그먼트, 추천 서비스까지 이어집니다)"
if curl -fsS "http://sns.localhost/api/v1/demo/trace?userId=1" 2>/dev/null; then
    echo
else
    echo "  http://sns.localhost 에 접근할 수 없습니다. 클러스터와 Ingress 를 확인하세요." >&2
    hosts_hint
fi
