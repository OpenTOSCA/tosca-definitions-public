package org.opentosca.nodetypeimplementations;

import java.util.HashMap;
import java.util.Objects;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.eclipse.winery.generators.ia.jaxws.AbstractService;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.AWSElasticBeanstalkException;

@WebService
public class org_opentosca_nodetypes_AWS__org_opentosca_interfaces_test extends AbstractService {

    @WebMethod
    @SOAPBinding
    @Oneway
    public void testCredentials(@WebParam(name = "AWSAccessKey",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSAccessKey,
                                @WebParam(name = "AWSSecretKey",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSSecretKey,
                                @WebParam(name = "AWSRegion",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AWSRegion) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        final Regions region = parseStringToRegion(AWSRegion);

        try {
            final AWSElasticBeanstalk client =
                AWSElasticBeanstalkClientBuilder.standard().withRegion(region)
                                                .withCredentials(new AWSStaticCredentialsProvider(
                                                    new BasicAWSCredentials(AWSAccessKey, AWSSecretKey)))
                                                .build();

            client.describeApplications();
            System.out.println("Credentials are valid!");
            returnParameters.put("Result", "success");
        }
        catch (final AWSElasticBeanstalkException e) {
            System.out.println("Credentials are invalid!");
            returnParameters.put("Result", "failure");
        }

        sendResponse(returnParameters);
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
}
