# Use OpenJDK 21 as the base image
FROM openjdk:21-jdk

# Create the directory for your application
RUN mkdir -p /opt/kcback

# Copy the jar file into the container
COPY build/libs/kcback-*.jar /opt/kcback/kogo-content-backend.jar

# Set environment variables for Java options, service URLs, and server configuration
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Expose port 8080
EXPOSE 8080

# Command to run the application with the appropriate profile and Java options
CMD ["sh", "-c", "java $JAVA_OPTS -jar /opt/kcback/kogo-content-backend.jar"]
