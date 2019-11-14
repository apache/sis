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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ConcurrentModificationException;
import java.util.function.Function;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.GeographicExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.internal.gazetteer.Resources;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import org.opengis.metadata.citation.Party;
import org.opengis.referencing.gazetteer.ReferenceSystemUsingIdentifiers;


/**
 * Helper class for building the description of a location. Temporary instances of this class can be used
 * during the construction of <cite>spatial reference systems using geographic identifiers</cite>.
 * Since {@code ModifiableLocationType} instances are modifiable, they should not be published directly.
 * Instead, unmodifiable {@linkplain #snapshot snapshots} should be published.
 * The same {@code ModifiableLocationType} instance can be used for many snapshots.
 *
 * <div class="note"><b>Example:</b>
 * the following code creates 3 levels of location types: <var>administrative areas</var>, which contain
 * <var>towns</var>, which themselves contain <var>streets</var>. Note that the {@code street} location
 * type has two parents, {@code town} and {@code area}, because a street can be outside any town and
 * directly under the authority of an administrative area instead.
 *
 * {@preformat java
 *   ModifiableLocationType area   = new ModifiableLocationType("administrative area");
 *   ModifiableLocationType town   = new ModifiableLocationType("town");
 *   ModifiableLocationType street = new ModifiableLocationType("street");
 *
 *   area  .setTheme("local administration");
 *   town  .setTheme("built environment");
 *   street.setTheme("access");
 *
 *   area  .setDefinition("area of responsibility of highest level local authority");
 *   town  .setDefinition("city or town");
 *   street.setDefinition("thoroughfare providing access to properties");
 *
 *   town  .addParent(area);
 *   street.addParent(area);
 *   street.addParent(town);
 * }
 *
 * A string representation of the {@code area} location type is as below:
 *
 * {@preformat text
 *   administrative area………………… area of responsibility of highest level local authority
 *     ├─town……………………………………………… city or town
 *     │   └─street……………………………… thoroughfare providing access to properties
 *     └─street………………………………………… thoroughfare providing access to properties
 * }
 * </div>
 *
 * <h2>Inheritance of property values</h2>
 * According ISO 19112:2003, all properties except the collection of
 * {@linkplain #getParents() parents} and {@linkplain #getChildren() children} are mandatory.
 * Those mandatory properties are the {@linkplain #getName() name}, {@linkplain #getTheme() theme},
 * {@linkplain #getIdentifications() identifications}, {@linkplain #getDefinition() definition},
 * {@linkplain #getTerritoryOfUse() territory of use} and {@linkplain #getOwner() owner}.
 * However in Apache SIS implementation, only the name is truly mandatory;
 * SIS is tolerant to missing value for all other properties.
 * But in the hope to improve ISO compliance, values of undefined properties are inherited
 * from the parents (if any) provided that all parents define the same values.
 *
 * <div class="note"><b>Example:</b>
 * if an <var>administrative area</var> is in some {@linkplain #getTerritoryOfUse() territory of use},
 * then all children of the administrative area (namely <var>towns</var> and <var>streets</var>) can
 * reasonably presumed to be in the same territory of use. That territory can be specified only once
 * as below:
 *
 * {@preformat java
 *   area.setTerritoryOfUse("Japan");
 * }
 *
 * Then, the towns and streets automatically inherit the same value for that property,
 * unless they are explicitly given another value.</div>
 *
 * <h2>Limitation</h2>
 * This class is not serializable and is not thread-safe. For thread safety or for serialization,
 * a {@linkplain #snapshot snapshots} of this location type should be taken.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see AbstractLocation
 * @see ReferencingByIdentifiers
 *
 * @since 0.8
 * @module
 */
public class ModifiableLocationType extends AbstractLocationType {      // Not Serializable on intent.
    /**
     * Name of the location type.
     */
    private final InternationalString name;

    /**
     * Property used as the defining characteristic of the location type.
     */
    private InternationalString theme;

