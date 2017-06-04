/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.hash.HashManager;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.element.JingleError;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingJingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferContentDescription;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingJingleFileTransferListener;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleFileTransferContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_ibb.JingleInBandByteStreamManager;
import org.jivesoftware.smackx.jingle_ibb.element.JingleInBandByteStreamTransport;
import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle File Transfers.
 *
 * @author Paul Schaub
 */
public final class JingleFileTransferManager extends Manager implements JingleHandler, JingleSessionHandler {

    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferManager.class.getName());

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final HashSet<IncomingJingleFileTransferListener> incomingJingleFileTransferListeners = new HashSet<>();
    private final HashMap<String, JingleSession> sessions = new HashMap<>();

    /**
     * Private constructor. This registers a JingleContentDescriptionFileTransferProvider with the
     * JingleContentProviderManager.
     * @param connection connection
     */
    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE_V5);

        JingleManager.getInstanceFor(connection).registerDescriptionHandler(
                NAMESPACE_V5, this);

        JingleContentProviderManager.addJingleContentDescriptionProvider(
                NAMESPACE_V5, new JingleFileTransferContentDescriptionProvider());

    }

    /**
     * Return a new instance of the FileTransferManager for the given connection.
     *
     * @param connection XMPPConnection we wish to get a FileTransferManager for.
     * @return manager instance.
     */
    public static JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void addIncomingFileTransferListener(IncomingJingleFileTransferListener listener) {
        incomingJingleFileTransferListeners.add(listener);
    }

    public void removeIncomingFileTransferListener(IncomingJingleFileTransferListener listener) {
        incomingJingleFileTransferListeners.remove(listener);
    }

    public JingleFileTransferChildElement.Builder fileTransferPayloadBuilderFromFile(File file) {
        JingleFileTransferChildElement.Builder payloadBuilder = JingleFileTransferChildElement.getBuilder();
        payloadBuilder.setDate(new Date(file.lastModified()));
        payloadBuilder.setName(file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(File.pathSeparator) + 1));
        payloadBuilder.setSize((int) file.length());
        return payloadBuilder;
    }

    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        JingleSession session = new JingleSession(jingle.getInitiator(), jingle.getResponder(), jingle.getSid());
        sessions.put(jingle.getSid(), session);

        for (IncomingJingleFileTransferListener l : incomingJingleFileTransferListeners) {
            l.onIncomingJingleFileTransfer(jingle, new IncomingJingleFileTransferCallback() {
                @Override
                public void acceptFileTransfer(File target) {

                }

                @Override
                public void cancelFileTransfer() {

                }
            });
        }

        return IQ.createResultIQ(jingle);
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle, String sessionId) {
        JingleSession session = sessions.get(sessionId);

        if (session == null) {
            // Handle unknown session (XEP-0166 §10)
            XMPPError.Builder errorBuilder = XMPPError.getBuilder();
            errorBuilder.setCondition(XMPPError.Condition.item_not_found)
                .addExtension(JingleError.UNKNOWN_SESSION);
            return IQ.createErrorResponse(jingle, errorBuilder);
        }

        for (int i = 0; i < jingle.getContents().size() && i < 1; i++) { //TODO: Remove && i<1 later
            JingleContent content = jingle.getContents().get(i);
            switch (jingle.getAction()) {
                case session_initiate:
                    throw new AssertionError("Session is already initiated.");
                case session_accept:
                    // Remote accepts our session-initiate
                    session.setState(JingleSession.State.active);
                    // ACK
                    return IQ.createResultIQ(jingle);
                case session_info:
                    // Remote sends session-info (eg. hash)
                case session_terminate:
                    // Remote wants to terminate our current session
                    sessions.remove(sessionId);
                    return IQ.createResultIQ(jingle);

                case content_accept:
                    // Remote accepts our content-add request.
                case content_add:
                    // Remote wants to add content to the session.
                case content_modify:
                    // Remote wants to change the directionality of the session
                case content_reject:
                    // Remote rejects our content-add request
                case content_remove:
                    // Remote wants to remove a content from the session/abort a single transfer
                case description_info:
                    // Additional parameters of exchanged media
                case security_info:
                    // Remote wants to exchange security information
                case transport_accept:
                    // Remote accepts our transport-replace
                case transport_info:
                    // Remote exchanges transport methods
                case transport_reject:
                    // Remote rejects our transport-replace
                case transport_replace:
                    // Remote wants to replace the transport
                default:
                    return IQ.createErrorResponse(jingle, XMPPError.Condition.feature_not_implemented);
            }
        }
        return null;
    }

    /**
     * QnD method.
     * @param file
     */
    public void sendFile(File file, final FullJid recipient) throws IOException, SmackException.NotConnectedException, InterruptedException {
        final byte[] bytes = new byte[(int) file.length()];
        HashElement hashElement = FileAndHashReader.readAndCalculateHash(file, bytes, HashManager.ALGORITHM.SHA_256);
        Date lastModified = new Date(file.lastModified());
        JingleFileTransferChildElement payload = new JingleFileTransferChildElement(
                lastModified, "A file", hashElement,
                "application/octet-stream", file.getName(), (int) file.length(), null);
        ArrayList<JingleContentDescriptionChildElement> payloadTypes = new ArrayList<>();
        payloadTypes.add(payload);

        JingleFileTransferContentDescription descriptionFileTransfer = new JingleFileTransferContentDescription(payloadTypes);
        final JingleInBandByteStreamTransport transport = new JingleInBandByteStreamTransport();
        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setDescription(descriptionFileTransfer)
                .addTransport(transport)
                .setCreator(JingleContent.Creator.initiator)
                .setSenders(JingleContent.Senders.initiator)
                .setName("file");
        JingleContent content = cb.build();

        final String sid = JingleInBandByteStreamManager.generateSessionId();

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setInitiator(connection().getUser())
                .setResponder(recipient)
                .setAction(JingleAction.session_initiate)
                .addJingleContent(content)
                .setSessionId(sid);
        Jingle jingle = jb.build();

        JingleManager.getInstanceFor(connection()).registerJingleSessionHandler(recipient, sid, new JingleSessionHandler() {
            @Override
            public IQ handleJingleSessionRequest(Jingle jingle, String sessionId) {
                if (sessionId.equals(sid)) {
                    if (jingle.getAction() == JingleAction.session_accept) {

                        InBandBytestreamSession session;
                        try {
                            session = InBandBytestreamManager.getByteStreamManager(connection())
                                    .establishSession(recipient, sid);
                        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                            LOGGER.log(Level.SEVERE, "Fail in handle request: " + e, e);
                            return null;
                        }

                        try {
                            session.getOutputStream().write(bytes);
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Fail while writing: " + e, e);
                        }
                    }
                }
                return null;
            }
        });

        connection().sendStanza(jingle);
    }
}
