#!/bin/bash

SCRIPT_DIRECTORY=$(cd `dirname $0` && pwd)

source ${SCRIPT_DIRECTORY}/library.sh

GREEN=`tput setaf 2`
RED=`tput setaf 5`
RST=`tput sgr0`

echo "${GREEN}About to stop Elasticsearch via Docker ${RST}"

import_gradle_properties

if [[ ! $(docker ps  -a --filter "name=^${conf_elasticsearch_dockerimage}$" --filter status=running --format "{{.Names}}") == "${conf_elasticsearch_dockerimage}" ]]; then
    echo "${GREEN}Container ${conf_elasticsearch_dockerimage} is not running. Nothing to do${RST}"
else
    echo "${GREEN}Container ${conf_elasticsearch_dockerimage} is somehow alive. Going to stop it.${RESET}"

    (
        docker stop ${conf_elasticsearch_dockerimage} 1>&- || exit 1
    )

    if [[ $? -ne 0 ]]
    then
        echo "${RED}Failed to stop ${conf_elasticsearch_dockerimage} properly${RST}"
        exit 1
    fi
fi
