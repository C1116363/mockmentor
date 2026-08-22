#!/usr/bin/env bash
# Starts the Spring Boot API on http://localhost:8080
# DB credentials come from .env so they never end up in application.properties.
set -euo pipefail

cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

exec mvn spring-boot:run
