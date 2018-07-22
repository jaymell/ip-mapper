FROM openjdk:8u171-jdk-stretch

WORKDIR /app

COPY build/libs/ipmapper-0.0.1-SNAPSHOT.jar ./app.jar
COPY static/ ./static/

CMD [ "java", "-jar", "app.jar" ] 
