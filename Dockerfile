FROM maven:3.9.2-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
COPY banList.txt ./banList.txt
RUN mvn clean package -DskipTests

# Use Maven exec plugin to run MainKt
CMD ["mvn", "exec:java", "-Dexec.mainClass=MainKt"]
