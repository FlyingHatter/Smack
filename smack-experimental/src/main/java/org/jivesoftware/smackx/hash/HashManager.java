/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.hash;

import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.jcajce.provider.digest.SHA224;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.MD5;
import org.jivesoftware.smack.util.SHA1;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.BLAKE2B256;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.BLAKE2B384;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.BLAKE2B512;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA3_256;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA3_384;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA3_512;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA_256;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA_384;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA_512;

/**
 * Manager that can be used to determine support for hash functions.
 */
public final class HashManager extends Manager {
    public static final String NAMESPACE_V2 = "urn:xmpp:hashes:2";
    public static final String PREFIX_NS_ALGO = "urn:xmpp:hash-function-text-names:";

    public static final List<ALGORITHM> RECOMMENDED = Collections.unmodifiableList(Arrays.asList(
            SHA_256, SHA_384, SHA_512,
            SHA3_256, SHA3_384, SHA3_512,
            BLAKE2B256, BLAKE2B384, BLAKE2B512));

    private static final WeakHashMap<XMPPConnection, HashManager> INSTANCES = new WeakHashMap<>();

    /**
     * Constructor of the HashManager.
     * By default the Manager announces support for XEP-0300, as well as for the RECOMMENDED set of hash algorithms.
     * Those contain SHA256, SHA384, SHA512, SHA3-256, SHA3-384, SHA3-512, BLAKE2B256, BLAKE2B384 and BLAKE2B512.
     * Those algorithms got recommended here: https://xmpp.org/extensions/xep-0300.html#recommendations
     * @param connection connection
     */
    private HashManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE_V2);
        addAlgorithmsToFeatures(RECOMMENDED);
    }

    /**
     * Announce support for the given list of algorithms.
     * @param algorithms
     */
    public void addAlgorithmsToFeatures(List<ALGORITHM> algorithms) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        for (ALGORITHM algo : algorithms) {
            sdm.addFeature(asFeature(algo));
        }
    }

    /**
     * Get an instance of the HashManager for the  given connection.
     * @param connection
     * @return
     */
    public HashManager getInstanceFor(XMPPConnection connection) {
        HashManager hashManager = INSTANCES.get(connection);
        if (hashManager == null) {
            hashManager = new HashManager(connection);
            INSTANCES.put(connection, hashManager);
        }
        return hashManager;
    }

    /**
     * Return the feature name of the given algorithm.
     * @param algorithm eg. 'SHA_1'
     * @return feature name (eg. urn:xmpp:hash-function-text-names:sha-1')
     */
    public static String asFeature(ALGORITHM algorithm) {
        return PREFIX_NS_ALGO + algorithm.toString();
    }

    public enum ALGORITHM {                 // RECOMMENDATION:
        MD5 ("md5"),                        // MUST NOT use this
        SHA_1 ("sha-1"),                    // SHOULD NOT use this
        SHA_224 ("sha-224"),
        SHA_256 ("sha-256"),                // MUST use this
        SHA_384 ("sha-384"),
        SHA_512 ("sha-512"),                // SHOULD use this
        SHA3_224 ("sha3-224"),
        SHA3_256 ("sha3-256"),              // MUST use this
        SHA3_384 ("sha3-384"),
        SHA3_512 ("sha3-512"),              // SHOULD use this
        BLAKE2B160("id-blake2b160"),
        BLAKE2B256("id-blake2b256"),        // MUST use this
        BLAKE2B384("id-blake2b384"),
        BLAKE2B512("id-blake2b512");        // SHOULD use this

        private final String name;

        ALGORITHM(String name) {
            this.name = name;
        }

        /**
         * Return the name of the algorithm as it is used in the XEP.
         * @return name.
         */
        @Override
        public String toString() {
            return this.name;
        }

        /**
         * Compensational method for static 'valueOf' function.
         * @param s
         * @return
         */
        public static ALGORITHM valueOfName(String s) {
            for (ALGORITHM a : ALGORITHM.values()) {
                if (s.equals(a.toString())) {
                    return a;
                }
            }
            throw new IllegalArgumentException("No ALGORITHM enum with this name (" + s + ") found.");
        }
    }

    /**
     * Calculate the hash sum of data using algorithm.
     * @param algorithm
     * @param data
     * @return
     */
    public static byte[] hash(ALGORITHM algorithm, byte[] data) {
        byte[] hash;
        switch (algorithm) {
            case MD5:
                hash = md5(data);
                break;
            case SHA_1:
                hash = sha_1(data);
                break;
            case SHA_224:
                hash = sha_224(data);
                break;
            case SHA_256:
                hash = sha_256(data);
                break;
            case SHA_384:
                hash = sha_384(data);
                break;
            case SHA_512:
                hash = sha_512(data);
                break;
            case SHA3_224:
                hash = sha3_224(data);
                break;
            case SHA3_256:
                hash = sha3_256(data);
                break;
            case SHA3_384:
                hash = sha3_384(data);
                break;
            case SHA3_512:
                hash = sha3_512(data);
                break;
            case BLAKE2B160:
                hash = blake2b160(data);
                break;
            case BLAKE2B256:
                hash = blake2b256(data);
                break;
            case BLAKE2B384:
                hash = blake2b384(data);
                break;
            case BLAKE2B512:
                hash = blake2b512(data);
                break;
            default:
                throw new AssertionError("Invalid enum value.");
        }
        return hash;
    }

    public static byte[] md5(byte[] data) {
        return MD5.bytes(data);
    }

    public static byte[] sha_1(byte[] data) {
        return SHA1.bytes(data);
    }

    public static byte[] sha_224(byte[] data) {
        return new SHA224.Digest().digest(data);
    }

    public static byte[] sha_256(byte[] data) {
        return new SHA256.Digest().digest(data);
    }

    public static byte[] sha_384(byte[] data) {
        return new SHA384.Digest().digest(data);
    }

    public static byte[] sha_512(byte[] data) {
        return new SHA512.Digest().digest(data);
    }

    public static byte[] sha3_224(byte[] data) {
        return new SHA3.Digest224().digest(data);
    }

    public static byte[] sha3_256(byte[] data) {
        return new SHA3.Digest256().digest(data);
    }

    public static byte[] sha3_384(byte[] data) {
        return new SHA3.Digest384().digest(data);
    }

    public static byte[] sha3_512(byte[] data) {
        return new SHA3.Digest512().digest(data);
    }

    public static byte[] blake2b160(byte[] data) {
        return new Blake2b.Blake2b160().digest(data);
    }

    public static byte[] blake2b256(byte[] data) {
        return new Blake2b.Blake2b256().digest(data);
    }

    public static byte[] blake2b384(byte[] data) {
        return new Blake2b.Blake2b384().digest(data);
    }

    public static byte[] blake2b512(byte[] data) {
        return new Blake2b.Blake2b512().digest(data);
    }

    /**
     * Encode a byte array in HEX.
     * @param hash
     * @return
     */
    public static String hex(byte[] hash) {
        return new BigInteger(1, hash).toString(16);
    }

}
