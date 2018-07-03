package org.jivesoftware.smackx.ox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import org.jxmpp.jid.BareJid;

public class SecretKeyBackupHelper {

    private static final Logger LOGGER = Logger.getLogger(SecretKeyBackupHelper.class.getName());

    /**
     * Generate a secure backup code.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#sect-idm140425111347232">XEP-0373 §5.3</a>
     * @return backup code
     */
    public static String generateBackupPassword() {
        final String alphabet = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        // 6 blocks
        for (int i = 0; i < 6; i++) {

            // of 4 chars
            for (int j = 0; j < 4; j++) {
                char c = alphabet.charAt(random.nextInt(alphabet.length()));
                code.append(c);
            }

            // dash after every block except the last one
            if (i != 5) {
                code.append('-');
            }
        }
        return code.toString();
    }

    public static SecretkeyElement createSecretkeyElement(OpenPgpProvider provider,
                                                    BareJid owner,
                                                    Set<OpenPgpV4Fingerprint> fingerprints,
                                                    String backupCode) throws SmackOpenPgpException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (OpenPgpV4Fingerprint fingerprint : fingerprints) {
            try {
                byte[] bytes = provider.getStore().getSecretKeyRingBytes(owner, fingerprint);
                buffer.write(bytes);
            } catch (MissingOpenPgpKeyPairException | IOException e) {
                LOGGER.log(Level.WARNING, "Cannot backup secret key " + Long.toHexString(fingerprint.getKeyId()) + ".", e);
            }

        }
        return createSecretkeyElement(provider, buffer.toByteArray(), backupCode);
    }

    public static SecretkeyElement createSecretkeyElement(OpenPgpProvider provider,
                                                    byte[] keys,
                                                    String backupCode)
            throws SmackOpenPgpException, IOException {
        byte[] encrypted = provider.symmetricallyEncryptWithPassword(keys, backupCode);
        return new SecretkeyElement(Base64.encode(encrypted));
    }

    public static OpenPgpV4Fingerprint restoreSecretKeyBackup(OpenPgpProvider provider, SecretkeyElement backup, String backupCode)
            throws InvalidBackupCodeException, IOException, MissingUserIdOnKeyException, SmackOpenPgpException {
        byte[] encrypted = Base64.decode(backup.getB64Data());

        byte[] decrypted;
        try {
            decrypted = provider.symmetricallyDecryptWithPassword(encrypted, backupCode);
        } catch (IOException | SmackOpenPgpException e) {
            throw new InvalidBackupCodeException("Could not decrypt secret key backup. Possibly wrong passphrase?", e);
        }

        return provider.importSecretKey(decrypted);
    }
}
