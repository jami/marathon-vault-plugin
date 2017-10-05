package comptelfwd.marathon.plugin

import mesosphere.marathon.plugin.{ApplicationSpec, EnvVarValue, PathId}
import mesosphere.marathon.state.{EnvVarSecretRef, Secret}
import org.apache.mesos.Protos.TaskInfo
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.collection.immutable
import scalaj.http.HttpResponse


class MarathonVaultPluginSpec extends FreeSpec with MustMatchers {

  private val config = Json.parse("""
    {
      "token": "48bc65f8-cc2f-f4e2-805f-e5581d66d2a0",
      "address": "https://vault.marathon.mesos:8200/"
    }
  """).as[JsObject]

  private val vaultResponseFoo: String = """{"request_id":"3f9d30fc-2ff1-9d63-113f-c8f03ff7fef4","lease_id":"","renewable":false,"lease_duration":2764800,"data":{"value":"foo"},"wrap_info":null,"warnings":null,"auth":null}"""
  private val vaultResponseBar: String = """{"request_id":"3f9d30fc-2ff1-9d63-113f-c8f03ff7fef4","lease_id":"","renewable":false,"lease_duration":2764800,"data":{"value":"bar"},"wrap_info":null,"warnings":null,"auth":null}"""

  "vault response is valid parsable json" in {
    val vaultRespFoo = Json.parse(vaultResponseFoo).as[JsObject]
    val vaultRespBar = Json.parse(vaultResponseBar).as[JsObject]


    ((vaultRespFoo \ "data") \ "value").as[String] mustBe "foo"
    ((vaultRespBar \ "data") \ "value").as[String] mustBe "bar"
  }

  "initialization with a configuration" in {
    val plugin = new MarathonVaultPlugin()
    plugin.initialize(Map.empty, config)

    plugin.vaultAddr mustBe "https://vault.marathon.mesos:8200/"
    plugin.vaultToken mustBe "48bc65f8-cc2f-f4e2-805f-e5581d66d2a0"
  }

//  "env":{
//     "MY_SECRET":{
//        "secret":"secret0"
//     }
//  },
//  "secrets":{
//     "secret0":{
//        "source":"hello"
//     }
//  }

  "applying the plugin" in {
    val plugin = new MarathonVaultPlugin {
      override def httpWrap(url: String): HttpResponse[String] = {
        var fooUrl = makeVaultUrl(vaultAddr, s"v1/secret/foo")
        var barUrl = makeVaultUrl(vaultAddr, s"v1/secret/bar")
        if (url == fooUrl) {
          HttpResponse[String](vaultResponseFoo, 200, Map.empty)
        } else if(url == barUrl) {
          HttpResponse[String](vaultResponseBar, 200, Map.empty)
        } else {
          HttpResponse[String]("", 503, Map.empty)
        }
      }
    }
    plugin.initialize(Map.empty, config)

    val builder = TaskInfo.newBuilder()

    val runSpec: ApplicationSpec = new ApplicationSpec {
      val user: Option[String] = Some("root")
      val labels: Map[String, String] = Map.empty
      val id: PathId = new PathId {
        def path: immutable.Seq[String] = immutable.Seq("some", "test", "application-v1.2.4")
        override def toString = "/some/test/application-v1.2.4"
      }
      val acceptedResourceRoles: Set[String] = Set.empty
      val secrets = Map[String, Secret](
        "secret0" -> Secret("foo"),
        "secret1" -> Secret("bar"),
        "secret2" -> Secret("qux")
      )
      // secret2 is not defined and it will not be present in final ENV
      val env = Map[String, EnvVarValue](
        "FOO" -> EnvVarSecretRef("secret0"),
        "BAR" -> EnvVarSecretRef("secret1"),
        "BAZ" -> EnvVarSecretRef("secret0"),
        "QUX" -> EnvVarSecretRef("secret2")
      )         
      val volumes: Seq[mesosphere.marathon.plugin.AppVolumeSpec] = Seq.empty
      val networks: Seq[mesosphere.marathon.plugin.NetworkSpec] = Seq.empty
    }

    plugin.taskInfo(runSpec, builder)

    builder.getCommand.getEnvironment.getVariablesList.toList.map(v ⇒ v.getName → v.getValue).toMap mustBe Map(
      "FOO" -> "foo",
      "BAR" -> "bar",
      "BAZ" -> "foo"
    )
  }
}
