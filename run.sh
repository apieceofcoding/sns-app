#!/usr/bin/env bash
# Phase 5. 메트릭
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "==> 클러스터의 앱에서 prometheus 엔드포인트 확인"
kubectl port-forward svc/sns-app -n sns 18080:8080 >/dev/null 2>&1 &
PF=$!
trap 'kill $PF 2>/dev/null || true' EXIT
sleep 3

if curl -fsS localhost:18080/actuator/prometheus | head -20; then
    echo
    echo "메트릭이 노출됩니다."
else
    echo "엔드포인트 응답이 없습니다. sns-app Pod 상태를 확인하세요." >&2
    exit 1
fi
