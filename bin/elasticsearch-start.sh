#!/bin/bash

SCRIPT_DIRECTORY=$(cd `dirname $0` && pwd)

source ${SCRIPT_DIRECTORY}/library.sh

GREEN=`tput setaf 2`
RED=`tput setaf 5`
RST=`tput sgr0`

echo "${GREEN}About to run Elasticsearch via Docker ${RST}"

import_gradle_properties

if [[ -n "$(set -x; docker images -q elasticsearch:${dep_elasticsearch} 2>/dev/null)" ]]
then
    echo "${GREEN}Docker image elasticsearch:${dep_elasticsearch} exists locally${RST}"
else
    echo "${RED}Docker image elasticsearch:${dep_elasticsearch} does not exist locally${RST}"
    echo "${RED}Going to pull image elasticsearch:${dep_elasticsearch} from Docker Hub${RST}"

    if [[ $(set -x; docker pull elasticsearch:${dep_elasticsearch}) ]]
    then
        echo "${GREEN}Pulled Docker image sucessfully${RST}"
    else
        echo "${RED}Failed to pull Docker image${RST}"
        exit 1
    fi
fi

# Create container if it does not exit

if [[ $(docker container ls -a --filter "name=^${conf_elasticsearch_dockerimage}$" --format "{{.Names}}") == "${conf_elasticsearch_dockerimage}" ]]; then
    echo "${GREEN}Container ${conf_elasticsearch_dockerimage} already exists. Will reuse it${RESET}"
else
    echo "${RED}Container ${conf_elasticsearch_dockerimage} does not exist. Will create it now for you.${RST}"

    (
        set -x;
        docker create \
           --publish 9200:9200 \
           --publish 9300:9300 \
           --name ${conf_elasticsearch_dockerimage} \
           elasticsearch:${dep_elasticsearch} || exit 1
    )

    if [[ $? -ne 0 ]]
    then
        echo "${RED}Failed to create container ${conf_elasticsearch_dockerimage}${RST}"
        exit 1
    fi
fi

# Ok, let us start the container

if [[ ! $(docker ps  -a --filter "name=^${conf_elasticsearch_dockerimage}$" --filter status=running --format "{{.Names}}") == "${conf_elasticsearch_dockerimage}" ]]; then
    echo "${GREEN}Container ${conf_elasticsearch_dockerimage} is not running. Will start the container now${RST}"

    (
        set -x;
        docker start ${conf_elasticsearch_dockerimage} 1>&- || exit 1
    )

    if [[ $? -ne 0 ]]
    then
        echo "${RED}Failed to start the container ${conf_elasticsearch_dockerimage} properly${RST}"
    fi

else
    echo "${GREEN}Container is somehow alive. Nothing to do.${RESET}"
fi
