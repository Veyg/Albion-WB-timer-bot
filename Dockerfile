FROM amazoncorretto:17

WORKDIR /app

COPY ./target/albionwbtimer-1.1.jar /app/

CMD ["java", "-jar", "/app/albionwbtimer-1.1.jar"]
