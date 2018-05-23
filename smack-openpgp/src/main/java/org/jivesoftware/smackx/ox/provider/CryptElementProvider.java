/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.ox.provider;

import org.jivesoftware.smackx.ox.element.CryptElement;

import org.xmlpull.v1.XmlPullParser;

public class CryptElementProvider extends OpenPgpContentElementProvider<CryptElement> {

    public static final CryptElementProvider TEST_INSTANCE = new CryptElementProvider();

    @Override
    public CryptElement parse(XmlPullParser parser, int initialDepth)
            throws Exception {
        OpenPgpContentElementData data = parseOpenPgpContentElementData(parser, initialDepth);

        return new CryptElement(data.to, data.rpad, data.timestamp, data.payload);
    }

}