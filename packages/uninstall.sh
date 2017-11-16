#!/bin/bash

set -e
set -x
: ${OPENWHISK_HOME:?"OPENWHISK_HOME must be set and non-empty"}
WSK_CLI="$OPENWHISK_HOME/bin/wsk"

if [ $# -eq 0 ]
then
    echo "Usage: ./uninstall.sh $EDGE_HOST $AUTH"
fi

EDGE_HOST="$1"
AUTH="$2"

echo Uninstalling Template Package \

$WSK_CLI --apihost $EDGE_HOST action delete -i --auth $AUTH deploy/wskdeploy

$WSK_CLI --apihost $EDGE_HOST package delete -i --auth $AUTH deploy
