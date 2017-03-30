#!/usr/bin/env bash


set -e


CURRENT_DIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ $# -eq 0 ]] ;
then
    shopt -u dotglob
    SERVICES=( $(find ${CURRENT_DIR}/ -type f -name "*.jar" | rev | cut -d '/' -f 3-3 | rev) )
else
    SERVICES=( $@ )
fi

echo "Terminating previously started services, if needed"
for ARGUMENT in "${SERVICES[@]}"
do
    PID=`jcmd | grep ${ARGUMENT%/} | cut -d ' ' -f 1`
    if [[ ! -z ${PID} ]];
    then
        kill -9 ${PID}
        echo "${ARGUMENT%/} was terminated"
    fi
done
