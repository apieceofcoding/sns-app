#!/usr/bin/env bash
# Phase 6. 로그
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "이 단계의 앱 설정(OTLP 로그 전송)은 이미 이 브랜치에 반영되어 있습니다."
echo "수집기(Loki, OTel Collector) 설치는 sns-devops 에서 진행합니다."
echo
echo "  git -C ../sns-devops checkout part-6-logs && (cd ../sns-devops && ./run.sh)"
echo
echo "==> 앱 로그 확인"
kubectl logs -n sns -l app=sns-app --tail=20 2>/dev/null || echo "  클러스터에 배포된 앱이 없습니다."
