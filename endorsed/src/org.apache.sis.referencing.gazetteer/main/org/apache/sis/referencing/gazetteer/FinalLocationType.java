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
package org.apache.sis.referencing.gazetteer;

import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.MetadataCopier;

// Specific to the main branch:
import org.apache.sis.metadata.iso.citation.AbstractParty;


/**
 * Unmodifiable description of a location created as a snapshot of another {@code LocationType} instance
 * at {@link ReferencingByIdentifiers} construction time. This instance will be set a different reference
 * system than the original location type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FinalLocationType extends AbstractLocationType implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 9032473745502779734L;

    /**
     * Name of the location type.
     */
    @SuppressWarnings("serial")         // Most Apache SIS implementations are serializable.
    private final InternationalString name;

    /**
     * Property used as the defining characteristic of the location type.
     */
    @SuppressWarnings("serial")         // Most Apache SIS implementations are serializable.
    private final InternationalString theme;

    /**
     * Method(s) of uniquely identifying location instances.
     * This list is unmodifiable.
     */
    @SuppressWarnings("serial")
    private final List<InternationalString> identifications;

    /**
     * The way in which location instances are defined.
     */
    @SuppressWarnings("serial")         // Most Apache SIS implementations are serializable.
    private final InternationalString definition;

    /**
     * The reference system that comprises this location type.
     */
    private final ReferencingByIdentifiers referenceSystem;

    /**
     * Geographic area within which the location type occurs.
     */
    @SuppressWarnings("serial")         // Most Apache SIS implementations are serializable.
    private final GeographicExtent territoryOfUse;

    /**
     * Name of organization or class of organization able to create and destroy location instances.
     */
    private final AbstractParty owner;

    /**
     * Parent location types (location types of which this location type is a sub-division).
     * This list is unmodifiable.
     */
    @SuppressWarnings("serial")
    private final List<AbstractLocationType> parents;

    /**
     * Child location types (location types which sub-divides this location type).
     * This list is unmodifiable.
     */
    @SuppressWarnings("serial")
    final List<AbstractLocationType> children;

    /**
     * Creates a copy of the given location type with the reference system set to the given value.
     *
     * @param source    the location type to copy.
     * @param rs        the reference system that comprises this location type.
     * @param existing  other {@code FinalLocationType} instances created before this one.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    private FinalLocationType(final AbstractLocationType source, final ReferencingByIdentifiers rs,
            final Map<AbstractLocationType, FinalLocationType> existing)
    {
        /*
         * Put 'this' in the map at the beginning in case the parents and children contain cyclic references.
         * Cyclic references are not allowed if the source are ModifiableLocationType, but the user could have
         * given its own implementation. Having the 'this' reference escaped in object construction should not
         * be an issue here because this is a private constructor, and we use it in such a way that if an
         * exception is thrown, the whole tree (with all 'this' references) will be discarded.
         */
        existing.put(source, this);
        /*
         * For the following properties, we will fallback on the given reference system if the property value
         * from the source location type is null. We do that because those properties are mandatory according
         * ISO 19112 and it happen quite often that they have the same value in the location type than in the
         * reference system.
         */
        InternationalString theme;
        GeographicExtent    territoryOfUse;
        AbstractParty       owner;
        /*
         * Copy the value from the source location type, make them unmodifiable,
         * fallback on the ReferenceSystemUsingIdentifiers if necessary.
         */
        name            = source.getName();
        theme           = source.getTheme();
        identifications = List.copyOf(source.getIdentifications());
        definition      = source.getDefinition();
        territoryOfUse  = (GeographicExtent) unmodifiable(source.getTerritoryOfUse());
        owner           = (AbstractParty) unmodifiable(source.getOwner());
        parents         = snapshot(source.getParents(),  rs, existing);
        children        = snapshot(source.getChildren(), rs, existing);
        referenceSystem = rs;
        if (rs != null) {
            if (theme == null) theme = rs.getTheme();
            if (owner == null) owner = rs.getOverallOwner();
            if (territoryOfUse == null) {
                final Extent domainOfValidity = rs.getDomainOfValidity();
                if (domainOfValidity instanceof GeographicExtent) {
                    territoryOfUse = (GeographicExtent) domainOfValidity;
                }
            }
        }
        this.theme          = theme;
        this.territoryOfUse = territoryOfUse;
        this.owner          = owner;
    }

    /**
     * Creates a snapshot of the given location types. This method returns a new collection within which
     * all elements are snapshots (as {@code FinalLocationType} instances) of the given location types,
     * except the reference system which is set to the given value.
     *
     * @param rs        the reference system to assign to the new location types.
     * @param existing  an initially empty identity hash map for internal usage by this method.
     */
    static List<AbstractLocationType> snapshot(final Collection<? extends AbstractLocationType> types,
            final ReferencingByIdentifiers rs, final Map<AbstractLocationType, FinalLocationType> existing)
    {
        final AbstractLocationType[] array = types.toArray(AbstractLocationType[]::new);
        for (int i=0; i < array.length; i++) {
            final AbstractLocationType source = array[i];
            ArgumentChecks.ensureNonNullElement("types", i, source);
            FinalLocationType copy = existing.get(source);
            if (copy == null) {
                copy = new FinalLocationType(source, rs, existing);
            }
            array[i] = copy;
        }
        return List.of(array);
    }

    /**
     * Returns an unmodifiable copy of the given metadata, if necessary and possible.
     *
     * @param  metadata  the metadata object to eventually copy, or {@code null}.
     * @return an unmodifiable copy of the given metadata object, or {@code null} if the given argument is {@code null}.
     */
    private static Object unmodifiable(Object metadata) {
        if (metadata instanceof ModifiableMetadata) {
            metadata = MetadataCopier.forModifiable(((ModifiableMetadata) metadata).getStandard()).copy(metadata);
            if (metadata instanceof ModifiableMetadata) {
                ((ModifiableMetadata) metadata).transitionTo(ModifiableMetadata.State.FINAL);
            }
        }
        return metadata;
    }

    /**
     * Returns the name of the location type.
     *
     * <h4>Examples</h4>
     * “administrative area”, “town”, “locality”, “street”, “property”.
     *
     * @return name of the location type.
     */
    @Override
    public InternationalString getName() {
        return name;
    }

    /**
     * Returns the property used as the defining characteristic of the location type.
     *
     * <h4>Examples</h4>
     * <q>local administration</q> for administrative areas,
     * <q>built environment</q> for towns or properties,
     * <q>access</q> for streets,
     * <q>electoral</q>,
     * <q>postal</q>.
     *
     * @return property used as the defining characteristic of the location type.
     *
     * @see ReferencingByIdentifiers#getTheme()
     */
    @Override
    public InternationalString getTheme() {
        return theme;
    }

    /**
     * Returns the method(s) of uniquely identifying location instances.
     *
     * <h4>Examples</h4>
     * “name”, “code”, “unique street reference number”, “geographic address”.
     *
     * @return method(s) of uniquely identifying location instances.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Because unmodifiable
    public Collection<InternationalString> getIdentifications() {
        return identifications;
    }

    /**
     * Returns the way in which location instances are defined.
     *
     * @return the way in which location instances are defined.
     */
    @Override
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Returns the geographic area within which the location type occurs.
     *
     * <h4>Examples</h4>
     * the geographic domain for a location type “rivers” might be “North America”.
     *
     * @return geographic area within which the location type occurs.
     */
    @Override
    public GeographicExtent getTerritoryOfUse() {
        return territoryOfUse;
    }

    /**
     * Returns the reference system that comprises this location type.
     *
     * @return the reference system that comprises this location type.
     */
    @Override
    public ReferencingByIdentifiers getReferenceSystem() {
        return referenceSystem;
    }

    /**
     * Returns the name of organization or class of organization able to create and destroy location instances.
     *
     * @return organization or class of organization able to create and destroy location instances.
     */
    @Override
    public AbstractParty getOwner() {
        return owner;
    }

    /**
     * Returns the parent location types (location types of which this location type is a sub-division).
     * A location type can have more than one possible parent. For example, the parent of a location type named
     * <q>street</q> could be <q>locality</q>, <q>town</q> or <q>administrative area</q>.
     *
     * @return parent location types, or an empty collection if none.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Because unmodifiable
    public Collection<AbstractLocationType> getParents() {
        return parents;
    }

    /**
     * Returns the child location types (location types which sub-divides this location type).
     *
     * @return child location types, or an empty collection if none.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Because unmodifiable
    public Collection<AbstractLocationType> getChildren() {
        return children;
    }
}
