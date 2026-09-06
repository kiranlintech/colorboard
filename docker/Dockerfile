FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY backend/pom.xml .

RUN mvn -q -DskipTests dependency:go-offline

COPY backend/src ./src

RUN mvn -q -DskipTests package

# Find the generated application JAR and give it a fixed name
RUN find target -maxdepth 1 -type f \
    -name "*.jar" \
    ! -name "*.original" \
    -exec cp {} /app/app.jar \;


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
