FROM openjdk:21-jdk

ARG KCB_VERSION
ARG ACTIVE_PROFILE

ENV ACTIVE_PROFILE=${ACTIVE_PROFILE}
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV JAR=kcback-${KCB_VERSION}.jar

RUN mkdir -p /opt/kcback
COPY target/${JAR} /opt/jar/kogo-content-backend.jar

# APP RUNNER AWS Profile Configuration
# =====================================================================================
ARG AWS_ACCESS_KEY_ID
ARG AWS_SECRET_ACCESS_KEY
ARG AWS_REGION

ENV AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
ENV AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
ENV AWS_REGION=${AWS_REGION}

RUN mkdir -p ~/.aws && \
    echo "[default]" > ~/.aws/credentials && \
    echo "aws_access_key_id=${AWS_ACCESS_KEY_ID}" >> ~/.aws/credentials && \
    echo "aws_secret_access_key=${AWS_SECRET_ACCESS_KEY}" >> ~/.aws/credentials && \
    echo "[default]" > ~/.aws/config && \
    echo "region=${AWS_REGION}" >> ~/.aws/config
# =====================================================================================

# Expose port 8080
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${ACTIVE_PROFILE} -jar /opt/jar/kogo-content-backend.jar"]
