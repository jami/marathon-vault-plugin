package mesosphere.marathon.vault.plugin.env

import mesosphere.marathon.plugin.task._
import mesosphere.marathon.plugin.plugin.PluginConfiguration
import org.apache.mesos.Protos
import scalaj.http._
import play.api.libs.json._
import org.slf4j.LoggerFactory

class EnvVaultPlugin extends RunSpecTaskProcessor with PluginConfiguration {
  private[env] var envVariables = Map.empty[String, String]
  private val log = LoggerFactory.getLogger(getClass.getName)

  def initialize(marathonInfo: Map[String, Any], configuration: play.api.libs.json.JsObject): Unit = {
    envVariables = (configuration \ "env").as[Map[String, String]]
  }

  def apply(runSpec: mesosphere.marathon.plugin.RunSpec, builder: org.apache.mesos.Protos.TaskInfo.Builder): Unit = {
    val envBuilder = builder.getCommand.getEnvironment.toBuilder
    val maybeVaultAddr = envVariables.get("address")
    val maybeToken = envVariables.get("token")

    for {
      vaultAddr <- maybeVaultAddr
      token <- maybeToken
    } yield {
      runSpec.secrets.foreach {
        case(key, secret) =>
          val resp = Http(makeVaultUrl(vault_addr, s"v1/secret/${secret.source}")).header("X-Vault-Token",token).option(HttpOptions.allowUnsafeSSL).asString
          if(resp.is2xx) {
            val jsonresp = Json.parse(resp.body)
            val secretval = (jsonresp \ "data").as[String]
            val envVariable = Protos.Environment.Variable.newBuilder()
            envVariable.setName(key)
            envVariable.setValue(secretval)
            envBuilder.addVariables(envVariable)
          } else {
            log.error(s"got unexpected response from vault $resp")
          }
      }
    }

    if (maybeVaultAddr.isEmpty || maybeToken.isEmpty) {
      log.error(s"missing address and/or token in plugin config")
    }

    val commandBuilder = builder.getCommand.toBuilder
    commandBuilder.setEnvironment(envBuilder)
    builder.setCommand(commandBuilder)
  }

  protected def makeVaultUrl(host: String, path: String): String = {
    // This ignores paths on `host`, e.g. company.com/vault + /v1/sys/self-capabilities
    (new URL(new URL(host), path)).toString
  }
}

