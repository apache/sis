/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.storage.gpx;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * An email address broken into two parts (id and domain) to help prevent email harvesting.
 * This class also implements its own converter for getting the {@link String} representation
 * at unmarshalling time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Email extends XmlAdapter<Email, String> {
    /**
     * The half before {@code @} in email address (for example "john.smith").
     */
    @XmlAttribute
    String id;

    /**
     * The half after {@code @} in email address (for example "hotmail.com").
     */
    @XmlAttribute
    String domain;

    /**
     * Invoked by JAXB at (un)marshalling time.
     */
    public Email() {
    }

    /**
     * Returns the complete email address.
     */
    @Override
    public String toString() {
        return id + '@' + domain;
    }

    /**
     * Invoked at reading time for creating the full email address from its components.
     */
    @Override
    public String unmarshal(final Email address) {
        return address.toString();
    }

    /**
     * Invoked at writing time for splitting an email address into two components.
     */
    @Override
    public Email marshal(final String address) {
        final Email r = new Email();
        final int s = address.indexOf('@');
        r.id = address.substring(0, Math.max(0, s));
        r.domain = address.substring(s+1);
        return r;
    }
}
