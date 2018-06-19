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

/** Mojo for uploading test packages to AWS Device Farm */
@Mojo(name = "uploadTestPackage")
public class UploadTestPackage extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(UploadTestPackage.class);

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
  private String testPackage;
  
  @Parameter(defaultValue = "APPIUM_JAVA_JUNIT_TEST_PACKAGE")
  private String uploadType;

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      AWSService awsService = new AWSService(awsAccessKey, awsSecretKey, awsRegion);

      // upload test package
      LOG.info("Uploading test package from {}", testPackage);
      String testArn =
          awsService
              .upload(
                  new File(testPackage),
                  projectArn,
                  UploadType.valueOf(uploadType),
                  true)
              .getArn();

      LOG.info("Setting testArn {} as Maven property", testArn);
      project.getProperties().put("testArn", testArn);

    } catch (AWSDeviceFarmException | InterruptedException | IOException exception) {
      LOG.error("Error while triggering AWS test", exception);
    }
  }
}
