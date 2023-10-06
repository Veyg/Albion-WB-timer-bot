FROM openjdk:17-jre-slim

WORKDIR /app

COPY ./target/albionwbtimer-1.1.jar /app/

CMD ["java", "-jar", "/app/albionwbtimer-1.1.jar"]
