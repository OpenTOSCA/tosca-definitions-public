package org.opentosca.nodetypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.springframework.http.MediaType;
import org.w3c.dom.Node;

public abstract class AbstractIAService {

	@Resource
	private WebServiceContext context;

	private Client client;

	public AbstractIAService() {
		this.client = ClientBuilder.newClient();
	}

	protected void sendResponse(HashMap<String, String> returnParameters) {

		// Extract message
		WrappedMessageContext wrappedContext = (WrappedMessageContext) context.getMessageContext();
		Message message = wrappedContext.getWrappedMessage();

		// Extract headers from message
		List<Header> headers = CastUtils.cast((List<?>) message.get(Header.HEADER_LIST));

		// Find ReplyTo and MessageID SOAP Header
		String replyTo = null;
		String messageID = null;
		for (Header iter : headers) {

			Object headerObject = iter.getObject();

			// Unmarshall to org.w3c.dom.Node
			if (headerObject instanceof Node) {
				Node node = (Node) headerObject;
				String localPart = iter.getName().getLocalPart();
				String content = node.getTextContent();

				// Extract ReplyTo Header value
				if ("ReplyTo".equals(localPart)) {
					replyTo = content;
				}

				// Extract MessageID Header value
				if ("MessageID".equals(localPart)) {
					messageID = content;
				}
			}
		}

		// Create asynchronous SOAP Response Message
		StringBuilder builder = new StringBuilder();

		builder.append(
				"<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:sch='http://siserver.org/schema'>");
		builder.append("   <soapenv:Header/>");
		builder.append("   <soapenv:Body>");
		builder.append("      <sch:invokeResponse>");
		builder.append("         <sch:MessageID>")
                .append(messageID)
                .append("</sch:MessageID>");

		// Insert return parameters into asynchronous SOAP Response Message
		for (Entry<String, String> paramIter : returnParameters.entrySet()) {

			String key = paramIter.getKey();
			String value = paramIter.getValue();

			builder.append("         <").append(key).append(">").append(value).append("</").append(key).append(">");
		}

		builder.append("      </sch:invokeResponse>");
		builder.append("	</soapenv:Body>");
		builder.append("</soapenv:Envelope>");

		// Send SOAP Response Message back to requester
		if (replyTo == null) {
			System.err.println(
					"No 'ReplyTo' header found!\nTherefore, reply message is printed here:\n" + builder.toString());
		} else {
			this.client.target(replyTo).request().post(Entity.xml(builder.toString()));
		}
	}
}
