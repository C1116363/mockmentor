#!/usr/bin/env bash
#
# Starts all three servers: backend :8080, frontend :5173, website :3000.
#
# Checks your tools and your database first, because a missing prerequisite is
# far easier to read here than as a Java stack trace ninety seconds later.
#
#   ./start.sh              start everything
#   ./start.sh backend      start just one part (backend | frontend | website)
#
# Ctrl+C stops everything it started.

set -uo pipefail
cd "$(dirname "$0")"

BOLD=$'\033[1m'; RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; DIM=$'\033[2m'; OFF=$'\033[0m'
ok()   { printf "  ${GREEN}✓${OFF} %s\n" "$1"; }
warn() { printf "  ${YELLOW}!${OFF} %s\n" "$1"; }
bad()  { printf "  ${RED}✗${OFF} %s\n" "$1"; }
die()  { printf "\n${RED}${BOLD}Stopped.${OFF} %s\n\n" "$1"; exit 1; }

LOG_DIR=".logs"
mkdir -p "$LOG_DIR"
PIDS=()

# Kill whatever we started, however we exit. Without the trap, Ctrl+C would
# leave three orphaned servers holding their ports.
cleanup() {
  printf "\n${BOLD}Stopping...${OFF}\n"
  for pid in "${PIDS[@]:-}"; do
    [ -n "$pid" ] && kill "$pid" 2>/dev/null && printf "  stopped %s\n" "$pid"
  done
  wait 2>/dev/null
  exit 0
}
trap cleanup INT TERM

WHICH="${1:-all}"

# ---------------------------------------------------------------- prerequisites
printf "\n${BOLD}Checking what you have${OFF}\n"

need() {
  command -v "$1" >/dev/null 2>&1 || { bad "$1 not found — $2"; return 1; }
  return 0
}

MISSING=0
if [ "$WHICH" = "all" ] || [ "$WHICH" = "backend" ]; then
  need java "install Java 21+ (brew install openjdk@21 / apt install openjdk-21-jdk)" || MISSING=1
  need mvn  "install Maven (brew install maven / apt install maven)" || MISSING=1
fi
if [ "$WHICH" = "all" ] || [ "$WHICH" = "frontend" ]; then
  need node "install Node 18+ (brew install node / apt install nodejs npm)" || MISSING=1
fi
if [ "$WHICH" = "all" ] || [ "$WHICH" = "website" ]; then
  need python3 "install Python 3 (it only serves one static file)" || MISSING=1
fi
[ "$MISSING" = "1" ] && die "Install what is missing above, then run this again. See SETUP.md."

if command -v java >/dev/null 2>&1; then
  JAVA_MAJOR=$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1)
  if [ -n "${JAVA_MAJOR:-}" ] && [ "$JAVA_MAJOR" -lt 21 ]; then
    die "Java $JAVA_MAJOR found, but this needs 21 or higher. See SETUP.md."
  fi
  ok "Java $JAVA_MAJOR"
fi
command -v node >/dev/null 2>&1 && ok "Node $(node --version)"

# Maven and Python are checked above and would have stopped the run if missing,
# but only Java and Node reported a version - so a first run listed two of the
# four things it had actually verified and looked like it had skipped the rest.
# `mvn -v` is slow enough to notice (it boots a JVM), hence the trimmed one-liner.
command -v mvn >/dev/null 2>&1 && ok "Maven $(mvn -v 2>/dev/null | head -1 | awk '{print $3}')"
command -v python3 >/dev/null 2>&1 && ok "Python $(python3 --version 2>&1 | awk '{print $2}')"

# ---------------------------------------------------------------------- database
if [ "$WHICH" = "all" ] || [ "$WHICH" = "backend" ]; then
  printf "\n${BOLD}Checking the database${OFF}\n"

  if ! nc -z 127.0.0.1 3306 2>/dev/null; then
    bad "Nothing is listening on 3306 — MySQL is not running."
    printf "      ${DIM}macOS:  brew services start mysql${OFF}\n"
    printf "      ${DIM}Linux:  sudo systemctl start mysql${OFF}\n"
    die "Start MySQL, then run this again."
  fi
  ok "MySQL is listening on 3306"

  [ -f backend/.env ] || {
    bad "backend/.env is missing."
    printf "      ${DIM}cp backend/.env.example backend/.env${OFF}   then put your MySQL password in it\n"
    die "See SETUP.md step 4."
  }
  ok "backend/.env found"

  # shellcheck disable=SC1091
  set -a; source backend/.env; set +a
  DB_USER="${DB_USER:-root}"
  DB_PASSWORD="${DB_PASSWORD:-}"

  # Create the schema up front. The JDBC URL would do it anyway, but doing it
  # here means a wrong password reads as one line instead of a stack trace.
  if command -v mysql >/dev/null 2>&1; then
    if MYSQL_PWD="$DB_PASSWORD" mysql -u "$DB_USER" -e \
        "CREATE DATABASE IF NOT EXISTS interview_mentor
         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null; then
      ok "Database 'interview_mentor' ready (user: $DB_USER)"
    else
      bad "Could not connect to MySQL as '$DB_USER'."
      printf "      Check DB_USER / DB_PASSWORD in backend/.env. Test it with:\n"
      printf "      ${DIM}mysql -u %s -p -e 'SELECT 1;'${OFF}\n" "$DB_USER"
      die "See the troubleshooting section in SETUP.md."
    fi
  else
    warn "mysql client not installed — skipping the check. The backend will still create the schema."
  fi