    /**
     * Method(s) of uniquely identifying location instances.
     */
    private final Map<String, InternationalString> identifications;

    /**
     * The way in which location instances are defined.
     */
    private InternationalString definition;

    /**
     * Geographic area within which the location type occurs.
     */
    private GeographicExtent territoryOfUse;

    /**
     * Name of organization or class of organization able to create and destroy location instances.
     */
    private Party owner;

    /**
     * Parent location types (location types of which this location type is a sub-division).
     */
    private final Map<String, ModifiableLocationType> parents;

    /**
     * Child location types (location types which sub-divides this location type).
     */
    private final Map<String, ModifiableLocationType> children;

    /**
     * Creates a new location type of the given name.
     *
     * @param name  the location type name.
     */
    public ModifiableLocationType(final CharSequence name) {
        ArgumentChecks.ensureNonNull("name", name);
        this.name       = Types.toInternationalString(name);
        identifications = new LinkedHashMap<>();
        parents         = new LinkedHashMap<>();
        children        = new LinkedHashMap<>();
    }

    /**
     * Returns the name of the location type.
     * This name is specified at construction time and can not be changed.
     *
     * <div class="note"><b>Examples:</b>
     * “administrative area”, “town”, “locality”, “street”, “property”.</div>
     *
     * @return name of the location type.
     */
    @Override
    public InternationalString getName() {
        return name;
    }

    /**
     * If all parents return the same value for the given property, returns that value.
     * Otherwise returns {@code null}.
     */
    private <E> E inherit(final Function<ModifiableLocationType, E> property) {
        E common = null;
        for (final ModifiableLocationType parent : parents.values()) {
            final E value = property.apply(parent);
            if (value != null) {
                if (common == null) {
                    common = value;
                } else if (!value.equals(common)) {
                    return null;
                }
            }
        }
        return common;
    }

    /**
     * Returns the property used as the defining characteristic of the location type.
     * If no theme has been explicitly set, then this method inherits the value from
     * the parents providing that all parents specify the same theme.
     *
     * @return property used as the defining characteristic of the location type,
     *         or {@code null} if no value has been defined or can be inherited.
     *
     * @see ReferencingByIdentifiers#getTheme()
     */
    @Override
    public InternationalString getTheme() {
        return (theme != null) ? theme : inherit(ModifiableLocationType::getTheme);
    }

    /**
     * Sets the property used as the defining characteristic of the location type.
     *
     * <div class="note"><b>Examples:</b>
     * <cite>“local administration”</cite> for administrative areas,
     * <cite>“built environment”</cite> for towns or properties,
     * <cite>“access”</cite> for streets,
     * <cite>“electoral”</cite>,
     * <cite>“postal”</cite>.</div>
     *
     * @param  value  the new theme, or {@code null} for inheriting a value from the parents.
     */
    public void setTheme(final CharSequence value) {
        theme = Types.toInternationalString(value);
    }

    /**
     * Returns the method(s) of uniquely identifying location instances.
     * If no methods have been explicitly set, then this method inherits the values from
     * the parents providing that all parents specify the same methods.
     *
     * <div class="note"><b>Examples:</b>
     * some identification methods are “name”, “code”, “unique street reference number” and “geographic address”.
     * A location using “name” identifications may have the “Spain” {@linkplain AbstractLocation#getGeographicIdentifier()
     * geographic identifier}, and a location using “postcode” identifications may have the “SW1P 3AD” geographic identifier.
     * </div>
     *
     * The collection returned by this method is unmodifiable. For adding or removing an identification,
     * use {@link #addIdentification(CharSequence)} or {@link #removeIdentification(CharSequence)}.
     *
     * @return method(s) of uniquely identifying location instances,
     *         or an empty list if no value has been defined or can be inherited.
     *
     * @see AbstractLocation#getGeographicIdentifier()
     */
    @Override
    public Collection<InternationalString> getIdentifications() {
        return identifications.isEmpty() ? inherit(ModifiableLocationType::getIdentifications)
                : Collections.unmodifiableCollection(identifications.values());
    }

