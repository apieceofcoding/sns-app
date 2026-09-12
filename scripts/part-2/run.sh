#!/usr/bin/env bash
# Phase 2. kind 에 올릴 이미지 준비
# scripts/part-2 폴더에서 실행: ./run.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/common.sh
require_setting build.gradle.kts com.google.cloud.tools.jib

echo "==> 이미지 생성 (Jib)"
./gradlew jibDockerBuild
docker images springboot-sns:latest

echo
echo "다음: sns-devops에서 02강 설정을 작성한 뒤 scripts/part-2에서 ./run.sh를 실행하세요."
echo "  sns-devops/scripts/part-2 폴더에서 ./run.sh를 실행하세요."
