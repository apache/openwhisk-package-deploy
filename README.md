# Using the Deploy Package

The `/whisk.system/deploy` package offers a convenient way for you to describe and deploy any part of the OpenWhisk programming model using a Manifest file written in YAML.

The package includes the following actions.

| Entity | Type | Parameters | Description |
| --- | --- | --- | --- |
| `/whisk.system/deploy` | package |  | Package to deploy OpenWhisk programming model elements |
| `/whisk.system/deploy/wskdeploy` | action | gitUrl, manifestPath, envData | Deploy from github repositories with the appropriate structure and a defining manifest. |

## wskdeploy Parameters
The `/whisk.system/deploy/wskdeploy` package deploys OpenWhisk assets from a github repository with a defining manifest.  The parameters are as follows:
- `gitUrl`: A string specifying the location of the github repository containing the assets to be deployed. For example: `https://github.com/ibm-functions/template-cloudant-trigger`

- `manifestPath`: Optional. A string specifying the location of the folder enclosing the manifest.yaml file. For example: `src/openwhisk`. If this parameter is not provided, it will default to the root of the github repo.

- `envData`: Optional. A string with a json object providing any optional enviroment data specified by the manifest.yaml file. For example:
  ```
  "{
    "CLOUDANT_HOSTNAME": "some-hostname-bluemix.cloudant.com",
    "CLOUDANT_USERNAME": "some-username",
    "CLOUDANT_PASSWORD": "my-password",
    "CLOUDANT_DATABASE": "database-name"
  }"
  ```


## Setting up your Repository

A simple hello world example of a deployable github repository can be found [here](https://github.com/ibm-functions/template-hello-world/).

A more complex example of a deployable github repository, including a trigger, a sequence, and cloudant credentials  can be found [here](https://github.com/ibm-functions/template-cloudant-trigger).

1. Create a github repository with a manifest.yaml at its root, and an actions directory containing any source files.
* actions
    * my\_action\_name.js
* manifest.yaml

If you would like the manifest.yaml file to be in a different location, you can do so, but you'll need to pass in the optional manifestPath parameter to let wskdeploy know where the file is.

* src
    * ...
    * manifest.yaml
* test

2. Please see the above referenced repositories for samples of the manifest.yaml.  The manifest.yaml describes the OpenWhisk elements to be created.  There is a great guide for writing manifests [here](https://github.com/apache/incubator-openwhisk-wskdeploy/blob/master/docs/programming_guide.md#wskdeploy-utility-by-example).


## Run the wskdeploy command

With the repository created, you can now deploy from it.

- For the most simple manifests, with no associated services you can run the command with a gitUrl parameter and a manifestPath parameter which tells wskdeploy which language you want from your project.

  ```
  wsk action invoke /whisk.system/deploy/wskdeploy
  -p gitUrl https://github.com/ibm-functions/template-hello-world/
  -p manifestPath "src/openwhisk"
  ```

# Working with repository

## Deploying the Deploy Package using `installCatalog.sh`

1. `git clone https://github.com/openwhisk/incubator-openwhisk-package-deploy`
2. `cd incubator-openwhisk-package-deploy/packages`
3. `./installCatalog.sh AUTH EDGE_HOST WSK_CLI DOCKER`
   AUTH is your auth key.  EDGE_HOST is the OpenWhisk hostname.  WSK_CLI is location of the Openwhisk CLI binary. DOCKER is an optional param for the desired `wskdeploy` docker image, resolves to `openwhisk/wskdeploy:0.9.3` by default.

> You can also remove the package using `uninstall.sh` in a similar fashion like so:
> `./uninstall.sh AUTH EDGE_HOST WSK_CLI`
