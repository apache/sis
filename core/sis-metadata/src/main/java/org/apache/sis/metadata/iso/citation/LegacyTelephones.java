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
package org.apache.sis.metadata.iso.citation;

import java.util.Collection;
import java.util.Iterator;
import org.opengis.util.CodeList;
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;


/**
 * An adapter for converting telephone lists from ISO 19115:2014 definition to ISO 19115:2003 definition.
 * Used for implementation of deprecated {@link DefaultTelephone#getVoices()} and
 * {@link DefaultTelephone#getFacsimiles()} methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class LegacyTelephones extends LegacyPropertyAdapter<String,Telephone> {
    /**
     * The type of telephone number.
     * Either {@link UnsupportedCodeList#VOICE} or {@link UnsupportedCodeList#FACSIMILE}.
     */
    private final CodeList<?> type;

    /**
     * Wraps the given telephone list for the given type.
     */
    LegacyTelephones(final Collection<Telephone> telephones, final CodeList<?> type) {
        super(telephones);
        this.type = type;
    }

    /**
     * Wraps the given telephone number in a new {@link DefaultTelephone} instance.
     */
    @Override
    protected Telephone wrap(final String value) {
        return new DefaultTelephone(value, type);
    }

    /**
     * Extracts the telephone number from the given {@link DefaultTelephone} instance.
     */
    @Override
    protected String unwrap(final Telephone container) {
        if (container instanceof DefaultTelephone) {
            final CodeList<?> ct = ((DefaultTelephone) container).numberType;
            if (ct != null) {
                if (type.name().equals(ct.name())) {
                    return ((DefaultTelephone) container).getNumber();
                }
            }
        }
        return null;
    }

    /**
     * Updates the telephone number in an existing {@link DefaultTelephone} instance, if possible.
     */
    @Override
    protected boolean update(final Telephone container, final String value) {
        if (container instanceof DefaultTelephone) {
            final CodeList<?> ct = ((DefaultTelephone) container).numberType;
            if (ct == null || type.name().equals(ct.name())) {
                if (ct == null) {
                    ((DefaultTelephone) container).numberType = type;
                }
                ((DefaultTelephone) container).setNumber(value);
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new telephone number. As a special case if the first element is empty, then the telephone number
     * will be set in that element. We test only the first element because {@link DefaultTelephone#getOwner()}
     * initialize new collections as collection containing {@code DefaultTelephone.this}.
     *
     * @param  value The telephone number to add.
     * @return {@code true} if the element has been added.
     */
    @Override
    public boolean add(final String value) {
        if (value == null || value.isEmpty()) { // Null value happen with empty XML elements like <gco:CharacterString/>
            return false;
        }
        final Iterator<Telephone> it = elements.iterator();
        if (it.hasNext()) {
            final Telephone telephone = it.next();
            if (telephone instanceof DefaultTelephone && ((DefaultTelephone) telephone).isEmpty()) {
                if (update(telephone, value)) {
                    return true;
                }
            }
        }
        return elements.add(wrap(value));
    }
}
