FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY data-simulator/target/data-simulator-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
