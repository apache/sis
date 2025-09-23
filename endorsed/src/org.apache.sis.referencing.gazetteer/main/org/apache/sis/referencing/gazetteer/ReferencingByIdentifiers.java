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
import java.util.logging.Logger;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.Quantity;
import javax.measure.IncommensurableException;
import org.opengis.util.InternationalString;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.iso.Types;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.system.Modules;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the main branch:
import org.apache.sis.metadata.iso.citation.AbstractParty;


/**
 * Base class of reference systems that describe locations using geographic identifiers instead of coordinates.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code ReferencingByIdentifiers} instances
 * created using only SIS factories and static constants can be shared by many objects and passed between threads
 * without synchronization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see ModifiableLocationType
 * @see AbstractLocation
 *
 * @since 0.8
 */
@XmlTransient
public abstract class ReferencingByIdentifiers extends AbstractReferenceSystem {
    /**
     * Key for the <code>{@value}</code> property to be given to the
     * object factory {@code createFoo(…)} methods.
     * This is used for setting the value to be returned by {@link #getTheme()}.
     *
     * @see #getTheme()
     */
    public static final String THEME_KEY = "theme";

    /**
     * Key for the <code>{@value}</code> property to be given to the
     * object factory {@code createFoo(…)} methods.
     * This is used for setting the value to be returned by {@link #getOverallOwner()}.
     *
     * @see #getOverallOwner()
     */
    public static final String OVERALL_OWNER_KEY = "overallOwner";

    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5353942546043471933L;

    /**
     * The logger for coordinate operations.
     */
    static final Logger LOGGER = Logger.getLogger(Modules.REFERENCING_BY_IDENTIFIERS);

    /**
     * Property used to characterize the spatial reference system.
     *
     * @see #getTheme()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final InternationalString theme;

    /**
     * Authority with overall responsibility for the spatial reference system.
     *
     * @see #getOverallOwner()
     */
    private final AbstractParty overallOwner;

    /**
     * Description of location type(s) in the spatial reference system.
     * This collection shall be unmodifiable.
     *
     * @see #getLocationTypes()
     */
    @SuppressWarnings("serial")
    final List<AbstractLocationType> locationTypes;

    /**
     * Creates a reference system from the given properties.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>"theme"</td>
     *     <td>{@link String} or {@link InternationalString}</td>
     *     <td>{@link #getTheme()}</td>
     *   </tr><tr>
     *     <td>"overallOwner"</td>
     *     <td>{@code Party}</td>
     *     <td>{@link #getOverallOwner()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getScope()}</td>
     *   </tr>
     * </table>
     *
     * This constructor copies the given {@code LocationType} instances as per
     * {@code ModifiableLocationType.snapshot(ReferenceSystemUsingIdentifiers, LocationType...)}.
     * Changes in the given location types after construction will not affect this {@code ReferencingByIdentifiers}.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * In a future SIS version, the type of array elements may be generalized to the
     * {@code org.opengis.referencing.gazetteer.LocationType} interface.
     * This change is pending GeoAPI revision.</div>
     *
     * @param properties  the properties to be given to the reference system.
     * @param types       description of location type(s) in the spatial reference system.
     */
    @SuppressWarnings("this-escape")
    public ReferencingByIdentifiers(final Map<String,?> properties, final ModifiableLocationType... types) {
        super(properties);
        theme = Types.toInternationalString(properties, THEME_KEY);
        overallOwner = Containers.property(properties, OVERALL_OWNER_KEY, AbstractParty.class);
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
     * @param id     an identifier for the reference system. Use SIS namespace until we find an authority for them.
     * @param party  the overall owner, or {@code null} if none.
     */
    static Map<String,Object> properties(final Object name, final String id, final AbstractParty party) {
        final Map<String,Object> properties = new HashMap<>(8);
        properties.put(NAME_KEY, name);
        properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(Citations.SIS, Constants.SIS, id));
        properties.put(DOMAIN_OF_VALIDITY_KEY, Extents.WORLD);
        properties.put(THEME_KEY, Vocabulary.formatInternational(Vocabulary.Keys.Mapping));
        properties.put(OVERALL_OWNER_KEY, party);
        return properties;
    }
    /**
     * Property used to characterize the spatial reference system.
     *
     * @return property used to characterize the spatial reference system.
     *
     * @see ModifiableLocationType#getTheme()
     */
    public InternationalString getTheme() {
        return theme;
    }

    /**
     * Authority with overall responsibility for the spatial reference system.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * in a future SIS version, the type of returned element may be generalized to the
     * {@code org.opengis.metadata.citation.Party} interface. This change is pending
     * GeoAPI revision for upgrade from ISO 19115:2003 to ISO 19115:2014.</div>
     *
     * @return authority with overall responsibility for the spatial reference system.
     *
     * @see ModifiableLocationType#getOwner()
     * @see AbstractLocation#getAdministrator()
     */
    public AbstractParty getOverallOwner() {
        return overallOwner;
    }

    /**
     * Description of location type(s) in the spatial reference system.
     * The collection returned by this method is unmodifiable.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * in a future SIS version, the type of elements type may be generalized to the
     * {@code org.opengis.referencing.gazetteer.Location} interface.
     * This change is pending GeoAPI revision.</div>
     *
     * @return description of location type(s) in the spatial reference system.
     *
     * @see ModifiableLocationType#getReferenceSystem()
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Because the collection is unmodifiable.
    public List<? extends ModifiableLocationType> getLocationTypes() {
        return ModifiableLocationTypeAdapter.copy(locationTypes);
    }

    /**
     * Returns the first location type.
     */
    final AbstractLocationType rootType() {
        return locationTypes.get(0);
    }

