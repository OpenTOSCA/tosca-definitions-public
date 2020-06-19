package org.opentosca.nodetypeimplementations;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Objects;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.eclipse.winery.generators.ia.jaxws.AbstractService;

import de.danielbechler.util.Strings;

@WebService
public class org_opentosca_nodetypes_Ubuntu_VM__org_opentosca_interfaces_test extends AbstractService {

    @WebMethod
    @SOAPBinding
    @Oneway
    public void testPing(@WebParam(name = "VMIP",
                                   targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String VMIP) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        // perform TCP test on given IP and port 22
        if (!(Objects.isNull(VMIP) && Strings.isEmpty(VMIP))) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(VMIP, 22), 1000);
                returnParameters.put("Result", "success");
            }
            catch (final Exception e) {
                System.err.println("Error executing test: " + e.getMessage());
                returnParameters.put("Result", "failure");
            }
        } else {
            System.err.println("VMIP is null or empty. Ping test failed.");
            returnParameters.put("Result", "failure");
        }

        sendResponse(returnParameters);
    }
}
