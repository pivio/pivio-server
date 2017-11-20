# pivio-server

> Aggregates all document information relevant for your platform.

## Build Status

[![Build Status](https://travis-ci.org/pivio/pivio-server.svg?branch=master)](https://travis-ci.org/pivio/pivio-server)


## How to run the server (without using Docker)

1. Build pivio-server: `./gradlew build -x test` (Note: If you want to run the tests, you need [Docker](https://docs.docker.com/engine/installation/))
2. Install [Elasticsearch 2.4.6](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/_installation.html)
3. Install the Elasticsearch [Delete By Query Plugin](https://www.elastic.co/guide/en/elasticsearch/plugins/2.4/plugins-delete-by-query.html)
3. Start Elasticsearch: `elasticsearch`
4. Start pivio-server: `java -jar build/libs/pivio-server-1.1.0.jar`
5. Access it at `http://localhost:9123/{document|changeset}` (see below)

## How to run the server using Docker (Compose)

1. Make sure [Docker](https://docs.docker.com/engine/installation/) is running and you have [Docker Compose](https://docs.docker.com/compose/install/) installed
2. Build pivio-server: `./gradlew build`
3. Start it, using `docker-compose up` - this will run Elasticsearch and pivio-server as Docker containers
4. Access it at `http://localhost:9123/{document|changeset}` (see below)

If you need to rebuild the Docker images (e.g. when you patched the sources) run:
```bash
./gradlew [clean] build
docker-compose up [-d] --build
```

## Insert document information

```bash
curl -H 'Content-Type: application/json' -X POST http://localhost:9123/document -d '{
  "id": "JustSomeId",
  "name": "Awesome Microservice",
  "type": "service",
  "owner": "lambda",
  "description": "Simple microservice"
}'
```

## Retrieve document information

```bash
curl -H 'Content-Type: application/json' -X GET http://localhost:9123/document/JustSomeId
```

## Retrieve changesets of documents

Everytime document is changed a new changeset will be generated.

### Retrieve all changesets

```bash
curl -H 'Content-Type: application/json' -X GET http://localhost:9123/changeset
```

### Retrieve all changesets for last 7 days

```bash
curl -H 'Content-Type: application/json' -X GET http://localhost:9123/changeset?since=7d
```

### Retrieve all changesets for last 4 weeks

```bash
curl -H 'Content-Type: application/json' -X GET http://localhost:9123/changeset?since=4w
```

### Retrieve all changesets of specific document

```bash
curl -H 'Content-Type: application/json' -X GET http://localhost:9123/document/JustSomeId/changeset
```

### Retrieve all changesets of specific document for last 7 days

```bash
curl -H 'Content-Type: application/json' -X GET http://localhost:9123/document/JustSomeId/changeset?since=7d
```

## Search API for document information

For searching, a `query` URL parameter can be passed (see examples below). Its value is a JSON string that needs to be URL encoded.
That is, at least the following JSON characters need to be replaced: 

* `{` needs to be replaced by `%7B`
* `"` needs to be replaced by `%22`
* `:` needs to be replaced by `%3A`
* `}` needs to be replaced by `%7D`
* for a quick overview of further characters see Wikipedia's page [Percent-encoding](https://en.wikipedia.org/wiki/Percent-encoding#Percent-encoding_reserved_characters) page


### Search for each document of owner Lambda

```bash
curl -H 'Content-Type: application/json' -X GET 'http://localhost:9123/document?query={"match":{"owner":"lambda"}}&fields=talks_to'
```

Encoded:

```bash
curl -H 'Content-Type: application/json' -X GET 'http://localhost:9123/document?query=%7B%22match%22%3A%7B%22owner%22%3A%22lambda%22%7D%7D'
```

### Search for each document of owner Lambda which has field talks_to present (will only return the field talks_to in results)

```bash
curl -H 'Content-Type: application/json' -X GET 'http://localhost:9123/document?query={"match":{"owner":"lambda"}}&fields=talks_to'
```

Encoded:

```bash
curl -H 'Content-Type: application/json' -X GET 'http://localhost:9123/document?query=%7B%22match%22%3A%7B%22owner%22%3A%22lambda%22%7D%7D&fields=talks_to'
```

### Search for each document of owner Lambda and sort ascending by field lastUpdated  

```bash
curl -H 'Content-Type: application/json' -X GET 'http://localhost:9123/document?query={"match":{"owner":"lambda"}}&sort=lastUpdate:asc'
```

Encoded:

```bash
curl -H 'Content-Type: application/json' -X GET 'http://localhost:9123/document?query=%7B%22match%22%3A%7B%22owner%22%3A%22lambda%22%7D%7D&sort=lastUpdate:asc'
```

You can sort descending with desc instead of asc. You can also sort by multiple fields, just truncate them via comma. Order is important in this case, e.g. lastUpdate:asc,owner:desc would first sort ascending by field lastUpdate and afterwards descending by field owner if two entries have same lastUpdate value.

You can use the whole query types of Elasticsearch Search API ([Search API Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/search.html)).
