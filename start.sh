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
    SERVICE_ARGUMENTS=${2:-''}

    echo "Starting ${SERVICE_NAME} ${SERVICE_ARGUMENTS}"
    nohup java -jar \
        ${CURRENT_DIR}/${SERVICE_NAME}/target/*.jar \
        ${SERVICE_ARGUMENTS}  \
        2>&1 > ${CURRENT_DIR}/logs/${SERVICE_NAME}.log &
}

function startAndWait () {
    SERVICE_NAME=${1}
    SERVICE_PORT=${2}
    SERVICE_ARGUMENTS=${3:-''}

    start ${SERVICE_NAME} ${SERVICE_ARGUMENTS}
    wait_service ${SERVICE_NAME} ${SERVICE_PORT}
}


${CURRENT_DIR}/stop.sh
trap "${CURRENT_DIR}/stop.sh; exit 1" SIGHUP SIGINT SIGQUIT SIGFPE SIGKILL SIGTERM ERR HUP INT TERM PIPE QUIT


echo "Building all services"
mvn --quiet --file ${CURRENT_DIR}/pom.xml clean package


start "configserver"
startAndWait "eureka" "9001"
# start "id-generator"
start "facade"
start "facade" "--spring.profiles.active=second"
# start "facade" "--spring.profiles.active=third"
# startAndWait "admin" "9003"
# startAndWait "zuul" "9002"
