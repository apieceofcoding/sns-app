#!/usr/bin/env bash
# Phase 9. AI Agent 기반 장애 분석
# 사용법: ./run.sh [요청수]   (기본 60)
set -euo pipefail
cd "$(dirname "$0")"
N="${1:-60}"

# 윈도우는 *.localhost 를 자동으로 127.0.0.1 로 풀지 않습니다. 안 될 때 안내할 위치를 고릅니다.
hosts_hint() {
    case "$(uname -s)" in
        MINGW* | MSYS* | CYGWIN*) file='C:\Windows\System32\drivers\etc\hosts (관리자 권한)' ;;
        *) file='/etc/hosts (sudo)' ;;
    esac
    echo "  이름이 풀리지 않으면 $file 에 아래 줄을 추가하세요." >&2
    echo "  127.0.0.1 sns.localhost grafana.localhost prometheus.localhost loki.localhost tempo.localhost argocd.localhost" >&2
}

echo "==> 장애 트래픽 생성 (userId 3의 배수는 beta 세그먼트라 실패합니다)"
ok=0; err=0
for i in $(seq 1 "$N"); do
    code=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://sns.localhost/api/v1/demo/feed?userId=$i" 2>/dev/null || echo 000)
    if [ "$code" = "200" ]; then ok=$((ok+1)); else err=$((err+1)); fi
done

echo "  성공 $ok / 실패 $err"

# 하나도 성공하지 못했다면 앱 장애가 아니라 접근 자체가 막힌 것입니다.
if [ "$ok" -eq 0 ]; then
    echo
    echo "http://sns.localhost 에 한 번도 닿지 못했습니다. 클러스터와 Ingress 를 확인하세요." >&2
    hosts_hint
    exit 1
fi

echo
echo "이제 sns-devops 에서 원인을 찾습니다."
echo "  git -C ../sns-devops checkout part-9-ai-agent-analysis"
echo "  cd ../sns-devops && tools/obsctl analyze sns-app"
echo
echo "또는 Claude Code 에게: \"sns-app 에러율이 올랐는데 원인 찾아줘\""
