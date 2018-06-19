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

/** Mojo for uploading extra data to AWS Device Farm */
@Mojo(name = "uploadExtraData")
public class UploadExtraData extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(UploadExtraData.class);

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

  @Parameter(required = false)
  private String extraData;

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      AWSService awsService = new AWSService(awsAccessKey, awsSecretKey, awsRegion);

      // upload extra data
      LOG.info("Uploading extra data from {}", extraData);
      String extraDataArn = null;
      if (extraData != null && !extraData.isEmpty()) {
        extraDataArn =
            awsService
                .upload(new File(extraData), projectArn, UploadType.EXTERNAL_DATA, true)
                .getArn();
      }

      LOG.info("Setting extraDataArn {} as Maven property", extraDataArn);
      project.getProperties().put("extraDataArn", extraDataArn);

    } catch (AWSDeviceFarmException | InterruptedException | IOException exception) {
      LOG.error("Error while triggering AWS test", exception);
    }
  }
}
