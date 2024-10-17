# Use OpenJDK 21 as the base image
FROM openjdk:21-jdk

ARG KCB_VERSION
ARG ACTIVE_PROFILE

ENV ACTIVE_PROFILE=${ACTIVE_PROFILE}
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV JAR=kcback-${KCB_VERSION}.jar

RUN mkdir -p /opt/kcback
COPY target/${JAR} /opt/jar/kogo-content-backend.jar

# Expose port 8080
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${ACTIVE_PROFILE} -jar /opt/jar/kogo-content-backend.jar"]
