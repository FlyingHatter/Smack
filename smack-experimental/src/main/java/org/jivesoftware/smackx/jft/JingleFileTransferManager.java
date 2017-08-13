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
package org.jivesoftware.smackx.jft;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jft.adapter.JingleFileTransferAdapter;
import org.jivesoftware.smackx.jft.component.JingleFileTransfer;
import org.jivesoftware.smackx.jft.component.JingleFileTransferFile;
import org.jivesoftware.smackx.jft.component.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jft.component.JingleIncomingFileRequest;
import org.jivesoftware.smackx.jft.component.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jft.component.JingleOutgoingFileRequest;
import org.jivesoftware.smackx.jft.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jft.controller.OutgoingFileRequestController;
import org.jivesoftware.smackx.jft.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jft.listener.IncomingFileRequestListener;
import org.jivesoftware.smackx.jft.provider.JingleFileTransferProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.util.Role;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 22.07.17.
 */
public final class JingleFileTransferManager extends Manager implements JingleDescriptionManager {
    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final JingleManager jingleManager;

    private final List<IncomingFileOfferListener> offerListeners =
            Collections.synchronizedList(new ArrayList<IncomingFileOfferListener>());
    private final List<IncomingFileRequestListener> requestListeners =
            Collections.synchronizedList(new ArrayList<IncomingFileRequestListener>());

    static {
        JingleManager.addJingleDescriptionAdapter(new JingleFileTransferAdapter());
        JingleManager.addJingleDescriptionProvider(new JingleFileTransferProvider());
    }

    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.addJingleDescriptionManager(this);
    }

    public static JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public OutgoingFileOfferController sendFile(File file, FullJid to)
            throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, SmackException.FeatureNotSupportedException {
            return sendFile(file, null, to);
    }

    public OutgoingFileOfferController sendFile(File file, String alternativeFilename, FullJid to)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, SmackException.FeatureNotSupportedException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        if (!ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(to, getNamespace())) {
            throw new SmackException.FeatureNotSupportedException(getNamespace(), to);
        }

        JingleSession session = jingleManager.createSession(Role.initiator, to);

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(content);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(file);
        if (alternativeFilename != null) {
            offer.getFile().setName(alternativeFilename);
        }
        content.setDescription(offer);

        JingleTransportManager transportManager = jingleManager.getBestAvailableTransportManager(to);
        JingleTransport<?> transport = transportManager.createTransportForInitiator(content);
        content.setTransport(transport);

        session.sendInitiate(connection());

        return offer;
    }

    public OutgoingFileOfferController sendStream(InputStream stream, String filename, FullJid to) {
        //TODO: Implement
        return null;
    }

    public OutgoingFileRequestController requestFile(JingleFileTransferFile.RemoteFile file, FullJid from) {
        JingleOutgoingFileRequest request = new JingleOutgoingFileRequest(file);

        //TODO at some point.

        return request;
    }

    public void addIncomingFileOfferListener(IncomingFileOfferListener listener) {
        offerListeners.add(listener);
    }

    public void removeIncomingFileOfferListener(IncomingFileOfferListener listener) {
        offerListeners.remove(listener);
    }

    public void notifyIncomingFileOfferListeners(JingleIncomingFileOffer offer) {
        LOGGER.log(Level.INFO, "Incoming File transfer: [" + offer.getNamespace() + ", "
                + offer.getParent().getTransport().getNamespace() + ", "
                + (offer.getParent().getSecurity() != null ? offer.getParent().getSecurity().getNamespace() : "") + "]");
        for (IncomingFileOfferListener l : offerListeners) {
            l.onIncomingFileOffer(offer);
        }
    }

    public void addIncomingFileRequestListener(IncomingFileRequestListener listener) {
        requestListeners.add(listener);
    }

    public void removeIncomingFileRequestListener(IncomingFileRequestListener listener) {
        requestListeners.remove(listener);
    }

    public void notifyIncomingFileRequestListeners(JingleIncomingFileRequest request) {
        for (IncomingFileRequestListener l : requestListeners) {
            l.onIncomingFileRequest(request);
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }

    private void notifyTransfer(JingleFileTransfer transfer) {
        if (transfer.isOffer()) {
            notifyIncomingFileOfferListeners((JingleIncomingFileOffer) transfer);
        } else {
            notifyIncomingFileRequestListeners((JingleIncomingFileRequest) transfer);
        }
    }

    @Override
    public void notifySessionInitiate(JingleSession session) {
        JingleContent content = session.getSoleContentOrThrow();
        notifyTransfer((JingleFileTransfer) content.getDescription());
    }

    @Override
    public void notifyContentAdd(JingleSession session, JingleContent content) {
        notifyTransfer((JingleFileTransfer) content.getDescription());
    }
}
