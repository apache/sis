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

import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.xml.bind.IdentifierMapAdapter;
import org.apache.sis.xml.bind.ModifiableIdentifierMap;
import org.apache.sis.xml.bind.NonMarshalledAuthority;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.system.Modules;
import org.apache.sis.util.collection.Containers;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;


/**
 * The base class of ISO 19115 implementation classes. Each sub-classes implements one
 * of the ISO Metadata interface provided by <a href="http://www.geoapi.org">GeoAPI</a>.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
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
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
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
     * @param  object  the metadata to copy values from, or {@code null} if none.
     */
    protected ISOMetadata(final Object object) {
        super(object);
        if (object instanceof IdentifiedObject) {
            if (object instanceof ISOMetadata && Containers.isNullOrEmpty(((ISOMetadata) object).identifiers)) {
                /*
                 * If the other object is an ISOMetadata instance,  take a look at its `identifiers` collection
                 * before to invoke object.getIdentifiers() in order to avoid unnecessary initialization of its
                 * backing collection. We do this optimization because the vast majority of metadata objects do
                 * not have `identifiers` collection.
                 *
                 * Actually this optimization is a little bit dangerous, since users could override getIdentifiers()
                 * without invoking super.getIdentifiers(), in which case their identifiers will not be copied.
                 * For safety, we will do this optimization only if the implementation is an Apache SIS one.
                 */
                if (object.getClass().getName().startsWith(Modules.CLASSNAME_PREFIX)) {
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
     * <h4>Note for implementers</h4>
     * Subclasses shall not override this method in a way that depends on the object state,
     * since this method may be indirectly invoked by copy constructors (i.e. is may be
     * invoked before this metadata object is fully constructed).
     *
     * @return the metadata standard, which is {@linkplain MetadataStandard#ISO_19115 ISO 19115} by default.
     */
    @Override
    public MetadataStandard getStandard() {
        return MetadataStandard.ISO_19115;
    }




    // --------------------------------------------------------------------------------------
    // Identifier methods below also appear in other IdentifiedObject implementations.
    // If this code is modified, consider revisiting also the following classes:
    //
    //   * org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction
    // --------------------------------------------------------------------------------------

    /**
     * Returns all identifiers associated to this object (from conceptual model and from XML document).
     * This collection may contain identifiers from different sources:
     *
     * <ul class="verbose">
     *   <li>Identifiers specified in the ISO 19115-1 or 19115-2 abstract models,
     *       typically (but not necessarily) as an {@code identifier} property
     *       (may also be {@link DefaultMetadata#getMetadataIdentifier() metadataIdentifier},
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getISBN() ISBN} or
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getISSN() ISSN} properties).</li>
     *   <li>Identifiers specified in the ISO 19115-3 or 19115-4 XML schemas.
     *       Those identifiers are typically stored as a result of unmarshalling an XML document.
     *       Those identifiers can be recognized by an {@linkplain Identifier#getAuthority() authority}
     *       sets as one of the {@link IdentifierSpace} constants.</li>
     * </ul>
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        return identifiers = nonNullCollection(identifiers, Identifier.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns a wrapper around the {@link #identifiers} list.
     * That map is <em>live</em>: changes in the identifiers list will be reflected in the map,
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
         * caching it we would need anyway to check if `identifiers` still references the same list.
         */
        return (super.state() != State.FINAL) ? new ModifiableIdentifierMap(identifiers)
                                              : new IdentifierMapAdapter(identifiers);
    }

    /**
     * Returns the first identifier which is presumed to be defined by ISO 19115 conceptual model.
     * This method checks the {@linkplain Identifier#getAuthority() authority} for filtering ignorable
     * identifiers like ISBN/ISSN codes and XML attributes.
     * This convenience method is provided for implementation of public {@code getIdentifier(Identifier)}
     * methods in subclasses having an {@code identifier} property with [0 … 1] multiplicity.
     *
     * @return an identifier from ISO 19115-3 conceptual model (excluding XML identifiers),
     *         or {@code null} if none.
     *
     * @since 1.0
     */
    protected Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets the identifier for metadata objects that are expected to contain at most one ISO 19115 identifier.
     * This convenience method is provided for implementation of public {@code setIdentifier(Identifier)} methods
     * in subclasses having an {@code identifier} property with [0 … 1] multiplicity.
     * The default implementation removes all identifiers that would be returned by {@link #getIdentifier()}
     * before to add the given one in the {@link #identifiers} collection.
     *
     * @param  newValue  the new identifier value, or {@code null} for removing the identifier.
     *
     * @since 1.0
     */
    protected void setIdentifier(final Identifier newValue) {
        checkWritePermission(valueIfDefined(identifiers));
        identifiers = nonNullCollection(identifiers, Identifier.class);
        identifiers = writeCollection(NonMarshalledAuthority.setMarshallable(identifiers, newValue), identifiers, Identifier.class);
    }

    // --------------------------------------------------------------------------------------
    // End of identifier methods.
    // --------------------------------------------------------------------------------------

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean transitionTo(final State target) {
        final Collection<Identifier> p = identifiers;
        final boolean changed = super.transitionTo(target);
        if (changed) {
            /*
             * The `identifiers` collection will have been replaced by an unmodifiable collection if
             * subclass has an "identifiers" property. If this is not the case, then the collection
             * is unchanged (or null) so we have to make it unmodifiable here.
             */
            if (p != null && p == identifiers) {
                if (p instanceof Set<?>) {
                    identifiers = CollectionsExt.unmodifiableOrCopy((Set<Identifier>) p);
                } else if (p instanceof List<?>) {
                    identifiers = CollectionsExt.unmodifiableOrCopy((List<Identifier>) p);
                } else {
                    identifiers = Collections.unmodifiableCollection(p);
                }
            }
        }
        return changed;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Returns an identifier unique for the XML document, or {@code null} if none.
     * This method is invoked automatically by JAXB and should never be invoked explicitly.
     */
    @XmlID
    @XmlAttribute                           // Defined in "gco" as unqualified attribute.
    @XmlSchemaType(name = "ID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getID() {
        return isNullOrEmpty(identifiers) ? null : ImplementationHelper.getObjectID(this);
    }

    /**
     * Sets an identifier unique for the XML document.
     * This method is invoked automatically by JAXB and should never be invoked explicitly.
     */
    private void setID(final String id) {
        ImplementationHelper.setObjectID(this, id);
    }

    /**
     * Returns an unique identifier, or {@code null} if none.
     * This method is invoked automatically by JAXB and should never be invoked explicitly.
     */
    @XmlAttribute                           // Defined in "gco" as unqualified attribute.
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
     * This method is invoked automatically by JAXB and should never be invoked explicitly.
     */
    private void setUUID(final String id) {
        /*
         * IdentifierMapAdapter will take care of converting the String to UUID if possible, or
         * will store the value as a plain String if it cannot be converted. In the latter case,
         * a warning will be emitted (logged or processed by listeners).
         */
        getIdentifierMap().put(IdentifierSpace.UUID, id);
    }
}
