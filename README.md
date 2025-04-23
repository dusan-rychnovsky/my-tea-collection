# My Tea Collection

A web application designed to help you track your tea collection.

## Features

You can list your teas with detailed parameters and filter them using various criteria for efficient searching.

You can share links to your tea collection with friends, which makes it easy to keep them updated on which teas you have, or share details about a particular tea, etc.

## Technology

The application is built using Java, Spring, Thymleaf and PostgreSQL, and is Dockerized for efficient deployments.

## How To

### Build

```
cd .\src\backend\
mvn clean package
```

### Run

TODO: how to set up DB

```
docker build --tag=my-tea-collection:latest .
docker run -p8080:8080 `
  -e SPRING_DATASOURCE_URL=X `
  -e SPRING_DATASOURCE_USERNAME=X `
  -e SPRING_DATASOURCE_PASSWORD=X `
  my-tea-collection:latest
```

and then go to `http://localhost:8080/`
