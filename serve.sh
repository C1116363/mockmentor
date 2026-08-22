#!/usr/bin/env bash
# Serves the public website at http://localhost:3000
# It is a single static HTML file - any static server works.
set -euo pipefail
cd "$(dirname "$0")"
echo "Website: http://localhost:3000"
exec python3 -m http.server 3000
