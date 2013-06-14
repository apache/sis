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
package org.apache.sis.internal.jaxb.gco;

import java.util.Locale;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.LocalName;
import org.opengis.util.NameFactory;
import org.apache.sis.util.CharSequences;


/**
 * JAXB adapter in order to map implementing class with the GeoAPI interface.
 * See package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final class LocalNameAdapter extends XmlAdapter<String,LocalName> {
    /**
     * The factory to use for creating names, fetched only if needed.
     *
     * {@section Restriction}
     * While this field type is the generic {@code NameFactory} interface in order to defer class
     * loading, {@code LocalNameAdapter} requires an instance of exactly {@link DefaultNameFactory}
     * class (not a subclass). See {@link #getNameFactory()} for more information.
     */
    private transient NameFactory factory;

    /**
     * Empty constructor for JAXB only.
     */
    public LocalNameAdapter() {
    }

    /**
     * Fetches the name factory. The returned factory shall be an instance
     * of {@code DefaultNameFactory}, not a subclass, because we are going
     * to cast the created {@code GenericName} to {@code AbstractName} for
     * XML marshalling and we know that {@code DefaultNameFactory} creates
     * the expected type. A subclass could create other types, so we are
     * better to avoid them.
     */
    static NameFactory getNameFactory() {
        return org.apache.sis.internal.system.DefaultFactories.NAMES;
    }

    /**
     * Does the link between a {@link LocalName} and the string associated.
     * JAXB calls automatically this method at marshalling-time.
     *
     * @param value The implementing class for this metadata value.
     * @return A string representation of the given local name.
     */
    @Override
    public String marshal(final LocalName value) {
        return (value == null) ? null : value.toInternationalString().toString(Locale.ROOT);
    }

    /**
     * Does the link between {@linkplain String strings} and the way they will be unmarshalled.
     * JAXB calls automatically this method at unmarshalling-time.
     *
     * @param  value The string value to unmarshal.
     * @return The implementing class for the given string.
     */
    @Override
    public LocalName unmarshal(String value) {
        value = CharSequences.trimWhitespaces(value);
        if (value == null || value.isEmpty()) {
            return null;
        }
        NameFactory factory = this.factory;
        if (factory == null) {
            // No need to synchronize. This is not a big deal if the factory is fetched twice.
            this.factory = factory = getNameFactory();
        }
        return factory.createLocalName(null, value);
    }
}
