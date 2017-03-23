package nl.knaw.huygens.alexandria.dropwizard.resources;

import java.time.Instant;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

@Path("/about")
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource {

  AboutInfo about = new AboutInfo();
  private Instant startedAt;

  public AboutResource(String appName) {
    about.setName(appName);
    startedAt = Instant.now();
  }

  @GET
  @Timed
  public AboutInfo getAbout() {
    return about;
  }

  class AboutInfo {
    String appName;

    public void setName(String appName) {
      this.appName = appName;
    }

    public String getAppName() {
      return appName;
    }

    @JsonSerialize(using = InstantSerializer.class)
    public Instant getStartedAt() {
      return startedAt;
    }
  }
}
