#!/usr/bin/env bash
# Phase 6. 로그
# scripts/part-6 폴더에서 실행: ./run.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/common.sh
require_files src/main/resources/logback-spring.xml src/main/java/com/apiece/springboot_sns_sample/config/OpenTelemetryLogConfig.java

echo "06강의 OTLP 로그 전송 설정을 작성하고 앱을 다시 배포한 뒤 로그를 확인합니다."
echo "수집기(Loki, OTel Collector) 설치는 sns-devops 에서 진행합니다."
echo
echo "  sns-devops/scripts/part-6 폴더에서 ./run.sh를 실행하세요."
echo
echo "==> 앱 로그 확인"
kubectl logs -n sns -l app=sns-app --tail=20 2>/dev/null || echo "  클러스터에 배포된 앱이 없습니다."
