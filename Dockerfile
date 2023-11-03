FROM amazoncorretto:21

WORKDIR /app
# Copy the VERSION file from your repository into the root directory of the Docker container
COPY VERSION /app/
COPY ./target/albionwbtimer-1.0.3.jar /app/

CMD ["java", "-jar", "/app/albionwbtimer-1.0.2.jar"]
