FROM java:8-jre

EXPOSE 8080

ADD build/libs/pivio-server-1.0.0.jar /pivio-server.jar

CMD ["java", "-jar", "/pivio-server.jar", "--spring.data.elasticsearch.cluster-nodes=elasticsearch:9300"]
