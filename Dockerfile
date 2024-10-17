# Use OpenJDK 21 as the base image
FROM openjdk:21-jdk

RUN mkdir -p /opt/kcback
COPY build/libs/kcback-*.jar /opt/kcback/kogo-content-backend.jar

ARG ACTIVE_PROFILE=local

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Expose port 8080
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$ACTIVE_PROFILE -jar /opt/kcback/kogo-content-backend.jar"]
