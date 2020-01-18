#!/usr/bin/env bash

SCRIPT_DIRECTORY=$(cd `dirname $0` && pwd)
BASE_DIRECTORY=$(cd `dirname $0`/.. && pwd)
DOCKER_DIRECTORY=${BASE_DIRECTORY}/docker

source ${SCRIPT_DIRECTORY}/library.sh

GREEN=`tput setaf 2`
RED=`tput setaf 5`
RST=`tput sgr0`

echo "${GREEN}About to run Elasticsearch and Elastic HQ via Docker Compose ${RST}"

import_gradle_properties

export DEP_ELASTICSEARCH
export DEP_ELASTICHQ
export COMPOSE_FILE=${DOCKER_DIRECTORY}/docker-compose.yml

exec docker-compose $@



