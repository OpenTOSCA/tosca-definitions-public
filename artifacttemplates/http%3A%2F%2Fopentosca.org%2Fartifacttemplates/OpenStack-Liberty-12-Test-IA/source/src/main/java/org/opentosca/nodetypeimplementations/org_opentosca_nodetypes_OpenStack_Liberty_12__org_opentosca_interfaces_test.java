package org.opentosca.nodetypeimplementations;

import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.eclipse.winery.generators.ia.jaxws.AbstractService;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder.V2;
import org.openstack4j.api.client.IOSClientBuilder.V3;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

@WebService
public class org_opentosca_nodetypes_OpenStack_Liberty_12__org_opentosca_interfaces_test extends AbstractService {

    @WebMethod
    @SOAPBinding
    @Oneway
    public void testCredentials(@WebParam(name = "HypervisorEndpoint",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String HypervisorEndpoint,
                                @WebParam(name = "HypervisorTenantID",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String HypervisorTenantID,
                                @WebParam(name = "HypervisorUserName",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String HypervisorUserName,
                                @WebParam(name = "HypervisorUserPassword",
                                          targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String HypervisorUserPassword) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        // try to create a client using V2
        final V2 credentials = OSFactory.builderV2().endpoint("http://" + HypervisorEndpoint + ":5000/v2.0")
                                        .credentials(HypervisorUserName, HypervisorUserPassword);

        OSClient osClient = null;
        if (!HypervisorTenantID.isEmpty()) {
            credentials.tenantName(HypervisorTenantID);
        }

        try {
            osClient = credentials.authenticate();
            returnParameters.put("Result", "success");
        }
        catch (final Exception e) {
            System.err.println("Couldn't connect with v2 API, trying v3");
        }

        // try to create a client using V3 if V2 failed
        if (osClient == null) {
            final V3 creds =
                OSFactory.builderV3().useNonStrictSSLClient(true).perspective(Facing.PUBLIC)
                         .endpoint("https://" + HypervisorEndpoint + ":5000/v3")
                         .credentials(HypervisorUserName, HypervisorUserPassword, Identifier.byName("Default"));
            try {
                osClient = creds.authenticate();
                returnParameters.put("Result", "success");
            }
            catch (final Exception e) {
                System.err.println("Couldn't connect with v3 API, test failed");
                returnParameters.put("Result", "failure");
            }
        }

        sendResponse(returnParameters);
    }
}
