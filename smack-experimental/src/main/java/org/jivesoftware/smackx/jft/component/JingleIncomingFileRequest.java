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
package org.jivesoftware.smackx.jft.component;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.controller.IncomingFileRequestController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Created by vanitas on 27.07.17.
 * TODO: RemoteFile????
 */
public class JingleIncomingFileRequest extends AbstractJingleFileRequest<JingleFileTransferFile.RemoteFile> implements IncomingFileRequestController {

    public JingleIncomingFileRequest(JingleFileTransferChildElement request) {
        super(new JingleFileTransferFile.RemoteFile(request));
    }

    @Override
    public JingleFileTransferElement getElement() {
        return null;
    }

    @Override
    public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
        return null;
    }

    @Override
    public boolean isOffer() {
        return false;
    }

    @Override
    public boolean isRequest() {
        return true;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {

    }

    @Override
    public JingleFileTransferFile.RemoteFile getFile() {
        return (JingleFileTransferFile.RemoteFile) file;
    }
}
