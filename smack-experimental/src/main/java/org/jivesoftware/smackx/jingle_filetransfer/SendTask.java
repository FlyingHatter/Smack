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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.element.JingleReason;

/**
 * Created by vanitas on 21.06.17.
 */
public class SendTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SendTask.class.getName());

    private final BytestreamSession byteStream;
    private final JingleFileTransferSession session;
    private final File source;

    public SendTask(JingleFileTransferSession session, BytestreamSession byteStream, File source) {
        this.byteStream = byteStream;
        this.source = source;
        this.session = session;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(source);
            outputStream = byteStream.getOutputStream();

            byte[] filebuf = new byte[(int) source.length()];
            int r = inputStream.read(filebuf);

            if (r < 0) {
                throw new IOException("Read returned -1");
            }

            LOGGER.log(Level.INFO, "WRITE");
            outputStream.write(filebuf);
            outputStream.flush();
            LOGGER.log(Level.INFO, "WRITING FINISHED");
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not send file: " + e, e);
        }

        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    LOGGER.log(Level.INFO, "InputStream closed.");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close session.", e);
            }

            session.notifyEndedListeners(JingleReason.Reason.success);
        }
    }
}