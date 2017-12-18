const fs = require('fs');
const path = require('path');
const exec = require('child_process').exec;
const git = require('simple-git');
const yaml = require('js-yaml');

let command = '';

/**
 * Action to deploy openwhisk elements from a compliant repository
 *  @param {string} gitUrl - github url containing the manifest and elements to deploy
 *  @param {string} manifestPath - (optional) the path to the manifest file, e.g. "openwhisk/src"
 *  @param {object} envData - (optional) some specific details such as cloudant username or cloudant password
 *  @return {object} Promise
 */
function main(params) {

  if(params.__ow_method === "get") {
    return {
      statusCode: 200,
      headers: {'Content-Type': 'application/json'},
      body: new Buffer(JSON.stringify("success")).toString('base64')
    }
  }

  // Grab optional envData and manifestPath params for wskdeploy
  let {
    envData,
    manifestPath,
    gitUrl
  } = params;

  // confirm gitUrl was provided as a parameter
  if (!gitUrl) {
    return sendError(400, 'no github repo url was provided');
    // reject({
    //   error: 'Please enter the GitHub repo url in params',
    // });
  }

  if(params.__ow_method === "post") {
    console.log("MADE IT HERE")
    return new Promise((resolve, reject) => {

      // if no manifestPath was provided, use current directory
      if (!manifestPath) {
        manifestPath = '.';
      }
      // Grab wsp api host and auth from params, or process.env
      const { wskApiHost, wskAuth } = getWskApiAuth(params);

      // Extract the name of the repo for the tmp directory
      const repoSplit = params.gitUrl.split('/');
      const repoName = repoSplit[repoSplit.length - 1];
      const localDirName = `${__dirname}/../tmp/${repoName}`;
      console.log("about to clone");
      return git()
      .clone(gitUrl, localDirName, ['--depth', '1'], (err, data) => {
        if (err) {
          //reject('There was a problem cloning from github.  Does that github repo exist?  Does it begin with http?', err);
          reject(sendError(400, 'There was a problem cloning from github.  Does that github repo exist?  Does it begin with http?', err));
        }
        resolve({
          repoDir: localDirName,
          manifestPath,
          manifestFileName: 'manifest.yaml',
          wskAuth,
          wskApiHost,
          envData,
        });
      });
    })
    .then((data) => {
      const {
        wskAuth,
        wskApiHost,
        manifestPath,
        manifestFileName,
        repoDir,
        envData,
      } = data;

      // Set the cwd of the command to be where the manifest/actions live
      const execOptions = {
        cwd: `${repoDir}/${manifestPath}`,
      };

      // If we were passed environment data (Cloudant bindings, etc.) add it to the options for `exec`
      if (envData) {
        console.log("envData is ", envData);
        execOptions.env = envData;
      } else {
        execOptions.env = {};
      }

      // Send 'y' to the wskdeploy command so it will actually run the deployment
      command = `printf 'y' | ${__dirname}/../wskdeploy -v -m ${manifestFileName} --auth ${wskAuth} --apihost ${wskApiHost}`;

      return new Promise((resolve, reject) => {
        console.log("command is ", command)
        const manifestFilePath = `${repoDir}/${manifestPath}/${manifestFileName}`;
        console.log("manifest file path is", manifestFilePath);
        if (!fs.existsSync(manifestFilePath)) {
          console.log("will reject");
          reject(sendError(400, `Error loading ${manifestFilePath}. Does a manifest file exist?`));
        } else {
          console.log("just before exec");
          exec(command, execOptions, (err, stdout, stderr) => {
            console.log("execing command");
            deleteFolder(repoDir);
            if (err) {
              //reject('Error running `./wskdeploy`: ', err);
              reject(sendError(400, `there was an error running wskdeploy: `, err))
            }
            console.log("no err?");
            if (stdout) {
              console.log('stdout from wskDeploy: ', stdout, ' type ', typeof stdout)

              if (typeof stdout === 'string') {
                try {
                  stdout = JSON.parse(stdout);
                } catch (e) {
                  console.log('Failed to parse stdout, it wasn\'t a JSON object');
                }
              }

              if (typeof stdout === 'object') {
                if (stdout.error) {
                  stdout.descriptiveError = 'Could not successfully run wskdeploy. Please run again with the verbose flag, -v.';
                  //reject(stdout);
                  reject(sendError(400, stdout))
                }
              }
            }
            if (stderr) {
              console.log('stderr from wskDeploy: ', stderr);
              //reject(stderr);
              reject(sendError(400, stderr))
            }
            console.log('Finished! Resolving now');
            // resolve({
            //   status: 'success',
            //   success: true,
            // });
            resolve({
              statusCode: 200,
              headers: {'Content-Type': 'application/json'},
              body: new Buffer(JSON.stringify({'status': 'success'})).toString('base64')
            })
          });
        }
      });
    });
  }
}

/**
 * Checks if wsk API host and auth were provided in params, if not, gets them from process.env
 * @param  {[Object]} params    [Params object]
 * @return {[Object]}           [Object containing wskApiHost and wskAuth]
 */
function getWskApiAuth(params) {
  let {
    wskApiHost,
    wskAuth,
  } = params;

  if (!wskApiHost) {
    wskApiHost = process.env.__OW_API_HOST;
  }

  if (!wskAuth) {
    wskAuth = process.env.__OW_API_KEY;
  }

  return {
    wskApiHost,
    wskAuth,
  };
}

/**
 * recursive funciton to delete a folder, must first delete items inside.
 * @param  {string} pathToDelete    inclusive path to folder to delete
 */
function deleteFolder(pathToDelete) {
  if (fs.existsSync(pathToDelete)) {
    fs.readdirSync(pathToDelete).forEach(function(file, index){
      var curPath = path.join(pathToDelete, file);
      if (fs.lstatSync(curPath).isDirectory()) {
        deleteFolder(curPath);
      } else {
        //unlinkSync deletes files.
        fs.unlinkSync(curPath);
      }
    });
    fs.rmdirSync(pathToDelete);
  }
}

function sendError(statusCode, error, message) {
    var params = {error: error};
    if (message) {
        params.message = message;
    }

    return {
        statusCode: statusCode,
        headers: { 'Content-Type': 'application/json' },
        body: new Buffer(JSON.stringify(params)).toString('base64')
    };
}

exports.main = main;
