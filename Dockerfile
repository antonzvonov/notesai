FROM ubuntu:latest

WORKDIR /app

RUN apt-get update && \
    apt-get install -y openjdk-17-jdk maven && \
    rm -rf /var/lib/apt/lists/*

COPY . .

RUN mvn -DskipTests package

EXPOSE 8080

CMD ["java", "-jar", "target/notesai-0.0.1-SNAPSHOT.jar"]