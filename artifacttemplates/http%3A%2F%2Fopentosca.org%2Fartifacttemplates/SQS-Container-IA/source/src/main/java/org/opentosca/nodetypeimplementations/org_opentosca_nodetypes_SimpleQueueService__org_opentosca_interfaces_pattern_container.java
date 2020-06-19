package org.opentosca.nodetypeimplementations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.winery.generators.ia.jaxws.AbstractService;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

@WebService
public class org_opentosca_nodetypes_SimpleQueueService__org_opentosca_interfaces_pattern_container extends
                                                                                                    AbstractService {

    // header field where the URLs to DAs are passed
    private static final String DA_HEADER = "DEPLOYMENT_ARTIFACTS_STRING";

    @Resource
    private WebServiceContext context;

    @WebMethod
    @SOAPBinding
    @Oneway
    public void create(@WebParam(name = "AWSAccessKey",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                       @WebParam(name = "AWSSecretKey",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                       @WebParam(name = "AWSRegion",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                       @WebParam(name = "FifoQueue",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String FifoQueue) {
        // This HashMap holds the return parameters of this operation
        final HashMap<String, String> returnParameters = new HashMap<>();

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon SQS client with given credentials
        final AmazonSQS sqs =
            AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWSAccessKey, AWSSecretKey))).build();

        String queueName = generateUniqueQueueName(sqs);

        // specify FIFO attribute if requested
        final Map<String, String> attributes = new HashMap<>();
        if (Boolean.parseBoolean(FifoQueue)) {
            queueName += ".fifo";
            attributes.put("FifoQueue", "true");
            attributes.put("ContentBasedDeduplication", "true");
        }
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes);

        // create the queue and retrieve the URL
        final String queueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();

        // wait until the queue is accessible
        while (!checkURL(queueUrl + "?wsdl")) {
            try {
                Thread.sleep(5 * 1000);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        returnParameters.put("QueueName", queueName);
        returnParameters.put("QueueUrl", queueUrl);
        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void terminate(@WebParam(name = "AWSAccessKey",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                          @WebParam(name = "AWSSecretKey",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                          @WebParam(name = "AWSRegion",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                          @WebParam(name = "QueueUrl",
                                    targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String QueueUrl) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon SQS client with given credentials
        final AmazonSQS sqs =
            AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWSAccessKey, AWSSecretKey))).build();

        // delete queue and forward result
        final DeleteQueueRequest deleteQueueRequest = new DeleteQueueRequest(QueueUrl);
        try {
            sqs.deleteQueue(deleteQueueRequest);
            returnParameters.put("Result", "true");
        }
        catch (final QueueDoesNotExistException e) {
            returnParameters.put("Result", "false");
        }

        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void freeze(@WebParam(name = "AWSAccessKey",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                       @WebParam(name = "AWSSecretKey",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                       @WebParam(name = "AWSRegion",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                       @WebParam(name = "QueueUrl",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String QueueUrl,
                       @WebParam(name = "StoreStateServiceEndpoint",
                                 targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String StoreStateServiceEndpoint) {

        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon SQS client with given credentials
        final AmazonSQS sqs =
            AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWSAccessKey, AWSSecretKey))).build();

        // list to store all received messages during multiple polling rounds
        final List<Message> receivedMessages = new ArrayList<>();

        // poll until no more messages are available
        boolean messagesAvailable = true;
        while (messagesAvailable) {

            System.out.println("Polling for messages...");

            // receive up to 10 currently available messages
            final ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest(QueueUrl).withMessageAttributeNames(".*").withWaitTimeSeconds(10)
                                                   .withMaxNumberOfMessages(10);
            final List<Message> currentMessages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            receivedMessages.addAll(currentMessages);

            System.out.println("Found " + currentMessages.size() + " messages with last poll.");

            // delete the messages during backup to prevent receiving them multiple times
            for (final Message message : currentMessages) {
                sqs.deleteMessage(new DeleteMessageRequest(QueueUrl, message.getReceiptHandle()));
            }

            // terminate if no further message is received
            messagesAvailable = !currentMessages.isEmpty();
        }

        System.out.println("Number of messages retrieved: " + receivedMessages.size());

        // extract the messages into a file and upload it to Winery
        try {
            final File tempFile = File.createTempFile("sqs-backup-", ".state");
            tempFile.deleteOnExit();

            // parse sqs messages to textual representation
            final PrintWriter writer = new PrintWriter(tempFile, "UTF-8");
            for (final Message message : receivedMessages) {
                String attributes = "";
                for (final Entry<String, MessageAttributeValue> attributeEntry : message.getMessageAttributes()
                                                                                        .entrySet()) {
                    final MessageAttributeValue attribute = attributeEntry.getValue();
                    attributes += attributeEntry.getKey() + "," + attribute.getDataType() + ",";
                    switch (attribute.getDataType()) {
                        case "String":
                        case "Number":
                            attributes += attribute.getStringValue();
                            break;
                        case "Binary":
                            attributes +=
                                StandardCharsets.UTF_8.decode(attribute.getBinaryValue().asReadOnlyBuffer()).toString();
                            break;
                        default:
                            System.err.println("Unsported data type: " + attribute.getDataType());
                            break;
                    }
                    attributes += ";";
                }

                writer.println(message.getMessageId() + ";" + message.getBody() + ";" + attributes);
            }
            writer.close();

            System.out.println("Uploading state artifact to Winery...");

            // perform post with file to given endpoint
            final HttpClient httpClient = new DefaultHttpClient();
            final MultipartEntity entity = new MultipartEntity();
            entity.addPart("file", new FileBody(tempFile));
            final HttpPost httpPost = new HttpPost(StoreStateServiceEndpoint);
            httpPost.setEntity(entity);
            final HttpResponse response = httpClient.execute(httpPost);
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
            returnParameters.put("Result", "true");
        }
        catch (final IOException e) {
            returnParameters.put("Result", "false");
            System.err.print("Unable to create state artifact locally: " + e.getMessage());
        }
        catch (final IllegalArgumentException e) {
            returnParameters.put("Result", "false");
            System.err.print("Unable to access StoreStateServiceEndpoint: " + e.getMessage());
        }

        System.out.println("Restoring deleted messages to queue...");

        // add all deleted messages to the queue again to bring it into state before backup
        for (final Message message : receivedMessages) {
            sqs.sendMessage(new SendMessageRequest().withQueueUrl(QueueUrl)
                                                    .withMessageAttributes(message.getMessageAttributes())
                                                    .withMessageBody(message.getBody()))
               .withMD5OfMessageAttributes(message.getMD5OfMessageAttributes())
               .withMD5OfMessageBody(message.getMD5OfBody()).withMessageId(message.getMessageId());
        }
        // FIXME: The message ID changes when deleting and adding the message again and this should not be
        // the case when performing a backup. But as the API only does currently not allow to get all
        // messages without deleting them this is the only possibility. Unfortunately, sending
        // 'withMessageId' does not work properly.

        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void defrost(@WebParam(name = "AWSAccessKey",
                                  targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                        @WebParam(name = "AWSSecretKey",
                                  targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                        @WebParam(name = "AWSRegion",
                                  targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                        @WebParam(name = "FifoQueue",
                                  targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String FifoQueue) {

        // This HashMap holds the return parameters of this operation
        final HashMap<String, String> returnParameters = new HashMap<>();

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon SQS client with given credentials
        final AmazonSQS sqs =
            AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWSAccessKey, AWSSecretKey))).build();

        String queueName = generateUniqueQueueName(sqs);

        // specify FIFO attribute if requested
        final Map<String, String> attributes = new HashMap<>();
        if (Boolean.parseBoolean(FifoQueue)) {
            queueName += ".fifo";
            attributes.put("FifoQueue", "true");
            attributes.put("ContentBasedDeduplication", "true");
        }
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes);

        // create the queue and retrieve the URL
        final String queueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();

        // wait until the queue is accessible
        while (!checkURL(queueUrl + "?wsdl")) {
            try {
                Thread.sleep(5 * 1000);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        // retrieve all state DAs and add their input to the created queue
        final List<String> stateDAs = getStateDaUrls();
        for (final String stateDA : stateDAs) {

            try {
                // download state artifact
                final URL url = new URL(stateDA);
                final File tempFile = File.createTempFile("sqs-state", ".state");
                tempFile.deleteOnExit();
                FileUtils.copyURLToFile(url, tempFile);

                // read messages from file and add them to the queue
                final BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                String nextLine = reader.readLine();
                while (Objects.nonNull(nextLine)) {
                    addStoredMessageToQueue(sqs, queueUrl, nextLine);
                    nextLine = reader.readLine();
                }
                reader.close();
            }
            catch (final IOException e) {
                System.out.println("IO exception while retrieving state artifact with URL " + stateDA);
            }
        }

        returnParameters.put("QueueName", queueName);
        returnParameters.put("QueueUrl", queueUrl);
        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void testProperties(@WebParam(name = "AWSAccessKey",
                                         targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                               @WebParam(name = "AWSSecretKey",
                                         targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                               @WebParam(name = "AWSRegion",
                                         targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion,
                               @WebParam(name = "FifoQueue",
                                         targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String FifoQueue,
                               @WebParam(name = "QueueUrl",
                                         targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String QueueUrl,
                               @WebParam(name = "QueueName",
                                         targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String QueueName) {

        // This HashMap holds the return parameters of this operation
        final HashMap<String, String> returnParameters = new HashMap<>();

        boolean propertiesValid = true;

        // get the given region as enum or the default region
        final Regions region = parseStringToRegion(AWSRegion);

        // create Amazon SQS client with given credentials
        final AmazonSQS sqs =
            AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWSAccessKey, AWSSecretKey))).build();

        // the queue must exist and the name needs to end with .fifo if FifoQueue is true
        final ListQueuesResult result = sqs.listQueues(new ListQueuesRequest());
        propertiesValid &= result.getQueueUrls().contains(QueueUrl);
        System.out.println("Queue exists: " + propertiesValid);
        if (Boolean.parseBoolean(FifoQueue)) {
            propertiesValid &= QueueName.endsWith(".fifo");
        } else {
            propertiesValid &= !QueueName.endsWith(".fifo");
        }

        if (propertiesValid) {
            returnParameters.put("Result", "success");
        } else {
            returnParameters.put("Result", "failure");
        }
        sendResponse(returnParameters);
    }

    /**
     * Parse the stored message format and add the corresponding message to the queue.
     *
     * @param sqs the client to access the queue
     * @param QueueUrl the name of the queue to add the message
     * @param message the message in the state artifact format
     */
    private void addStoredMessageToQueue(final AmazonSQS sqs, final String QueueUrl, final String message) {

        System.out.println("Trying to add message with content " + message);

        try {
            final String[] messageParts = message.split(";");

            // messageID and body are required to add a message
            if (messageParts.length < 2) {
                System.out.println("Message does not contain all required elements.");
                return;
            }

            // handle attributes if specified
            final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            if (messageParts.length >= 3) {
                System.out.println("Message contains attributes.");

                for (int i = 2; i < messageParts.length; i++) {

                    System.out.println("Attribute: " + messageParts[i]);
                    final String[] attributeParts = messageParts[i].split(",");
                    if (attributeParts.length == 3) {
                        final MessageAttributeValue messageAttribute = new MessageAttributeValue();
                        messageAttribute.withDataType(attributeParts[1]);
                        messageAttribute.withStringValue(attributeParts[2]);
                        messageAttributes.put(attributeParts[0], messageAttribute);
                    } else {
                        System.out.println("Attribute is no triple as required.");
                    }
                }
            }

            // send the message to the SQS queue
            sqs.sendMessage(new SendMessageRequest().withQueueUrl(QueueUrl).withMessageAttributes(messageAttributes)
                                                    .withMessageBody(messageParts[1]))
               .withMessageId(messageParts[0]);
        }
        catch (final Exception e) {
            System.err.println("Unable to add message to the queue " + e.getMessage());
        }
    }

    private String generateUniqueQueueName(final AmazonSQS sqs) {

        // create random name for the queue and check if it already exists
        String queueName = "";
        boolean exists = true;
        while (exists) {
            final String newName = getAlphaNumericString(20);
            exists = sqs.listQueues().getQueueUrls().stream().map(url -> url.substring(url.lastIndexOf("/")))
                        .filter(name -> name.equals(newName)).findFirst().isPresent();
            queueName = newName;
        }
        return queueName;
    }

    /**
     * Extracts the URLs of all state DAs specified in the message headers.
     *
     * @return a List with URLs if state DAs are found, <code>null</code> otherwise
     */
    private List<String> getStateDaUrls() {

        final List<String> urls = new ArrayList<>();
        System.out.println("Searching for state artifacts in DA headers...");

        // get DA header from incoming message as json
        final org.apache.cxf.message.Message incomingMessage =
            ((WrappedMessageContext) this.context.getMessageContext()).getWrappedMessage();
        final String headerDA = getHeaderValueFromMessage(incomingMessage, DA_HEADER);

        try {
            // iterate through DAs and add all state artifacts to the list
            final JSONObject json = new JSONObject(headerDA);

            for (final String type : json.keySet()) {
                System.out.println("DA has type: " + type);

                if (type.equals("{http://opentosca.org/artifacttypes}State")) {
                    final JSONObject daJson = json.getJSONObject(type);
                    final String name = daJson.keys().next();
                    System.out.println("State artifact has name " + name + " and URL " + daJson.getString(name));
                    urls.add(daJson.getString(name));
                }
            }
        }
        catch (final JSONException e) {
            System.out.println("JSON exception while extracting DA URL: " + e.getMessage());
        }

        return urls;
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
     * Create an alphanumeric String with the given length.
     *
     * @param length the length of the String
     * @return the generated String
     */
    private String getAlphaNumericString(final int length) {

        final String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";

        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(AlphaNumericString.charAt((int) (AlphaNumericString.length() * Math.random())));
        }

        return sb.toString();
    }

    /**
     * Checks if a HTTP request to an URL returns a HTTP OK status code.
     *
     * @param queueURL the URL for the availability check
     * @return <code>true</code> if request returns ok, <code>false</code> otherwise
     */
    private boolean checkURL(final String queueURL) {
        try {
            final URL url = new URL(queueURL);
            final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.connect();
            return urlConn.getResponseCode() == HttpURLConnection.HTTP_OK;
        }
        catch (final IOException e) {
            System.err.println("Error creating HTTP connection");
        }

        return false;
    }

    /**
     * Read a header value from the incoming message
     *
     * @param incomingMessage The message containing the header
     * @param headerName The name of the header field
     * @return The value of the header field or <code>null</code> if it does not exist
     */
    private String getHeaderValueFromMessage(final org.apache.cxf.message.Message incomingMessage,
                                             final String headerName) {
        final List<Header> headers = CastUtils.cast((List<?>) incomingMessage.get(Header.HEADER_LIST));

        return headers.stream()
                      .filter(header -> header.getName().getLocalPart().equals(headerName)
                          && header.getObject() instanceof Node)
                      .map(header -> (Node) header.getObject()).findFirst().map(node -> node.getTextContent())
                      .orElse(null);
    }
}
