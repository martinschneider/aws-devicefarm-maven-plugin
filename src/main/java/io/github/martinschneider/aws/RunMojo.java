package io.github.martinschneider.aws;

import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.model.BillingMethod;
import com.amazonaws.services.devicefarm.model.ExecutionConfiguration;
import com.amazonaws.services.devicefarm.model.Location;
import com.amazonaws.services.devicefarm.model.Radios;
import com.amazonaws.services.devicefarm.model.ScheduleRunConfiguration;
import com.amazonaws.services.devicefarm.model.ScheduleRunRequest;
import com.amazonaws.services.devicefarm.model.ScheduleRunResult;
import com.amazonaws.services.devicefarm.model.ScheduleRunTest;
import com.amazonaws.services.devicefarm.model.TestType;
import java.util.ArrayList;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mojo for triggering a test execution on AWS Device Farm.
 */
@Mojo(name = "run")
public class RunMojo extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(RunMojo.class);

  @Parameter(required = true)
  private String projectArn;

  @Parameter(required = true)
  private String devicePoolArn;

  @Parameter(required = true)
  private String testArn;

  @Parameter(required = true)
  private String appArn;

  @Parameter(required = true)
  private String extraDataArn;
  
  @Parameter(defaultValue = "APPIUM_JAVA_JUNIT")
  private String testType;
  
  @Parameter(required = false)
  private String testSpecArn;

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

  @Parameter(required = true)
  private String awsAccessKey;

  @Parameter(required = true)
  private String awsSecretKey;

  @Parameter(defaultValue = "us-east-1")
  private String awsRegion;
  
  @Parameter
  private Map<String, String> additionalRunParameters;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    AWSDeviceFarm aws = new AWSService(awsAccessKey, awsSecretKey, awsRegion).getAws();
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
    test.setTestPackageArn(testArn);
    test.setType(TestType.fromValue(testType));
    
    if (testSpecArn != null && !testSpecArn.isEmpty())
    {
      test.setTestSpecArn(testSpecArn);
    }

    // attach additional parameters (e.g. appium_version) to test run
    for (Map.Entry<String, String> entry : additionalRunParameters.entrySet())
    {
      test.addParametersEntry(entry.getKey(), entry.getValue());
    }
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
  }
}
