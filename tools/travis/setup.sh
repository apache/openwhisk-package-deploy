#!/bin/bash

SCRIPTDIR=$(cd $(dirname "$0") && pwd)
HOMEDIR="$SCRIPTDIR/../../../"

# jshint support
sudo apt-get -y install nodejs npm
sudo npm install -g jshint

# clone utilties repo. in order to run scanCode.py
cd $HOMEDIR
git clone https://github.com/apache/incubator-openwhisk-utilities.git

# shallow clone OpenWhisk repo.
git clone --depth=1 --single-branch -b scala-2-12 https://github.com/chetanmeh/incubator-openwhisk.git openwhisk

cd openwhisk
./tools/travis/setup.sh