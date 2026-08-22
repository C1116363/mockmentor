#!/usr/bin/env bash
# Serves the landing page at http://localhost:3000
# It is a single static HTML file - any static server works.
set -euo pipefail
cd "$(dirname "$0")"
echo "Landing page: http://localhost:3000"
exec python3 -m http.server 3000
