function import_gradle_properties() {
    PROPERTIES="${SCRIPT_DIRECTORY}/../gradle.properties"

    if [[ -f ${PROPERTIES} ]]
    then
        while IFS='=' read -r key value
        do
            if [[ "$key" =~ [^[:space:]] ]]
            then
                key=$(echo $key | tr '.' '_')
                eval "$key=\"$value\""
            fi

        done < ${PROPERTIES}

        echo "${GREEN}Used Elasticsearch version is ${dep_elasticsearch}${RST}"
    else
        echo "${RED}Properties file not found${RST}"
        exit 1
    fi
}