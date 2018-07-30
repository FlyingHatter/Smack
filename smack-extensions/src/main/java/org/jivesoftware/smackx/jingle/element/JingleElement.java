/**
 *
 * Copyright 2003-2007 Jive Software, 2014-2017 Florian Schmaus
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

package org.jivesoftware.smackx.jingle.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.component.JingleSession;

import org.jxmpp.jid.FullJid;

/**
 * The Jingle element. This represents a {@link JingleSession}.
 *
 * @author Florian Schmaus
 */
public final class JingleElement extends IQ {

    public static final String NAMESPACE = "urn:xmpp:jingle:1";

    public static final String ACTION_ATTRIBUTE_NAME = "action";

    public static final String INITIATOR_ATTRIBUTE_NAME = "initiator";

    public static final String RESPONDER_ATTRIBUTE_NAME = "responder";

    public static final String SESSION_ID_ATTRIBUTE_NAME = "sid";

    public static final String ELEMENT = "jingle";

    /**
     * The session ID related to this session. The session ID is a unique identifier generated by the initiator. This
     * should match the XML Nmtoken production so that XML character escaping is not needed for characters such as &.
     */
    private final String sessionId;

    /**
     * The jingle action. This attribute is required.
     */
    private final JingleAction action;

    private final FullJid initiator;

    private final FullJid responder;

    private final JingleReasonElement reason;

    private final List<JingleContentElement> contents;

    private JingleElement(String sessionId, JingleAction action, FullJid initiator, FullJid responder, JingleReasonElement reason,
                          List<JingleContentElement> contents) {
        super(ELEMENT, NAMESPACE);
        this.sessionId = StringUtils.requireNotNullNorEmpty(sessionId, "Jingle session ID must not be null");
        this.action = Objects.requireNonNull(action, "Jingle action must not be null");
        this.initiator = initiator;
        this.responder = responder;
        this.reason = reason;
        if (contents != null) {
            this.contents = Collections.unmodifiableList(contents);
        }
        else {
            this.contents = Collections.emptyList();
        }
        setType(Type.set);
    }

    /**
     * Get the initiator. The initiator will be the full JID of the entity that has initiated the flow (which may be
     * different to the "from" address in the IQ)
     *
     * @return the initiator
     */
    public FullJid getInitiator() {
        return initiator;
    }

    /**
     * Get the responder. The responder is the full JID of the entity that has replied to the initiation (which may be
     * different to the "to" addresss in the IQ).
     *
     * @return the responder
     */
    public FullJid getResponder() {
        return responder;
    }

    /**
     * Returns the session ID related to the session. The session ID is a unique identifier generated by the initiator.
     * This should match the XML Nmtoken production so that XML character escaping is not needed for characters such as
     * &amp;.
     *
     * @return Returns the session ID related to the session.
     */
    public String getSid() {
        return sessionId;
    }

    /**
     * Get the action specified in the jingle IQ.
     *
     * @return the action.
     */
    public JingleAction getAction() {
        return action;
    }

    public JingleReasonElement getReason() {
        return reason;
    }

    /**
     * Get a List of the contents.
     *
     * @return the contents.
     */
    public List<JingleContentElement> getContents() {
        return contents;
    }

    /**
     * If there is only one {@link JingleContentElement}, return it.
     * If there is none, return null.
     * Otherwise throw a new {@link IllegalStateException}.
     * @return jingleContentElement or null.
     */
    public JingleContentElement getSoleContentOrThrow() {
        if (contents.isEmpty()) {
            return null;
        }

        if (contents.size() == 1) {
            return contents.get(0);
        }

        throw new IllegalStateException("More than one content is present.");
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.optAttribute(INITIATOR_ATTRIBUTE_NAME, getInitiator());
        xml.optAttribute(RESPONDER_ATTRIBUTE_NAME, getResponder());
        xml.optAttribute(ACTION_ATTRIBUTE_NAME, getAction());
        xml.optAttribute(SESSION_ID_ATTRIBUTE_NAME, getSid());
        xml.rightAngleBracket();

        xml.optElement(reason);

        xml.append(contents);

        return xml;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String sid;

        private JingleAction action;

        private FullJid initiator;

        private FullJid responder;

        private JingleReasonElement reason;

        private List<JingleContentElement> contents;

        private Builder() {
        }

        public Builder setSessionId(String sessionId) {
            this.sid = sessionId;
            return this;
        }

        public Builder setAction(JingleAction action) {
            this.action = action;
            return this;
        }

        public Builder setInitiator(FullJid initator) {
            this.initiator = initator;
            return this;
        }

        public Builder setResponder(FullJid responder) {
            this.responder = responder;
            return this;
        }

        public Builder addJingleContent(JingleContentElement content) {
            if (contents == null) {
                contents = new ArrayList<>(1);
            }
            contents.add(content);
            return this;
        }

        public Builder setReason(JingleReasonElement.Reason reason) {
            this.reason = new JingleReasonElement(reason);
            return this;
        }

        public Builder setReason(JingleReasonElement reason) {
            this.reason = reason;
            return this;
        }

        public JingleElement build() {
            return new JingleElement(sid, action, initiator, responder, reason, contents);
        }
    }

