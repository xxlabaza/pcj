#!/usr/bin/env bash


set -e


CURRENT_DIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
mkdir -p ${CURRENT_DIR}/logs



function service_is_up () {
    SERVICE_PORT=${1}
    # Ask Service health check
    RESPONSE=`curl -sL -w "%{http_code}\\n" localhost:${SERVICE_PORT}/admin/health -o /dev/null`
    if [[ "${RESPONSE}" == 2* ]] ; # response code starts with 2 (200, 201, 202 and etc.)
    then
        return 0
    else
        return 1
    fi
}

function wait_service () {
    SERVICE_NAME=${1}
    SERVICE_PORT=${2}
    COUNTER=0

    while :
    do
        echo "Waiting ${SERVICE_NAME}..."
        sleep 10

        if service_is_up ${SERVICE_PORT} ;
        then
            echo "${SERVICE_NAME} is up!"
            break
        fi

        let COUNTER=COUNTER+1
        if [ $COUNTER -eq 40 ]
        then
            echo "Service ${SERVICE_NAME} is starting too much long, terminating it..."
            exit 1
        fi
    done
}

function start () {
    SERVICE_NAME=${1}
    SERVICE_PORT=${2}

    echo "Building ${SERVICE_NAME}"
    mvn --quiet --file ${SERVICE_NAME}/pom.xml clean package

    echo "Starting ${SERVICE_NAME}"
    nohup java -jar \
        ${CURRENT_DIR}/${SERVICE_NAME}/target/*.jar  \
        2>&1 > ${CURRENT_DIR}/logs/${SERVICE_NAME}.log &

    # Wait service's health check 'OK' if port set
    if [[ ${SERVICE_PORT} ]];
    then
        wait_service ${SERVICE_NAME} ${SERVICE_PORT}
    fi
}


${CURRENT_DIR}/stop.sh
trap "${CURRENT_DIR}/stop.sh; exit 1" SIGHUP SIGINT SIGQUIT SIGFPE SIGKILL SIGTERM ERR HUP INT TERM PIPE QUIT

start "configserver"
start "eureka" "9001"
start "id-generator"
start "facade"
start "zuul" "9002"
