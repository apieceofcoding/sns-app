#!/usr/bin/env bash
# Phase 7. 트레이스
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

# 윈도우는 *.localhost 를 자동으로 127.0.0.1 로 풀지 않습니다. 안 될 때 안내할 위치를 고릅니다.
hosts_hint() {
    case "$(uname -s)" in
        MINGW* | MSYS* | CYGWIN*) file='C:\Windows\System32\drivers\etc\hosts (관리자 권한)' ;;
        *) file='/etc/hosts (sudo)' ;;
    esac
    echo "  이름이 풀리지 않으면 $file 에 아래 줄을 추가하세요." >&2
    echo "  127.0.0.1 sns.localhost grafana.localhost prometheus.localhost loki.localhost tempo.localhost argocd.localhost" >&2
}

echo "이 단계의 앱 설정(샘플링, 데모 컨트롤러, 추천 서비스 연동)은 이미 이 브랜치에 반영되어 있습니다."
echo "Tempo 설치와 추천 서비스 배포는 sns-devops 에서 진행합니다."
echo
echo "==> 트레이스 생성용 호출 (ga 세그먼트, 추천 서비스까지 이어집니다)"
if curl -fsS "http://sns.localhost/api/v1/demo/trace?userId=1" 2>/dev/null; then
    echo
else
    echo "  http://sns.localhost 에 접근할 수 없습니다. 클러스터와 Ingress 를 확인하세요." >&2
    hosts_hint
fi
