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
package org.jivesoftware.smackx.ox.v2.store;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

import org.jxmpp.jid.BareJid;

public abstract class AbstractOpenPgpMetadataStore implements OpenPgpMetadataStore {

    private final Map<BareJid, Set<OpenPgpV4Fingerprint>> announcedFingerprints = new HashMap<>();

    @Override
    public Set<OpenPgpV4Fingerprint> getAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        Set<OpenPgpV4Fingerprint> fingerprints = announcedFingerprints.get(contact);
        if (fingerprints == null) {
            fingerprints = readAnnouncedFingerprintsOf(contact);
            announcedFingerprints.put(contact, fingerprints);
        }
        return fingerprints;
    }
}
