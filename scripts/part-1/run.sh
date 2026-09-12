#!/usr/bin/env bash
# Phase 1. 컨테이너화
# scripts/part-1 폴더에서 실행: ./run.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/common.sh
require_setting build.gradle.kts com.google.cloud.tools.jib
require_setting docker-compose.yml "  app:"

echo "==> 앱 이미지 빌드 (Jib)"
./gradlew jibDockerBuild

echo "==> 앱 포함 전체 스택 기동"
docker compose up -d
docker compose ps

echo "==> 앱 health 대기"
for _ in $(seq 1 60); do
    curl -fsS localhost:8080/actuator/health >/dev/null 2>&1 && break
    sleep 2
done

echo "==> 확인"
curl -fsS localhost:8080/actuator/health; echo
echo
echo "종료하려면: docker compose down"
