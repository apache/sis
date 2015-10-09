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
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.internal.jaxb.IdentifierMapAdapter;
import org.apache.sis.internal.jaxb.ModifiableIdentifierMap;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.util.Utilities;
import org.apache.sis.util.collection.Containers;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * The base class of ISO 19115 implementation classes. Each sub-classes implements one
 * of the ISO Metadata interface provided by <a href="http://www.geoapi.org">GeoAPI</a>.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@XmlTransient
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
            if (object instanceof ISOMetadata && Containers.isNullOrEmpty(((ISOMetadata) object).identifiers)) {
                /*
                 * If the other object is an ISOMetadata instance,  take a look at its 'identifiers' collection
                 * before to invoke object.getIdentifiers() in order to avoid unnecessary initialization of its
                 * backing collection. We do this optimization because the vast majority of metadata objects do
                 * not have 'identifiers' collection.
                 *
                 * Actually this optimization is a little bit dangerous, since users could override getIdentifiers()
                 * without invoking super.getIdentifiers(), in which case their identifiers will not be copied.
                 * For safety, we will do this optimization only if the implementation is an Apache SIS one.
                 */
                if (Utilities.isSIS(object.getClass())) {
                    return;
                }
            }
            identifiers = copyCollection(((IdentifiedObject) object).getIdentifiers(), Identifier.class);
        }
    }

    /**
     * Returns the metadata standard implemented by subclasses,
     * which is {@linkplain MetadataStandard#ISO_19115 ISO 19115}.
     *
     * <div class="section">Note for implementors</div>
     * Subclasses shall not override this method in a way that depends on the object state,
     * since this method may be indirectly invoked by copy constructors (i.e. is may be
     * invoked before this metadata object is fully constructed).
     *
     * @return The metadata standard, which is {@linkplain MetadataStandard#ISO_19115 ISO 19115} by default.
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
            return IdentifierMapAdapter.EMPTY;
        }
        /*
         * We do not cache (for now) the IdentifierMap because it is cheap to create, and if we were
         * caching it we would need anyway to check if 'identifiers' still references the same list.
         */
        return isModifiable() ? new ModifiableIdentifierMap(identifiers)
                              : new IdentifierMapAdapter(identifiers);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns an identifier unique for the XML document, or {@code null} if none.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    @XmlID
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlSchemaType(name = "ID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getID() {
        return isNullOrEmpty(identifiers) ? null : MetadataUtilities.getObjectID(this);
    }

    /**
     * Sets an identifier unique for the XML document.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    private void setID(final String id) {
        MetadataUtilities.setObjectID(this, id);
    }

    /**
     * Returns an unique identifier, or {@code null} if none.
     * This method is invoked automatically by JAXB and should never be invoked explicitely.
     */
    @XmlAttribute  // Defined in "gco" as unqualified attribute.
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getUUID() {
        /*
         * IdentifierMapAdapter will take care of converting UUID to String,
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
         * IdentifierMapAdapter will take care of converting the String to UUID if possible, or
         * will store the value as a plain String if it can not be converted. In the later case,
         * a warning will be emitted (logged or processed by listeners).
         */
        getIdentifierMap().put(IdentifierSpace.UUID, id);
    }
}
