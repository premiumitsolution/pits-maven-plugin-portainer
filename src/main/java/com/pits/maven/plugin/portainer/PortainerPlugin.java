package com.pits.maven.plugin.portainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pits.maven.plugin.data.docker.dto.EndpointIPAMConfig;
import com.pits.maven.plugin.data.docker.dto.EndpointSettings;
import com.pits.maven.plugin.data.docker.dto.HostConfig;
import com.pits.maven.plugin.data.docker.dto.NetworkingConfig;
import com.pits.maven.plugin.data.docker.dto.PortBinding;
import com.pits.maven.plugin.data.docker.dto.RestartPolicy;
import com.pits.maven.plugin.data.portainer.ApiClient;
import com.pits.maven.plugin.data.portainer.ApiException;
import com.pits.maven.plugin.data.portainer.controller.AuthApi;
import com.pits.maven.plugin.data.portainer.controller.ContainerApi;
import com.pits.maven.plugin.data.portainer.controller.EndpointsApi;
import com.pits.maven.plugin.data.portainer.controller.ResourceControlsApi;
import com.pits.maven.plugin.data.portainer.controller.TeamsApi;
import com.pits.maven.plugin.data.portainer.dto.AuthenticateUserRequest;
import com.pits.maven.plugin.data.portainer.dto.AuthenticateUserResponse;
import com.pits.maven.plugin.data.portainer.dto.ContainerSummary;
import com.pits.maven.plugin.data.portainer.dto.EndpointSubset;
import com.pits.maven.plugin.data.portainer.dto.ResourceControlUpdateRequest;
import com.pits.maven.plugin.data.portainer.dto.Team;
import com.pits.maven.plugin.portainer.api.PortainerDockerApi;
import com.pits.maven.plugin.portainer.data.dto.docker.ContainerCreatePortainerRequest;
import com.pits.maven.plugin.portainer.data.dto.docker.ContainerCreatePortainerResponse;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
@Mojo(name = "pits-portainer")
public class PortainerPlugin extends AbstractMojo {

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
  private RestartPolicy.NameEnum restartPolicyName;

  @Parameter(property = "containerAccessSettingTeams", required = true)
  private List<String> containerAccessSettingTeams;

  @Parameter(property = "containerAccessSettingAdministratorsOnly", required = true)
  private boolean containerAccessSettingAdministratorsOnly;

  @Parameter(property = "containerAccessSettingPublicAccess", required = true)
  private boolean containerAccessSettingPublicAccess;

  @Parameter(property = "volumes")
  private Map<String, String> volumes;

  private PortainerDockerApi portainerDockerApi;
  private ApiClient portainerApiClient;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("PitS Portainer Plugin Start");

    initDockerApi();

    getLog().info(String.format("Deploy image '%s' to portainer '%s' with endpoint '%s'", dockerImageName, portainerApiUrl, portainerEndPointName));

    getLog().info("Initialize portainerApi");
    initPortainerApi();

    getLog().info("Authenticate portainerApi");
    String apiToken = authenticate();

    getLog().info("Determine endpoint");
    Integer endPointId = determineEndPoint();

    getLog().info("Remove old container");
    removeOldContainer(apiToken, endPointId);

    getLog().info("Pull image container");
    pullImage(apiToken, endPointId);

    getLog().info("Create new container with specified image");
    String containerId = createNewContainer(apiToken, endPointId);

