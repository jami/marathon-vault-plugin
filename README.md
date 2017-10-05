# Marathon Vault Plugin
POC implementation of [Marathon Secret API](https://github.com/mesosphere/marathon/blob/master/docs/docs/secrets.md),
it allows to expose secrets from [Vault](https://www.vaultproject.io/) to Marathon ENV variables. See also [dcos](https://docs.mesosphere.com/1.10/security/secrets/use-secrets/) guide for more info.

## Compatibility

Tested with Marathon v1.5.1.

## Package

`sbt test clean assembly`

Following artifact will be builded
```target/scala-2.11/marathon-vault-plugin-assembly-0.1.0.jar```

## Installation
0. Install [vault](https://github.com/brndnmtthws/vault-dcos) using this package. Unseal it and add test key to `developer/my-secret`.
1. Change and upload provided [plugin-conf.json](/plugin-conf.json) to marathon host. Put it to `/etc/marathon/plugin-conf.json` folder.
2. Upload builded artifact to `/etc/marathon/plugins/marathon-vault-plugin-assembly-0.1.0.jar`
3. Provide following options to marathon config:
```
   --plugin_dir "/etc/marathon/plugins" \
   --plugin_conf "/etc/marathon/plugin-conf.json" \
   --enable_features "secrets,..."
```
`/etc/systemd/system/dcos-marathon.service` is one of possible candidate to look for marathon config.

4. Restart marathon to load plugin.
5. Deploy test container
```
{
  "id":"/developer/service",
  "cmd":"sleep 100",
  "env":{
     "MY_SECRET":{
        "secret":"secret0"
     }
  },
  "secrets":{
     "secret0":{
        "source":"developer/my-secret"
     }
  }
}
```


### Kudos 
Based on [blackgold/marathon-vault-plugin](https://github.com/blackgold/marathon-vault-plugin) and
[servehub/marathon-secrets-plugin](https://github.com/servehub/marathon-secrets-plugin).

See also blackgold [blog post](http://popalgo.blogspot.com/2017/02/handling-secrets-in-dcos-or.html) for more info.