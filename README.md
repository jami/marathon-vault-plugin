# Vault plugin to handle secrets for Marathon
It is POC implementation of [Marathon Secret API](https://github.com/mesosphere/marathon/blob/master/docs/docs/secrets.md),
see also [dcos](https://docs.mesosphere.com/1.10/security/secrets/use-secrets/) guide for more info.

## Compatibility

Tested with v1.5.1 of Marathon API.

## Package

`sbt test clean assembly`

Following artefact will be builded
```target/scala-2.11/marathon-vault-plugin-assembly-0.1.0.jar```

## Installation
0. Install [vault](https://github.com/brndnmtthws/vault-dcos) using this package. Unseal it and add test keys to `develope/my-secret`.
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
Based on [blackgold/marathon-vault-plugin](https://github.com/blackgold/marathon-vault-plugin),
see also author [blog post](http://popalgo.blogspot.com/2017/02/handling-secrets-in-dcos-or.html).