fi

# ------------------------------------------------------------------ ports free?
printf "\n${BOLD}Checking ports${OFF}\n"
port_busy() { lsof -ti:"$1" >/dev/null 2>&1; }
for spec in "8080 backend" "5173 frontend" "3000 website"; do
  set -- $spec
  { [ "$WHICH" != "all" ] && [ "$WHICH" != "$2" ]; } && continue
  if port_busy "$1"; then
    bad "Port $1 ($2) is already in use."
    printf "      ${DIM}lsof -ti:%s | xargs kill${OFF}\n" "$1"
    die "Free the port, then run this again."
  fi
  ok "port $1 free ($2)"
done

# --------------------------------------------------------------------- start up
start_backend() {
  printf "  starting backend"
  (cd backend && ./run.sh) > "$LOG_DIR/backend.log" 2>&1 &
  PIDS+=($!)
  # Poll a real endpoint rather than sleeping - Maven's first run can take
  # minutes, and a fixed sleep is either too short or wasted time.
  for _ in $(seq 1 180); do
    curl -s -o /dev/null --max-time 2 http://localhost:8080/api/public/plans && {
      printf "\r  ${GREEN}✓${OFF} backend    http://localhost:8080          ${DIM}%s/backend.log${OFF}\n" "$LOG_DIR"; return 0; }
    grep -qE "APPLICATION FAILED|BUILD FAILURE" "$LOG_DIR/backend.log" 2>/dev/null && {
      printf "\r  ${RED}✗${OFF} backend failed to start\n"
      printf "\n${DIM}--- last 25 lines of %s/backend.log ---${OFF}\n" "$LOG_DIR"
      tail -25 "$LOG_DIR/backend.log"; return 1; }
    printf "."; sleep 1
  done
  printf "\r  ${RED}✗${OFF} backend timed out after 3 minutes — see %s/backend.log\n" "$LOG_DIR"; return 1
}

start_frontend() {
  if [ ! -d frontend/node_modules ]; then
    printf "  installing frontend dependencies (first run, this takes a minute)..."
    (cd frontend && npm install) > "$LOG_DIR/npm-install.log" 2>&1 \
      || { printf "\r  ${RED}✗${OFF} npm install failed — see %s/npm-install.log\n" "$LOG_DIR"; return 1; }
    printf "\r  ${GREEN}✓${OFF} frontend dependencies installed                    \n"
  fi
  printf "  starting frontend"
  (cd frontend && npm run dev) > "$LOG_DIR/frontend.log" 2>&1 &
  PIDS+=($!)
  for _ in $(seq 1 60); do
    curl -s -o /dev/null --max-time 2 http://localhost:5173/ && {
      printf "\r  ${GREEN}✓${OFF} frontend   http://localhost:5173          ${DIM}%s/frontend.log${OFF}\n" "$LOG_DIR"; return 0; }
    printf "."; sleep 1
  done
  printf "\r  ${RED}✗${OFF} frontend timed out — see %s/frontend.log\n" "$LOG_DIR"; return 1
}

start_website() {
  printf "  starting website"
  (cd website && ./serve.sh) > "$LOG_DIR/website.log" 2>&1 &
  PIDS+=($!)
  for _ in $(seq 1 20); do
    curl -s -o /dev/null --max-time 2 http://localhost:3000/ && {
      printf "\r  ${GREEN}✓${OFF} website    http://localhost:3000          ${DIM}%s/website.log${OFF}\n" "$LOG_DIR"; return 0; }
    printf "."; sleep 1
  done
  printf "\r  ${RED}✗${OFF} website timed out — see %s/website.log\n" "$LOG_DIR"; return 1
}

printf "\n${BOLD}Starting${OFF}\n"
FAILED=0
case "$WHICH" in
  all)      start_backend || FAILED=1
            [ "$FAILED" = "0" ] && { start_frontend || FAILED=1; }
            [ "$FAILED" = "0" ] && { start_website  || FAILED=1; } ;;
  backend)  start_backend  || FAILED=1 ;;
  frontend) start_frontend || FAILED=1 ;;
  website)  start_website  || FAILED=1 ;;
  *)        die "Unknown target '$WHICH'. Use: all | backend | frontend | website" ;;
esac

[ "$FAILED" = "1" ] && { cleanup; }

cat <<BANNER

${BOLD}Running.${OFF}

  The app          ${BOLD}http://localhost:5173${OFF}
  Marketing site   http://localhost:3000
  API docs         http://localhost:8080/swagger-ui.html

  Log in with      ${BOLD}admin@example.com${OFF} / ${BOLD}password123${OFF}
                   (also rahul@ = student, ananya@ = mentor — same password)

  Logs in ./${LOG_DIR}/          ${DIM}Ctrl+C stops everything${OFF}

BANNER

# Hold the terminal open so the trap can catch Ctrl+C.
wait
