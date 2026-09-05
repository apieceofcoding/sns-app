#!/usr/bin/env bash
# Phase 5. 메트릭
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "==> 클러스터의 앱에서 prometheus 엔드포인트 확인"
kubectl port-forward svc/sns-app -n sns 18080:8080 >/dev/null 2>&1 &
PF=$!
trap 'kill "$PF" 2>/dev/null || true' EXIT

# 포트포워드가 열리는 속도는 환경마다 다릅니다. 고정 대기 대신 열릴 때까지 확인합니다.
for _ in $(seq 1 20); do
    curl -fsS -o /dev/null localhost:18080/actuator/health 2>/dev/null && break
    sleep 1
done

# head 로 파이프하면 앞쪽 명령이 SIGPIPE 로 죽어 pipefail 이 실패로 판정합니다.
# 먼저 변수에 받아 두고 히어스트링으로 잘라 씁니다.
if metrics=$(curl -fsS localhost:18080/actuator/prometheus); then
    head -20 <<<"$metrics"
    echo
    echo "메트릭이 노출됩니다."
else
    echo "엔드포인트 응답이 없습니다. sns-app Pod 상태를 확인하세요." >&2
    exit 1
fi
