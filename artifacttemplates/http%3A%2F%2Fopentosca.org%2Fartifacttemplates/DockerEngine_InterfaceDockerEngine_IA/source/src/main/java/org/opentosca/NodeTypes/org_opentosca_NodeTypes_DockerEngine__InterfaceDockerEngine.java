
package org.opentosca.NodeTypes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.logging.Log;
import org.vngx.jsch.Session;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.io.Files;
import com.jcabi.ssh.SSHByPassword;
import com.jcabi.ssh.Shell;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import ssh.SSHRemoteFileTransfer;
import ssh.SSHSessionFactory;

@WebService(targetNamespace = "http://NodeTypes.opentosca.org/")
public class org_opentosca_NodeTypes_DockerEngine__InterfaceDockerEngine extends AbstractIAService {

	@WebMethod
	@SOAPBinding
	@Oneway
	public void startContainer(
			@WebParam(name = "DockerEngineURL", targetNamespace = "http://NodeTypes.opentosca.org/") String DockerEngineURL,
			@WebParam(name = "DockerEngineCertificate", targetNamespace = "http://NodeTypes.opentosca.org/") String DockerEngineCertificate,
			@WebParam(name = "ContainerImage", targetNamespace = "http://NodeTypes.opentosca.org/") String ContainerImage,
			@WebParam(name = "ContainerPorts", targetNamespace = "http://NodeTypes.opentosca.org/") String ContainerPorts,
			@WebParam(name = "SSHPort", targetNamespace = "http://NodeTypes.opentosca.org/") String SSHPort,
			@WebParam(name = "ContainerEnv", targetNamespace = "http://NodeTypes.opentosca.org/") String ContainerEnv,
			@WebParam(name = "ImageLocation", targetNamespace = "http://NodeTypes.opentosca.org/") String ImageLocation,
			@WebParam(name = "PrivateKey", targetNamespace = "http://NodeTypes.opentosca.org/") String PrivateKey,
			@WebParam(name = "Links", targetNamespace = "http://NodeTypes.opentosca.org/") String Links,
			@WebParam(name = "Devices", targetNamespace = "http://NodeTypes.opentosca.org/") String Devices,
			@WebParam(name = "RemoteVolumeData", targetNamespace = "http://NodeTypes.opentosca.org/") String RemoteVolumeData,
			@WebParam(name = "HostVolumeData", targetNamespace = "http://NodeTypes.opentosca.org/") String HostVolumeData,
			@WebParam(name = "ContainerMountPath", targetNamespace = "http://NodeTypes.opentosca.org/") String ContainerMountPath,
			@WebParam(name = "VMIP", targetNamespace = "http://NodeTypes.opentosca.org/") String VMIP,
			@WebParam(name = "VMPrivateKey", targetNamespace = "http://NodeTypes.opentosca.org/") String VMPrivateKey) {
		// create connection to the docker engine

		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost(DockerEngineURL).withDockerTlsVerify(false).withApiVersion("1.21").build();

		if (DockerEngineCertificate == null) {
			config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(DockerEngineURL)
					.withDockerTlsVerify(false).withApiVersion("1.21").build();
		} else {
			config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(DockerEngineURL)
					.withDockerCertPath(DockerEngineCertificate).withApiVersion("1.21").build();

		}

		DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

		// cut ip address out of DockerEngineURL
		String ipAddress = DockerEngineURL.split(":")[1].substring(2);

		System.out.println("Try to connect to " + ipAddress);

		// pull needed image if not already done

		System.out.println("Fetching SSH container image");

		boolean pullRes = this.pullImage(dockerClient, "jeroenpeeters/docker-ssh:latest");

		// create image or pull it if a remote image shall be used
		String image = null;
		if (ContainerImage == null) { // either ContainerImage or ImageLocation
			// has to be set
			image = "da/" + System.currentTimeMillis();

			File basePath = new File(ImageLocation);

			try {
				URI dockerImageURI = new URI(ImageLocation);

				String[] pathSplit = dockerImageURI.getRawPath().split("/");
				String fileName = pathSplit[pathSplit.length - 1];

				if (dockerImageURI.isAbsolute() | new File(dockerImageURI.toString()).exists()) {
					File tempDir = Files.createTempDir();
					File tempUnpackDir = Files.createTempDir();
					File tempFile = new File(tempDir, fileName);

					if (dockerImageURI.toString().startsWith("http")) {

						URLConnection connection = dockerImageURI.toURL().openConnection();
						connection.setRequestProperty("Accept", "application/octet-stream");

						InputStream input = connection.getInputStream();
						byte[] buffer = new byte[4096];
						int n = -1;

						OutputStream output = new FileOutputStream(tempFile);
						while ((n = input.read(buffer)) != -1) {
							output.write(buffer, 0, n);
						}
						output.close();
						input.close();
					} else {
						tempFile = basePath;
					}

					if (fileName.endsWith("zip")) {

						ZipFile zipFile = new ZipFile(tempFile);

						zipFile.extractAll(tempUnpackDir.toString());

						basePath = new File(tempUnpackDir, "Dockerfile");
						System.out.println(
								"Unpacked DockerContainer Files, base Dockerfile at " + basePath.getAbsolutePath());

					} else if (fileName.endsWith("tar.gz")) {
						basePath = tempFile;
						// open tarbal and look into repository file for the image tag

						TarArchiveInputStream tarIn = new TarArchiveInputStream(
								new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))));

						TarArchiveEntry entry = null;

						while ((entry = tarIn.getNextTarEntry()) != null) {
							if (entry.getName().trim().equals("repositories")) {
								File entryFile = new File(tempDir, entry.getName());

								OutputStream out = new FileOutputStream(entryFile);

								IOUtils.copy(tarIn, out);
								out.close();

								String repositoryContents = FileUtils.readFileToString(entryFile);

								ObjectMapper objMapper = new ObjectMapper();

								JsonNode rootNode = objMapper.readTree(repositoryContents);

								if (rootNode.size() == 1) {

									Iterator<String> fieldNames = rootNode.fieldNames();
									image = fieldNames.next();
								}

								break;

							}
						}

						tarIn.close();

					}
				}

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ZipException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (basePath.getName().contains("Dockerfile")) {
				BuildImageResultCallback callback = new BuildImageResultCallback() {
					@Override
					public void onNext(BuildResponseItem item) {
						System.out.println("" + item);
						super.onNext(item);
					}
				};

				System.out.println("Starting to build image from zip file at " + basePath);
				dockerClient.buildImageCmd(basePath).withTag(image).exec(callback).awaitImageId();

				try {
					callback.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				// the tar.gz case
				if (!this.isImageAvailable(image, dockerClient)) {
					System.out.println("Image " + image + " not found");
					System.out.println("Starting to load image from tar.gz file at " + basePath);
					FileInputStream fis = null;
					try {
						fis = new FileInputStream(basePath);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					dockerClient.loadImageCmd(fis).exec();
				} else {
					System.out.println("Image " + image + " already available skipping upload");
				}
			}

		} else {
			boolean foundImage = false;
			System.out.println("Searching available Images: ");
			for (Image availImage : dockerClient.listImagesCmd().exec()) {
				for (String tag : availImage.getRepoTags()) {
					if (tag.startsWith(ContainerImage)) {
						image = availImage.getId();
						foundImage = true;
						break;
					}
				}
			}
			if (!foundImage) {
				dockerClient.pullImageCmd(ContainerImage).exec(new PullImageResultCallback()).awaitSuccess();
				image = ContainerImage;
			}
		}

		// expose ports if needed for the container
		List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
		Ports portBindings = new Ports();
		if (ContainerPorts != null) {
			for (String portMapping : Arrays.asList(ContainerPorts.split(";"))) {
				if (portMapping.trim().isEmpty()) {
					continue;
				}
				String[] portMapKV = portMapping.split(",");
				ExposedPort tempPort = ExposedPort.tcp(Integer.parseInt(portMapKV[0]));
				Integer externalPort = Integer.parseInt(portMapKV[1]);
				exposedPorts.add(tempPort);

				System.out.println("Creating PortBinding " + tempPort + ":" + externalPort);
				portBindings.bind(tempPort, Ports.Binding.bindPort(externalPort)); // map
				// to
				// random
				// port
			}
		}

		// parse environment variables
		List<String> environmentVariables = new ArrayList<String>();
		if (ContainerEnv != null) {
			environmentVariables = Arrays.asList(ContainerEnv.split(";"));
		}

		System.out.println("Will start container with following environment variables: ");
		System.out.println(environmentVariables.toString());

		List<Link> links = new ArrayList<Link>();
		if (Links != null) {
			String[] idsToLinkSplit = Links.split(";");
			for (String idToLink : idsToLinkSplit) {
				System.out.println("Will linking container to container with id " + idToLink);
				links.add(new Link(idToLink.trim(), null));
			}
		}

		List<Device> devices = new ArrayList<Device>();
		if (Devices != null) {
			String[] devMappingSplit = Devices.split(";");
			for (String devMapping : devMappingSplit) {
				String[] devMapSplit = devMapping.split("=");
				if (devMapSplit.length == 2) {
					System.out.println("Will add device " + devMapSplit[0] + ":" + devMapSplit[1]);
					devices.add(new Device("mrw", devMapSplit[0], devMapSplit[1]));
				}
			}
		}

		Volume volume = null;
		String hostVolPath = "/volumeFor" + image.replace("/", "_") + System.currentTimeMillis();

		if (ContainerMountPath != null) {

			boolean pullResVol = this.pullImage(dockerClient, "phusion/baseimage:latest");

			// CreateVolumeResponse volResp = dockerClient.createVolumeCmd()
			// .withName().exec();

			// FIXME It is important to notice that here the bindings are reversed! Which
			// atleast I think is a bug in the java sdk!
			// <groupId>com.github.docker-java</groupId>
			// <artifactId>docker-java</artifactId>
			// <version>3.0.10</version>

			volume = new Volume(ContainerMountPath);

			CreateContainerResponse volumeContainer = dockerClient.createContainerCmd("phusion/baseimage:latest")
					.withBinds(new Bind(hostVolPath, volume, AccessMode.rw)).withVolumes(volume).exec();
			System.out.println("Created volume container " + volumeContainer.getId());
			dockerClient.startContainerCmd(volumeContainer.getId()).exec();
			System.out.println("Started volume container " + volumeContainer.getId());
			try {
				ExecCreateCmdResponse execCmdResp = dockerClient.execCreateCmd(volumeContainer.getId())
						.withCmd("mkdir", "-p", ContainerMountPath).exec();
				dockerClient.execStartCmd(execCmdResp.getId()).exec(new ExecStartResultCallback(System.out, System.err))
						.awaitCompletion();

				if(RemoteVolumeData != null) {
					
				// volumeData is a set of http paths pointing to tar files
				String[] dataPaths = RemoteVolumeData.split(";");

				for (String dataPath : dataPaths) {
					File volumeFile = this.downloadFile(dataPath);

					File volumeTarFile = this.createTempTarFromFile(volumeFile);

					dockerClient.copyArchiveToContainerCmd(volumeContainer.getId()).withRemotePath(ContainerMountPath)
							.withTarInputStream(new FileInputStream(volumeTarFile)).exec();
					
					ExecCreateCmdResponse execChmodCmdResp = dockerClient.execCreateCmd(volumeContainer.getId())
							.withCmd("chmod", "600", ContainerMountPath + "/" + volumeFile.getName()).exec();
					dockerClient.execStartCmd(execChmodCmdResp.getId()).exec(new ExecStartResultCallback(System.out, System.err))
							.awaitCompletion();
				}
				}
				
				if(HostVolumeData != null && VMIP != null && VMPrivateKey != null) {
					String[] dataPaths = HostVolumeData.split(";");
					
					for(String dataPath : dataPaths) {
						
						File tempFile = this.downloadFileFromSFTP(dataPath, VMIP, VMPrivateKey);
																		
						dockerClient.copyArchiveToContainerCmd(volumeContainer.getId()).withHostResource(tempFile.getAbsolutePath()).withRemotePath(ContainerMountPath).exec();
						
						ExecCreateCmdResponse execRenameCmdResp = dockerClient.execCreateCmd(volumeContainer.getId())
								.withCmd("mv", ContainerMountPath + "/" + tempFile.getName() ,ContainerMountPath + "/" + new File(dataPath).getName()).exec();
						dockerClient.execStartCmd(execRenameCmdResp.getId()).exec(new ExecStartResultCallback(System.out, System.err))
						.awaitCompletion();
						
						ExecCreateCmdResponse execChmodCmdResp = dockerClient.execCreateCmd(volumeContainer.getId())
								.withCmd("chmod", "600", ContainerMountPath + "/" + new File(dataPath).getName()).exec();
						dockerClient.execStartCmd(execChmodCmdResp.getId()).exec(new ExecStartResultCallback(System.out, System.err))
								.awaitCompletion();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		CreateContainerResponse container = null;
		if (volume != null && ContainerMountPath != null) {				
			container = dockerClient.createContainerCmd(image).withEnv(environmentVariables).withTty(true).withLinks(links).withExposedPorts(exposedPorts)
					.withPortBindings(portBindings)
					.withBinds(new Bind(hostVolPath, volume)).withDevices(devices).withCmd("-v")
					.exec();
		} else {
			// start container
			container = dockerClient.createContainerCmd(image).withExposedPorts(exposedPorts)
					.withPortBindings(portBindings).withEnv(environmentVariables).withTty(true).withLinks(links)
					.withDevices(devices).exec();
		}

		System.out.println("Created container " + container.getId());
		dockerClient.startContainerCmd(container.getId()).exec();
		System.out.println("Started container " + container.getId());

		// get name of the new container
		String containerName = dockerClient.inspectContainerCmd(container.getId()).exec().getName().substring(1);

		// start ssh container for the created container
		portBindings = new Ports();
		ExposedPort exposedPort = ExposedPort.tcp(22);

		if (SSHPort != null && !SSHPort.isEmpty()) {
			Integer sshPort = Integer.parseInt(SSHPort);
			portBindings.bind(exposedPort, Ports.Binding.bindPort(sshPort));
		} else {
			portBindings.bind(exposedPort, Ports.Binding.empty());
		}

		CreateContainerResponse sshContainer = dockerClient.createContainerCmd("jeroenpeeters/docker-ssh:latest")
				.withExposedPorts(exposedPort).withPortBindings(portBindings)
				.withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
				.withEnv("CONTAINER=" + containerName, "AUTH_MECHANISM=noAuth").exec();
		System.out.println("Created ssh container " + sshContainer.getId());
		dockerClient.startContainerCmd(sshContainer.getId()).exec();
		System.out.println("Started ssh container " + sshContainer.getId());
		// return outer ports for the requested inner ports
		String portMapping = "";
		boolean first = true;
		for (ExposedPort port : exposedPorts) {
			if (!first) {
				portMapping += ",";
			}
			portMapping += port.getPort() + "-->" + findPort(dockerClient, container.getId(), port.getPort());
			first = false;
		}

		// this HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();

		returnParameters.put("ContainerPorts", portMapping);
		returnParameters.put("ContainerID", container.getId() + ";" + sshContainer.getId());
		returnParameters.put("ContainerIP", ipAddress);

		returnParameters.put("SSHPort", String.valueOf(findPort(dockerClient, sshContainer.getId(), 22)));

		try {
			dockerClient.close();
		} catch (Exception e) {
			System.out.println("Error while closing docker client.");
		}

		sendResponse(returnParameters);
	}

	private File downloadFile(String url) {

		try {
			URI dockerImageURI = new URI(url);

			String[] pathSplit = dockerImageURI.getRawPath().split("/");
			String fileName = pathSplit[pathSplit.length - 1];

			File tempDir = Files.createTempDir();
			File tempFile = new File(tempDir, fileName);

			URLConnection connection = dockerImageURI.toURL().openConnection();
			connection.setRequestProperty("Accept", "application/octet-stream");

			InputStream input = connection.getInputStream();
			byte[] buffer = new byte[4096];
			int n = -1;

			OutputStream output = new FileOutputStream(tempFile);
			while ((n = input.read(buffer)) != -1) {
				output.write(buffer, 0, n);
			}
			output.close();
			input.close();
			return tempFile;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	private File downloadFileFromSFTP(String filePath, String vmip, String vmprivateKey) {
		Session session = SSHSessionFactory.createSSHSession(vmip.trim(), "ubuntu", vmprivateKey.trim());
		
		
		SSHRemoteFileTransfer fileTransf = new SSHRemoteFileTransfer(session);
		
		try {
			File tempFile = File.createTempFile(new File(filePath).getName(), "");
			
			fileTransf.getFile(filePath, tempFile);
			
			return tempFile;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	private File createTempTarFromFile(File file) {
		TarArchiveEntry entry = new TarArchiveEntry(file, file.getName());

		File tarArchive = null;
		try {
			tarArchive = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".tar");
			TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(tarArchive));
			out.putArchiveEntry(entry);
			IOUtils.copy(new FileInputStream(file), out);
			out.closeArchiveEntry();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return tarArchive;
	}

	private boolean isImageAvailable(String image, DockerClient dockerClient) {
		System.out.println("Searching available Images: ");
		for (Image availImage : dockerClient.listImagesCmd().exec()) {
			for (String tag : availImage.getRepoTags()) {
				if (tag.startsWith(image)) {
					return true;
				}
			}
		}
		return false;
	}

	@WebMethod
	@SOAPBinding
	@Oneway
	public void removeContainer(
			@WebParam(name = "DockerEngineURL", targetNamespace = "http://NodeTypes.opentosca.org/") String DockerEngineURL,
			@WebParam(name = "DockerEngineCertificate", targetNamespace = "http://NodeTypes.opentosca.org/") String DockerEngineCertificate,
			@WebParam(name = "ContainerID", targetNamespace = "http://NodeTypes.opentosca.org/") String ContainerID) {
		// create connection to the docker engine
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost(DockerEngineURL).withDockerTlsVerify(false).withApiVersion("1.21").build();

		if (DockerEngineCertificate == null) {
			config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(DockerEngineURL)
					.withDockerTlsVerify(false).withApiVersion("1.21").build();
		} else {
			config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(DockerEngineURL)
					.withDockerCertPath(DockerEngineCertificate).withApiVersion("1.21").build();

		}

		DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

		// stop ssh and real container together
		for (String id : ContainerID.split(";")) {
			// stop and remove container
			dockerClient.stopContainerCmd(id).exec();
			dockerClient.removeContainerCmd(id).exec();
		}

		// this HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();

		returnParameters.put("Result", "Stopped and Removed container " + ContainerID);

		try {
			dockerClient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sendResponse(returnParameters);
	}

	private boolean pullImage(DockerClient dockerClient, String imageName) {
		// pull needed image if not already done
		try {
			System.out.println("Fetching container image " + imageName);
			dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitSuccess();
			return true;
		} catch (Exception e) {
			System.out.println("Couldnt connect, thus, wait and retry.");
			e.printStackTrace();
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			}
			dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitSuccess();
		}
		return true;
	}

	/**
	 * Returns the port to which a docker container is bound.
	 *
	 * @param dockerClient
	 *            The docker client where the container is running.
	 * @param containerID
	 *            The ID of the container
	 * @param searchedPort
	 *            The inner port of the container
	 * @return The outer port to which the specified inner port of the container is
	 *         bound.
	 */
	private int findPort(DockerClient dockerClient, String containerID, int searchedPort) {
		for (Container container : dockerClient.listContainersCmd().exec()) {
			if (container.getId().equals(containerID)) {
				for (ContainerPort port : container.getPorts()) {
					if (port.getPrivatePort() == searchedPort) {
						return port.getPublicPort();
					}
				}
			}
		}
		return -1;
	}

	public static final String MSG_FAILED = "FAILED";
	public static final String TESTMODE = "TESTMODE";

}
