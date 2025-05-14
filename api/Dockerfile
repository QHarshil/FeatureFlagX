# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory in the container
WORKDIR /app

# Arguments for user and group
ARG APP_USER=appuser
ARG APP_GROUP=appgroup

# Create a non-root user and group
RUN groupadd -r ${APP_GROUP} && useradd --no-log-init -r -g ${APP_GROUP} ${APP_USER}

# Copy thefat jar to the container
# Ensure your Spring Boot application is packaged as a fat JAR (e.g., api-0.1.0.jar)
# The JAR file name might need to be adjusted based on your pom.xml (artifactId and version)
ARG JAR_FILE=target/api-0.1.0.jar
COPY ${JAR_FILE} app.jar

# Make port 8080 available to the world outside this container
# This is informational; actual port mapping is done in docker-compose or docker run
EXPOSE 8080

# Change ownership of the app directory and jar file to the non-root user
RUN chown -R ${APP_USER}:${APP_GROUP} /app

# Switch to the non-root user
USER ${APP_USER}

# Run the jar file 
ENTRYPOINT ["java","-jar","/app/app.jar"]