    /**
     * Returns a new object performing conversions between {@code DirectPosition} and identifiers.
     * The returned object is <strong>not</strong> thread-safe; a new instance must be created
     * for each thread, or synchronization must be applied by the caller.
     *
     * @return a new object performing conversions between {@link DirectPosition} and identifiers.
     *
     * @since 1.3
     */
    public abstract Coder createCoder();

    /**
     * Conversions between direct positions and identifiers.
     * Each {@code Coder} instance can read references at arbitrary precision,
     * but formats at the {@linkplain #setPrecision specified approximate precision}.
     * The same {@code Coder} instance can be reused for reading or writing many identifiers.
     *
     * <h2>Immutability and thread safety</h2>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.4
     * @since   1.3
     */
    public abstract static class Coder {
        /**
         * Creates a new instance.
         */
        protected Coder() {
        }

        /**
         * Returns the reference system for which this coder is reading or writing identifiers.
         *
         * @return the enclosing reference system.
         */
        public abstract ReferencingByIdentifiers getReferenceSystem();

        /**
         * Returns approximate precision of the identifiers formatted by this coder at the given location.
         * The returned value is typically a length in linear unit (e.g. metres).
         * Precisions in angular units should be converted to linear units at the specified location.
         * If the location is {@code null}, then this method should return a precision for the worst case scenario.
         *
         * @param  position  where to evaluate the precision, or {@code null} for the worst case scenario.
         * @return approximate precision in metres of formatted identifiers.
         */
        public abstract Quantity<?> getPrecision(DirectPosition position);

        /**
         * Sets the desired precision of the identifiers formatted by this coder.
         * The given value is converted to coder-specific representation (e.g. number of digits).
         * The value returned by {@link #getPrecision(DirectPosition)} may be different than the
         * value specified to this method.
         *
         * @param  precision  the desired precision.
         * @param  position   location where the specified precision is desired, or {@code null} for the worst case scenario.
         * @throws IncommensurableException if the given precision uses incompatible units of measurement.
         */
        public abstract void setPrecision(Quantity<?> precision, DirectPosition position) throws IncommensurableException;

        /**
         * A combined method which sets the encoder precision to the given value, then formats the given position.
         * The default implementation is equivalent to the following code:
         *
         * {@snippet lang="java" :
         *     setPrecision(precision, position);
         *     return encode(position);
         *     }
         *
         * Subclasses should override with more efficient implementation,
         * for example by transforming the given position only once.
         *
         * @param  position   the coordinate to encode.
         * @param  precision  the desired precision.
         * @return identifier of the given position.
         * @throws IncommensurableException if the given precision uses incompatible units of measurement.
         * @throws TransformException if an error occurred while transforming the given coordinate to an identifier.
         */
        public String encode(DirectPosition position, Quantity<?> precision) throws IncommensurableException, TransformException {
            setPrecision(precision, position);
            return encode(position);
        }

        /**
         * Encodes the given position into an identifier.
         * The given position must have a Coordinate Reference System (CRS) associated to it.
         *
         * @param  position  the coordinate to encode.
         * @return identifier of the given position.
         * @throws TransformException if an error occurred while transforming the given coordinate to an identifier.
         */
        public abstract String encode(DirectPosition position) throws TransformException;

        /**
         * Decodes the given identifier into a latitude and a longitude.
         * The axis order depends on the coordinate reference system of the enclosing {@link ReferencingByIdentifiers}.
         *
         * <div class="warning"><b>Upcoming API change — generalization</b><br>
         * in a future SIS version, the type of returned element may be generalized
         * to the {@code org.opengis.referencing.gazetteer.Location} interface.
         * This change is pending GeoAPI revision.</div>
         *
         * @param  identifier  identifier string to decode.
         * @return a new geographic coordinate for the given identifier.
         * @throws TransformException if an error occurred while parsing the given string.
         */
        public abstract AbstractLocation decode(CharSequence identifier) throws TransformException;

        /**
         * Logs a warning for a recoverable error while transforming a position. This is used for implementations
         * of method such as {@link #getPrecision(DirectPosition)}, which can fallback on "worst case" scenario.
         *
         * @param caller  the class that wanted to transform a position.
         * @param method  the method that wanted to transform a position.
         * @param e       the transformation error.
         */
        static void recoverableException(final Class<?> caller, final String method, final Exception e) {
            Logging.recoverableException(LOGGER, caller, method, e);
        }
    }

    /**
     * Compares this reference system with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties
     * are compared including the {@linkplain #getTheme() theme} and
     * the {@linkplain #getOverallOwner() overall owner}.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
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
                final ReferencingByIdentifiers that = (ReferencingByIdentifiers) object;
                if (!Utilities.deepEquals(getTheme(),        that.getTheme(),        mode) ||
                    !Utilities.deepEquals(getOverallOwner(), that.getOverallOwner(), mode))
                {
                    return false;
                }
                // Fall through
            }
            default: {
                // Theme and owner are metadata, so they can be ignored.
                final ReferencingByIdentifiers that = (ReferencingByIdentifiers) object;
                return Utilities.deepEquals(locationTypes, that.locationTypes, mode);
            }
        }
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(theme, overallOwner, locationTypes);
    }

    /**
     * Formats a pseudo-<i>Well Known Text</i> (WKT) representation for this object.
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
        for (final AbstractLocationType type : locationTypes) {
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
