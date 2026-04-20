#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REQUIRE_CONFIG="false"

usage() {
  cat <<'EOF'
Usage: bash scripts/run-sonar.sh [--require-config]

Environment:
  SONAR_HOST_URL       Required SonarQube server URL
  SONAR_TOKEN          Required SonarQube token
  SONAR_PROJECT_KEY    Optional project key, defaults to sqlforge
  SONAR_PROJECT_NAME   Optional project name, defaults to SQLForge
  SONAR_QUALITY_GATE_WAIT  Optional, defaults to true
  SONAR_MAVEN_ARGS     Optional extra Maven arguments

Behavior:
  Without --require-config, the script exits successfully when Sonar
  environment variables are not configured yet. This keeps local and CI
  entry points executable before secrets are provisioned.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --require-config)
      REQUIRE_CONFIG="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

missing=()
[[ -n "${SONAR_HOST_URL:-}" ]] || missing+=("SONAR_HOST_URL")
[[ -n "${SONAR_TOKEN:-}" ]] || missing+=("SONAR_TOKEN")

if [[ ${#missing[@]} -gt 0 ]]; then
  if [[ "$REQUIRE_CONFIG" == "true" ]]; then
    echo "Missing required Sonar configuration: ${missing[*]}" >&2
    exit 1
  fi
  echo "Skipping SonarQube scan because configuration is not complete: ${missing[*]}"
  exit 0
fi

cd "$ROOT_DIR"

PROJECT_KEY="${SONAR_PROJECT_KEY:-sqlforge}"
PROJECT_NAME="${SONAR_PROJECT_NAME:-SQLForge}"
QUALITY_GATE_WAIT="${SONAR_QUALITY_GATE_WAIT:-true}"
EXTRA_ARGS=()
if [[ -n "${SONAR_MAVEN_ARGS:-}" ]]; then
  # shellcheck disable=SC2206
  EXTRA_ARGS=(${SONAR_MAVEN_ARGS})
fi

echo "Running SonarQube scan for project: $PROJECT_KEY"
mvn -B verify sonar:sonar \
  -DskipTests \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.projectKey="$PROJECT_KEY" \
  -Dsonar.projectName="$PROJECT_NAME" \
  -Dsonar.qualitygate.wait="$QUALITY_GATE_WAIT" \
  "${EXTRA_ARGS[@]}"
