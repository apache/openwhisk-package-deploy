#!/bin/bash
#
# use the command line interface to install standard actions deployed
# automatically
#
# To run this command
# ./installCatalog.sh  <AUTH> <EDGE_HOST> <WSK_CLI> <DOCKER>
# AUTH and EDGE_HOST are found in $HOME/.wskprops
# WSK_CLI="$OPENWHISK_HOME/bin/wsk"

set -e
set -x

if [ $# -eq 0 ]
then
echo "Usage: ./installCatalog.sh <authkey> <edgehost> <pathtowskcli> <docker>"
fi

AUTH="$1"
EDGE_HOST="$2"
WSK_CLI="$3"
DOCKER="$4"

# If docker is not provided, set to default version.
if [ -z "$4" ]
  then
    DOCKER="openwhisk/wskdeploy:0.8.10"
fi

# If the auth key file exists, read the key in the file. Otherwise, take the
# first argument as the key itself.
if [ -f "$AUTH" ]; then
    AUTH=`cat $AUTH`
fi

PACKAGE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export WSK_CONFIG_FILE= # override local property file to avoid namespace clashes

# wskdeploy actions

echo Installing wskdeploy package.

$WSK_CLI -i --apihost "$EDGE_HOST" package update --auth "$AUTH"  --shared yes "deploy" \
-a description "This package offers a convenient way for you to describe and deploy any part of the OpenWhisk programming model using a Manifest file written in YAML." \
-a prettyName "Whisk Deploy"

$WSK_CLI -i --apihost "$EDGE_HOST" action update --auth "$AUTH" "deploy/wskdeploy" "$PACKAGE_HOME/actions/deploy.js" \
-a description 'Creates an action that allows you to run wskdeploy from OpenWhisk' \
-a parameters '[ {"name":"gitUrl", "required":true, "bindTime":true, "description": "The URL to the GitHub repository to deploy"}, {"name":"manifestPath", "required":false, "bindTime":true, "description": "The relative path to the manifest file from the GitHub repo root"}, {"name":"envData", "required":false, "description": "Blueprint-specific environment data object"} ]' \
-a sampleInput '{"gitUrl":"github.com/my_blueprint", "manifestPath":"runtimes/swift", "envData": "{\"ENV_VARIABLE_1\":\"VALUE_1\", \"ENV_VARIABLE_2\":\"VALUE_2\"}"}' \
--docker "$DOCKER"
     