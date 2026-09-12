#!/usr/bin/env bash
# Phase 3. CI/CD 트리거
# scripts/part-3 폴더에서 실행: ./run.sh          워크플로우 상태만 확인
#         ./run.sh --push   빈 커밋을 푸시해 CI 를 실제로 트리거
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/common.sh
require_files .github/workflows/ci.yaml

if [ "${1:-}" = "--push" ]; then
    if [ "$(git branch --show-current)" != main ]; then
        echo "CI 실습은 main에서 진행하세요." >&2
        exit 1
    fi
    if [ -n "$(git status --porcelain)" ]; then
        echo "작성한 코드를 먼저 커밋한 뒤 --push를 실행하세요." >&2
        exit 1
    fi
    echo "==> 빈 커밋 푸시로 CI 트리거"
    git commit --allow-empty -m "ci: trigger build"
    git push
else
    echo "푸시하지 않았습니다. 현재 단원 폴더에서 ./run.sh --push로 트리거하세요."
fi

echo
echo "==> 최근 워크플로우 실행"
gh run list --limit 5 2>/dev/null || echo "  gh 인증이 필요합니다: gh auth login"

echo
echo "CI 가 끝나면 sns-devops 의 app.yaml 이미지 태그가 갱신됩니다."
echo "  sns-devops/scripts/part-3 폴더에서 ./run.sh로 갱신한 매니페스트를 배포하세요."
