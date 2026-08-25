#!/bin/sh
# Fix up the upload directory, then hand the JVM to an unprivileged user.
#
# Why this script exists at all
# ----------------------------
# A mounted volume REPLACES whatever the image had at that path, and it arrives
# owned by root. So the `chown app:app /data` in the Dockerfile is undone the
# moment a volume is attached - and all three Storage classes call
# Files.createDirectories() in their constructor, so the app does not degrade,
# it fails to start. Every deploy, with an AccessDeniedException that names a
# path the Dockerfile clearly created.
#
# The fix has to happen at runtime, after the mount, which means starting as
# root and dropping privileges before exec'ing Java.
set -e

UPLOAD_ROOT="${UPLOAD_ROOT:-/data/uploads}"

mkdir -p "$UPLOAD_ROOT"
chown -R app:app "$UPLOAD_ROOT" 2>/dev/null || true

# exec, not a plain call: the JVM replaces this shell as PID 1 so it receives
# SIGTERM directly when the platform stops the container. Wrapped in a shell
# that lingers, the JVM gets killed instead of shutting down, and Spring's
# shutdown hooks - closing the connection pool, finishing in-flight requests -
# never run.
if command -v setpriv >/dev/null 2>&1; then
  exec setpriv --reuid=app --regid=app --clear-groups \
    java -XX:MaxRAMPercentage=75 -jar /app/app.jar "$@"
fi

# setpriv is in util-linux and present on every Debian/Ubuntu base, so this
# should not be reached. Starting as root beats refusing to start, but say so
# loudly rather than letting it pass unnoticed.
echo "WARNING: setpriv not found - running the JVM as root." >&2
exec java -XX:MaxRAMPercentage=75 -jar /app/app.jar "$@"
