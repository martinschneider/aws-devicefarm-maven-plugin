package io.github.martinschneider.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.AWSDeviceFarmClient;
import com.amazonaws.services.devicefarm.AWSDeviceFarmClientBuilder;
import com.amazonaws.services.devicefarm.model.AWSDeviceFarmException;
import com.amazonaws.services.devicefarm.model.BillingMethod;
import com.amazonaws.services.devicefarm.model.CreateUploadRequest;
import com.amazonaws.services.devicefarm.model.ExecutionConfiguration;
import com.amazonaws.services.devicefarm.model.GetUploadRequest;
import com.amazonaws.services.devicefarm.model.GetUploadResult;
import com.amazonaws.services.devicefarm.model.Location;
import com.amazonaws.services.devicefarm.model.Radios;
import com.amazonaws.services.devicefarm.model.ScheduleRunConfiguration;
import com.amazonaws.services.devicefarm.model.ScheduleRunRequest;
import com.amazonaws.services.devicefarm.model.ScheduleRunResult;
import com.amazonaws.services.devicefarm.model.ScheduleRunTest;
import com.amazonaws.services.devicefarm.model.TestType;
import com.amazonaws.services.devicefarm.model.Upload;
import com.amazonaws.services.devicefarm.model.UploadType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "run")
public class TestRunMojo extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(TestRunMojo.class);

  @Parameter(required = true)
  private String awsAccessKey;

  @Parameter(required = true)
  private String awsSecretKey;

  @Parameter(defaultValue = "us-east-1")
  private String awsRegion;

  @Parameter(required = true)
  private String projectArn;

  @Parameter(required = true)
  private String devicePoolArn;

  @Parameter(required = true)
  private String testPackage;

  @Parameter(required = true)
  private String appPackage;

  @Parameter(required = true)
  private String platform;

  @Parameter(required = false)
  private String extraData;

  // default location = Singapore
  @Parameter(defaultValue = "1.3521")
  private double deviceLatitude;

  @Parameter(defaultValue = "103.8198")
  private double deviceLongitude;

  @Parameter(defaultValue = "triggered by aws-devicefarm-maven-plugin")
  private String executionName;

  @Parameter(defaultValue = "true")
  private boolean bluetooth;

  @Parameter(defaultValue = "true")
  private boolean gps;

  @Parameter(defaultValue = "true")
  private boolean nfc;

  @Parameter(defaultValue = "true")
  private boolean wifi;

  @Parameter(defaultValue = "true")
  private boolean runUnmetered;

  @Parameter(required = false)
  private boolean accountsCleanup;

  @Parameter(required = false)
  private boolean appPackagesCleanup;

  @Parameter(required = false, defaultValue = "60")
  private int jobTimeoutMinutes;

  @Parameter(required = false)
  private boolean skipAppResign;

  @Parameter(required = false, defaultValue = "1.7.2")
  private String appiumVersion;

  private AWSDeviceFarm aws;

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      LOG.info("Building AWS Device Farm client");
      LOG.debug("awsAccessKey={}", awsAccessKey);
      LOG.debug("awsSecretKey={}", awsSecretKey);
      LOG.debug("awsRegion={}", awsRegion);
      aws = buildClient();

      LOG.info("Triggering AWS Device Farm test run");
      // log configuration
      LOG.debug("projectArn={}", projectArn);
      LOG.debug("devicePoolArn={}", devicePoolArn);
      LOG.debug("testPackage={}", testPackage);
      LOG.debug("appPackage={}", appPackage);
      LOG.debug("platform={}", platform);
      LOG.debug("extraData={}", extraData);
      LOG.debug("deviceLatitude={}", deviceLatitude);
      LOG.debug("deviceLongitude={}", deviceLongitude);
      LOG.debug("executionName={}", executionName);
      LOG.debug("bluetooth={}", bluetooth);
      LOG.debug("gps={}", gps);
      LOG.debug("nfc={}", nfc);
      LOG.debug("wifi={}", wifi);
      LOG.debug("accountsCleanup={}", accountsCleanup);
      LOG.debug("appPackagesCleanup={}", appPackagesCleanup);
      LOG.debug("jobTimeoutMinutes={}", jobTimeoutMinutes);
      LOG.debug("skipAppResign={}", skipAppResign);

      // upload test package
      LOG.info("Uploading test package from {}", testPackage);
      String testPackageArn =
          upload(new File(testPackage), projectArn, UploadType.APPIUM_JAVA_JUNIT_TEST_PACKAGE, true)
              .getArn();

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
      String appArn = upload(new File(appPackage), projectArn, uploadType, true).getArn();

      // upload extra data
      LOG.info("Uploading extra data from {}", extraData);
      String extraDataArn = null;
      if (extraData != null && !extraData.isEmpty()) {
        extraDataArn =
            upload(new File(extraData), projectArn, UploadType.EXTERNAL_DATA, true).getArn();
      }

      ScheduleRunConfiguration configuration = new ScheduleRunConfiguration();
      if (runUnmetered) {
        configuration.setBillingMethod(BillingMethod.UNMETERED);
      } else {
        configuration.setBillingMethod(BillingMethod.METERED);
      }
      configuration.setAuxiliaryApps(new ArrayList<String>());
      configuration.setLocale("en_US");

      Location location = new Location();
      location.setLatitude(deviceLatitude);
      location.setLongitude(deviceLongitude);
      configuration.setLocation(location);

      Radios radios = new Radios();
      radios.setBluetooth(bluetooth);
      radios.setGps(gps);
      radios.setNfc(nfc);
      radios.setWifi(wifi);
      configuration.setRadios(radios);
      configuration.setExtraDataPackageArn(extraDataArn);

      ExecutionConfiguration executionConfiguration = new ExecutionConfiguration();
      executionConfiguration.setAccountsCleanup(accountsCleanup);
      executionConfiguration.setAppPackagesCleanup(appPackagesCleanup);
      executionConfiguration.setJobTimeoutMinutes(jobTimeoutMinutes);
      executionConfiguration.setSkipAppResign(skipAppResign);

      ScheduleRunTest test = new ScheduleRunTest();
      test.setTestPackageArn(testPackageArn);
      test.setType(TestType.APPIUM_JAVA_JUNIT);
      test.addParametersEntry("appium_version", appiumVersion);

      ScheduleRunRequest runRequest = new ScheduleRunRequest();
      runRequest.setAppArn(appArn);
      runRequest.setConfiguration(configuration);
      runRequest.setDevicePoolArn(devicePoolArn);
      runRequest.setExecutionConfiguration(executionConfiguration);
      runRequest.setName(executionName);
      runRequest.setProjectArn(projectArn);
      runRequest.setTest(test);

      ScheduleRunResult runResult = aws.scheduleRun(runRequest);
      String runArn = runResult.getRun().getArn();

      LOG.info("Triggered test with arn {}", runArn);
    } catch (AWSDeviceFarmException | InterruptedException | IOException exception) {
      LOG.error("Error while triggering AWS test", exception);
    }
  }

  private AWSDeviceFarm buildClient() {
    AWSDeviceFarmClientBuilder awsDeviceFarmBuilder = AWSDeviceFarmClient.builder();
    AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    AWSCredentialsProvider awsCredentialsProvider =
        new AWSStaticCredentialsProvider(awsCredentials);
    awsDeviceFarmBuilder.setCredentials(awsCredentialsProvider);
    awsDeviceFarmBuilder.setRegion(awsRegion);
    return awsDeviceFarmBuilder.build();
  }

  /** modified from https://github.com/awslabs/aws-device-farm-jenkins-plugin */
  private Upload upload(File file, String projectArn, UploadType uploadType, Boolean synchronous)
      throws InterruptedException, IOException, AWSDeviceFarmException {
    CreateUploadRequest appUploadRequest =
        new CreateUploadRequest()
            .withName(file.getName())
            .withProjectArn(projectArn)
            .withContentType("application/octet-stream")
            .withType(uploadType.toString());
    Upload upload = aws.createUpload(appUploadRequest).getUpload();

    CloseableHttpClient httpClient = HttpClients.createSystem();
    HttpPut httpPut = new HttpPut(upload.getUrl());
    httpPut.setHeader("Content-Type", upload.getContentType());

    FileEntity entity = new FileEntity(file);
    httpPut.setEntity(entity);

    LOG.debug("S3 upload URL: {}", upload.getUrl());
    HttpResponse response = httpClient.execute(httpPut);
    if (response.getStatusLine().getStatusCode() != 200) {
      throw new AWSDeviceFarmException(
          String.format(
              "Upload returned non-200 responses: %d", response.getStatusLine().getStatusCode()));
    }

    if (synchronous) {
      while (true) {
        GetUploadRequest describeUploadRequest = new GetUploadRequest().withArn(upload.getArn());
        GetUploadResult describeUploadResult = aws.getUpload(describeUploadRequest);
        String status = describeUploadResult.getUpload().getStatus();

        if ("SUCCEEDED".equalsIgnoreCase(status)) {
          LOG.info("Uploading {} succeeded: {}", file.getName(), describeUploadRequest.getArn());
          break;
        } else if ("FAILED".equalsIgnoreCase(status)) {
          LOG.info(
              "Error message from device farm: '{}'",
              describeUploadResult.getUpload().getMetadata());
          throw new AWSDeviceFarmException(String.format("Upload %s failed!", upload.getName()));
        } else {
          try {
            LOG.info(
                "Waiting for upload {} to be ready (current status: {})", file.getName(), status);
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            LOG.info("Thread interrupted while waiting for the upload to complete");
            throw e;
          }
        }
      }
    }
    return upload;
  }
}
