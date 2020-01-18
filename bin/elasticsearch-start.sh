#!/usr/bin/env bash

SCRIPT_DIRECTORY=$(cd `dirname $0` && pwd)
BASE_DIRECTORY=$(cd `dirname $0`/.. && pwd)

source ${SCRIPT_DIRECTORY}/library.sh

GREEN=`tput setaf 2`
RED=`tput setaf 5`
RST=`tput sgr0`

echo "${GREEN}About to run Elasticsearch via Docker ${RST}"

import_gradle_properties

if [[ -n "$(set -x; docker images -q elasticsearch:${DEP_ELASTICSEARCH} 2>/dev/null)" ]]
then
    echo "${GREEN}Docker image elasticsearch:${DEP_ELASTICSEARCH} exists locally${RST}"
else
    echo "${RED}Docker image elasticsearch:${DEP_ELASTICSEARCH} does not exist locally${RST}"
    echo "${RED}Going to pull image elasticsearch:${DEP_ELASTICSEARCH} from Docker Hub${RST}"

    if [[ $(set -x; docker pull elasticsearch:${DEP_ELASTICSEARCH}) ]]
    then
        echo "${GREEN}Pulled Docker image sucessfully${RST}"
    else
        echo "${RED}Failed to pull Docker image${RST}"
        exit 1
    fi
fi

# Create container if it does not exit

if [[ $(docker container ls -a --filter "name=^${CONF_ELASTICSEARCH_DOCKERIMAGE}$" --format "{{.Names}}") == "${CONF_ELASTICSEARCH_DOCKERIMAGE}" ]]; then
    echo "${GREEN}Container ${CONF_ELASTICSEARCH_DOCKERIMAGE} already exists. Will reuse it${RESET}"
else
    echo "${RED}Container ${CONF_ELASTICSEARCH_DOCKERIMAGE} does not exist. Will create it now for you.${RST}"

    (
           #--network pivio-dev-net \
        set -x;
        docker create \
           --publish 9200:9200 \
           --publish 9300:9300 \
           --env "discovery.type=single-node" \
           --name ${CONF_ELASTICSEARCH_DOCKERIMAGE} \
           elasticsearch:${DEP_ELASTICSEARCH} || exit 1
    )

    if [[ $? -ne 0 ]]
    then
        echo "${RED}Failed to create container ${CONF_ELASTICSEARCH_DOCKERIMAGE}${RST}"
        exit 1
    fi
fi

# Ok, let us start the container

if [[ ! $(docker ps  -a --filter "name=^${CONF_ELASTICSEARCH_DOCKERIMAGE}$" --filter status=running --format "{{.Names}}") == "${CONF_ELASTICSEARCH_DOCKERIMAGE}" ]]; then
    echo "${GREEN}Container ${CONF_ELASTICSEARCH_DOCKERIMAGE} is not running. Will start the container now${RST}"

    (
        set -x;
        docker start ${CONF_ELASTICSEARCH_DOCKERIMAGE} 1>&- || exit 1
    )

    if [[ $? -ne 0 ]]
    then
        echo "${RED}Failed to start the container ${CONF_ELASTICSEARCH_DOCKERIMAGE} properly${RST}"
    fi

else
    echo "${GREEN}Container is somehow alive. Nothing to do.${RESET}"
fi
