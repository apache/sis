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
package org.apache.sis.metadata.iso;

import java.util.Collection;
import java.io.Serializable;
import net.jcip.annotations.ThreadSafe;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.internal.jaxb.IdentifierMapWithSpecialCases;
import org.apache.sis.util.ArgumentChecks;


/**
 * The base class of ISO 19115 implementation classes. Each sub-classes implements one
 * of the ISO Metadata interface provided by <a href="http://www.geoapi.org">GeoAPI</a>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class ISOMetadata extends ModifiableMetadata implements IdentifiedObject, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4997239501383133209L;

    /**
     * All identifiers associated with this metadata, or {@code null} if none.
     * This field is initialized to a non-null value when first needed.
     */
    protected Collection<Identifier> identifiers;

    /**
     * The {@linkplain #getIdentifierMap() identifier map} as a wrapper around the
     * {@linkplain #identifiers} collection. This map is created only when first needed.
     *
     * @see #getIdentifierMap()
     */
    private transient IdentifierMap identifierMap;

    /**
     * Constructs an initially empty metadata.
     */
    protected ISOMetadata() {
    }

    /**
     * Constructs a new metadata initialized with the values from the specified object.
     * If the given object is an instance of {@link IdentifiedObject}, then this constructor
     * copies the {@linkplain #identifiers collection of identifiers}.
     *
     * @param object The metadata to copy values from.
     */
    protected ISOMetadata(final Object object) {
        ArgumentChecks.ensureNonNull("object", object);
        if (object instanceof IdentifiedObject) {
            identifiers = copyCollection(((IdentifiedObject) object).getIdentifiers(), Identifier.class);
        }
    }

    /**
     * Returns the metadata standard implemented by subclasses,
     * which is {@linkplain MetadataStandard#ISO_19115 ISO 19115}.
     *
     * {@note Subclasses shall not override this method in a way that depends on the object state,
     *        since this method may be indirectly invoked by copy constructors (i.e. is may be
     *        invoked before this metadata object is fully constructed).}
     */
    @Override
    public MetadataStandard getStandard() {
        return MetadataStandard.ISO_19115;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Collection<Identifier> getIdentifiers() {
        return identifiers = nonNullCollection(identifiers, Identifier.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns a wrapper around the {@linkplain #identifiers} list.
     * That map is <cite>live</cite>: changes in the identifiers list will be reflected in the map,
     * and conversely.</p>
     */
    @Override
    public synchronized IdentifierMap getIdentifierMap() {
        if (identifierMap == null) {
            final Collection<Identifier> identifiers = getIdentifiers();
            if (identifiers == null) {
                return IdentifierMapWithSpecialCases.EMPTY;
            }
            identifierMap = new IdentifierMapWithSpecialCases(identifiers);
        }
        return identifierMap;
    }
}
