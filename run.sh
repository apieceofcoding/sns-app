#!/usr/bin/env bash
# Phase 8. 통합
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "==> 정상 요청과 에러 요청을 섞어 발생시킵니다"
for _ in $(seq 1 10); do
    curl -fsS -o /dev/null "http://sns.localhost/api/v1/demo/trace?userId=1" 2>/dev/null || true
done
for _ in $(seq 1 20); do
    curl -sS -o /dev/null http://sns.localhost/api/v1/demo/error 2>/dev/null || true
done
echo "완료. Grafana 에서 메트릭, 로그, 트레이스를 함께 확인하세요."
echo "  http://grafana.localhost"
