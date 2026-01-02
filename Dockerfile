FROM eclipse-temurin:17-jdk AS build

WORKDIR /app


COPY pom.xml ./


RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean


RUN mvn dependency:go-offline -B


COPY src ./src


RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-jre

WORKDIR /app


COPY --from=build /app/target/*.jar app.jar


EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]