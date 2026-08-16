#!/usr/bin/env bash
# Phase 9. AI Agent 기반 RCA
# 사용법: ./run.sh [요청수]   (기본 60)
set -euo pipefail
cd "$(dirname "$0")"
N="${1:-60}"

echo "==> 장애 트래픽 생성 (userId 3의 배수는 beta 세그먼트라 실패합니다)"
ok=0; err=0
for i in $(seq 1 "$N"); do
    code=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://sns.localhost/api/v1/demo/feed?userId=$i" 2>/dev/null || echo 000)
    if [ "$code" = "200" ]; then ok=$((ok+1)); else err=$((err+1)); fi
done

echo "  성공 $ok / 실패 $err"
echo
echo "이제 sns-devops 에서 원인을 찾습니다."
echo "  git -C ../sns-devops checkout part-9-ai-agent-rca"
echo "  cd ../sns-devops && tools/obsctl rca sns-app"
echo
echo "또는 Claude Code 에게: \"sns-app 에러율이 올랐는데 원인 찾아줘\""
