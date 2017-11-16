#!/bin/bash
#
# use the command line interface to uninstall standard actions deployed
# automatically
#
# To run this command
# ./uninstall.sh  <AUTH> <EDGE_HOST> <WSK_CLI>
# AUTH and EDGE_HOST are found in $HOME/.wskprops
# WSK_CLI="$OPENWHISK_HOME/bin/wsk"

set -e
set -x

if [ $# -eq 0 ]
then
    echo "Usage: ./uninstall.sh <auth> <edgehost> <pathtowskcli>"
fi

AUTH="$1"
EDGE_HOST="$2"
WSK_CLI="$3"

# If the auth key file exists, read the key in the file. Otherwise, take the
# first argument as the key itself.
if [ -f "$AUTH" ]; then
    AUTH=`cat $AUTH`
fi

PACKAGE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export WSK_CONFIG_FILE= # override local property file to avoid namespace clashes


echo Uninstalling Template Package \

$WSK_CLI --apihost $EDGE_HOST action delete -i --auth $AUTH deploy/wskdeploy

$WSK_CLI --apihost $EDGE_HOST package delete -i --auth $AUTH deploy
