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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.gazetteer.LocationType;
import org.opengis.referencing.gazetteer.ReferenceSystemUsingIdentifiers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Types;


/**
 * Description of a location to be used as a template in reference system construction.
 * This object can be used as a {@link LocationType} builder.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class LocationTypeTemplate implements LocationType {
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
    private final List<InternationalString> identifications;

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
    private final Map<String, LocationTypeTemplate> parents;

    /**
     * Child location types (location types which sub-divides this location type).
     */
    private final Map<String, LocationTypeTemplate> children;

    /**
     * Creates a new location type of the given name.
     *
     * @param name the location type name.
     */
    public LocationTypeTemplate(final CharSequence name) {
        ArgumentChecks.ensureNonNull("name", name);
        this.name       = Types.toInternationalString(name);
        identifications = new ArrayList<>();
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
     * Returns the property used as the defining characteristic of the location type.
     *
     * <div class="note"><b>Examples:</b>
     * <cite>“local administration”</cite> for administrative areas,
     * <cite>“built environment”</cite> for towns or properties,
     * <cite>“access”</cite> for streets,
     * <cite>“electoral”</cite>,
     * <cite>“postal”</cite>.</div>
     *
     * @return property used as the defining characteristic of the location type.
     *
     * @see ReferenceSystemUsingIdentifiers#getTheme()
     */
    @Override
    public InternationalString getTheme() {
        return theme;
    }

    /**
     * Sets the property used as the defining characteristic of the location type.
     *
     * @param  value  the new theme.
     */
    public void setTheme(final CharSequence value) {
        theme = Types.toInternationalString(value);
    }

    /**
     * Returns the method(s) of uniquely identifying location instances.
     *
     * <div class="note"><b>Examples:</b>
     * “name”, “code”, “unique street reference number”, “geographic address”.</div>
     *
     * The list returned by this method is <cite>live</cite>; changes in that list
     * will be reflected immediately in this {@code LocationTypeTemplate} and conversely.
     *
     * @return method(s) of uniquely identifying location instances.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<InternationalString> getIdentifications() {
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
     * Sets the way in which location instances are defined.
     *
     * @param  value  the new identification.
     */
    public void setDefinition(final InternationalString value) {
        definition = value;
    }

    /**
     * Returns the geographic area within which the location type occurs.
     *
     * <div class="note"><b>Examples:</b>
     * the geographic domain for a location type “rivers” might be “North America”.</div>
     *
     * @return geographic area within which the location type occurs.
     */
    @Override
    public GeographicExtent getTerritoryOfUse() {
        return territoryOfUse;
    }

    /**
     * Sets the geographic area within which the location type occurs.
     *
     * @param  value  the new geographic extent.
     */
    public void setTerritoryOfUse(final GeographicExtent value) {
        territoryOfUse = value;
    }

    /**
     * Returns the reference system that comprises this location type.
     * For a {@code LocationTypeTemplate}, the reference system is always null.
     * The reference system is set when the template is given to {@link ReferencingByIdentifiers} constructor.
     *
     * @return {@code null}.
     */
    @Override
    public ReferenceSystemUsingIdentifiers getReferenceSystem() {
        return null;
    }

    /**
     * Returns the name of organization or class of organization able to create and destroy location instances.
     *
     * @return organization or class of organization able to create and destroy location instances.
     */
    @Override
    public Party getOwner() {
        return owner;
    }

    /**
     * Sets the organization or class of organization able to create and destroy location instances.
     *
     * @param  value  the new owner.
     */
    public void setOwner(final Party value) {
        owner = value;
    }

    /**
     * Returns the parent location types (location types of which this location type is a sub-division).
     * A location type can have more than one possible parent. For example the parent of a location type named
     * <cite>“street”</cite> could be <cite>“locality”</cite>, <cite>“town”</cite> or <cite>“administrative area”</cite>.
     *
     * @return parent location types, or an empty collection if none.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<LocationTypeTemplate> getParents() {
        return parents.values();
    }

    /**
     * Returns the child location types (location types which sub-divides this location type).
     *
     * @return child location types, or an empty collection if none.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<LocationTypeTemplate> getChildren() {
        return children.values();
    }
}
