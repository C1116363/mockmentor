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

# Not root. If anything in the app is ever exploited, this is the difference
# between reaching the app's own files and owning the container.
RUN useradd --create-home --shell /bin/false app

# Uploads live here. Mount a PERSISTENT VOLUME at this path or every CV,
# screenshot and study-material file disappears on the next deploy - the
# database rows survive and point at nothing.
RUN mkdir -p /data/uploads && chown -R app:app /data
ENV UPLOAD_ROOT=/data/uploads

COPY --from=build /build/target/*.jar app.jar
RUN chown app:app app.jar
USER app

# The platform overrides this; 8080 is only the local default.
EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: containers get a memory limit
# rather than a machine, and a JVM that ignores it gets OOM-killed by the
# platform with no Java error to explain why.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -jar app.jar"]
