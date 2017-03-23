package nl.knaw.huygens.alexandria.dropwizard;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huygens.alexandria.dropwizard.health.ServerHealthCheck;
import nl.knaw.huygens.alexandria.dropwizard.resources.AboutResource;

public class ServerApplication extends Application<ServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new ServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "Alexandria Markup Server";
  }

  @Override
  public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
  }

  @Override
  public void run(ServerConfiguration configuration,
                  Environment environment) {
    environment.jersey().register(new AboutResource(getName()));
    environment.healthChecks().register("server",new ServerHealthCheck());
  }
}
