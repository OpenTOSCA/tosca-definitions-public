package org.opentosca.nodetypeimplementations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.eclipse.winery.generators.ia.jaxws.AbstractService;

@WebService
public class org_opentosca_nodetypes_Beanstalk_App__org_opentosca_interfaces_test extends AbstractService {

    @WebMethod
    @SOAPBinding
    @Oneway
    public void testHttp(@WebParam(name = "AppUrl",
                                   targetNamespace = "http://nodetypeimplementations.opentosca.org/") String AppUrl) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        System.out.println("Http test for URL: " + AppUrl);

        boolean success = false;
        try {
            if (!AppUrl.startsWith("http")) {
                AppUrl = "http://" + AppUrl;
            }

            final URL url = new URL(AppUrl);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            final int code = connection.getResponseCode();
            System.out.println("Http status code: " + code);

            if (code == 200) {
                success = true;
            }
        }
        catch (final IOException e) {
            System.out.println("IOExecption during Http test " + e.getMessage());
        }

        if (success) {
            returnParameters.put("Result", "success");
        } else {
            returnParameters.put("Result", "failure");
        }
        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void testTcpPing(@WebParam(name = "AppUrl",
                                      targetNamespace = "http://nodetypeimplementations.opentosca.org/") final String AppUrl) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        // perform TCP test on given hostname and port 80
        try (Socket socket = new Socket()) {
            final InetAddress address = InetAddress.getByName(AppUrl);
            socket.connect(new InetSocketAddress(address, 80), 1000);
            returnParameters.put("Result", "success");
        }
        catch (final Exception e) {
            System.err.println("Error executing test: " + e.getMessage());
            returnParameters.put("Result", "failure");
        }

        sendResponse(returnParameters);
    }
}
