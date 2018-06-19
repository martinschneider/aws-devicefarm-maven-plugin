package io.github.martinschneider.aws;

import com.amazonaws.services.devicefarm.model.AWSDeviceFarmException;
import com.amazonaws.services.devicefarm.model.UploadType;
import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mojo for uploading application packages (APK, IPA) to AWS Device Farm */
@Mojo(name = "uploadAppPackage")
public class UploadAppPackage extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(UploadAppPackage.class);

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(required = true)
  private String awsAccessKey;

  @Parameter(required = true)
  private String awsSecretKey;

  @Parameter(defaultValue = "us-east-1")
  private String awsRegion;

  @Parameter(required = true)
  private String projectArn;

  @Parameter(required = true)
  private String appPackage;

  @Parameter(required = true)
  private String platform;

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      AWSService awsService = new AWSService(awsAccessKey, awsSecretKey, awsRegion);

      // upload app
      LOG.info("Uploading app package from {}", appPackage);
      UploadType uploadType;
      if (platform.equalsIgnoreCase("IOS")) {
        uploadType = UploadType.IOS_APP;
      } else if (platform.equalsIgnoreCase("Android")) {
        uploadType = UploadType.ANDROID_APP;
      } else {
        throw new MojoExecutionException("Unknown platform " + platform);
      }
      String appArn =
          awsService.upload(new File(appPackage), projectArn, uploadType, true).getArn();

      LOG.info("Setting appArn {} as Maven property", appArn);
      project.getProperties().put("appArn", appArn);

    } catch (AWSDeviceFarmException | InterruptedException | IOException exception) {
      LOG.error("Error while triggering AWS test", exception);
    }
  }
}
