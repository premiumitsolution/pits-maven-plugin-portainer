package com.pits.maven.plugin.portainer.data.dto.docker;

import com.google.gson.annotations.SerializedName;
import com.pits.maven.plugin.data.portainer.dto.ResourceControl;
import lombok.Data;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.8
 */
@Data
public class PortainerDto {

  @SerializedName("ResourceControl")
  private ResourceControl resourceControl;

}
