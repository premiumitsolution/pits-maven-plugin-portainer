package com.pits.maven.plugin.portainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pits.maven.plugin.data.docker.dto.RestartPolicy;
import com.pits.maven.plugin.data.portainer.ApiClient;
import com.pits.maven.plugin.portainer.api.PortainerDockerApi;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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
  private RestartPolicy.NameEnum restartPolicy;

  @Parameter(property = "containerAccessSettingTeams", required = true)
  private String[] containerAccessSettingTeams;

  @Parameter(property = "containerAccessSettingAdministratorsOnly", required = true)
  private String[] containerAccessSettingAdministratorsOnly;

  @Parameter(property = "containerAccessSettingPublicAccess", required = true)
  private String[] containerAccessSettingPublicAccess;

  private PortainerDockerApi portainerDockerApi;
  private ApiClient portainerApiClient;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("PitS Portainer Plugin Start");

    initDockerApi();

    getLog().info(String.format("Deploy image '%s' to portainer '%s' with endpoint '%s'", dockerImageName, portainerApiUrl, portainerEndPointName));

    getLog().info("PitS Portainer Plugin Finish");

    getLog().info("Initialize portainerApi");
    initPortainerApi();

  }

  private void initPortainerApi() {
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(Level.BODY);
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.addInterceptor(loggingInterceptor);

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
}
