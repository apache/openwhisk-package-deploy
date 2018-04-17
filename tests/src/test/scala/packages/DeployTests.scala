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
import spray.json._

@RunWith(classOf[JUnitRunner])
class DeployTests extends TestHelpers
    with WskTestHelpers
    with BeforeAndAfterAll {

    implicit val wskprops = WskProps()
    val wsk = new Wsk()

    // set parameters for deploy tests
    val deployTestRepo = "https://github.com/apache/incubator-openwhisk-package-deploy"
    val incorrectGithubRepo = "https://github.com/apache/openwhisk-package-deploy-incorrect"
    val helloWorldPath = "tests/src/test/scala/testFixtures/helloWorld"
    val helloWorldWithNoManifest = "tests/src/stest/scala/testFixtures/helloWorldNoManifest"
    val helloWorldPackageParam = "tests/src/test/scala/testFixtures/helloWorldPackageParam"
    val incorrectManifestPath = "does/not/exist"
    val uselessEnvData = """{ "something": "useless" }"""
    val packageEnvData = """{ "PACKAGE_NAME": "myPackage" }"""
    val deployAction = "/whisk.system/deploy/wskdeploy"
    val helloWorldAction = "openwhisk-helloworld/helloworld"
    val helloWorldActionPackage = "myPackage/helloworld"

    behavior of "Deploy Package"

    // test to create the hello world template from github
    it should "create the hello world action from github url" in {
      val run = wsk.action.invoke(deployAction, Map(
        "gitUrl" -> deployTestRepo.toJson,
        "manifestPath" -> helloWorldPath.toJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe true
          val logs = activation.logs.get.toString
          logs should include(s"action [$helloWorldAction] has been successfully deployed.")
        }
        // clean up after test
        wsk.action.delete(helloWorldAction)
    }

    // test to create the hello world template from github with myPackage as package name
    it should s"create the $helloWorldActionPackage action from github url" in {
      val run = wsk.action.invoke(deployAction, Map(
        "gitUrl" -> deployTestRepo.toJson,
        "manifestPath" -> helloWorldPackageParam.toJson,
        "envData" -> packageEnvData.parseJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe true
          val logs = activation.logs.get.toString
          logs should include(s"action [$helloWorldActionPackage] has been successfully deployed.")
        }
        // clean up after test
        wsk.action.delete(helloWorldActionPackage)
    }

    // test to create a template with no github repo provided
    it should "return error if there is no github repo provided" in {
      val run = wsk.action.invoke(deployAction, Map(
        "manifestPath" -> helloWorldPath.toJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe false
          activation.response.result.get.toString should include("Please enter the GitHub repo url in params")
        }
    }

    // test to create a template with a nonexistant github repo provided
    it should "return error if there is an nonexistant repo provided" in {
      val run = wsk.action.invoke(deployAction, Map(
        "gitUrl" -> incorrectGithubRepo.toJson,
        "manifestPath" -> helloWorldPath.toJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe false
          activation.response.result.get.toString should include("There was a problem cloning from github.")
        }
    }

    // test to create a template with useless EnvData provided
    it should "return succeed if useless envData is provided" in {
      val run = wsk.action.invoke(deployAction, Map(
        "gitUrl" -> deployTestRepo.toJson,
        "manifestPath" -> helloWorldPath.toJson,
        "envData" -> uselessEnvData.parseJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe true
          val logs = activation.logs.get.toString
          logs should include(s"action [$helloWorldAction] has been successfully deployed.")
        }
        // clean up after test
        wsk.action.delete(helloWorldAction)
    }

    // test to create a template with an incorrect manifestPath provided
    it should "return with failure if incorrect manifestPath is provided" in {
      val run = wsk.action.invoke(deployAction, Map(
        "gitUrl" -> deployTestRepo.toJson,
        "manifestPath" -> incorrectManifestPath.toJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe false
          activation.response.result.get.toString should include("Does a manifest file exist?")
        }
    }

    // test to create a template with manifestPath provided, but no manifestFile existing
    it should "return with failure if no manifest exists at manifestPath" in {
      val run = wsk.action.invoke(deployAction, Map(
        "gitUrl" -> deployTestRepo.toJson,
        "manifestPath" -> helloWorldWithNoManifest.toJson))
        withActivation(wsk.activation, run) {
          activation =>
          activation.response.success shouldBe false
          activation.response.result.get.toString should include("Does a manifest file exist?")
        }
    }
}
