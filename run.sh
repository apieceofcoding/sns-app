#!/usr/bin/env bash
# Phase 7. 트레이스
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "이 단계의 앱 설정(샘플링, 데모 컨트롤러, 추천 서비스 연동)은 이미 이 브랜치에 반영되어 있습니다."
echo "Tempo 설치와 추천 서비스 배포는 sns-devops 에서 진행합니다."
echo
echo "==> 트레이스 생성용 호출 (ga 세그먼트, 추천 서비스까지 이어집니다)"
curl -fsS "http://sns.localhost/api/v1/demo/trace?userId=1" 2>/dev/null && echo \
    || echo "  http://sns.localhost 에 접근할 수 없습니다. 클러스터와 Ingress 를 확인하세요."
