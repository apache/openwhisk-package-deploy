#!/bin/bash
#
# use the command line interface to install standard actions deployed
# automatically
#
# To run this command
# ./installCatalog.sh <authkey> <edgehost> <apihost> <workers>

set -e
set -x

: ${OPENWHISK_HOME:?"OPENWHISK_HOME must be set and non-empty"}
WSK_CLI="$OPENWHISK_HOME/bin/wsk"

if [ $# -eq 0 ]
then
echo "Usage: ./installCatalog.sh <authkey> <edgehost> <apihost>"
fi

AUTH="$1"
EDGEHOST="$2"
APIHOST="$3"

# If the auth key file exists, read the key in the file. Otherwise, take the
# first argument as the key itself.
if [ -f "$AUTH" ]; then
    AUTH=`cat $AUTH`
fi

# Make sure that the EDGEHOST is not empty.
: ${EDGEHOST:?"EDGEHOST must be set and non-empty"}

# Make sure that the APIHOST is not empty.
: ${APIHOST:?"APIHOST must be set and non-empty"}

PACKAGE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export WSK_CONFIG_FILE= # override local property file to avoid namespace clashes

echo Installing Deploy package.

$WSK_CLI -i --apihost "$EDGEHOST" package update --auth "$AUTH" --shared yes deploy \
     -a description 'Alarms and periodic utility' \
     -a parameters '[ {"message":"theMessage", "required":true} ]' \
     -p apihost "$APIHOST" \
     -p trigger_payload ''

$WSK_CLI -i --apihost "$EDGEHOST" action update --auth "$AUTH" deploy/wskdeploy "$PACKAGE_HOME/actions/deploy.js" \
     -a description 'Creates an action that allows you to run wskdeploy from OpenWhisk' \
     -a parameters '[ {"name":"gitUrl", "required":true, "bindTime":true, "description": "The URL to the GitHub repository to deploy"}, {"name":"manifestPath", "required":false, "bindTime":true, "description": "The relative path to the manifest file from the GitHub repo root"},{"name":"wskApiHost", "required":false, "description": "The URL of the OpenWhisk api host you want to use"}, {"name":"envData", "required":false, "description": "Blueprint-specific environment data object"} ]' \
     -a sampleInput '{"gitUrl":"github.com/my_blueprint", "manifestPath":"runtimes/swift", "wskApiHost":"openwhisk.stage1.ng.bluemix.net", "envData": "{\"KAFKA_ADMIN_URL\":\"https://my_kafka_service\", \"MESSAGEHUB_USER\":\"MY_MESSAGEHUB_USERNAME\"}"}' \
     --docker "openwhisk/wskdeploy:0.8.9.2"
