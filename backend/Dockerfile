ARG JAVA_VERSION=25
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x ./mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

WORKDIR /app
RUN apk add --no-cache ffmpeg
COPY --from=build /workspace/target/demo-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
