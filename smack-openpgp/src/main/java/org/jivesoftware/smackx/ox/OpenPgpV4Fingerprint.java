/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import java.nio.charset.Charset;

import org.jivesoftware.smack.util.Objects;

/**
 * This class represents an hex encoded, uppercase OpenPGP v4 fingerprint.
 */
public class OpenPgpV4Fingerprint implements CharSequence, Comparable<OpenPgpV4Fingerprint> {

    private final String fingerprint;

    /**
     * Create an {@link OpenPgpV4Fingerprint}.
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#annoucning-pubkey">
     *     XEP-0373 §4.1: The OpenPGP Public-Key Data Node about how to obtain the fingerprint</a>
     * @param fingerprint hexadecimal representation of the fingerprint.
     */
    public OpenPgpV4Fingerprint(String fingerprint) {
        String fp = Objects.requireNonNull(fingerprint)
                .trim()
                .toUpperCase();
        if (!isValid(fp)) {
            throw new IllegalArgumentException("Fingerprint " + fingerprint +
                    " does not appear to be a valid OpenPGP v4 fingerprint.");
        }
        this.fingerprint = fp;
    }

    public OpenPgpV4Fingerprint(byte[] bytes) {
        this(new String(bytes, Charset.forName("UTF-8")));
    }

    /**
     * Check, whether the fingerprint consists of 40 valid hexadecimal characters.
     * @param fp fingerprint to check.
     * @return true if fingerprint is valid.
     */
    private boolean isValid(String fp) {
        return fp.matches("[0-9A-F]{40}");
    }

    /**
     * Return the key id of the OpenPGP public key this {@link OpenPgpV4Fingerprint} belongs to.
     * This method uses {@link Util#keyIdFromFingerprint(OpenPgpV4Fingerprint)}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc4880#section-12.2">
     *     RFC-4880 §12.2: Key IDs and Fingerprints</a>
     * @return key id
     */
    public long getKeyId() {
        return Util.keyIdFromFingerprint(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof CharSequence)) {
            return false;
        }

        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return fingerprint.hashCode();
    }

    @Override
    public int length() {
        return fingerprint.length();
    }

    @Override
    public char charAt(int i) {
        return fingerprint.charAt(i);
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return fingerprint.subSequence(i, i1);
    }

    @Override
    public String toString() {
        return fingerprint;
    }

    @Override
    public int compareTo(OpenPgpV4Fingerprint openPgpV4Fingerprint) {
        return fingerprint.compareTo(openPgpV4Fingerprint.fingerprint);
    }
}
