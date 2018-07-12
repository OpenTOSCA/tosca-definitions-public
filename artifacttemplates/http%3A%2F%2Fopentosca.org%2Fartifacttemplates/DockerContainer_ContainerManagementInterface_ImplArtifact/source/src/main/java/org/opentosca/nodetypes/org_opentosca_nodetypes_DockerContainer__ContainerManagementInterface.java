package org.opentosca.nodetypes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.apache.commons.io.IOUtils;
import org.apache.geronimo.mail.util.StringBufferOutputStream;

import com.jcabi.ssh.SSHByPassword;
import com.jcabi.ssh.Shell;

@WebService
public class org_opentosca_nodetypes_DockerContainer__ContainerManagementInterface extends AbstractIAService {
	
	public static final String MSG_FAILED = "FAILED";
	public static final String TESTMODE = "TESTMODE";
	private String sshUser = "dockeruser";
	private String sshKey = "dockerpw";
	
	@WebMethod
	@SOAPBinding
	@Oneway
	public void runScript(
			@WebParam(name = "ContainerIP", targetNamespace = "http://nodetypes.opentosca.org/") String ContainerIP,
			@WebParam(name = "SSHPort", targetNamespace = "http://nodetypes.opentosca.org/") String SSHPort,
			@WebParam(name = "Script", targetNamespace = "http://nodetypes.opentosca.org/") String Script) {
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		
		waitForAvailability(ContainerIP, SSHPort, sshUser, sshKey);
		
		if (!hasSudo(ContainerIP, SSHPort, sshUser, sshKey)) {
			installSudo(ContainerIP, SSHPort, sshUser, sshKey);
		}
		
		String augmentedScript = replaceHome(ContainerIP, SSHPort, sshUser, sshKey, Script);
		String res = "";
		try {
			res = executeCommand(ContainerIP, SSHPort, sshUser, sshKey, augmentedScript);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		returnParameters.put("ScriptResult", res);
		
		sendResponse(returnParameters);
	}
	
	private void installSudo(String host, String port, String user, String passwd) {
		try {
			executeCommand(host, port, user, passwd, "apt-get update && apt-get -yq install sudo");
			//			this.executeCommand(host, port, user, passwd,
			//					"useradd -m docker && echo \"docker:docker\" | chpasswd && adduser docker sudo");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean hasSudo(String host, String port, String user, String passwd) {
		
		String result = "";
		try {
			System.out.println("Checking if sudo is installed");
			result = executeCommand(host, port, user, passwd, "sudo");
			System.out.println("Sudo check result: " + result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (result.isEmpty() | result.contains("command not found")) {
			return false;
		}
		
		return true;
	}
	
	private void waitForAvailability(String host, String port, String user, String passwd) {
		long startTime = System.currentTimeMillis();
		long endTime = startTime + 25000;
		while (System.currentTimeMillis() < endTime) {
			try {
				executeCommand(host, port, user, passwd, "pwd");
				// if we can execute pwd without issues ssh is up!
				break;
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	private String executeCommand(String host, String port, String user, String passwd, String script)
			throws IOException {
		Shell shell = new SSHByPassword(host, Integer.valueOf(port), user, passwd);
		
		
		System.out.println("Executing shell command: " + script);
		
		String res = new Shell.Plain(shell).exec(script);
		
		InputStream inputRes = new StringBufferInputStream("");
		
		ByteArrayOutputStream outputRes = new ByteArrayOutputStream();
		ByteArrayOutputStream errorRes = new ByteArrayOutputStream();
		int exitCode = new Shell.Verbose(shell).exec(script, inputRes, outputRes, errorRes);
		
		System.out.println("ExitCode of command: " + exitCode);
		
		System.out.println("Result of command: " + outputRes.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
		
		System.out.println("Errors of command: " + errorRes.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
		return res;
		
	}
	
	@WebMethod
	@SOAPBinding
	@Oneway
	public void transferFile(
			@WebParam(name = "ContainerIP", targetNamespace = "http://nodetypes.opentosca.org/") String ContainerIP,
			@WebParam(name = "SSHPort", targetNamespace = "http://nodetypes.opentosca.org/") String SSHPort,
			@WebParam(name = "TargetAbsolutePath", targetNamespace = "http://nodetypes.opentosca.org/") String TargetAbsolutePath,
			@WebParam(name = "SourceURLorLocalPath", targetNamespace = "http://nodetypes.opentosca.org/") String SourceURLorLocalPath) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		
		waitForAvailability(ContainerIP, SSHPort, sshUser, sshKey);
		
		// Transform sourceURLorLocalAbsolutePath to URL
		URL url = null;
		try {
			// Check if the string is a URL right away?
			url = new URL(SourceURLorLocalPath);
		} catch (Exception e) {
			// It's not a URL
			// Check if string is a local path
			File file = new File(SourceURLorLocalPath);
			if (file.exists()) {
				try {
					url = file.toURI().toURL();
					
				} catch (Exception e2) {
					// FAILED: Return async message
					// Also problem processing as file, giving up
					returnParameters.put("TransferResult", "TRANSFER_FAILED: File " + SourceURLorLocalPath
							+ " is no valid URL and does not exist on the local file system. (Exception: " + e + ")");
					sendResponse(returnParameters);
					return;
				}
			} else {
				// FAILED: Return async message
				returnParameters.put("TransferResult", "TRANSFER_FAILED: File " + SourceURLorLocalPath
						+ " is no valid URL and does not exist on the local file system.");
				sendResponse(returnParameters);
				return;
			}
		}
		
		if (TargetAbsolutePath.startsWith("~")) {
			// TODO FIXME our docker containers are provisioned currently with
			// an attach ssh service which doesn't have authing enabled, use
			// fake creds
			String pwd = "";
			try {
				pwd = executeCommand(ContainerIP, SSHPort, sshUser, sshKey, "pwd").trim();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			TargetAbsolutePath = TargetAbsolutePath.replaceFirst("~", pwd);
			System.out.println("Replaced ~ with user home ('" + pwd + "'): '" + TargetAbsolutePath + "'");
		}
		
		// Opens stream and uploads file
		HttpURLConnection httpConnection = null;
		InputStream inputStream = null;
		try {
			// If there is no output stream a HTTP GET is done by default
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setRequestProperty("Accept", "application/octet-stream");
			inputStream = httpConnection.getInputStream();
			
			String encoding = httpConnection.getContentEncoding();
			
			String fileContent = IOUtils.toString(inputStream, encoding);
			
			uploadFile(ContainerIP, SSHPort, sshUser, sshKey, fileContent, TargetAbsolutePath);
			
			returnParameters.put("TransferResult", "sucessfull");
			
		} catch (Exception e) {
			e.printStackTrace();
			returnParameters.put("TransferResult", "TRANSFER_FAILED: " + e);
		} finally {
			try {
				inputStream.close();
				httpConnection.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Returning a parameter is required so that we can wait asynchronously
		// in the process.
		
		sendResponse(returnParameters);
		
	}
	
	private String replaceHome(String host, String port, String user, String pw, String commandString) {
		if (commandString.contains("~")) {
			// TODO FIXME our docker containers are provisioned currently with
			// an attach ssh service which doesn't have authing enabled, use
			// fake creds
			String pwd = "";
			try {
				pwd = executeCommand(host, port, user, pw, "pwd").trim();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Replaced ~ with user home ('" + pwd + "'): '" + commandString + "'");
			return commandString.replaceAll("~", pwd);
			
		}
		
		return commandString;
	}
	
	private boolean uploadFile(String host, String port, String user, String passwd, String fileContent, String target)
			throws Exception {
		
		// encode the file contents as base64
		String fileContentBase64 = new String(Base64.getEncoder().encode(fileContent.getBytes()));
		
		String tempTarget = target + System.currentTimeMillis();
		
		// echo the base64 content into a file
		executeCommand(host, port, user, passwd, "echo '" + fileContentBase64 + "' > " + tempTarget);
		
		executeCommand(host, port, user, passwd, "cat " + tempTarget + " | base64 --decode > " + target);
		
		executeCommand(host, port, user, passwd, "rm " + tempTarget);
		
		return true;
	}
	
}
