FROM openjdk:8-jre-alpine

EXPOSE 8080

RUN apk update && apk upgrade

ADD build/libs/pivio-server-1.1.0.jar /pivio-server.jar

CMD ["java", "-jar", "/pivio-server.jar", "--spring.data.elasticsearch.cluster-nodes=elasticsearch:9300"]