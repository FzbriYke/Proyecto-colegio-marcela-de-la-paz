FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app

COPY gradlew .
COPY gradle/wrapper/ gradle/wrapper/
COPY backend/deploy/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/src/ src/
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon test bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/build/libs/gestion-escolar-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=salas,reservas,neon,render
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
