# docker build --tag=my-tea-collection:latest .
# docker run -p8080:8080 SPRING_DATASOURCE_URL=X -e SPRING_DATASOURCE_USERNAME=X -e SPRING_DATASOURCE_PASSWORD=X my-tea-collection:latest

FROM openjdk:17-jdk-alpine
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
