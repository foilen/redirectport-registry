FROM openjdk:11-jre-slim

COPY app/ /app

WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/redirectport-registry.jar"]
