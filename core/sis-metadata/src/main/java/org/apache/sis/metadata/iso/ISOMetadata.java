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
import java.util.logging.Logger;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.internal.jaxb.IdentifierMapWithSpecialCases;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ThreadSafe;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


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
     * The logger for warnings emitted by any class in the {@code org.apache.sis.metadata.iso.*} packages.
     * Warnings are emitted when an action causes the lost of data. For example the {@code "distance"} and
     * {@code "equivalentScale"} properties in {@link org.apache.sis.metadata.iso.identification.DefaultResolution}
     * are mutually exclusive: setting one discards the other. In such case, a warning is logged.
     */
    public static final Logger LOGGER = Logging.getLogger(ISOMetadata.class);

    /**
     * All identifiers associated with this metadata, or {@code null} if none.
     * This field is initialized to a non-null value when first needed.
     */
    protected Collection<Identifier> identifiers;

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
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    protected ISOMetadata(final Object object) {
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




    // --------------------------------------------------------------------------------------
    // Code below this point also appears in other IdentifiedObject implementations.
    // If this code is modified, consider revisiting also the following classes:
    //
    //   * org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction
    // --------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        return identifiers = nonNullCollection(identifiers, Identifier.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns a wrapper around the {@link #identifiers} list.
     * That map is <cite>live</cite>: changes in the identifiers list will be reflected in the map,
     * and conversely.</p>
     */
    @Override
    public IdentifierMap getIdentifierMap() {
        /*
         * Do not invoke getIdentifiers(), because some subclasses like DefaultCitation and
         * DefaultObjective override getIdentifiers() in order to return a filtered list.
         */
        identifiers = nonNullCollection(identifiers, Identifier.class);
        if (identifiers == null) {
            return IdentifierMapWithSpecialCases.EMPTY;
        }
        /*
         * We do not cache (for now) the IdentifierMap because it is cheap to create, and if we were
         * caching it we would need anyway to check if 'identifiers' still references the same list.
         */
        return new IdentifierMapWithSpecialCases(identifiers);
    }

    /**
     * Returns an identifier unique for the XML document, or {@code null} if none.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    @XmlID
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getID() {
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().getSpecialized(IdentifierSpace.ID);
    }

    /**
     * Sets an identifier unique for the XML document.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    private void setID(String id) {
        id = CharSequences.trimWhitespaces(id);
        if (id != null && !id.isEmpty()) {
            getIdentifierMap().putSpecialized(IdentifierSpace.ID, id);
        }
    }

    /**
     * Returns an unique identifier, or {@code null} if none.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getUUID() {
        /*
         * IdentifierMapWithSpecialCases will take care of converting UUID to String,
         * or to return a previously stored String if it was an unparsable UUID.
         */
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().get(IdentifierSpace.UUID);
    }

    /**
     * Sets an unique identifier.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    private void setUUID(final String id) {
        /*
         * IdentifierMapWithSpecialCases will take care of converting the String to UUID if possible,
         * or will store the value as a plain String if it can not be converted. In the later case, a
         * warning will be emitted (logged or processed by listeners).
         */
        getIdentifierMap().put(IdentifierSpace.UUID, id);
    }
}
