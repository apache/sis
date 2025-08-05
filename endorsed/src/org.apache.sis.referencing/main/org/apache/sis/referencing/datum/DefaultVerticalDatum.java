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
import java.util.Objects;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.privy.LegacyNamespaces;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.metadata.privy.ImplementationHelper;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.datum.VerticalDatumType;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.pending.geoapi.referencing.DynamicReferenceFrame;


/**
 * Identifies a particular reference level surface used as a zero-height surface.
 * There are several types of vertical datums, and each may place constraints on the
 * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis coordinate system axis} with which
 * it is combined to create a {@linkplain org.opengis.referencing.crs.VerticalCRS vertical CRS}.
 *
 * <h2>Creating new vertical datum instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a vertical datum.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code VerticalDatum} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS.Vertical#datum()}.</li>
 *   <li>Create a {@code VerticalDatum} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createVerticalDatum(String)}.</li>
 *   <li>Create a {@code VerticalDatum} by invoking the {@code DatumFactory.createVerticalDatum(…)} method
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code DefaultVerticalDatum} by invoking the
 *       {@linkplain #DefaultVerticalDatum(Map, VerticalDatumType) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a vertical datum for height above the geoid:
 *
 * {@snippet lang="java" :
 *     VerticalDatum datum = CommonCRS.Vertical.GEOID.datum();
 *     }
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.CommonCRS.Vertical#datum()
 * @see org.apache.sis.referencing.cs.DefaultVerticalCS
 * @see org.apache.sis.referencing.crs.DefaultVerticalCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createVerticalDatum(String)
 *
 * @since 0.4
 */
