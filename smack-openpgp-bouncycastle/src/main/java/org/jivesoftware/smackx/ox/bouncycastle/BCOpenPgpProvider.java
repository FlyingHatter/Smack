/**
 *
 * Copyright 2018 Paul Schaub.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PublicKeySize;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.XmppKeySelectionStrategy;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;

public class BCOpenPgpProvider implements OpenPgpProvider {

    private final BareJid user;

    private BCOpenPgpStore store;


    public BCOpenPgpProvider(BareJid user) {
        this.user = user;
    }

    public void setStore(BCOpenPgpStore store) {
        this.store = store;
    }

    @Override
    public OpenPgpV4Fingerprint primaryOpenPgpKeyPairFingerprint() {
        return store.primaryOpenPgpKeyPairFingerprint();
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeyPairFingerprints() {
        return store.availableOpenPgpKeyPairFingerprints();
    }

    @Override
    public Set<OpenPgpV4Fingerprint> announcedOpenPgpKeyFingerprints(BareJid contact) {
        return store.announcedOpenPgpKeyFingerprints(contact);
    }

    @Override
    public OpenPgpElement signAndEncrypt(SigncryptElement element, OpenPgpV4Fingerprint signingKey, Set<OpenPgpV4Fingerprint> encryptionKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException {
        if (encryptionKeys.isEmpty()) {
            throw new IllegalArgumentException("Set of recipients must not be empty");
        }

        encryptionKeys.addAll(store.announcedOpenPgpKeyFingerprints(user));
        long[] recipientIds = new long[encryptionKeys.size()];
        int pos = 0;
        for (OpenPgpV4Fingerprint f : encryptionKeys) {
            recipientIds[pos++] = f.getKeyId();
        }

        InputStream inputStream = element.toInputStream();
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();

        try {
            OutputStream encryptor = BouncyGPG.encryptToStream()
                    .withConfig(store.getKeyringConfig())
                    .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                    .withOxAlgorithms()
                    .toRecipients(recipientIds)
                    .andSignWith(signingKey.getKeyId())
                    .binaryOutput()
                    .andWriteTo(encryptedOut);

            Streams.pipeAll(inputStream, encryptor);
            encryptor.close();

            String base64 = Base64.encodeToString(encryptedOut.toByteArray());

            return new OpenPgpElement(base64);
        } catch (Exception e) {
            throw new AssertionError(e);
            // TODO: Later
        }
    }

    @Override
    public OpenPgpMessage decryptAndVerify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> sendersKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException {

        ByteArrayInputStream encryptedIn = new ByteArrayInputStream(
                element.getEncryptedBase64MessageContent().getBytes(Charset.forName("UTF-8")));

        try {
            InputStream decrypted = BouncyGPG.decryptAndVerifyStream()
                    .withConfig(store.getKeyringConfig())
                    .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                    .andValidateSomeoneSigned() // TODO: Validate using sender keys
                    .fromEncryptedInputStream(encryptedIn);

            ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();

            Streams.pipeAll(decrypted, decryptedOut);

            return new OpenPgpMessage(OpenPgpMessage.State.signcrypt, new String(decryptedOut.toByteArray(), Charset.forName("UTF-8")));
        } catch (IOException | NoSuchProviderException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public OpenPgpElement sign(SignElement element, OpenPgpV4Fingerprint singingKeyFingerprint)
            throws MissingOpenPgpKeyPairException {
        throw new AssertionError("Feature not implemented!");
    }

    @Override
    public OpenPgpMessage verify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> singingKeyFingerprints)
            throws MissingOpenPgpPublicKeyException {
        throw new AssertionError("Feature not implemented!");
    }

    @Override
    public OpenPgpElement encrypt(CryptElement element, Set<OpenPgpV4Fingerprint> encryptionKeyFingerprints)
            throws MissingOpenPgpPublicKeyException {
        throw new AssertionError("Feature not implemented!");
    }

    @Override
    public OpenPgpMessage decrypt(OpenPgpElement element)
            throws MissingOpenPgpKeyPairException {
        throw new AssertionError("Feature not implemented!");
    }

    @Override
    public PubkeyElement createPubkeyElement(OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException, SmackOpenPgpException {
        return store.createPubkeyElement(fingerprint);
    }

    @Override
    public void storePublicKey(BareJid owner, OpenPgpV4Fingerprint fingerprint, PubkeyElement element)
            throws SmackOpenPgpException {
        store.storePublicKey(owner, fingerprint, element);
    }

    @Override
    public void storePublicKeysList(XMPPConnection connection, PublicKeysListElement listElement, BareJid owner) {
        store.storePublicKeysList(connection, listElement, owner);
    }

    @Override
    public OpenPgpV4Fingerprint createOpenPgpKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException, SmackOpenPgpException {
        return store.createOpenPgpKeyPair();
    }

    @Override
    public SecretkeyElement createSecretkeyElement(Set<OpenPgpV4Fingerprint> fingerprints, String password)
            throws MissingOpenPgpKeyPairException, SmackOpenPgpException {
        return store.createSecretkeyElement(fingerprints, password);
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpPublicKeysFingerprints(BareJid contact)
            throws SmackOpenPgpException {
        return store.availableOpenPgpPublicKeysFingerprints(contact);
    }

    @Override
    public void restoreSecretKeyBackup(SecretkeyElement secretkeyElement, String password, SecretKeyRestoreSelectionCallback callback)
            throws SmackOpenPgpException, InvalidBackupCodeException {
        store.restoreSecretKeyBackup(secretkeyElement, password, callback);
    }

    static PGPKeyRingGenerator generateKey(BareJid owner)
            throws NoSuchAlgorithmException, PGPException, NoSuchProviderException {
        PGPKeyRingGenerator generator = BouncyGPG.createKeyPair()
                .withRSAKeys()
                .ofSize(PublicKeySize.RSA._2048)
                .forIdentity("xmpp:" + owner.toString())
                .withoutPassphrase()
                .build();
        return generator;
    }

    public static OpenPgpV4Fingerprint getFingerprint(PGPPublicKey publicKey) {
        byte[] hex = Hex.encode(publicKey.getFingerprint());
        return new OpenPgpV4Fingerprint(hex);
    }
}
