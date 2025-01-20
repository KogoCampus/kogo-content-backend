FROM eclipse-temurin:21-jdk-jammy

# File Uploader ========================================
# Install Python and required system dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Setup file uploader
COPY file-uploader /opt/file-uploader
WORKDIR /opt/file-uploader
RUN pip3 install -r requirements.txt

# Create a startup script for both services
RUN echo '#!/bin/sh\n\
cd /opt/file-uploader && \
uvicorn src.main:app --host 0.0.0.0 --port 3300 & \
cd /opt && \
java $JAVA_OPTS -Dspring.profiles.active=${ACTIVE_PROFILE} -jar /opt/jar/kogo-content-backend.jar' > /opt/start.sh && chmod +x /opt/start.sh
# =======================================================

ARG KCB_VERSION
ARG ACTIVE_PROFILE

ENV ACTIVE_PROFILE=${ACTIVE_PROFILE}
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV JAR=kcback-${KCB_VERSION}.jar

RUN mkdir -p /opt/kcback
COPY target/${JAR} /opt/jar/kogo-content-backend.jar

# Expose ports for both services
EXPOSE 8080 3300

CMD ["/opt/start.sh"]
