# syntax=docker/dockerfile:1.6

FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY gradlew build.gradle settings.gradle /build/
COPY gradle /build/gradle
RUN chmod +x /build/gradlew
COPY src /build/src
RUN --mount=type=cache,target=/root/.gradle /build/gradlew bootJar -x test --no-daemon
RUN mkdir -p /build/extracted && cp build/libs/*.jar application.jar && java -Djarmode=tools -jar application.jar extract --layers --destination /build/extracted

FROM eclipse-temurin:21-jre AS prod
WORKDIR /application
ENV TZ=Asia/Seoul
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./
ENTRYPOINT ["java", "-jar", "application.jar"]
