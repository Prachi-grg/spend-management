FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY target/spend-management-api-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
