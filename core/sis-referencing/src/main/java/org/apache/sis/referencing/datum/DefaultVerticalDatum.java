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
package org.apache.sis.referencing.datum;

import java.util.Map;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gml.GMLAdapter;
import org.apache.sis.internal.referencing.VerticalDatumTypes;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import java.util.Objects;


/**
 * Identifies a particular reference level surface used as a zero-height surface.
 * There are several types of vertical datums, and each may place constraints on the
 * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis coordinate system axis} with which
 * it is combined to create a {@linkplain org.opengis.referencing.crs.VerticalCRS vertical CRS}.
 *
 * {@section Creating new vertical datum instances}
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a vertical datum.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code VerticalDatum} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.GeodeticObjects.Vertical#datum()}.</li>
 *   <li>Create a {@code VerticalDatum} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createVerticalDatum(String)}.</li>
 *   <li>Create a {@code VerticalDatum} by invoking the {@code createVerticalDatum(…)}
 *       method defined in the {@link org.opengis.referencing.datum.DatumFactory} interface.</li>
 *   <li>Create a {@code DefaultVerticalDatum} by invoking the
 *       {@linkplain #DefaultVerticalDatum(Map, VerticalDatumType) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a vertical datum for height above the geoid:
 *
 * {@preformat java
 *     VerticalDatum datum = GeodeticObjects.Vertical.GEOID.datum();
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.GeodeticObjects.Vertical#datum()
 */
@Immutable
@XmlType(name = "VerticalDatumType")
@XmlRootElement(name = "VerticalDatum")
public class DefaultVerticalDatum extends AbstractDatum implements VerticalDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 380347456670516572L;

    /**
     * The type of this vertical datum. Consider this field as final.
     * If {@code null}, a value will be inferred from the name by {@link #type()}.
     */
    private VerticalDatumType type;

    /**
     * Creates a vertical datum from the given properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getAnchorPoint()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#REALIZATION_EPOCH_KEY}</td>
     *     <td>{@link java.util.Date}</td>
     *     <td>{@link #getRealizationEpoch()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the identified object.
     * @param type       The type of this vertical datum.
     */
    public DefaultVerticalDatum(final Map<String,?> properties, final VerticalDatumType type) {
        super(properties);
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Creates a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     *
     * @see #castOrCopy(VerticalDatum)
     */
    protected DefaultVerticalDatum(final VerticalDatum datum) {
        super(datum);
        type = datum.getVerticalDatumType();
    }

    /**
     * Returns a SIS datum implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVerticalDatum castOrCopy(final VerticalDatum object) {
        return (object == null) || (object instanceof DefaultVerticalDatum) ?
                (DefaultVerticalDatum) object : new DefaultVerticalDatum(object);
    }

    /**
     * Returns the type of this datum, or infers the type from the datum name if no type were specified.
     * The later case occurs after unmarshalling, since GML 3.2 does not contain any attribute for the datum type.
     * It may also happen if the datum were created using reflection.
     *
     * <p>This method uses heuristic rules and may be changed in any future SIS version. If the type can not be
     * determined, default on the ellipsoidal type since it will usually implies no additional calculation.</p>
     *
     * <p>No synchronization needed; this is not a problem if this value is computed twice.
     * This method returns only existing immutable instances.</p>
     */
    private VerticalDatumType type() {
        VerticalDatumType t = type;
        if (t == null) {
            type = t = VerticalDatumTypes.guess(this);
        }
        return t;
    }

    /**
     * Returns the type of this vertical datum.
     *
     * @return The type of this vertical datum.
     */
    @Override
    public VerticalDatumType getVerticalDatumType() {
        return type();
    }

    /**
     * Returns the legacy code for the datum type, or 0 if none.
     *
     * @see #getVerticalDatumType()
     */
    @Override
    final int getLegacyDatumType() {
        return VerticalDatumTypes.toLegacy(getVerticalDatumType().ordinal());
    }

    /**
     * Returns the type to be marshalled to XML.
     * This element was present in GML 3.0 and 3.1, but has been removed from GML 3.2.
     */
    @XmlElement(name = "verticalDatumType")
    private VerticalDatumType getMarshalled() {
        return (Context.isGMLVersion(Context.current(), GMLAdapter.GML_3_2)) ? null : getVerticalDatumType();
    }

    /**
     * Invoked by JAXB only. The vertical datum type is set only if it has not already been specified.
     */
    private void setMarshalled(final VerticalDatumType t) {
        if (type != null) {
            throw new IllegalStateException();
        }
        type = t;
    }

    /**
     * Compare this vertical datum with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (!(object instanceof VerticalDatum && super.equals(object, mode))) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                return Objects.equals(type(), ((DefaultVerticalDatum) object).type());
            }
            default: {
                return Objects.equals(getVerticalDatumType(), ((VerticalDatum) object).getVerticalDatumType());
            }
        }
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     *
     * @return The hash code value for the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        /*
         * The "^ (int) serialVersionUID" is an arbitrary change applied to the hash code value in order to
         * differentiate this VerticalDatum implementation from implementations of other GeoAPI interfaces.
         */
        int code = super.hashCode(mode) ^ (int) serialVersionUID;
        code += Objects.hashCode(mode == ComparisonMode.STRICT ? type() : getVerticalDatumType());
        return code;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT) element.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "VERT_DATUM"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return "VERT_DATUM";
    }
}
