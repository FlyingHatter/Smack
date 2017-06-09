package org.jivesoftware.smackx.jingle_filetransfer.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 09.06.17.
 */
public class IncomingFileTransferResponded implements JingleSessionHandler, BytestreamListener {

    private static final Logger LOGGER = Logger.getLogger(IncomingFileTransferResponded.class.getName());

    private final WeakReference<JingleFileTransferManager> manager;
    private final File target;
    private final int size;
    private final FullJid initiator;
    private final String sessionId;

    public IncomingFileTransferResponded(JingleFileTransferManager manager, Jingle initiate, File target) {
        this.manager = new WeakReference<>(manager);
        this.target = target;
        this.size = ((JingleFileTransferChildElement) initiate.getContents().get(0).getDescription()
                .getJingleContentDescriptionChildren().get(0)).getSize();
        this.initiator = initiate.getInitiator();
        this.sessionId = initiate.getSid();
    }

    @Override
    public void incomingBytestreamRequest(BytestreamRequest request) {
        if (!request.getFrom().asFullJidIfPossible().equals(initiator) || !sessionId.equals(request.getSessionID())) {
            LOGGER.log(Level.INFO, "Not our session.");
            return;
        }

        BytestreamSession session;
        try {
            session = request.accept();
        } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException e) {
            LOGGER.log(Level.SEVERE, "Exception while accepting session: " + e, e);
            return;
        }
        byte[] fileBuf = new byte[size];
        byte[] buf = new byte[4096];
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            if (!target.exists()) {
                target.createNewFile();
            }

            fileOutputStream = new FileOutputStream(target);
            inputStream = session.getInputStream();

            int read = 0;
            while (read < fileBuf.length) {
                int r = inputStream.read(buf);
                if (r != -1) {
                    System.arraycopy(buf, 0, fileBuf, read, r);
                    read += r;
                }
            }

            fileOutputStream.write(fileBuf);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Caught IOException while receiving files: " + e, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Caught Exception while closing streams: " + e, e);
            }
        }
        return;
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle, String sessionId) {
        return null;
    }
}
