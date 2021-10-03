# Premium IT Solution Portainer.io maven-plugin

## Version History

### 1.0.1

- Добавлена поддержка Portainer.io Версии 2.9

### 1.0.0

- Первая версия плагина 

## Enabling the plugin

Add to your `pom.xml`:

```xml
<plugin>
  <groupId>com.pits.maven</groupId>
  <artifactId>pits-portainer-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <configuration>
    <portainerApiUrl></portainerApiUrl>
    <portainerLogin></portainerLogin>
    <portainerPassword></portainerPassword>
    <portainerEndPointName></portainerEndPointName>
    <containerName></containerName>
    <dockerImageName></dockerImageName>
    <dockerImageTag></dockerImageTag>
    <registryUrl></registryUrl>
    <publishedPorts></publishedPorts>
    <removeOldImages></removeOldImages>
    <restartPolicy></restartPolicy>
    <containerAccessSettingTeams>
      <param></param>
    </containerAccessSettingTeams>
    <containerAccessSettingAdministratorsOnly></containerAccessSettingAdministratorsOnly>
    <containerAccessSettingPublicAccess></containerAccessSettingPublicAccess>
    <volumes>
      <test_volume_name>/var/example</test_volume_name>
    </volumes>
  </configuration>
</plugin>
```

portainerApiUrl - URL for portainer api, for example: https://repo.yourdomain.ru/api

publishedPorts - port for publish in format: type/containerPort/hostPort;type/containerPort/hostPort. For example: tcp/8080/80, tcp/8081/8888

removeOldImages - if true, the old images will be removed from portainer.io restartPolicy - 'ALWAYS', 'ONFAILURE', 'UNLESSSTOPPED'

## Test
Tested with portainer.io version:

- 2.0.0
- 2.1.1
- 2.6.0
- 2.9.0