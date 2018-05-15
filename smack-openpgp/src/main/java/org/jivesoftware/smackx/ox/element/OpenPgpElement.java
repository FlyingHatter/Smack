/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;

import org.xmlpull.v1.XmlPullParserException;

public class OpenPgpElement implements ExtensionElement {

    public static final String ELEMENT = "openpgp";
    public static final String NAMESPACE = "urn:xmpp:openpgp:0";

    // Represents the OpenPGP message, but encoded using base64.
    private final String base64EncodedOpenPgpMessage;

    private OpenPgpMessage openPgpMessage;

    // Represents the OpenPGP message, but base64 decoded.
    private byte[] openPgpMessageBytes;

    private OpenPgpContentElement openPgpContentElement;

    public OpenPgpElement(String base64EncodedOpenPgpMessage) {
        this.base64EncodedOpenPgpMessage = base64EncodedOpenPgpMessage;
    }

    public OpenPgpMessage getOpenPgpMessage(OpenPgpProvider provider) {
        if (openPgpMessage == null) {
            ensureOpenPgpMessageBytesSet();
            InputStream is = new ByteArrayInputStream(openPgpMessageBytes);
            openPgpMessage = OpenPgpManager.toOpenPgpMessage(is);
        }

        return openPgpMessage;
    }

    public OpenPgpContentElement getContentElement(OpenPgpProvider provider) throws XmlPullParserException, IOException {
        if (openPgpContentElement == null) {
            openPgpContentElement = getOpenPgpMessage(provider).getOpenPgpContentElement();
        }

        return openPgpContentElement;
    }

    public String getEncryptedBase64MessageContent() {
        return base64EncodedOpenPgpMessage;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket().append(base64EncodedOpenPgpMessage).closeElement(this);
        return xml;
    }

    private void ensureOpenPgpMessageBytesSet() {
        if (openPgpMessageBytes != null) return;

        openPgpMessageBytes = Base64.decode(base64EncodedOpenPgpMessage);
    }
}
