[<img src="https://travis-ci.com/martinschneider/aws-devicefarm-maven-plugin.svg?branch=master" height="41" alt="Build status"/>](https://travis-ci.com/martinschneider/aws-devicefarm-maven-plugin)
[<img src="https://www.buymeacoffee.com/assets/img/guidelines/download-assets-sm-1.svg" height="41" alt="Buy me a coffee"/>](https://www.buymeacoffee.com/mschneider)

# aws-devicefarm-maven-plugin

## Summary
The purpose of this Maven plugin is to prepare and trigger test runs on AWS Device Farm.

## Maven goals
The following goals are available:

### uploadAppPackage
This goal will upload the app package (apk or ipa) to AWS S3 for further use within Device Farm. The arn of the uploaded file will be exposed as a Maven property `appArn` for use in further build steps.

### uploadTestPackage
This goal will upload the test package (zip) to AWS S3 for further use within Device Farm. Its arn will be exposed as a Maven property `testArn` for use in further build steps. Refer to [the AWS documentation](https://docs.aws.amazon.com/devicefarm/latest/developerguide/test-types-android-appium-java-junit.html#test-types-android-appium-java-junit-prepare) for details on how to package this file using Maven.

### uploadExtraData
This goal will upload extra data (zip) to AWS S3 for further use within Device Farm. Its arn will be exposed as a Maven property `extraDataArn` for use in further build steps. AWS Device Farm will extract the contents of the extra data zip file to external data for Android or the app's sandbox for iOS. 

## Sample configuration

You can find a sample configuration below. In real-life applications you might want to use properties instead of hard-coding the values into the `pom.xml`. 

```
<properties>
  <aws.devicefarm.maven.plugin.version>1.2</aws.devicefarm.maven.plugin.version>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>io.github.martinschneider</groupId>
      <artifactId>aws-devicefarm-maven-plugin</artifactId>
      <version>${aws.devicefarm.maven.plugin.version}</version>
      <configuration>
        <projectArn>YOUR_AWS_PROJECT_ARN</projectArn>
        <awsAccessKey>YOUR_AWS_ACCESS_KEY</awsAccessKey>
        <awsSecretKey>YOUR_AWS_SECRET_KEY</awsSecretKey>
        <awsRegion>YOUR_AWS_REGION</awsRegion>
      </configuration>
      <executions>
        <execution>
          <id>uploadAppPackage</id>
          <phase>verify</phase>
          <goals>
            <goal>uploadAppPackage</goal>
          </goals>
          <configuration>
            <appPackage>PATH_TO_YOUR_APP_PACKAGE</appPackage>
            <platform>android</platform>
          </configuration>
        </execution>
        <execution>
          <id>uploadTestPackage</id>
          <phase>verify</phase>
          <goals>
            <goal>uploadTestPackage</goal>
          </goals>
          <configuration>
            <testPackage>PATH_TO_YOUR_TEST_PACKAGE</testPackage>
          </configuration>
        </execution>
        <execution>
          <id>uploadExtraData</id>
          <phase>verify</phase>
          <goals>
            <goal>uploadExtraData</goal>
          </goals>
          <configuration>
            <extraData>PATH_TO_YOUR_EXTRA_DATA</extraData>
          </configuration>
        </execution>
        <execution>
          <id>scheduleRun</id>
          <phase>verify</phase>
          <goals>
            <goal>run</goal>
          </goals>
          <configuration>
            <devicePoolArn>YOUR_DEVICE_POOL_ARN</devicePoolArn>
            <executionName>demo</executionName>
            <accountsCleanup>true</accountsCleanup>
            <appPackagesCleanup>true</appPackagesCleanup>
            <additionalRunParameters>
              <appium_version>1.7.2</appium_version>
            </additionalRunParameters>
            <!-- these will be set by the previous goals -->
            <appArn>${appArn}</appArn>
            <testArn>${testArn}</testArn>
            <extraDataArn>${extraDataArn}</extraDataArn>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Execute with `mvn verify`

## Contact
Martin Schneider, mart.schneider@gmail.com

[![Buy me a coffee](https://www.buymeacoffee.com/assets/img/guidelines/download-assets-1.svg)](https://www.buymeacoffee.com/mschneider)