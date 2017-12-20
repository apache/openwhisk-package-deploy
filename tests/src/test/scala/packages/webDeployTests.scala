/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package packages


import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import common.{TestHelpers, Wsk, WskProps, WskTestHelpers}
import common.TestUtils.FORBIDDEN

import com.jayway.restassured.RestAssured
import com.jayway.restassured.config.SSLConfig

import spray.json._

@RunWith(classOf[JUnitRunner])
class WebDeployTests extends TestHelpers
    with WskTestHelpers
    with BeforeAndAfterAll {

    implicit val wskprops = WskProps()
    val wsk = new Wsk()

    //action and web action url
    val deployAction = "/whisk.system/deployWeb/wskdeploy"
    val deployActionURL = s"https://${wskprops.apihost}/api/v1/web${deployAction}.http"

    //set parameters for deploy tests
    val deployTestRepo = "https://github.com/apache/incubator-openwhisk-package-deploy"
    val incorrectGithubRepo = "https://github.com/apache/openwhisk-package-deploy-incorrect"
    val malformedRepoUrl = "github.com/ibm-functions/blueprint-hello-world"
    val helloWorldPath = "tests/src/test/scala/testFixtures/helloWorld"
    val helloWorldWithNoManifest = "tests/src/stest/scala/testFixtures/helloWorldNoManifest"
    val helloWorldPackageParam = "tests/src/test/scala/testFixtures/helloWorldPackageParam"
    val incorrectManifestPath = "does/not/exist"
    val helloWorldAction = "openwhisk-helloworld/helloworld"
    val helloWorldActionPackage = "myPackage/helloworld"

    // statuses from deployWeb
    val successStatus = """{"status":"success"}"""
    val githubNonExistentStatus = """{"error":"There was a problem cloning from github.  Does that github repo exist?  Does it begin with http?"}"""

    def makePostCallWithExpectedResult(params: JsObject, expectedResult: String, expectedCode: Int) = {
      val response = RestAssured.given()
          .contentType("application/json\r\n")
          .config(RestAssured.config().sslConfig(new SSLConfig().relaxedHTTPSValidation()))
          .body(params.toString())
          .post(deployActionURL)
      assert(response.statusCode() == expectedCode)
      response.body.asString shouldBe expectedResult
    }
    
    behavior of "deployWeb Package"

    // test to ensure action not obtainable using CLI
    it should "not be obtainable using the CLI" in {
      wsk.action.get(deployAction, FORBIDDEN)
    }

    //test to create the hello world blueprint from github
    it should "create the hello world action from github url" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(helloWorldPath),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), successStatus, 200);

      // clean up after test
      wsk.action.delete(helloWorldAction)
    }

    // test to create the hello world blueprint from github with myPackage as package name
    it should s"create the $helloWorldActionPackage action from github url" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(helloWorldPackageParam),
        "envData" -> JsObject("PACKAGE_NAME" -> JsString("myPackage")),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), successStatus, 200)

      // clean up after test
      wsk.action.delete(helloWorldActionPackage)
    }

    // test to create a blueprint with no github repo provided
    it should "return error if there is no github repo provided" in {
      makePostCallWithExpectedResult(JsObject(
        "manifestPath" -> JsString(helloWorldPath),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), """{"error":"Please enter the GitHub repo url in params"}""", 400)
    }

    //test to create a blueprint with a nonexistant github repo provided
    it should "return error if there is an nonexistant repo provided" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(incorrectGithubRepo),
        "manifestPath" -> JsString(helloWorldPath),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), githubNonExistentStatus, 400)
    }
    //test to create a blueprint with a malformed github repo
    it should "return error if there is a malformed gitUrl provided" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(malformedRepoUrl),
        "manifestPath" -> JsString(helloWorldPath),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), githubNonExistentStatus, 400)
    }

    // test to create a blueprint with useless EnvData provided
    it should "return succeed if useless envData is provided" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(helloWorldPath),
        "envData" -> JsObject("something" -> JsString("useless")),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), successStatus, 200)

      // clean up after test
      wsk.action.delete(helloWorldAction)
    }

    // test to create a blueprint with an incorrect manifestPath provided
    it should "return with failure if incorrect manifestPath is provided" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(incorrectManifestPath),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), """{"error":"Error loading manifest file. Does a manifest file exist?"}""", 400)
    }

    // test to create a blueprint with manifestPath provided, but no manifestFile existing
    it should "return with failure if no manifest exists at manifestPath" in {
      makePostCallWithExpectedResult(JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(helloWorldWithNoManifest),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ), """{"error":"Error loading manifest file. Does a manifest file exist?"}""", 400)
    }
}
