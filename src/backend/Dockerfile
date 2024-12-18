# docker build --tag=my-tea-collection:latest .
# docker run -p8080:8080 my-tea-collection:latest

FROM openjdk:17-jdk-alpine
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