    /**
     * Adds a method of uniquely identifying location instances.
     *
     * <div class="note"><b>Examples:</b>
     * “name”, “code”, “unique street reference number”, “geographic address”.</div>
     *
     * @param  value  the method to add.
     * @throws IllegalArgumentException if the given value is already defined.
     */
    public void addIdentification(final CharSequence value) {
        ArgumentChecks.ensureNonNull("value", value);
        final String key = value.toString();
        if (identifications.putIfAbsent(key, Types.toInternationalString(value)) != null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, key));
        }
    }

    /**
     * Removes a method of uniquely identifying location instances.
     *
     * @param  value  the method to remove.
     * @throws IllegalArgumentException if the given value is not found.
     */
    public void removeIdentification(final CharSequence value) {
        ArgumentChecks.ensureNonNull("value", value);
        final String key = value.toString();
        if (identifications.remove(key) == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementNotFound_1, key));
        }
    }

    /**
     * Returns the way in which location instances are defined.
     * If no definition has been explicitly set, then this method inherits the value from
     * the parents providing that all parents specify the same definition.
     *
     * @return the way in which location instances are defined,
     *         or {@code null} if no value has been defined or can be inherited.
     */
    @Override
    public InternationalString getDefinition() {
        return (definition != null) ? definition : inherit(ModifiableLocationType::getDefinition);
    }

    /**
     * Sets the way in which location instances are defined.
     *
     * @param  value  the new identification.
     */
    public void setDefinition(final CharSequence value) {
        definition = Types.toInternationalString(value);
    }

    /**
     * Returns the geographic area within which the location type occurs.
     * If no geographic area has been explicitly set, then this method inherits the value from
     * the parents providing that all parents specify the same geographic area.
     *
     * @return geographic area within which the location type occurs,
     *         or {@code null} if no value has been defined or can be inherited.
     *
     * @see ReferencingByIdentifiers#getDomainOfValidity()
     */
    @Override
    public GeographicExtent getTerritoryOfUse() {
        return (territoryOfUse != null) ? territoryOfUse : inherit(ModifiableLocationType::getTerritoryOfUse);
    }

    /**
     * Sets the geographic area within which the location type occurs. The given value is typically an instance of
     * {@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox}. For an alternative where only the
     * territory name is specified, see {@link #setTerritoryOfUse(String)}.
     *
     * @param  value  the new geographic extent.
     */
    public void setTerritoryOfUse(final GeographicExtent value) {
        territoryOfUse = value;
    }

    /**
     * Sets the name of the geographic area within which the location type occurs.
     *
     * <div class="note"><b>Examples:</b>
     * the geographic domain for a location type “rivers” might be “North America”.</div>
     *
     * @param  identifier  the identifier of the geographic extent.
     */
    public void setTerritoryOfUse(final String identifier) {
        territoryOfUse = (identifier != null) ? new DefaultGeographicDescription(null, identifier) : null;
    }

    /**
     * Returns the name of organization or class of organization able to create and destroy location instances.
     * If no organization has been explicitly set, then this method inherits the value from
     * the parents providing that all parents specify the same organization.
     *
     * @return organization or class of organization able to create and destroy location instances,
     *         or {@code null} if no value has been defined or can be inherited.
     *
     * @see AbstractLocation#getAdministrator()
     * @see ReferencingByIdentifiers#getOverallOwner()
     */
    @Override
    public Party getOwner() {
        return (owner != null) ? owner : inherit(ModifiableLocationType::getOwner);
    }

    /**
     * Sets the organization or class of organization able to create and destroy location instances.
     * The given value is typically an instance of {@link DefaultOrganisation}.
     * For an alternative where only the organization name is specified, see {@link #setOwner(CharSequence)}.
     *
     * @param  value  the new owner.
     */
    public void setOwner(final Party value) {
        owner = value;
    }

    /**
     * Sets the name of the organization or class of organization able to create and destroy location instances.
     *
     * @param  name  the organization name.
     */
    public void setOwner(final CharSequence name) {
        owner = (name != null) ? new DefaultOrganisation(name, null, null, null) : null;
    }

    /**
     * Returns the parent location types (location types of which this location type is a sub-division).
     * A location type can have more than one possible parent. For example the parent of a location type named
     * <cite>“street”</cite> could be <cite>“locality”</cite>, <cite>“town”</cite> or <cite>“administrative area”</cite>.
     *
     * <p>The collection returned by this method is unmodifiable. For adding or removing a parent,
     * use {@link #addParent(ModifiableLocationType)} or {@link #removeParent(ModifiableLocationType)}.</p>
     *
     * @return parent location types, or an empty collection if none.
     *
     * @see AbstractLocation#getParents()
     */
    @Override
    public final Collection<ModifiableLocationType> getParents() {
        return Collections.unmodifiableCollection(parents.values());
    }

    /**
     * Returns the child location types (location types which sub-divides this location type).
     * The collection returned by this method is unmodifiable. For adding or removing a child,
     * use <code>child.{@linkplain #addParent addParent}(this)</code>
     * or  <code>child.{@linkplain #removeParent removeParent}(this)</code>.
     *
     * @return child location types, or an empty collection if none.
     *
     * @see AbstractLocation#getChildren()
     */
    @Override
    public final Collection<ModifiableLocationType> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    /**
     * Adds the given element to the list of parents.
     *
     * @param  parent  the parent to add.
     * @throws IllegalStateException if this location type already have a parent of the same name.
     * @throws IllegalArgumentException if the given parent already have a child of the same name than this location type.
     */
    public void addParent(final ModifiableLocationType parent) {
        ArgumentChecks.ensureNonNull("parent", parent);
        final String parentName = parent.name.toString();
        if (parents.putIfAbsent(parentName, parent) != null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.ParentAlreadyExists_1, parentName));
        }
        final String childName = name.toString();
        if (parent.children.putIfAbsent(childName, this) != null) {
            if (parents.remove(parentName) != parent) {
                throw new ConcurrentModificationException();                    // Paranoiac check.
            }
            throw new IllegalArgumentException(Resources.format(Resources.Keys.ChildAlreadyExists_1, parentName));
        }
        /*
         * Following verification is not very efficient (lot of verifications are repeated every time that a new
         * parent is added), and is redundant with the verification performed by snapshot(…) method.  For now we
         * perform this verification here on the assumption that the tree is small, and that reporting errors
         * early make development easier. However if performance become a concern, we could remove this code
         * and perform the verification when first needed in getParents() and getChildren(), or make equals(…)
         * and toString() methods tolerance to cycles.
         */
        try {
            parent.checkForCycles();
        } catch (IllegalArgumentException e) {
            parent.children.remove(childName);                                  // Rollback
            parents.remove(parentName);
            throw e;
        }
    }

    /**
     * Removes the given element from the list of parent.
     *
     * @param  parent  the parent to remove.
     * @throws IllegalArgumentException if the given parent has not been found.
     */
    public void removeParent(final ModifiableLocationType parent) {
        ArgumentChecks.ensureNonNull("parent", parent);
        final String key = parent.name.toString();
        final ModifiableLocationType removed = parents.remove(key);
        if (removed == null) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.LocationTypeNotFound_1, key));
        }
        if (removed.children.remove(name.toString()) != this || removed != parent) {
            throw new CorruptedObjectException();
        }
    }

    /**
     * Returns the reference system that comprises this location type. For {@code ModifiableLocationType}s,
     * the reference system is always null. The reference system is defined when the location types are
     * given to the {@link ReferencingByIdentifiers} constructor for example.
     *
     * @return {@code null}.
     *
     * @see ReferencingByIdentifiers#getLocationTypes()
     */
    @Override
    public ReferenceSystemUsingIdentifiers getReferenceSystem() {
        return null;
    }
}
