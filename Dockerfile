FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

EXPOSE 10000

CMD ["java", "-jar", "target/studybuddy-ai.jar"]
