#!/usr/bin/env bash
# Phase 8. 알림과 장애 대응
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

# 아래 반복문은 실패를 모두 삼키므로, 접근 자체가 안 되면 여기서 먼저 알려 줍니다.
if ! curl -fsS -o /dev/null --max-time 5 "http://sns.localhost/actuator/health" 2>/dev/null; then
    echo "http://sns.localhost 에 접근할 수 없습니다. 클러스터와 Ingress 를 확인하세요." >&2
    hosts_hint
    exit 1
fi

echo "==> 정상 요청과 에러 요청을 섞어 발생시킵니다"
for _ in $(seq 1 10); do
    curl -fsS -o /dev/null "http://sns.localhost/api/v1/demo/trace?userId=1" 2>/dev/null || true
done
for _ in $(seq 1 20); do
    curl -sS -o /dev/null http://sns.localhost/api/v1/demo/error 2>/dev/null || true
done
echo "완료. Grafana 에서 메트릭, 로그, 트레이스를 함께 확인하세요."
echo "  http://grafana.localhost"
