/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.jingle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;

import org.jxmpp.jid.FullJid;

public abstract class JingleSession implements JingleSessionHandler {
    private static final Logger LOGGER = Logger.getLogger(JingleSession.class.getName());
    protected HashSet<String> failedTransportMethods = new HashSet<>();

    protected final FullJid local;

    protected final FullJid remote;

    protected final Role role;

    protected final String sid;

    protected final List<JingleContent> contents = new ArrayList<>();

    protected ArrayList<Future<?>> queued = new ArrayList<>();
    protected JingleTransportSession<?> transportSession;

    public JingleSession(FullJid initiator, FullJid responder, Role role, String sid) {
        this(initiator, responder, role, sid, null);
    }

    public JingleSession(FullJid initiator, FullJid responder, Role role, String sid, List<JingleContent> contents) {
        if (role == Role.initiator) {
            this.local = initiator;
            this.remote = responder;
        } else {
            this.local = responder;
            this.remote = initiator;
        }
        this.sid = sid;
        this.role = role;

        if (contents != null) {
            this.contents.addAll(contents);
        }
    }

    public FullJid getInitiator() {
        return isInitiator() ? local : remote;
    }

    public boolean isInitiator() {
        return role == Role.initiator;
    }

    public FullJid getResponder() {
        return isResponder() ? local : remote;
    }

    public boolean isResponder() {
        return role == Role.responder;
    }

    public FullJid getRemote() {
        return remote;
    }

    public FullJid getLocal() {
        return local;
    }

    public String getSessionId() {
        return sid;
    }

    public FullJidAndSessionId getFullJidAndSessionId() {
        return new FullJidAndSessionId(remote, sid);
    }

    public List<JingleContent> getContents() {
        return contents;
    }

    public JingleTransportSession<?> getTransportSession() {
        return transportSession;
    }

    protected void setTransportSession(JingleTransportSession<?> transportSession) {
        this.transportSession = transportSession;
    }

    @Override
    public int hashCode() {
        int hashCode = 31 + getInitiator().hashCode();
        hashCode = 31 * hashCode + getResponder().hashCode();
        hashCode = 31 * hashCode + getSessionId().hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JingleSession)) {
            return false;
        }

        JingleSession otherJingleSession = (JingleSession) other;
        return getInitiator().equals(otherJingleSession.getInitiator())
                && getResponder().equals(otherJingleSession.getResponder())
                && sid.equals(otherJingleSession.sid);
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle) {
        try {
            switch (jingle.getAction()) {
                case content_accept:
                    return handleContentAccept(jingle);
                case content_add:
                    return handleContentAdd(jingle);
                case content_modify:
                    return handleContentModify(jingle);
                case content_reject:
                    return handleContentReject(jingle);
                case content_remove:
                    return handleContentRemove(jingle);
                case description_info:
                    return handleDescriptionInfo(jingle);
                case session_info:
                    return handleSessionInfo(jingle);
                case security_info:
                    return handleSecurityInfo(jingle);
                case session_accept:
                    return handleSessionAccept(jingle);
                case transport_accept:
                    return handleTransportAccept(jingle);
                case transport_info:
                    return transportSession.handleTransportInfo(jingle);
                case session_initiate:
                    return handleSessionInitiate(jingle);
                case transport_reject:
                    return handleTransportReject(jingle);
                case session_terminate:
                    return handleSessionTerminate(jingle);
                case transport_replace:
                    return handleTransportReplace(jingle);
                default:
                    return IQ.createResultIQ(jingle);
            }
        } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
            LOGGER.log(Level.SEVERE, "Caught an Exception: ", e);
            return null; //TODO: Handle better?
        }
    }

    protected IQ handleSessionInitiate(Jingle sessionInitiate) throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        return IQ.createResultIQ(sessionInitiate);
    }

    protected IQ handleSessionTerminate(Jingle sessionTerminate) {
        return IQ.createResultIQ(sessionTerminate);
    }

    protected IQ handleSessionInfo(Jingle sessionInfo) {
        return IQ.createResultIQ(sessionInfo);
    }

    protected IQ handleSessionAccept(Jingle sessionAccept) throws SmackException.NotConnectedException, InterruptedException {
        return IQ.createResultIQ(sessionAccept);
    }

    protected IQ handleContentAdd(Jingle contentAdd) {
        return IQ.createResultIQ(contentAdd);
    }

    protected IQ handleContentAccept(Jingle contentAccept) {
        return IQ.createResultIQ(contentAccept);
    }

    protected IQ handleContentModify(Jingle contentModify) {
        return IQ.createResultIQ(contentModify);
    }

    protected IQ handleContentReject(Jingle contentReject) {
        return IQ.createResultIQ(contentReject);
    }

    protected IQ handleContentRemove(Jingle contentRemove) {
        return IQ.createResultIQ(contentRemove);
    }

    protected IQ handleDescriptionInfo(Jingle descriptionInfo) {
        return IQ.createResultIQ(descriptionInfo);
    }

    protected IQ handleSecurityInfo(Jingle securityInfo) {
        return IQ.createResultIQ(securityInfo);
    }

    protected IQ handleTransportAccept(Jingle transportAccept) throws SmackException.NotConnectedException, InterruptedException {
        return IQ.createResultIQ(transportAccept);
    }

    protected IQ handleTransportReplace(Jingle transportReplace)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        return IQ.createResultIQ(transportReplace);
    }

    protected IQ handleTransportReject(Jingle transportReject) {
        return IQ.createResultIQ(transportReject);
    }

    public abstract XMPPConnection getConnection();

    public abstract void onTransportMethodFailed(String namespace);

}
