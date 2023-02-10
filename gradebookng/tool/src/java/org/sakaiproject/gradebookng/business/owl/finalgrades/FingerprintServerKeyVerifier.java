package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.sakaiproject.component.cover.ServerConfigurationService;

/**
 * A class for verifying ssh public key fingerprints, for use with the Apache Mina ssh library.
 * Valid fingerprints sourced from sakai.properties
 *
 * @author plukasew
 */
@Slf4j
public class FingerprintServerKeyVerifier implements ServerKeyVerifier
{
	private static final String FINGERPRINTS_PROPERTY = "gradebook.courseGradeSubmission.sftp.fingerprints";

	private final List<String> fingerprints;

	public FingerprintServerKeyVerifier()
	{
		fingerprints = Arrays.asList(ArrayUtils.nullToEmpty(ServerConfigurationService.getStrings(FINGERPRINTS_PROPERTY)));
	}

	@Override
	public boolean verifyServerKey(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey)
	{
		if (fingerprints.isEmpty())
		{
			log.warn("SSH fingerprints not provided, skipping server key verification.");
			return true;
		}
		String sftpFingerprint = KeyUtils.getFingerPrint(serverKey);
		log.debug("SFTP server presented public key with fingerprint: {}", sftpFingerprint);

		return fingerprints.contains(sftpFingerprint);
    }
}
