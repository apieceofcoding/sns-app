#!/usr/bin/env bash
# Phase 7. 트레이스
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "이 단계의 앱 설정(샘플링, 데모 컨트롤러)은 이미 이 브랜치에 반영되어 있습니다."
echo "Tempo 설치는 sns-devops 에서 진행합니다."
echo
echo "==> 트레이스 생성용 호출"
curl -fsS http://sns.localhost/api/v1/demo/trace 2>/dev/null && echo \
    || echo "  http://sns.localhost 에 접근할 수 없습니다. 클러스터와 Ingress 를 확인하세요."
