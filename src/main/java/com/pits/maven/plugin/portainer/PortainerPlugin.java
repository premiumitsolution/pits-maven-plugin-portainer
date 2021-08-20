package com.pits.maven.plugin.portainer;

import com.pits.maven.plugin.data.docker.dto.RestartPolicy;
import java.net.URL;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
@Mojo(name = "portainer")
public class PortainerPlugin {

  @Parameter(property = "portainerApiUrl", required = true)
  private URL portainerApiUrl;

  @Parameter(property = "portainerLogin", required = true)
  private String portainerLogin;

  @Parameter(property = "portainerPassword", required = true)
  private String portainerPassword;

  @Parameter(property = "portainerEndPointName", required = true)
  private String portainerEndPointName;

  @Parameter(property = "dockerImageName", required = true)
  private String dockerImageName;

  @Parameter(property = "dockerImageTag", required = true)
  private String dockerImageTag;

  @Parameter(property = "containerName", required = true)
  private String containerName;

  @Parameter(property = "registryUrl", required = true)
  private String registryUrl;

  @Parameter(property = "publishedPorts", required = true)
  private String publishedPorts;

  @Parameter(property = "removeOldImages", required = true)
  private String removeOldImages;

  @Parameter(property = "restartPolicy", required = true)
  private RestartPolicy.NameEnum restartPolicy;

  @Parameter(property = "containerAccessSettingTeams", required = true)
  private String[] containerAccessSettingTeams;

  @Parameter(property = "containerAccessSettingAdministratorsOnly", required = true)
  private String[] containerAccessSettingAdministratorsOnly;

  @Parameter(property = "containerAccessSettingPublicAccess", required = true)
  private String[] containerAccessSettingPublicAccess;

}
