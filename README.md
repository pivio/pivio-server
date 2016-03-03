# pivio-server

> Aggregates all document information relevant for your platform.

## How to run the server

1. Download Elasticsearch 1.7.x
- Run Elasticsearch on localhost (default configuration)
- Build pivio-server: `gradle build`
- `java -jar build/libs/pivio-server-1.0.0.jar`
- Access it on `http://localhost:9123`

## How to run the server in docker

1. Build the jar: `gradle build`
- `docker-compose up`

### This will launch two containers.

1. The server itself
- Elasticsearch as a container

You can access it now your docker host at port 9123.

If you need to rebuild the images (e.g. when you patched the sources) run

```bash
gradle build
docker-compose build
docker-compose up
```

## Insert Document Information

```bash
curl -H "Content-Type: application/json" -X POST http://localhost:9123/document -d '{
  "id": "JustSomeId",
  "name": "Awesome Microservice",
  "type": "service",
  "team": "lambda",
  "description": "Simple microservice"
}'
```

## Retrieve Document Information

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document/JustSomeId
```

## Retrieve Changesets of documents

Everytime document is changed a new changeset will be generated.

### Retrieve all changesets

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/changeset
```

### Retrieve all changesets for last 7 days

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/changeset?since=7d
```

### Retrieve all changesets for last 4 weeks

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/changeset?since=4w
```

### Retrieve all changesets of specific document

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document/JustSomeId/changeset
```

### Retrieve all changesets of specific document for last 7 days

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document/JustSomeId/changeset?since=7d
```

## Search API for Document Information

### Search for each Document of team Lambda

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document?query={"match":{"team":"lambda"}}'
```

You have to url encode the query parameter:

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document?query=%7B%22match%22%3A%7B%22team%22%3A%22lambda%22%7D%7D
```

### Search for each Document of team Lambda which has field talks_to present (will only return the field talks_to in results)

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document?query={"match":{"team":"lambda"}}'&fields=talks_to
```

Don't forget to url encode the query parameter:

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document?query=%7B%22match%22%3A%7B%22team%22%3A%22lambda%22%7D%7D&fields=talks_to
```

### Search for each Document of team Lambda and sort ascending by field lastUpdated

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document?query={"match":{"team":"lambda"}}'&sort=lastUpdate:asc
```

Don't forget to url encode the query parameter:

```bash
curl -H "Content-Type: application/json" -X GET http://localhost:9123/document?query=%7B%22match%22%3A%7B%22team%22%3A%22lambda%22%7D%7D&sort=lastUpdate:asc
```

You can sort descending with desc instead of asc. You can also sort by multiple fields, just truncate them via comma. Order is important in this case, e.g. lastUpdate:asc,team:desc would first sort ascending by field lastUpdate and afterwards descending by field team if two entries have same lastUpdate value.

You can use the whole query types of Elasticsearch Search API ([Search API Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/search.html)).