    public static JingleElement createContentAccept(FullJid recipient, String sessionId, List<JingleContentElement> contents) {
        JingleElement.Builder builder = JingleElement.getBuilder().setAction(JingleAction.content_accept).setSessionId(sessionId);

        for (JingleContentElement element : contents) {
            builder.addJingleContent(element);
        }

        JingleElement jingleElement = builder.build();
        jingleElement.setTo(recipient);

        return jingleElement;
    }

    /**
     * Accept a session.
     * XEP-0166 Example 17.
     * @param initiator recipient of the stanza.
     * @param responder our jid.
     * @param sessionId sessionId.
     * @param content content
     * @return session-accept stanza.
     */
    public static JingleElement createSessionAccept(FullJid initiator,
                                             FullJid responder,
                                             String sessionId,
                                             JingleContentElement content) {
        return createSessionAccept(initiator, responder, sessionId, Collections.singletonList(content));
    }

    /**
     * Accept a session.
     * XEP-0166 Example 17.
     * @param initiator recipient of the stanza.
     * @param responder our jid.
     * @param sessionId sessionId.
     * @param contents contents
     * @return session-accept stanza.
     */
    public static JingleElement createSessionAccept(FullJid initiator,
                                             FullJid responder,
                                             String sessionId,
                                             List<JingleContentElement> contents) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setResponder(responder)
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionId);

        for (JingleContentElement content : contents) {
            jb.addJingleContent(content);
        }

        JingleElement jingle = jb.build();
        jingle.setTo(initiator);
        jingle.setFrom(responder);

        return jingle;
    }

    /**
     * Initiate a Jingle session.
     * XEP-0166 Example 10.
     * @param initiator our jid.
     * @param responder jid of the recipient.
     * @param sessionId sessionId.
     * @param content content.
     * @return session-initiate stanza.
     */
    public static JingleElement createSessionInitiate(FullJid initiator,
                                               FullJid responder,
                                               String sessionId,
                                               JingleContentElement content) {
        return createSessionInitiate(initiator, responder, sessionId, Collections.singletonList(content));
    }

    /**
     * Initiate a Jingle session.
     * XEP-0166 Example 10.
     * @param initiator our jid.
     * @param responder jid of the recipient.
     * @param sessionId sessionId.
     * @param contents contents.
     * @return session-initiate stanza.
     */
    public static JingleElement createSessionInitiate(FullJid initiator,
                                               FullJid responder,
                                               String sessionId,
                                               List<JingleContentElement> contents) {

        JingleElement.Builder builder = JingleElement.getBuilder();
        builder.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(initiator);

        for (JingleContentElement content : contents) {
            builder.addJingleContent(content);
        }

        JingleElement jingle = builder.build();
        jingle.setTo(responder);

        return jingle;
    }

    /**
     * Create a session ping stanza.
     * XEP-0166 Example 32.
     * @param recipient recipient of the stanza.
     * @param sessionId id of the session.
     * @return ping stanza
     */
    public static JingleElement createSessionPing(FullJid recipient, String sessionId) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_info);

        JingleElement jingle = jb.build();
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Create a session-terminate stanza.
     * XEP-0166 §6.7.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param reason reason of termination.
     * @return session-terminate stanza.
     */
    public static JingleElement createSessionTerminate(FullJid recipient, String sessionId, JingleReasonElement reason) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId)
                .setReason(reason);

        JingleElement jingle = jb.build();
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Create a session-terminate stanza.
     * XEP-0166 §6.7.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param reason reason of termination.
     * @return session-terminate stanza.
     */
    public static JingleElement createSessionTerminate(FullJid recipient, String sessionId, JingleReasonElement.Reason reason) {
        return createSessionTerminate(recipient, sessionId, new JingleReasonElement(reason));
    }

    /**
     * Cancel a single contents transfer.
     * XEP-0234 Example 10.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param contentCreator creator of the content.
     * @param contentName name of the content.
     * @return session-terminate stanza.
     */
    public static JingleElement createSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                                             JingleContentElement.Creator contentCreator, String contentName) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId).setReason(JingleReasonElement.Reason.cancel);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);

        return jingle;
    }


    /**
     * Accept a transport.
     * XEP-0260 Example 17.
     * @param recipient recipient of the stanza
     * @param initiator initiator of the session
     * @param sessionId sessionId
     * @param contentCreator creator of the content
     * @param contentName name of the content
     * @param transport transport to accept
     * @return transport-accept stanza
     */
    public static JingleElement createTransportAccept(FullJid initiator, FullJid recipient, String sessionId,
                                                      JingleContentElement.Creator contentCreator,
                                                      String contentName, JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.transport_accept)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Reject a transport.
     * XEP-0166 §7.2.14.
     * @param recipient recipient of the stanza
     * @param initiator initiator of the session
     * @param sessionId sessionId
     * @param contentCreator creator of the content
     * @param contentName name of the content
     * @param transport transport to reject
     * @return transport-reject stanza
     */
    public static JingleElement createTransportReject(FullJid initiator, FullJid recipient, String sessionId,
                                                      JingleContentElement.Creator contentCreator,
                                                      String contentName, JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.transport_reject)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Replace a transport with another one.
     * XEP-0260 Example 15.
     * @param recipient recipient of the stanza
     * @param initiator initiator of the session
     * @param sessionId sessionId
     * @param contentCreator creator of the content
     * @param contentName name of the content
     * @param transport proposed transport
     * @return transport-replace stanza
     */
    public static JingleElement createTransportReplace(FullJid initiator, FullJid recipient, String sessionId,
                                                       JingleContentElement.Creator contentCreator,
                                                       String contentName, JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.getBuilder()
                .setAction(JingleAction.transport_replace)
                .setSessionId(sessionId)
                .setInitiator(initiator);

        JingleContentElement content = JingleContentElement.getBuilder()
                .setCreator(contentCreator)
                .setName(contentName)
                .setTransport(transport).build();

        jb.addJingleContent(content);

        JingleElement jingle = jb.build();
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Create an error response to a request with an unknown session id.
     * XEP-0166 Example 29.
     * @param request request with unknown sessionId.
     * @return error stanza.
     */
    public static IQ createJingleErrorUnknownSession(JingleElement request) {
        StanzaError.Builder error = StanzaError.getBuilder();
        error.setCondition(StanzaError.Condition.item_not_found)
                .addExtension(JingleErrorElement.UNKNOWN_SESSION);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a request coming from a unknown initiator.
     * XEP-0166 Example 12.
     * @param request request from unknown initiator.
     * @return error stanza.
     */
    public static IQ createJingleErrorUnknownInitiator(JingleElement request) {
        StanzaError.Builder b = StanzaError.getBuilder().setType(StanzaError.Type.CANCEL).setCondition(StanzaError.Condition.service_unavailable);
        return IQ.createErrorResponse(request, b);
    }

    /**
     * Create an error response to a request with an unsupported info.
     * XEP-0166 Example 31.
     * @param request request with unsupported info.
     * @return error stanza.
     */
    public static IQ createJingleErrorUnsupportedInfo(JingleElement request) {
        StanzaError.Builder error = StanzaError.getBuilder();
        error.setCondition(StanzaError.Condition.feature_not_implemented)
                .addExtension(JingleErrorElement.UNSUPPORTED_INFO).setType(StanzaError.Type.MODIFY);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a tie-breaking request.
     * XEP-0166 Example 34.
     * @param request tie-breaking request
     * @return error stanza
     */
    public static IQ createJingleErrorTieBreak(JingleElement request) {
        StanzaError.Builder error = StanzaError.getBuilder();
        error.setCondition(StanzaError.Condition.conflict)
                .addExtension(JingleErrorElement.TIE_BREAK);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a request that was out of order.
     * TODO: Find example.
     * @param request request out of order.
     * @return error stanza.
     */
    public static IQ createJingleErrorOutOfOrder(JingleElement request) {
        StanzaError.Builder error = StanzaError.getBuilder();
        error.setCondition(StanzaError.Condition.unexpected_request)
                .addExtension(JingleErrorElement.OUT_OF_ORDER);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a malformed request.
     * XEP-0166 Ex. 16
     * @param request malformed request
     * @return error stanza.
     */
    public static IQ createJingleErrorMalformedRequest(JingleElement request) {
        StanzaError.Builder error = StanzaError.getBuilder();
        error.setType(StanzaError.Type.CANCEL);
        error.setCondition(StanzaError.Condition.bad_request);
        return IQ.createErrorResponse(request, error);
    }
}
