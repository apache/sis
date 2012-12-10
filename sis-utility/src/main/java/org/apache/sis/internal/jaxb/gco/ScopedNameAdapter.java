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

import java.util.List;
import java.util.ConcurrentModificationException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.type.DefaultScopedName;


/**
 * JAXB adapter in order to map implementing class with the GeoAPI interface.
 * See package documentation for more information about JAXB and interface.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class ScopedNameAdapter extends XmlAdapter<DefaultScopedName,ScopedName> {
    /**
     * The factory to use for creating names, fetched only if needed.
     *
     * {@section Restriction}
     * While this field type is the generic {@code NameFactory} interface in order to defer class
     * loading, {@code ScopedNameAdapter} requires an instance of exactly {@link DefaultNameFactory}
     * class (not a subclass). See {@link LocalNameAdapter#getNameFactory()} for more information.
     */
    private transient NameFactory factory;

    /**
     * Empty constructor for JAXB only.
     */
    public ScopedNameAdapter() {
    }

    /**
     * Recreates a new name for the given name, using the given factory.
     * This is used in order to get a SIS implementation from an arbitrary implementation.
     */
    static GenericName wrap(final GenericName value, final NameFactory factory) {
        final List<? extends LocalName> parsedNames = value.getParsedNames();
        final CharSequence[] names = new CharSequence[parsedNames.size()];
        int i=0;
        for (final LocalName name : parsedNames) {
            // Asks for the unlocalized name, since we are going to marshal that.
            names[i++] = name.toInternationalString().toString(null);
        }
        if (i != names.length) {
            throw new ConcurrentModificationException(Errors.format(Errors.Keys.UnexpectedChange_1, "parsedNames"));
        }
        return factory.createGenericName(value.scope(), names);
    }

    /**
     * Does the link between a {@link ScopedName} and the associated string.
     * JAXB calls automatically this method at marshalling-time.
     *
     * @param  value The implementing class for this metadata value.
     * @return A scoped name which represents the metadata value.
     */
    @Override
    public DefaultScopedName marshal(final ScopedName value) {
        if (value == null) {
            return null;
        }
        if (value instanceof DefaultScopedName) {
            return (DefaultScopedName) value;
        }
        NameFactory factory = this.factory;
        if (factory == null) {
            // No need to synchronize. This is not a big deal if the factory is fetched twice.
            this.factory = factory = LocalNameAdapter.getNameFactory();
        }
        /*
         * The following cast should not fail  because we asked specifically for the
         * DefaultNameFactory instance (which is known to create DefaultLocalName or
         * DefaultScopedName instances),  and the names array should contains two or
         * more elements (otherwise the argument value should have been a LocalName).
         */
        return (DefaultScopedName) wrap(value, factory);
    }

    /**
     * Does the link between {@linkplain DefaultScopedName scoped names} and the way they
     * will be unmarshalled. JAXB calls automatically this method at unmarshalling-time.
     *
     * @param  value The {@linkplain DefaultScopedName scoped name} value.
     * @return The implementing class for this string.
     */
    @Override
    public ScopedName unmarshal(final DefaultScopedName value) {
        return value;
    }
}
