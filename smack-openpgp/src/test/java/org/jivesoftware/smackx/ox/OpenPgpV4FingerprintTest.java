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

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.Test;

public class OpenPgpV4FingerprintTest extends SmackTestSuite {

    @Test(expected = IllegalArgumentException.class)
    public void fpTooShort() {
        String fp = "484f57414c495645"; // Asking Mark
        new OpenPgpV4Fingerprint(fp);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidHexTest() {
        String fp = "UNFORTUNATELYTHISISNOVALIDHEXADECIMALDOH";
        new OpenPgpV4Fingerprint(fp);
    }

    @Test
    public void validFingerprintTest() {
        String fp = "4A4F48414E4E53454E2049532041204E45524421";
        OpenPgpV4Fingerprint finger = new OpenPgpV4Fingerprint(fp);
        assertEquals(fp, finger.toString());
    }

    @Test
    public void convertsToUpperCaseTest() {
        String fp = "444f4e5420552048415645204120484f4242593f";
        OpenPgpV4Fingerprint finger = new OpenPgpV4Fingerprint(fp);
        assertEquals("444F4E5420552048415645204120484F4242593F", finger.toString());
    }

    @Test
    public void equalsOtherFingerprintTest() {
        OpenPgpV4Fingerprint finger = new OpenPgpV4Fingerprint("5448452043414b452049532041204c4945212121");
        assertEquals(finger, new OpenPgpV4Fingerprint("5448452043414B452049532041204C4945212121"));
    }
}
