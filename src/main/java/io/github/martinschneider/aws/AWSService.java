package io.github.martinschneider.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.AWSDeviceFarmClient;
import com.amazonaws.services.devicefarm.AWSDeviceFarmClientBuilder;
import com.amazonaws.services.devicefarm.model.AWSDeviceFarmException;
import com.amazonaws.services.devicefarm.model.CreateUploadRequest;
import com.amazonaws.services.devicefarm.model.GetUploadRequest;
import com.amazonaws.services.devicefarm.model.GetUploadResult;
import com.amazonaws.services.devicefarm.model.Upload;
import com.amazonaws.services.devicefarm.model.UploadType;
import java.io.File;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service class to interact with AWS, basically a wrapper around {@link AWSDeviceFarm}. */
public class AWSService {

  private static final Logger LOG = LoggerFactory.getLogger(UploadAppPackage.class);

  private AWSDeviceFarm aws;

  /**
   * Constructor
   * 
   * @param awsAccessKey the AWS access key
   * @param awsSecretKey the AWS secret key
   * @param awsRegion the AWS region
   */
  public AWSService(String awsAccessKey, String awsSecretKey, String awsRegion) {
    aws = buildClient(awsAccessKey, awsSecretKey, awsRegion);
  }

  /**
   * @return {@link AWSDeviceFarm}
   */
  public AWSDeviceFarm getAws() {
    return aws;
  }

  private AWSDeviceFarm buildClient(String awsAccessKey, String awsSecretKey, String awsRegion) {
    LOG.debug("Building AWS Device Farm client");
    LOG.debug("awsAccessKey={}", awsAccessKey);
    LOG.debug("awsSecretKey={}", awsSecretKey);
    LOG.debug("awsRegion={}", awsRegion);
    AWSDeviceFarmClientBuilder awsDeviceFarmBuilder = AWSDeviceFarmClient.builder();
    AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    AWSCredentialsProvider awsCredentialsProvider =
        new AWSStaticCredentialsProvider(awsCredentials);
    awsDeviceFarmBuilder.setCredentials(awsCredentialsProvider);
    awsDeviceFarmBuilder.setRegion(awsRegion);
    return awsDeviceFarmBuilder.build();
  }

  /** 
   * Upload a file to AWS Device Farm (modified from https://github.com/awslabs/aws-device-farm-jenkins-plugin)
   * 
   * @param file the file to upload
   * @param projectArn the ARN of the Device Farm project
   * @param uploadType the {@link UploadType}
   * @param synchronous true, if the execution should wait for the download to succeed
   * @return {@link Upload}
   * @throws InterruptedException {@link InterruptedException}
   * @throws IOException {@link IOException}
   * @throws AWSDeviceFarmException {@link AWSDeviceFarmException}
   */
  public Upload upload(File file, String projectArn, UploadType uploadType, Boolean synchronous)
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
