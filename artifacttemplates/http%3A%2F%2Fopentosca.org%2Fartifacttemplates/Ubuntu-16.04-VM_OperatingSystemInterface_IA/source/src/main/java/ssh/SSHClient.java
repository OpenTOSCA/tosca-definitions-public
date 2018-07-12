package ssh;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class SSHClient {

	static public void transferFile(String host, String sshUser, String sshKey, String source, String target)
			throws IOException {

		final net.schmizz.sshj.SSHClient ssh = new net.schmizz.sshj.SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier());
		ssh.connect(host);
		try {
			ssh.authPublickey(sshUser, getKeyFile(sshKey).getAbsolutePath());
			final SFTPClient sftp = ssh.newSFTPClient();
			try {
				sftp.put(source, target);
			} finally {
				sftp.close();
			}
		} finally {
			ssh.close();
		}
	}

	static public String executeCommand(String host, String sshUser, String sshKey, String command) throws IOException {

		net.schmizz.sshj.SSHClient client = new net.schmizz.sshj.SSHClient();
		client.addHostKeyVerifier(new PromiscuousVerifier());
		client.connect(host);

		try {
			client.authPublickey(sshUser, getKeyFile(sshKey).getAbsolutePath());

			final Session session = client.startSession();
			session.allocateDefaultPTY();

			try {
				final Command cmd = session.exec(command);

				InputStream output = cmd.getInputStream();
				String response = IOUtils.readFully(output).toString()
						.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
				System.out.println(response);
				session.close();
				client.close();

				return response;

			} finally {
				session.close();
			}
		} finally {
			client.disconnect();
		}
	}

	static private File getKeyFile(String sshKey) throws IOException {
		// Add to Host Key Repository & Identity Manager
		// ...by putting it into a temp file
		File keyFile = File.createTempFile("ssh", ".key");
		// Delete temp file when program exits.
		keyFile.deleteOnExit();

		// Write to temp file
		BufferedWriter out = new BufferedWriter(new FileWriter(keyFile));
		out.write(sshKey);
		out.close();

		return keyFile;
	}

}