    getLog().info("PitS Portainer Plugin Finished");
  }

  private void initPortainerApi() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    if (getLog().isDebugEnabled()) {
      HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
      loggingInterceptor.setLevel(Level.BODY);
      builder.addInterceptor(loggingInterceptor);
    }
    portainerApiClient = new ApiClient(builder.build());
    portainerApiClient.setBasePath(portainerApiUrl.toString().substring(0, portainerApiUrl.toString().length() - 1));
    portainerApiClient.setUserAgent("");
  }

  private void initDockerApi() {
    getLog().info("Initialize initDockerApi");
    Gson gson = new GsonBuilder()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        .create();

    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(Level.BODY);
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.addInterceptor(loggingInterceptor);
    builder.readTimeout(2, TimeUnit.MINUTES);
    builder.writeTimeout(2, TimeUnit.MINUTES);
    builder.hostnameVerifier((hostname, session) -> true);
    OkHttpClient okHttpClient = builder.build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(portainerApiUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build();
    portainerDockerApi = retrofit.create(PortainerDockerApi.class);
  }

  private String authenticate() throws MojoFailureException {
    try {
      AuthApi authApi = new AuthApi(portainerApiClient);
      AuthenticateUserRequest authenticateUserRequest = new AuthenticateUserRequest()
          .username(portainerLogin)
          .password(portainerPassword);

      AuthenticateUserResponse response = authApi.authenticateUser(authenticateUserRequest);
      portainerApiClient.addDefaultHeader("Authorization", "Bearer " + response.getJwt());
      return response.getJwt();
    } catch (ApiException error) {
      throw new MojoFailureException("Error while authenticate for Portainer", error);
    }
  }

  private Integer determineEndPoint() throws MojoFailureException {
    try {
      EndpointsApi endpointsApi = new EndpointsApi(portainerApiClient);
      List<EndpointSubset> endpointList = endpointsApi.endpointList();
      Optional<EndpointSubset> endpointSubsetOptional = endpointList.stream()
          .filter(endpointSubset -> endpointSubset.getName() != null && endpointSubset.getName().equals(portainerEndPointName)).findFirst();
      return endpointSubsetOptional.orElseThrow(() -> new MojoFailureException("Can't found endpoint by name:" + portainerEndPointName)).getId();
    } catch (ApiException error) {
      throw new MojoFailureException("Error while determine endPoint inside Portainer", error);
    }
  }

  private void removeOldContainer(String apiToken, Integer endpointId) throws MojoFailureException {
    try {
      ContainerApi containerApi = new ContainerApi(portainerApiClient);
      String fillerByName = String.format("{\"name\": [\"%s\"]}", containerName);
      List<ContainerSummary> foundedContainers = containerApi.endpointContainerList(endpointId, true, null, null, fillerByName);
      if ((foundedContainers != null) && (foundedContainers.size() > 0)) {
        for (ContainerSummary containerSummary : foundedContainers) {
          getLog().info(String.format("Remove container with id='%s', name='%s' and image='%s'", containerSummary.getId(), containerSummary.getNames(),
              containerSummary.getImage()));
          Call<Void> callDeleteContainer = portainerDockerApi.removeContainer(endpointId, containerSummary.getId(), true, true, false, apiToken);
          Response<Void> dockerResponse = callDeleteContainer.execute();
          if (dockerResponse.code() != 204) {
            throw new RuntimeException(String.format("Error while delete container '%s': %s", containerSummary.getId(), dockerResponse.message()));
          }
        }
      } else {
        getLog().info("There is not containers for remove");
      }
    } catch (ApiException | IOException error) {
      throw new MojoFailureException("Error while remove old containers", error);
    }
  }

  private void pullImage(String apiToken, Integer endPointId) throws MojoFailureException {
    try {
      String containerName = String.format("%s:%s", dockerImageName, dockerImageTag);
      String registryAuth = String.format("{\n" + "  \"serveraddress\": \"%s\"" + "}", registryUrl);
      registryAuth = Base64.getEncoder().encodeToString(registryAuth.getBytes(StandardCharsets.UTF_8));
      Response<Void> createImageResponse = portainerDockerApi
          .createImage(endPointId, containerName, registryAuth, apiToken)
          .execute();
      if (createImageResponse.code() != 200) {
        throw new MojoFailureException("Error while pull container:" + createImageResponse.message());
      }
    } catch (IOException error) {
      throw new MojoFailureException("Error while pull container:" + containerName, error);
    }
  }

  private String createNewContainer(String apiToken, Integer endPointId) throws MojoFailureException {
    try {
      ContainerCreatePortainerRequest containerConfig = new ContainerCreatePortainerRequest();
      containerConfig.image(String.format("%s:%s", dockerImageName, dockerImageTag));
      containerConfig.openStdin(false);
      containerConfig.tty(false);
      containerConfig.volumes(createMapOfVolumes());

      RestartPolicy restartPolicy = new RestartPolicy().name(restartPolicyName);

      HostConfig hostConfig = new HostConfig()
          .networkMode("bridge")
          .restartPolicy(restartPolicy)
          .publishAllPorts(false)
          .autoRemove(false)
          .privileged(false)
          .init(false);

      if (publishedPorts != null) {
        String[] portArray = publishedPorts.split(",");
        if (portArray.length > 0) {
          Map<String, Object> exposedPorts = new HashMap<>();
          Map<String, List<PortBinding>> hostPortBindings = new HashMap<>();
          Arrays.stream(portArray).map(s -> s.split("/")).forEach(strings -> {
            String protocol = strings[0].trim();
            String containerPort = strings[1].trim();
            String hostPort = strings[2].trim();

            // Add exposed port
            String exposeValue = String.format("%s/%s", containerPort, protocol);
            exposedPorts.put(exposeValue, new Object());

            // Add port binding
            hostPortBindings.put(exposeValue, Collections.singletonList(new PortBinding().hostPort(hostPort)));
          });
          hostConfig.portBindings(hostPortBindings);
          setupVolumeBinds(hostConfig);
          containerConfig.setExposedPorts(exposedPorts);
          containerConfig.setHostConfig(hostConfig);

          Map<String, EndpointSettings> endpointSettingsMap = new HashMap<>();
          endpointSettingsMap.put("bridge", new EndpointSettings().ipAMConfig(new EndpointIPAMConfig().ipv4Address("").ipv6Address("")));
          NetworkingConfig networkingConfig = new NetworkingConfig().endpointsConfig(endpointSettingsMap);

          containerConfig.setNetworkingConfig(networkingConfig);
        }
      }

      Call<ContainerCreatePortainerResponse> callDeleteContainer = portainerDockerApi
          .createContainer(endPointId, containerConfig, containerName, apiToken);
      Response<ContainerCreatePortainerResponse> dockerResponse = callDeleteContainer.execute();
      if ((dockerResponse.code() == 200) || (dockerResponse.code() == 201)) {
        ContainerCreatePortainerResponse createResponse = dockerResponse.body();
        StringJoiner sb = new StringJoiner("\n");
        if (createResponse.getWarnings() != null) {
          createResponse.getWarnings().forEach(sb::add);
        }
        getLog().info(String.format("Created new container with id='%s', resourceId='%s', warnings='%s'", createResponse.getId(),
            createResponse.getPortainer().getResourceControl().getId(), sb));

        //Установка прав доступа к созданному контейнеру
        setupContainerSecurity(createResponse);

        return createResponse.getId();
      } else {
        throw new MojoFailureException("Error while create container:" + dockerResponse.message());
      }

    } catch (IOException error) {
      throw new MojoFailureException("Error while create new container", error);
    }
  }

  /**
   * Берем заданные пользователем volumes, берем из них директорию и создаем map где ключ - директория контейнера, а значение - null (так надо)
   *
   * @return
   */
  @NotNull
  private Map<String, Object> createMapOfVolumes() {
    Collection<String> volumeDirs = volumes.values();
    Map<String, Object> resultVolumesMap = new HashMap<>();
    for (String volumeDir : volumeDirs) {
      resultVolumesMap.put(volumeDir, null);
    }
    return resultVolumesMap;
  }

  private void setupContainerSecurity(ContainerCreatePortainerResponse createResponse) throws MojoFailureException {
    try {
      Integer resourceControlId = createResponse.getPortainer().getResourceControl().getId();
      List<Team> portainerTeamList = getPortainerTeamList();
      Map<String, Integer> teamMap = portainerTeamList.stream().collect(Collectors.toMap(Team::getName, Team::getId));
      ResourceControlsApi resourceControlsApi = new ResourceControlsApi(portainerApiClient);
      ResourceControlUpdateRequest updateData = new ResourceControlUpdateRequest()
          ._public(containerAccessSettingPublicAccess);
      for (String teamCode : containerAccessSettingTeams) {
        if (teamMap.containsKey(teamCode)) {
          updateData.addTeamsItem(teamMap.get(teamCode));
        } else {
          throw new MojoFailureException("Can't found team by code:" + teamCode);
        }
      }
      resourceControlsApi.resourceControlUpdate(resourceControlId, updateData);
    } catch (ApiException error) {
      throw new MojoFailureException("Error while setup container security", error);
    }
  }

  private List<Team> getPortainerTeamList() throws MojoFailureException {
    try {
      TeamsApi teamsApi = new TeamsApi(portainerApiClient);
      return teamsApi.teamList();
    } catch (ApiException error) {
      throw new MojoFailureException("Error while get team list", error);
    }
  }

  /**
   * Устанавливает заданные volumes в binds указанного host config-а
   *
   * @param hostConfig конфиг, в который необходимо установить значение
   */
  private void setupVolumeBinds(HostConfig hostConfig) {
    if (volumes.isEmpty()) {
      return;
    }
    List<String> volumesList = volumes
        .entrySet()
        .stream()
        .map(entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    final List<String> binds = hostConfig.getBinds();

    if (binds == null) {
      hostConfig.binds(volumesList);
    } else {
      binds.addAll(volumesList);
    }
  }

}