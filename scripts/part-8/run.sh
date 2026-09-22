#!/usr/bin/env bash
# 사용법: ./run.sh rate|error|latency
set -euo pipefail
base_url="${BASE_URL:-http://sns.localhost}"
duration="${DURATION_SECONDS:-180}"
recovery="${RECOVERY_SECONDS:-180}"
case "${1:-}" in
    rate) path=ok; expected=200; pause=0.05 ;;
    error) path=error; expected=500; pause=1 ;;
    latency) path=slow; expected=200; pause=1 ;;
    *) echo "사용법: $0 rate|error|latency" >&2; exit 2 ;;
esac
for seconds in "$duration" "$recovery"; do
    if ! [[ "$seconds" =~ ^[1-9][0-9]*$ ]]; then
        echo "실행 시간은 양의 정수(초)로 입력하세요." >&2
        exit 2
    fi
done

request() {
    local status
    status=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 10 "$base_url/api/v1/demo/$1")
    if [ "$status" != "$2" ]; then
        echo "예상 HTTP $2, 실제 $status: $1 (08강 앱 배포를 확인하세요.)" >&2
        exit 1
    fi
}

traffic() {
    local end=$((SECONDS + $1))
    while (( SECONDS < end )); do
        request "$2" "$3"
        sleep "$4"
    done
}

trap 'echo "요청을 중단했습니다. 정상 요청으로 복구를 확인하세요."; exit 130' INT TERM
request "$path" "$expected"
echo "==> 정상 요청으로 30초 준비합니다"
traffic 30 ok 200 1
echo "==> $1 요청을 ${duration}초 보냅니다. Prometheus Alerts와 Slack을 확인하세요."
traffic "$duration" "$path" "$expected" "$pause"
echo "==> 정상 요청으로 ${recovery}초 복구합니다"
traffic "$recovery" ok 200 1
echo "완료. 경보가 inactive로 돌아왔는지 확인하세요. Slack 해소 알림은 조금 더 걸릴 수 있어요."
