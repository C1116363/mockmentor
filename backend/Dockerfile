# Multi-stage: build with the full JDK and Maven, ship only the JRE and the jar.
#
# The runtime image has no compiler, no Maven and no source in it. That is
# smaller to push, faster to boot, and there is simply less inside it that could
# be exploited.

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# pom.xml alone first, so Docker caches the dependency download in its own
# layer. Copying the source at the same time would re-download every dependency
# on every code change, which is minutes each time.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests run in CI, not here. A deploy that fails because a test is flaky on a
# builder with different timing is a deploy blocked for no reason.
RUN mvn -B clean package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# The account the JVM actually runs as. If anything in the app is ever
# exploited, this is the difference between reaching the app's own files and
# owning the container.
RUN useradd --create-home --shell /bin/false app

# Uploads live here. Mount a PERSISTENT VOLUME at this path, or every CV,
# screenshot and study-material file disappears on the next deploy - the
# database rows survive and point at nothing.
#
# Created here for the no-volume case; when a volume IS mounted it replaces
# this directory entirely and arrives owned by root, which is what the
# entrypoint exists to fix.
RUN mkdir -p /data/uploads && chown -R app:app /data
ENV UPLOAD_ROOT=/data/uploads

COPY --from=build /build/target/*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chown app:app /app/app.jar && chmod +x /app/docker-entrypoint.sh

# Deliberately still root at this point. The entrypoint has to chown the
# mounted volume before anything can write to it, then drops to `app` itself.
# USER app here would move that failure earlier, not remove it.

# The platform overrides this; 8080 is only the local default.
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
