const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

let command = '';

/**
 * Common function to enable deployment from deployWeb.js & deploy.js
 */
function main(params) {
  return new Promise((resolve, reject) => {
    const {
      wskAuth,
      wskApiHost,
      manifestPath,
      usingTemp,
      manifestFileName,
      repoDir,
      envData,
    } = params;

    // Set the cwd of the command to be where the manifest/actions live
    const execOptions = {
      cwd: `${repoDir}/${manifestPath}`,
    };

    // If we were passed environment data (Cloudant bindings, etc.) add it to the options for `exec`
    if (envData) {
      execOptions.env = envData;
    } else {
      execOptions.env = {};
    }

    // Send 'y' to the wskdeploy command so it will actually run the deployment
    command = `printf 'y' | ${__dirname}/../../wskdeploy -v -m ${manifestFileName} --auth ${wskAuth} --apihost ${wskApiHost}`;

    const manifestFilePath = `${repoDir}/${manifestPath}/${manifestFileName}`;
    if (!fs.existsSync(manifestFilePath)) {
      const altManifestFilePath = `${repoDir}/${manifestPath}/manifest.yml`;
      if (!fs.existsSync(altManifestFilePath)) {
        if (usingTemp) {
          deleteFolder(repoDir);
        }
        reject(new Error('Error loading manifest file. Does a manifest file exist?'));
      }
    } else {
      exec(command, execOptions, (err, stdout, stderr) => {
        if (usingTemp) {
          deleteFolder(repoDir);
        }
        if (err) {
          reject(new Error('Error running `./wskdeploy`: ', err));
        }
        if (stdout) {
          console.log('stdout from wskDeploy: ', stdout, ' type ', typeof stdout);

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
              reject(new Error(stdout));
            }
          }
        }
        if (stderr) {
          console.log('stderr from wskDeploy: ', stderr);
          reject(stderr);
        }
        console.log('Finished! Resolving now');
        resolve({
          status: 'success',
          success: true,
        });
      });
    }
  });
}

/**
 * recursive function to delete a folder, must first delete items inside.
 * @param  {string} pathToDelete    inclusive path to folder to delete
 */
function deleteFolder(pathToDelete) {
  if (fs.existsSync(pathToDelete)) {
    fs.readdirSync(pathToDelete).forEach((file, index) => {
      const curPath = path.join(pathToDelete, file);
      if (fs.lstatSync(curPath).isDirectory()) {
        deleteFolder(curPath);
      } else {
        // unlinkSync deletes files.
        fs.unlinkSync(curPath);
      }
    });
    fs.rmdirSync(pathToDelete);
  }
}

module.exports = {
  main,
};
