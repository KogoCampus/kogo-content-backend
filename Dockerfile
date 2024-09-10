FROM openjdk:21-jdk

RUN mkdir -p /opt/kcback
COPY kcback-*.jar /opt/kcback/kogo-content-backend.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m" \
	ENDPOINT_BASE_URL=http://localhost:8080/loyalty/v1 \
	LOYALTY_SERVER=lcback \
	LOYALTY_PORT=10041

EXPOSE 8080

CMD ["java", "-jar", "/opt/kcback/kogo-content-backend.jar"]
