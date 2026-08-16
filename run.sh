#!/usr/bin/env bash
# Phase 4. GitOps 와 ArgoCD
# 사용법: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "이 단계는 앱 코드 작업이 없습니다."
echo "ArgoCD 설치와 Application 등록은 sns-devops 에서 진행합니다."
echo
echo "  git -C ../sns-devops checkout part-4-gitops-argocd && (cd ../sns-devops && ./run.sh)"
