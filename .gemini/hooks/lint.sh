#!/bin/bash

# AfterTool hook: Run spotlessApply on project files after write_file/replace_in_file

INPUT=$(cat)

# Extract file_path from tool_input
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.path // empty')

# Check if it's a file type that Spotless handles
case "$FILE_PATH" in
  *.java|*.kts|*.json|*.yaml|*.yml|*.md|*.properties|*.xml|*.sql)
    echo "[lint hook] Running spotlessApply on: $FILE_PATH" >&2
    cd "$GEMINI_PROJECT_DIR" || exit 0
    ./gradlew spotlessApply -q 2>/dev/null
    ;;
esac

# Return empty JSON to indicate success without modifications
echo '{}'
exit 0
