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
import java.util.Objects;
import java.util.HashMap;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.InternationalString;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Debug;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.resources.Vocabulary;

// Branch-dependent imports
import org.opengis.metadata.citation.Party;
import org.opengis.referencing.gazetteer.LocationType;
import org.opengis.referencing.gazetteer.ReferenceSystemUsingIdentifiers;


/**
 * Base class of reference systems that describe locations using geographic identifiers instead than coordinates.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code ReferencingByIdentifiers} instances
 * created using only SIS factories and static constants can be shared by many objects and passed between threads
 * without synchronization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see ModifiableLocationType
 * @see AbstractLocation
 *
 * @since 0.8
 * @module
 */
@XmlTransient
public class ReferencingByIdentifiers extends AbstractReferenceSystem implements ReferenceSystemUsingIdentifiers {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5353942546043471933L;

    /**
     * Property used to characterize the spatial reference system.
     *
     * @see #getTheme()
     */
    private final InternationalString theme;

    /**
     * Authority with overall responsibility for the spatial reference system.
     *
     * @see #getOverallOwner()
     */
    private final Party overallOwner;

    /**
     * Description of location type(s) in the spatial reference system.
     * This collection shall be unmodifiable.
     *
     * @see #getLocationTypes()
     */
    private final List<LocationType> locationTypes;

    /**
     * Creates a reference system from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.gazetteer.ReferenceSystemUsingIdentifiers#THEME_KEY}</td>
     *     <td>{@link String} or {@link InternationalString}</td>
     *     <td>{@link #getTheme()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.gazetteer.ReferenceSystemUsingIdentifiers#OVERALL_OWNER_KEY}</td>
     *     <td>{@link Party}</td>
     *     <td>{@link #getOverallOwner()}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * This constructor copies the given {@link LocationType} instances as per
     * {@link ModifiableLocationType#snapshot(ReferenceSystemUsingIdentifiers, LocationType...)}.
     * Changes in the given location types after construction will not affect this {@code ReferencingByIdentifiers}.
     *
     * @param properties  the properties to be given to the reference system.
     * @param types       description of location type(s) in the spatial reference system.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public ReferencingByIdentifiers(final Map<String,?> properties, final LocationType... types) {
        super(properties);
        theme = Types.toInternationalString(properties, THEME_KEY);
        overallOwner = Containers.property(properties, OVERALL_OWNER_KEY, Party.class);
        /*
         * Having the 'this' reference escaped in object construction should not be an issue here because
         * we invoke package-private method in such a way that if an exception is thrown, the whole tree
         * (with all 'this' references) will be discarded.
         */
        locationTypes = AbstractLocationType.snapshot(this, types);
    }

    /**
     * Convenience method for helping subclasses to build their argument for the constructor.
     * The returned properties have the domain of validity set to the whole word and the theme to "mapping".
     *
     * @param name   the reference system name as an {@link org.opengis.metadata.Identifier} or a {@link String}.
     * @param party  the overall owner, or {@code null} if none.
     */
    static Map<String,Object> properties(final Object name, final Party party) {
        final Map<String,Object> properties = new HashMap<>(6);
        properties.put(NAME_KEY, name);
        properties.put(DOMAIN_OF_VALIDITY_KEY, Extents.WORLD);
        properties.put(THEME_KEY, Vocabulary.formatInternational(Vocabulary.Keys.Mapping));
        properties.put(OVERALL_OWNER_KEY, party);
        return properties;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code ReferenceSystemUsingIdentifiers.class}.
     *
     * @return the GeoAPI interface implemented by this class.
     */
    @Override
    public Class<? extends ReferenceSystemUsingIdentifiers> getInterface() {
        return ReferenceSystemUsingIdentifiers.class;
    }

    /**
     * Property used to characterize the spatial reference system.
     *
     * @return property used to characterize the spatial reference system.
     *
     * @see ModifiableLocationType#getTheme()
     */
    @Override
    public InternationalString getTheme() {
        return theme;
    }

    /**
     * Authority with overall responsibility for the spatial reference system.
     *
     * @return authority with overall responsibility for the spatial reference system.
     *
     * @see ModifiableLocationType#getOwner()
     * @see AbstractLocation#getAdministrator()
     */
    @Override
    public Party getOverallOwner() {
        return overallOwner;
    }

    /**
     * Description of location type(s) in the spatial reference system.
     * The collection returned by this method is unmodifiable.
     *
     * @return description of location type(s) in the spatial reference system.
     *
     * @see ModifiableLocationType#getReferenceSystem()
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Because the collection is unmodifiable.
    public List<? extends LocationType> getLocationTypes() {
        return locationTypes;
    }

    /**
     * Returns the first location type.
     */
    final LocationType rootType() {
        return locationTypes.get(0);
    }

    /**
     * Compares this reference system with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties are
     * compared including the {@linkplain #getTheme() theme} and
     * the {@linkplain #getOverallOwner() overall owner}.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *                 {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only
     *                 properties relevant to location identifications.
     * @return {@code true} if both objects are equal.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final ReferencingByIdentifiers that = (ReferencingByIdentifiers) object;
                return Objects.equals(theme,        that.theme) &&
                       Objects.equals(overallOwner, that.overallOwner) &&
                       locationTypes.equals(that.locationTypes);
            }
            case BY_CONTRACT: {
                final ReferenceSystemUsingIdentifiers that = (ReferenceSystemUsingIdentifiers) object;
                if (!Utilities.deepEquals(getTheme(),        that.getTheme(),        mode) ||
                    !Utilities.deepEquals(getOverallOwner(), that.getOverallOwner(), mode))
                {
                    return false;
                }
                // Fall through
            }
            default: {
                // Theme and owner are metadata, so they can be ignored.
                final ReferenceSystemUsingIdentifiers that = (ReferenceSystemUsingIdentifiers) object;
                return Utilities.deepEquals(getLocationTypes(), that.getLocationTypes(), mode);
            }
        }
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return the hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(theme, overallOwner, locationTypes);
    }

    /**
     * Formats a pseudo-<cite>Well Known Text</cite> (WKT) representation for this object.
     * The format produced by this method is non-standard and may change in any future Apache SIS version.
     *
     * @param  formatter  the formatter where to format the inner content of this pseudo-WKT element.
     * @return an arbitrary keyword for the pseudo-WKT element.
     */
    @Debug
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, ElementKind.NAME);
        if (theme != null) {
            formatter.newLine();
            formatter.append(new SubElement("Theme", theme));
        }
        if (overallOwner != null) {
            formatter.newLine();
            formatter.append(new SubElement("Owner", overallOwner.getName()));
        }
        for (final LocationType type : locationTypes) {
            formatter.newLine();
            formatter.append(new SubElement("LocationType", type.getName()));
        }
        return "ReferenceSystemUsingIdentifiers";
    }

    /**
     * A sub-element inside the pseudo-WKT.
     */
    private static final class SubElement extends FormattableObject {
        /** The pseudo-WKT name of the element to format. */
        private final String name;

        /** The value of the element to format. */
        private final InternationalString value;

        /** Creates a new citation with the given value. */
        SubElement(final String name, final InternationalString value) {
            this.name  = name;
            this.value = value;
        }

        /** Formats the sub-element. */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(value != null ? value.toString(formatter.getLocale()) : null, null);
            return name;
        }
    }
}
