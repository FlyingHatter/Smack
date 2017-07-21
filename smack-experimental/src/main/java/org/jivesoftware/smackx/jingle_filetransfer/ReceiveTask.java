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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Thread for receiving data.
 */
public class ReceiveTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ReceiveTask.class.getName());

    private final BytestreamSession byteStream;
    private final JingleFileTransferElement fileTransfer;
    private final File target;
    private final JingleFileTransferSession session;

    public ReceiveTask(JingleFileTransferSession session, BytestreamSession byteStream, JingleFileTransferElement fileTransfer, File target) {
        this.byteStream = byteStream;
        this.fileTransfer = fileTransfer;
        this.target = target;
        this.session = session;
    }

    @Override
    public void run() {
        JingleFileTransferChildElement transfer = (JingleFileTransferChildElement) fileTransfer.getJingleContentDescriptionChildren().get(0);
        FileOutputStream outputStream = null;
        InputStream inputStream;

        try {
            outputStream = new FileOutputStream(target);
            inputStream = byteStream.getInputStream();

            byte[] filebuf = new byte[transfer.getSize()];
            int read = 0;
            byte[] bufbuf = new byte[4096];
            LOGGER.log(Level.INFO, "Begin receiving bytes.");
            while (read < filebuf.length) {
                int r = inputStream.read(bufbuf);
                if (r >= 0) {
                    System.arraycopy(bufbuf, 0, filebuf, read, r);
                    read += r;
                    LOGGER.log(Level.INFO, "Read " + r + " (" + read + " of " + filebuf.length + ") bytes.");
                } else {
                    break;
                }
            }

            outputStream.write(filebuf);
            LOGGER.log(Level.INFO, "File successfully received.");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while receiving data: ", e);
        } finally {
            try {
                byteStream.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close InputStream.", e);
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close FileOutputStream.", e);
                }
            }

            session.notifyEndedListeners(JingleReasonElement.Reason.success);
        }
    }
}