@XmlType(name = "VerticalDatumType")
@XmlRootElement(name = "VerticalDatum")
public class DefaultVerticalDatum extends AbstractDatum implements VerticalDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 380347456670516572L;

    /**
     * The type of this vertical datum.
     * If {@code null}, a value will be inferred from the name by {@link #type()}.
     *
     * @see #type()
     * @see #getVerticalDatumType()
     */
    private VerticalDatumType type;

    /**
     * Creates a vertical datum from the given properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>"domains"</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getAnchorDefinition()}</td>
     *   </tr><tr>
     *     <td>{@code "anchorEpoch"}</td>
     *     <td>{@link java.time.temporal.Temporal}</td>
     *     <td>{@link #getAnchorEpoch()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  type        the type of this vertical datum.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createVerticalDatum(Map, VerticalDatumType)
     */
    public DefaultVerticalDatum(final Map<String,?> properties, final VerticalDatumType type) {
        super(properties);
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Creates a new datum with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  datum  the datum to copy.
     *
     * @see #castOrCopy(VerticalDatum)
     */
    protected DefaultVerticalDatum(final VerticalDatum datum) {
        super(datum);
        type = datum.getVerticalDatumType();
    }

    /**
     * Returns a SIS datum implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVerticalDatum castOrCopy(final VerticalDatum object) {
        return (object == null) || (object instanceof DefaultVerticalDatum) ?
                (DefaultVerticalDatum) object : new DefaultVerticalDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code VerticalDatum.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code VerticalDatum}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code VerticalDatum.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends VerticalDatum> getInterface() {
        return VerticalDatum.class;
    }

    /**
     * Returns the type of this datum, or infers the type from the datum name if no type were specified.
     * The latter case occurs after unmarshalling, since GML 3.2 does not contain any attribute for the datum type.
     * It may also happen if the datum were created using reflection.
     *
     * <p>This method uses heuristic rules and may be changed in any future SIS version.</p>
     *
     * <p>No synchronization needed; this is not a problem if this value is computed twice.
     * This method returns only existing immutable instances.</p>
     *
     * @see #getVerticalDatumType()
     * @see #getTypeElement()
     */
    private VerticalDatumType type() {
        VerticalDatumType t = type;
        if (t == null) {
            final ReferenceIdentifier name = super.getName();
            type = t = VerticalDatumTypes.fromDatum(name != null ? name.getCode() : null, super.getAlias(), null);
        }
        return t;
    }

    /**
     * Returns the type of this vertical datum.
     *
     * <h4>Historical note:</h4>
     * This property was defined in the ISO 19111 specification published in 2003,
     * but removed from the revision published 2007.
     * This property provides an information similar to the {@linkplain #getAnchorPoint() anchor definition},
     * but in a programmatic way more suitable to coordinate transformation engines.
     *
     * @return the type of this vertical datum.
     */
    @Override
    public VerticalDatumType getVerticalDatumType() {
        return type();
    }

    /**
     * A vertical reference frame in which some of the defining parameters have time dependency.
     * The parameter values are valid at the time given by the
     * {@linkplain #getFrameReferenceEpoch() frame reference epoch}.
     *
     * <div class="note"><b>Upcoming API change:</b>
     * this class may implement a {@code DynamicReferenceFrame} interface from the GeoAPI standard
     * after the next GeoAPI release. In the meantime, {@code DynamicReferenceFrame} is not a public API.</div>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.5
     * @since   1.5
     */
    public static class Dynamic extends DefaultVerticalDatum implements DynamicReferenceFrame {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -2047994195060747008L;

        /**
         * The epoch to which the definition of the dynamic reference frame is referenced.
         */
        @SuppressWarnings("serial")                     // Standard Java implementations are serializable.
        private final Temporal frameReferenceEpoch;

        /**
         * Creates a dynamic reference frame from the given properties.
         * See super-class constructor for more information.
         *
         * @param  properties  the properties to be given to the identified object.
         * @param  type        the type of this vertical datum.
         * @param  epoch       the epoch to which the definition of the dynamic reference frame is referenced.
         */
        public Dynamic(Map<String,?> properties, VerticalDatumType type, Temporal epoch) {
            super(properties, type);
            frameReferenceEpoch = Objects.requireNonNull(epoch);
        }

        /**
         * Creates a new datum with the same values as the specified datum, which must be dynamic.
         *
         * @param  datum  the datum to copy.
         * @throws ClassCastException if the given datum is not an instance of {@code DynamicReferenceFrame}.
         *
         * @see #castOrCopy(VerticalDatum)
         */
        protected Dynamic(final VerticalDatum datum) {
            super(datum);
            frameReferenceEpoch = Objects.requireNonNull(((DynamicReferenceFrame) datum).getFrameReferenceEpoch());
        }

        /**
         * Returns the epoch to which the coordinates of stations defining the dynamic reference frame are referenced.
         * The type of the returned object depends on the epoch accuracy and the calendar in use.
         * It may be merely a {@link java.time.Year}.
         *
         * @return the epoch to which the definition of the dynamic reference frame is referenced.
         */
        @Override
        public Temporal getFrameReferenceEpoch() {
            return frameReferenceEpoch;
        }

        /**
         * Compares the specified object with this datum for equality.
         *
         * @hidden because nothing new to said.
         */
        @Override
        public boolean equals(final Object object, final ComparisonMode mode) {
            return super.equals(object, mode) && (mode != ComparisonMode.STRICT ||
                    frameReferenceEpoch.equals(((Dynamic) object).frameReferenceEpoch));
        }

        /**
         * Invoked by {@code hashCode()} for computing the hash code when first needed.
         *
         * @hidden because nothing new to said.
         */
        @Override
        protected long computeHashCode() {
            return super.computeHashCode() + 31 * frameReferenceEpoch.hashCode();
        }
    }

    /**
     * Compares this vertical datum with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *                 {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only
     *                 properties relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                                                    // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final var other = (DefaultVerticalDatum) object;
                return type().equals(other.type());
            }
            case BY_CONTRACT: {
                final var other = (VerticalDatum) object;
                return Objects.equals(getVerticalDatumType(), other.getVerticalDatumType());
            }
            default: {
                /*
                 * VerticalDatumType is considered as metadata because it is related to the anchor definition,
                 * which is itself considered as metadata. Furthermore, GeodeticObjectParser and EPSGDataAccess
                 * do not always set this property to the same value, because of historical changes in the WKT.
                 */
                return true;
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
        return super.computeHashCode() + type().hashCode();
    }

    /**
     * Formats this datum as a <i>Well Known Text</i> {@code VerticalDatum[…]} element.
     *
     * <h4>Compatibility note</h4>
     * OGC 01-009 defined numerical codes for various vertical datum types, for example 2005 for geoidal height.
     * Such codes were formatted for all {@code Datum} subtypes in WKT 1. Datum types became specified only for
     * vertical datum in the ISO 19111:2003 standard, then removed completely in the ISO 19111:2007 standard.
     * They were reintroduced in a different form ({@code RealizationMethod}) in the ISO 19111:2019 standard.
     *
     * @return {@code "VerticalDatum"} (WKT 2) or {@code "Vert_Datum"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#71">WKT 2 specification §10.2</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.append(VerticalDatumTypes.toLegacyCode(type()));
            return WKTKeywords.Vert_Datum;
        }
        return formatter.shortOrLong(WKTKeywords.VDatum, WKTKeywords.VerticalDatum);
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
     * Constructs a new datum in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultVerticalDatum() {
    }

    /**
     * Returns the type to be marshalled to XML.
     * This element was present in GML 3.0 and 3.1, but has been removed from GML 3.2.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-160">SIS-160: Need XSLT between GML 3.1 and 3.2</a>
     */
    @XmlElement(name = "verticalDatumType")
    private VerticalDatumType getTypeElement() {
        return Context.isGMLVersion(Context.current(), LegacyNamespaces.VERSION_3_2) ? null : getVerticalDatumType();
    }

    /**
     * Invoked by JAXB only. The vertical datum type is set only if it has not already been specified.
     */
    private void setTypeElement(final VerticalDatumType t) {
        if (type == null) {
            type = t;
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultVerticalDatum.class, "setTypeElement", "verticalDatumType");
        }
    }
}
