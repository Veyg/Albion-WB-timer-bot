FROM amazoncorretto:17

WORKDIR /app
# Copy the VERSION file from your repository into the root directory of the Docker container
COPY VERSION /app/
COPY ./target/albionwbtimer-1.1.jar /app/

CMD ["java", "-jar", "/app/albionwbtimer-1.1.jar"]
