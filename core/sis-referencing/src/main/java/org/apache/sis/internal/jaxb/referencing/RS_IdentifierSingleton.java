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
package org.apache.sis.internal.jaxb.referencing;

import java.util.Set;
import java.util.Iterator;
import org.opengis.referencing.ReferenceIdentifier;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import static java.util.Collections.singleton;


/**
 * A JAXB adapter for expressing the {@code Set<ReferenceIdentifier>} collection as a singleton.
 * We have to define this adapter because ISO 19111 defines the {@code identifiers} property as
 * a collection while GML 3.2 defines it as a singleton.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class RS_IdentifierSingleton extends XmlAdapter<Code, Set<ReferenceIdentifier>> {
    /**
     * Substitutes the wrapper value read from an XML stream by the object which will
     * represents the identifier. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value The wrapper for this metadata value.
     * @return An identifier which represents the value.
     */
    @Override
    public Set<ReferenceIdentifier> unmarshal(final Code value) {
        return (value != null) ? singleton(value.getIdentifier()) : null;
    }

    /**
     * Substitutes the first identifier by the wrapper to be marshalled into an XML file or stream.
     * Only the first identifier is taken, on the assumption that it is the "main" one.
     *
     * @param  value The metadata value.
     * @return The adapter for the given metadata.
     */
    @Override
    public Code marshal(final Set<ReferenceIdentifier> value) {
        if (value != null) {
            final Iterator<ReferenceIdentifier> it = value.iterator();
            if (it.hasNext()) {
                return new Code(it.next());
            }
        }
        return null;
    }
}
