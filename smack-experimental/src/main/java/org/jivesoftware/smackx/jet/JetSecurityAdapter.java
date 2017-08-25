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
package org.jivesoftware.smackx.jet;

import org.jivesoftware.smackx.jet.component.JetSecurity;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;

/**
 * Adapter to parse JetSecurityElements into JetSecurity components.
 */
public class JetSecurityAdapter implements JingleSecurityAdapter<JetSecurity> {

    @Override
    public JetSecurity securityFromElement(JingleContentSecurityElement element) {
        return new JetSecurity((JetSecurityElement) element);
    }

    @Override
    public String getNamespace() {
        return JetSecurity.NAMESPACE;
    }
}