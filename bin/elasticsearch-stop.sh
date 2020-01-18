#!/usr/bin/env bash

SCRIPT_DIRECTORY=$(cd `dirname $0` && pwd)
BASE_DIRECTORY=$(cd `dirname $0`/.. && pwd)

source ${SCRIPT_DIRECTORY}/library.sh

GREEN=`tput setaf 2`
RED=`tput setaf 5`
RST=`tput sgr0`

echo "${GREEN}About to stop Elasticsearch via Docker ${RST}"

import_gradle_properties

if [[ ! $(docker ps  -a --filter "name=^${CONF_ELASTICSEARCH_DOCKERIMAGE}$" --filter status=running --format "{{.Names}}") == "${CONF_ELASTICSEARCH_DOCKERIMAGE}" ]]; then
    echo "${GREEN}Container ${CONF_ELASTICSEARCH_DOCKERIMAGE} is not running. Nothing to do${RST}"
else
    echo "${GREEN}Container ${CONF_ELASTICSEARCH_DOCKERIMAGE} is somehow alive. Going to stop it.${RESET}"

    (
        docker stop ${CONF_ELASTICSEARCH_DOCKERIMAGE} 1>&- || exit 1
    )

    if [[ $? -ne 0 ]]
    then
        echo "${RED}Failed to stop ${CONF_ELASTICSEARCH_DOCKERIMAGE} properly${RST}"
        exit 1
    fi
fi
