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
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Vocabulary;
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
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
     * Default vertical datum for {@linkplain VerticalDatumType#BAROMETRIC barometric heights}.
     */
    public static final DefaultVerticalDatum BAROMETRIC =
            new DefaultVerticalDatum(name(Vocabulary.Keys.BarometricAltitude), VerticalDatumType.BAROMETRIC);

    /**
     * Default vertical datum for {@linkplain VerticalDatumType#GEOIDAL geoidal heights}.
     */
    public static final DefaultVerticalDatum GEOIDAL =
            new DefaultVerticalDatum(name(Vocabulary.Keys.Geoidal), VerticalDatumType.GEOIDAL);

    /**
     * Default vertical datum for ellipsoidal heights. Ellipsoidal heights are measured
     * along the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p><b>This datum is not part of ISO 19111 international standard.</b>
     * Usage of this datum is generally not recommended since ellipsoidal heights make little sense without
     * their (<var>latitude</var>, <var>longitude</var>) locations. The ISO specification defines instead
     * three-dimensional {@code GeographicCRS} for that reason.</p>
     */
    public static final DefaultVerticalDatum ELLIPSOIDAL =
            new DefaultVerticalDatum(name(Vocabulary.Keys.Ellipsoidal), VerticalDatumType.valueOf("ELLIPSOIDAL"));
    // Do not use the VerticalDatumTypes.ELLIPSOIDAL constant in order to avoid unneeded class initialisation.

    /**
     * Default vertical datum for {@linkplain VerticalDatumType#OTHER_SURFACE other surface}.
     */
    public static final DefaultVerticalDatum OTHER_SURFACE =
            new DefaultVerticalDatum(name(Vocabulary.Keys.OtherSurface), VerticalDatumType.OTHER_SURFACE);

    /**
     * Constructs a vertical datum from a name.
     *
     * @param name The datum name.
     * @param type The type of this vertical datum.
     */
    public DefaultVerticalDatum(final String name, final VerticalDatumType type) {
        this(Collections.singletonMap(NAME_KEY, name), type);
    }

    /**
     * Constructs a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     */
    public DefaultVerticalDatum(final VerticalDatum datum) {
        super(datum);
        type = datum.getVerticalDatumType();
    }

    /**
     * Constructs a vertical datum from a set of properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     *
     * @param properties Set of properties. Should contains at least {@code "name"}.
     * @param type       The type of this vertical datum.
     */
    public DefaultVerticalDatum(final Map<String,?> properties, final VerticalDatumType type) {
        super(properties);
        this.type = type;
        ensureNonNull("type", type);
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
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final DefaultVerticalDatum that = (DefaultVerticalDatum) object;
                    return Objects.equals(this.type(), that.type());
                }
                default: {
                    if (!(object instanceof VerticalDatum)) break;
                    final VerticalDatum that = (VerticalDatum) object;
                    return Objects.equals(getVerticalDatumType(), that.getVerticalDatumType());
                }
            }
        }
        return false;
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
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
    public String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return "VERT_DATUM";
    }
}
