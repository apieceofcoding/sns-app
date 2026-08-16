#!/usr/bin/env bash
# Phase 0. 원본 앱 실행
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "==> 인프라 기동 (postgres, redis, rustfs)"
docker compose up -d

echo "==> healthy 대기"
for _ in $(seq 1 40); do
    ready=$(docker compose ps --format '{{.Health}}' 2>/dev/null | grep -c healthy || true)
    [ "$ready" -ge 2 ] && break
    sleep 2
done
docker compose ps

echo
echo "rustfs 가 unhealthy 로 보이는 것은 컨테이너 내부 IPv6 헬스체크 오탐입니다."
echo
echo "==> 앱 실행 (Ctrl+C 로 종료)"
./gradlew bootRun
