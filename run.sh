#!/usr/bin/env bash
# Phase 2. kind 에 올릴 이미지 준비
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "==> 이미지 생성 (Jib)"
./gradlew jibDockerBuild
docker images springboot-sns:latest

echo
echo "다음: sns-devops 로 이동해 ./run.sh 를 실행하세요."
echo "  git -C ../sns-devops checkout part-2-kind-deployment && (cd ../sns-devops && ./run.sh)"
