#!/usr/bin/env bash
# Phase 3. CI/CD 트리거
# 사용법: ./run.sh          워크플로우 상태만 확인
#         ./run.sh --push   빈 커밋을 푸시해 CI 를 실제로 트리거
set -euo pipefail
cd "$(dirname "$0")"

if [ "${1:-}" = "--push" ]; then
    echo "==> 빈 커밋 푸시로 CI 트리거"
    git commit --allow-empty -m "ci: trigger build"
    git push
else
    echo "푸시하지 않았습니다. 실제로 트리거하려면 ./run.sh --push"
fi

echo
echo "==> 최근 워크플로우 실행"
gh run list --limit 5 2>/dev/null || echo "  gh 인증이 필요합니다: gh auth login"

echo
echo "CI 가 끝나면 sns-devops 의 app.yaml 이미지 태그가 갱신됩니다."
echo "  git -C ../sns-devops pull && grep 'image:' ../sns-devops/k8s/sns-app/app.yaml"
