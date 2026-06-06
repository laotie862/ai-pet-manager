ARG JAVA_VERSION=25
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

WORKDIR /app
COPY target/demo-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
