package nl.knaw.huygens.alexandria.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/about")
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource {

  AboutInfo about = new AboutInfo();

  public AboutResource(String appName) {
    about.setName(appName);
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
  }
}
