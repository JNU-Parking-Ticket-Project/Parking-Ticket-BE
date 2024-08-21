FROM amazoncorretto:17-alpine3.17-jdk
RUN mkdir -p /logs

ENV PROFILE default
ENV TZ=Asia/Seoul

ARG JAVA_OPTS

ARG RELEASE_VERSION
ENV DD_VERSION=${RELEASE_VERSION}

ARG JAR_FILE="./Ticket-Api/build/libs/Ticket-Api-*.jar"
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
