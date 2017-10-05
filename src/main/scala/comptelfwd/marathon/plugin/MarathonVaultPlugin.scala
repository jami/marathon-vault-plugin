package comptelfwd.marathon.plugin

import java.net.URL

import mesosphere.marathon.plugin.plugin.PluginConfiguration
import mesosphere.marathon.plugin.task._
import mesosphere.marathon.plugin.{ApplicationSpec, PodSpec}
import mesosphere.marathon.state.EnvVarSecretRef
import org.apache.mesos.Protos
import org.apache.mesos.Protos.{ExecutorInfo, TaskGroupInfo, TaskInfo}
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scalaj.http._

class MarathonVaultPlugin extends RunSpecTaskProcessor with PluginConfiguration {

  private val log = LoggerFactory.getLogger(getClass.getName)

  private[plugin] var vaultAddr: String = ""
  private[plugin] var vaultToken: String = ""

  def httpWrap(url: String): HttpResponse[String] = {
    Http(url)
      .header("X-Vault-Token", vaultToken)
      .option(HttpOptions.allowUnsafeSSL)
      .asString
  }

  def initialize(marathonInfo: Map[String, Any], configuration: JsObject): Unit = {
    vaultAddr = (configuration \ "address").as[String].trim
    vaultToken = (configuration \ "token").as[String].trim
    log.info(s"MarathonVaultPlugin initialized: vaultAddress=${vaultAddr}")
  }

  def taskInfo(appSpec: ApplicationSpec, builder: TaskInfo.Builder): Unit = {
    val envBuilder = builder.getCommand.getEnvironment.toBuilder

    if (vaultAddr.isEmpty || vaultToken.isEmpty) {
      log.error(s"missing address and/or token in plugin config")
    }

    appSpec.secrets.foreach {
      case(key, secret) =>
        val resp = httpWrap(makeVaultUrl(vaultAddr, s"v1/secret/${secret.source}"))

        if(resp.is2xx) {
          val jsonresp = Json.parse(resp.body)
          val secretval = ((jsonresp \ "data") \ "value").as[String]
          appSpec.env.foreach {
            case(envKey, envValue) =>
              val envVariable = Protos.Environment.Variable.newBuilder()
              if(EnvVarSecretRef(key) == envValue) {
                envVariable.setName(envKey)
                envVariable.setValue(secretval)
                envBuilder.addVariables(envVariable)              
              }
          }
        } else {
          log.error(s"got unexpected response from vault $resp")
        }
    }
    

    val commandBuilder = builder.getCommand.toBuilder
    commandBuilder.setEnvironment(envBuilder)
    builder.setCommand(commandBuilder) 
  }

  def taskGroup(podSpec: PodSpec, executor: ExecutorInfo.Builder, taskGroup: TaskGroupInfo.Builder): Unit = {}

  protected def makeVaultUrl(host: String, path: String): String = {
    // This ignores paths on `host`, e.g. company.com/vault + /v1/sys/self-capabilities
    (new URL(new URL(host), path)).toString
  }
}