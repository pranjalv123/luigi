FROM --platform=linux/amd64 amazoncorretto:22-headless
LABEL authors="pranjal"
MAINTAINER 43mar.io
COPY build/libs/luigi-all.jar luigi.jar
ENTRYPOINT ["java","-jar","/luigi.jar"]