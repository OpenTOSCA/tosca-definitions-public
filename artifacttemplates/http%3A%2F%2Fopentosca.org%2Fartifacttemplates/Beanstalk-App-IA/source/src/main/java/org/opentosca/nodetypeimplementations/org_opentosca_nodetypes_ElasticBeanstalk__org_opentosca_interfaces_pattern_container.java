package org.opentosca.nodetypeimplementations;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Resource;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.WebServiceContext;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.eclipse.winery.generators.ia.jaxws.AbstractService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.AWSElasticBeanstalkException;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksRequest;
import com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksResult;
import com.amazonaws.services.elasticbeanstalk.model.OperationInProgressException;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@WebService
public class org_opentosca_nodetypes_ElasticBeanstalk__org_opentosca_interfaces_pattern_container extends
                                                                                                  AbstractService {

    private static final Logger logger =
        LoggerFactory.getLogger(org_opentosca_nodetypes_ElasticBeanstalk__org_opentosca_interfaces_pattern_container.class);

    // header field where the URLs to DAs are passed
    private static final String DA_HEADER = "DEPLOYMENT_ARTIFACTS_STRING";

    // beanstalk state when the environment can be used completely
    private static final String STATE_READY = "Ready";

    @Resource
    private WebServiceContext context;

    @WebMethod
    @SOAPBinding
    @Oneway
    public void start(@WebParam(name = "AWSAccessKey",
                                targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                      @WebParam(name = "AWSSecretKey",
                                targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                      @WebParam(name = "AWSRegion",
                                targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                      @WebParam(name = "Environment",
                                targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String Environment) {

        // this HashMap holds the return parameters of this operation
        final HashMap<String, String> returnParameters = new HashMap<>();

        // determine URL of DA with application files
        final String daUrl = getDaUrl();

        if (Objects.isNull(daUrl) || daUrl.isEmpty()) {
            logger.error("Unable to deploy application with DA header empty or equal to null!");
            sendResponse(returnParameters);
            return;
        }

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon Beanstalk/S3 client with given credentials
        final AWSStaticCredentialsProvider credentials =
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWSAccessKey, AWSSecretKey));
        final AWSElasticBeanstalk beanstalk =
            AWSElasticBeanstalkClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentials).build();

        // check if given environment is valid solution stack and abort otherwise
        boolean environmentValid = false;
        final ListAvailableSolutionStacksResult solutionStacks =
            beanstalk.listAvailableSolutionStacks(new ListAvailableSolutionStacksRequest());
        for (final String solution : solutionStacks.getSolutionStacks()) {
            if (solution.equals(Environment)) {
                environmentValid = true;
                break;
            }
        }

        if (environmentValid) {
            try {
                // create unused name for the application and s3 bucket
                // Caution: We currently use the same name for the application and s3 to ease later deletion.
                String appName = getAlphaNumericString(20);
                while (s3.doesBucketExistV2(appName) || existsAppWithName(beanstalk, appName)) {
                    appName = getAlphaNumericString(20);
                }

                // create application
                final CreateApplicationRequest request = new CreateApplicationRequest(appName);
                beanstalk.createApplication(request);

                // download application file
                final URL url = new URL(daUrl);
                final String fileName = daUrl.substring(daUrl.lastIndexOf('/') + 1);
                final File tempFile = File.createTempFile("s3file", fileName);
                tempFile.deleteOnExit();
                FileUtils.copyURLToFile(url, tempFile);

                // upload application code to S3 bucket for later deployment
                s3.createBucket(appName);
                final PutObjectRequest putRequest = new PutObjectRequest(appName, fileName, tempFile);
                s3.putObject(putRequest);

                // create new application version related to the S3 file
                beanstalk.createApplicationVersion(new CreateApplicationVersionRequest().withApplicationName(appName)
                                                                                        .withVersionLabel("v1")
                                                                                        .withSourceBundle(new S3Location(
                                                                                            appName, fileName)));

                // create environment containing the application version
                final CreateEnvironmentRequest envRequest = new CreateEnvironmentRequest(appName, appName);
                envRequest.setSolutionStackName(Environment);
                envRequest.setVersionLabel("v1");
                final CreateEnvironmentResult result = beanstalk.createEnvironment(envRequest);

                // wait for URL assignment for the created environment
                final String envId = result.getEnvironmentId();
                String appUrl = result.getEndpointURL();
                int count = 0;
                while (Objects.isNull(appUrl) && count < 100) {
                    count++;
                    System.out.println("Waiting for environment to come up: " + count + "/100");

                    try {
                        Thread.sleep(5000);
                    }
                    catch (final InterruptedException e) {
                        e.printStackTrace();
                    }

                    appUrl = beanstalk.describeEnvironments().getEnvironments().stream()
                                      .filter(env -> env.getEnvironmentId().equals(envId)).findFirst()
                                      .map(env -> env.getEndpointURL()).orElse(null);
                }

                // wait until the environment is ready
                waitForEnvironment(beanstalk, appName, appName, 50);

                // the name of the created application needed for later deletion
                returnParameters.put("AppName", appName);

                // the URL to access the deployed application
                returnParameters.put("AppUrl", appUrl);
            }
            catch (final AWSElasticBeanstalkException e) {
                logger.error("Unable to create application: " + e.getErrorMessage());
            }
            catch (final MalformedURLException e) {
                logger.error("Unable to parse given URL: " + e.getMessage());
            }
            catch (final IOException e) {
                logger.error("Unable to retrieve application file: " + e.getMessage());
            }
        } else {
            logger.error("No available solution stack available matching the given Environment!");
        }

        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void stop(@WebParam(name = "AWSAccessKey",
                               targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                     @WebParam(name = "AWSSecretKey",
                               targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                     @WebParam(name = "AWSRegion",
                               targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                     @WebParam(name = "AppName",
                               targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AppName) {

        // This HashMap holds the return parameters of this operation
        final HashMap<String, String> returnParameters = new HashMap<>();

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon Beanstalk/S3 client with given credentials
        final AWSStaticCredentialsProvider credentials =
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWSAccessKey, AWSSecretKey));
        final AWSElasticBeanstalk beanstalk =
            AWSElasticBeanstalkClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentials).build();

        try {
            // delete app and environment
            beanstalk.deleteApplication(new DeleteApplicationRequest().withApplicationName(AppName)
                                                                      .withTerminateEnvByForce(true));

            // clean all objects from the s3 bucket and delete the bucket
            final Iterator<S3ObjectSummary> objIter = s3.listObjects(AppName).getObjectSummaries().iterator();
            while (objIter.hasNext()) {
                s3.deleteObject(AppName, objIter.next().getKey());
            }
            s3.deleteBucket(AppName);

            returnParameters.put("Result", "true");
        }
        catch (final OperationInProgressException e) {
            logger.error("Unable to delete application because another operation is currently in progress: "
                + e.getMessage());
            returnParameters.put("Result", "false");
        }

        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void connectTo(@WebParam(name = "AWSAccessKey",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                          @WebParam(name = "AWSSecretKey",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                          @WebParam(name = "AWSRegion",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                          @WebParam(name = "SOURCE_AppName",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AppName,
                          @WebParam(name = "TARGET_QueueName",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String QueueName) {

        // This HashMap holds the return parameters of this operation
        final HashMap<String, String> returnParameters = new HashMap<>();

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon Beanstalk client with given credentials
        final AWSStaticCredentialsProvider credentials =
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWSAccessKey, AWSSecretKey));
        final AWSElasticBeanstalk beanstalk =
            AWSElasticBeanstalkClientBuilder.standard().withRegion(region).withCredentials(credentials).build();

        final ConfigurationOptionSetting envVarOption = getEnvironmentVariablesOption(beanstalk, AppName, AppName);

        if (Objects.nonNull(envVarOption)) {

            // wait until the environment is ready for the update
            System.out.println("Waiting for environment to be ready to update...");
            waitForEnvironment(beanstalk, AppName, AppName, 50);

            // update environment variables
            // TODO: get needed env var names through input parameters to generalize the connectTo
            String newValue = envVarOption.getValue();
            newValue += ",CLOUD_AWS_REGION_STATIC=" + AWSRegion;
            newValue += ",CLOUD_AWS_CREDENTIALS_ACCESSKEY=" + AWSAccessKey;
            newValue += ",CLOUD_AWS_CREDENTIALS_SECRETKEY=" + AWSSecretKey;
            newValue += ",DEMO_QUEUE=" + QueueName;
            envVarOption.setValue(newValue);

            // update the environment with the set variables
            beanstalk.updateEnvironment(new UpdateEnvironmentRequest().withApplicationName(AppName)
                                                                      .withEnvironmentName(AppName)
                                                                      .withOptionSettings(envVarOption));

            // wait until the environment has restarted after the update
            System.out.println("Environment updated!");
            waitForEnvironment(beanstalk, AppName, AppName, 50);
            returnParameters.put("Result", "true");
        } else {
            logger.error("Unable to connect without EnvironmentVariables configuration option!");
            returnParameters.put("Result", "false");
        }

        sendResponse(returnParameters);
    }

    /**
     * Extracts the URL of the first DA specified in the message headers.
     *
     * @return the URL if a DA is found, <code>null</code> otherwise
     */
    private String getDaUrl() {
        // get DA header from incoming message as json
        final Message incomingMessage = ((WrappedMessageContext) this.context.getMessageContext()).getWrappedMessage();
        final String headerDA = getHeaderValueFromMessage(incomingMessage, DA_HEADER);

        try {
            JSONObject json = new JSONObject(headerDA);

            // extract the URL of the first DA
            if (!json.keySet().isEmpty()) {
                final String DAType = json.keys().next();
                logger.debug("Beanstalk DA has type: {}", DAType);

                json = json.getJSONObject(DAType);
                if (!json.keySet().isEmpty()) {
                    final String DAName = json.keys().next();
                    logger.debug("Beanstalk DA has name: {} and URL: {}", DAName, json.getString(DAName));
                    return json.getString(DAName);
                }
            } else {
                logger.error("DA header is empty. Unable to retrieve URL to Beanstalk App!");
            }
        }
        catch (final JSONException e) {
            logger.error("JSOn exception while extracting DA URL: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Retrieve the EnvironmentVariables option for the given environment.
     *
     * @param beanstalk the Beanstalk Client to access the API
     * @param appName the name of the application containing the environment
     * @param envName the name of the environment
     * @return ConfigurationOptionSetting for the env vars, <code>null</code> if it can not be retrieved
     */
    private ConfigurationOptionSetting getEnvironmentVariablesOption(final AWSElasticBeanstalk beanstalk,
                                                                     final String appName, final String envName) {
        final DescribeConfigurationSettingsResult descs =
            beanstalk.describeConfigurationSettings(new DescribeConfigurationSettingsRequest().withApplicationName(appName)
                                                                                              .withEnvironmentName(appName));

        final Optional<ConfigurationOptionSetting> envVarOptional =
            descs.getConfigurationSettings().stream().flatMap(settings -> settings.getOptionSettings().stream())
                 .filter(option -> option.getOptionName().equals("EnvironmentVariables")).findFirst();

        if (envVarOptional.isPresent()) {
            return envVarOptional.get();
        } else {
            logger.error("Unable to retrieve EnvironmentVariables option from config.");
            return null;
        }
    }

    /**
     * Create an alphanumeric String with the given length.
     *
     * @param length the length of the String
     * @return the generated String
     */
    private String getAlphaNumericString(final int length) {
        final String AlphaNumericString = "0123456789" + "abcdefghijklmnopqrstuvxyz";

        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(AlphaNumericString.charAt((int) (AlphaNumericString.length() * Math.random())));
        }

        return sb.toString();
    }

    /**
     * Parse the given AWS region name to the enum used within AWS Clients.
     *
     * @param regionName the name of the region
     * @return the region name in the enum representation if it is valid, the default region EU_WEST_1
     *         otherwise
     */
    private Regions parseStringToRegion(final String regionName) {

        // parse input region to enumeration
        if (Objects.nonNull(regionName) && !regionName.isEmpty()) {
            try {
                return Regions.fromName(regionName);
            }
            catch (final IllegalArgumentException e) {
                System.out.println("Invalid region name: " + e.getMessage());
            }
        }

        // default region if nothing is specified or input is not valid
        return Regions.EU_WEST_1;
    }

    /**
     * Checks whether an application with the given name exists or not.
     *
     * @param beanstalk client to access AWS Elastic Beanstalk
     * @param appName the name to check
     * @return <code>true</code> if application with the given name exists, <code>false</code> otherwise
     */
    private boolean existsAppWithName(final AWSElasticBeanstalk beanstalk, final String appName) {
        return beanstalk.describeApplications().getApplications().stream()
                        .filter(app -> app.getApplicationName().equals(appName)).findFirst().isPresent();
    }

    /**
     * Read a header value from the incoming message
     *
     * @param incomingMessage The message containing the header
     * @param headerName The name of the header field
     * @return The value of the header field or <code>null</code> if it does not exist
     */
    private String getHeaderValueFromMessage(final Message incomingMessage, final String headerName) {
        final List<Header> headers = CastUtils.cast((List<?>) incomingMessage.get(Header.HEADER_LIST));

        return headers.stream()
                      .filter(header -> header.getName().getLocalPart().equals(headerName)
                          && header.getObject() instanceof Node)
                      .map(header -> (Node) header.getObject()).findFirst().map(node -> node.getTextContent())
                      .orElse(null);
    }

    /**
     * Wait until the given environment reaches the state 'Ready' or until a timeout occurs.
     *
     * @param beanstalk the Beanstalk Client to access the API
     * @param appName the name of the application containing the environment
     * @param envName the name of the environment
     * @param timeout how many times to wait 5 seconds
     */
    private static void waitForEnvironment(final AWSElasticBeanstalk beanstalk, final String appName,
                                           final String envName, final int timeout) {
        int count = 0;
        while (!getEnvironmentStatus(beanstalk, appName, envName).equals(STATE_READY)) {
            count++;
            System.out.println("Waiting for environment to be ready: " + count + "/" + timeout);
            try {
                Thread.sleep(5000);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the status of an environment.
     *
     * @param beanstalk the Beanstalk Client to access the API
     * @param appName the name of the application containing the environment
     * @param envName the name of the environment
     * @return the current status of the environment or <code>null</code> if the environment can not be
     *         found
     */
    private static String getEnvironmentStatus(final AWSElasticBeanstalk beanstalk, final String appName,
                                               final String envName) {
        final DescribeEnvironmentsResult envRes =
            beanstalk.describeEnvironments(new DescribeEnvironmentsRequest().withApplicationName(appName)
                                                                            .withEnvironmentNames(envName));

        if (Objects.nonNull(envRes)) {
            return envRes.getEnvironments().stream().filter(env -> env.getEnvironmentName().equals(envName)).findFirst()
                         .map(env -> env.getStatus()).orElse(null);
        }

        return null;
    }
}
