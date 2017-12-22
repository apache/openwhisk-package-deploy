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
  echo "Usage: ./installCatalog.sh <authkey> <edgehost> <pathtowskcli> <skipdeploy> <docker>"
fi

AUTH="$1"
EDGE_HOST="$2"
WSK_CLI="$3"
SKIP_DEPLOY="${4:-False}"
DOCKER="$5"

echo SKIP DEPLOY
echo $SKIP_DEPLOY

# If docker is not provided, set to default version.
if [ -z "$5" ]
  then
  if [ $SKIP_DEPLOY = False ] || [ $SKIP_DEPLOY = True ]
  then
    DOCKER="openwhisk/wskdeploy:0.8.10"
  else
    SKIP_DEPLOY=False
    DOCKER=$4
  fi
fi

echo DOCKER IS $DOCKER
echo SKIP DEPLOY IS
echo $SKIP_DEPLOY

# If the auth key file exists, read the key in the file. Otherwise, take the
# first argument as the key itself.
if [ -f "$AUTH" ]; then
  AUTH=`cat $AUTH`
fi

PACKAGE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export WSK_CONFIG_FILE= # override local property file to avoid namespace clashes

#clone all Blueprints
for bp in blueprint-hello-world blueprint-cloudant-trigger blueprint-messagehub-trigger
do
  if [ -e actions/blueprints/$bp ]
    then
    rm -rf actions/blueprints/$bp
  fi
  git clone --depth 1 https://github.com/ibm-functions/$bp actions/blueprints/$bp
done

# make deployWeb.zip & install
OLD_PATH=`pwd`
cd actions

if [ -e deployWeb.zip ]
  then
  rm -rf webDeploy.zip
fi

cp -f webDeploy_package.json package.json
zip -r webDeploy.zip package.json webDeploy.js lib/common.js blueprints/

cd $OLD_PATH

$WSK_CLI -i --apihost "$EDGE_HOST" package update --auth "$AUTH" --shared no "deployWeb" \
-a description "This package offers a convenient way for you to describe and deploy any part of the OpenWhisk programming model using a Manifest file written in YAML." \
-a prettyName "Whisk Deploy Web"

$WSK_CLI -i --apihost "$EDGE_HOST" action update --auth "$AUTH" "deployWeb/wskdeploy" "$PACKAGE_HOME/actions/webDeploy.zip" --web true \
-a description 'Creates an action that allows you to run wskdeploy from OpenWhisk' \
-a parameters '[ {"name":"gitUrl", "required":true, "bindTime":true, "description": "The URL to the GitHub repository to deploy"}, {"name":"manifestPath", "required":false, "bindTime":true, "description": "The relative path to the manifest file from the GitHub repo root"}, {"name":"envData", "required":false, "description": "Blueprint-specific environment data object"} ]' \
-a sampleInput '{"gitUrl":"github.com/my_blueprint", "manifestPath":"runtimes/swift", "envData": "{\"ENV_VARIABLE_1\":\"VALUE_1\", \"ENV_VARIABLE_2\":\"VALUE_2\"}"}' \
--docker "$DOCKER"


cd actions
if [ $SKIP_DEPLOY = False ]
  then
  if [ -e deploy.zip ]
  then
      rm -rf deploy.zip
  fi

  cp -f deploy_package.json package.json
  zip -r deploy.zip package.json deploy.js lib/common.js

  cd $OLD_PATH

  echo Installing wskdeploy package.

  $WSK_CLI -i --apihost "$EDGE_HOST" package update --auth "$AUTH"  --shared yes "deploy" \
  -a description "This package offers a convenient way for you to describe and deploy any part of the OpenWhisk programming model using a Manifest file written in YAML." \
  -a prettyName "Whisk Deploy"

  $WSK_CLI -i --apihost "$EDGE_HOST" action update --auth "$AUTH" "deploy/wskdeploy" "$PACKAGE_HOME/actions/deploy.zip" \
  -a description 'Creates an action that allows you to run wskdeploy from OpenWhisk' \
  -a parameters '[ {"name":"gitUrl", "required":true, "bindTime":true, "description": "The URL to the GitHub repository to deploy"}, {"name":"manifestPath", "required":false, "bindTime":true, "description": "The relative path to the manifest file from the GitHub repo root"}, {"name":"envData", "required":false, "description": "Blueprint-specific environment data object"} ]' \
  -a sampleInput '{"gitUrl":"github.com/my_blueprint", "manifestPath":"runtimes/swift", "envData": "{\"ENV_VARIABLE_1\":\"VALUE_1\", \"ENV_VARIABLE_2\":\"VALUE_2\"}"}' \
  --docker "$DOCKER"
fi
