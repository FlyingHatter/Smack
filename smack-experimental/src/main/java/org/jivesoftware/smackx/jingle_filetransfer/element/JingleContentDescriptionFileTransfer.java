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
package org.jivesoftware.smackx.jingle_filetransfer.element;

import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionPayloadType;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;

import java.util.List;

/**
 * Description.
 */
public class JingleContentDescriptionFileTransfer extends JingleContentDescription {

    public JingleContentDescriptionFileTransfer(List<JingleContentDescriptionPayloadType> payloadTypes) {
        super(payloadTypes);
    }

    @Override
    public String getNamespace() {
        return JingleFileTransferManager.NAMESPACE_V5;
    }
}
