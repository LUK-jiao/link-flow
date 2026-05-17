FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:17.0.2-slim-linuxarm64

LABEL authors="shawluke"

WORKDIR /app

COPY target/linkflow-campaign-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8084 20884

ENTRYPOINT ["java", "-jar", "app.jar"]
