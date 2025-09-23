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

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.xml.bind.gml.TemporalAdapter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;


/**
 * Defines the origin of a temporal coordinate reference system.
 *
 * <h2>Creating new temporal datum instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a temporal datum.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code TemporalDatum} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS.Temporal#datum()}.</li>
 *   <li>Create a {@code TemporalDatum} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createTemporalDatum(String)}.</li>
 *   <li>Create a {@code TemporalDatum} by invoking the {@code DatumFactory.createTemporalDatum(…)} method,
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code DefaultTemporalDatum} by invoking the
 *       {@linkplain #DefaultTemporalDatum(Map, Date) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a temporal datum having its origin at January 1st, 4713 BC at 12:00 UTC:
 *
 * {@snippet lang="java" :
 *     TemporalDatum datum = CommonCRS.Temporal.JULIAN.datum();
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
 * @see org.apache.sis.referencing.CommonCRS.Temporal#datum()
 * @see org.apache.sis.referencing.cs.DefaultTimeCS
 * @see org.apache.sis.referencing.crs.DefaultTemporalCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createTemporalDatum(String)
 *
 * @since 0.4
 */
@XmlType(name = "TemporalDatumType")
@XmlRootElement(name = "TemporalDatum")
public class DefaultTemporalDatum extends AbstractDatum implements TemporalDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1507650596130032757L;

    /**
     * The date and time origin of this temporal datum, or {@code null} if none.
     * This information is mandatory, but SIS is tolerant to missing value
     * is case a XML fragment was incomplete.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time.</p>
     */
    @SuppressWarnings("serial")         // Most implementations are serializable.
    @XmlSchemaType(name = "dateTime")
    @XmlElement(name = "origin", required = true)
    @XmlJavaTypeAdapter(TemporalAdapter.class)
    private Temporal origin;

    /**
     * Creates a temporal datum from the given properties. The properties map is given
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
     *     <td>{@link Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getAnchorDefinition()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_EPOCH_KEY}</td>
     *     <td>{@link java.time.temporal.Temporal}</td>
     *     <td>{@link #getAnchorEpoch()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  origin      the date and time origin of this temporal datum.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createTemporalDatum(Map, Temporal)
     *
     * @since 1.5
     */
    public DefaultTemporalDatum(final Map<String,?> properties, final Temporal origin) {
        super(properties);
        this.origin = origin;
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
     * @see #castOrCopy(TemporalDatum)
     */
    protected DefaultTemporalDatum(final TemporalDatum datum) {
        super(datum);
        origin = datum.getOrigin();
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
    public static DefaultTemporalDatum castOrCopy(final TemporalDatum object) {
        return (object == null) || (object instanceof DefaultTemporalDatum) ?
                (DefaultTemporalDatum) object : new DefaultTemporalDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code TemporalDatum.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code TemporalDatum}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code TemporalDatum.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends TemporalDatum> getInterface() {
        return TemporalDatum.class;
    }

    /**
     * Returns the date and time origin of this temporal datum.
     *
     * @return the date and time origin of this temporal datum.
     */
    @Override
    public Temporal getOrigin() {
        return origin;
    }

    /**
     * Compares this temporal datum with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
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
                return Objects.equals(origin, ((DefaultTemporalDatum) object).origin);
            }
            default: {
                return Objects.equals(getOrigin(), ((TemporalDatum) object).getOrigin());
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
        return super.computeHashCode() + Objects.hashCode(origin);
    }

    /**
     * Formats this datum as a <i>Well Known Text</i> {@code TimeDatum[…]} element.
     *
     * <h4>Compatibility note</h4>
     * {@code TimeDatum} is defined in the <abbr>WKT</abbr> 2 specification only.
     * Apache <abbr>SIS</abbr> accepts this type as members of datum ensembles,
     * but this is not valid <abbr>WKT</abbr> according <abbr>ISO</abbr> 19162:2019.
     *
     * @return {@code "TDatum"} or {@code "TimeDatum"}.
     *         May also be {@code "Member"} if this datum is inside a <abbr>WKT</abbr> {@code Ensemble[…]} element.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final String name = super.formatTo(formatter);
        if (name != null) {
            // Member of a datum ensemble, but ISO 19162:2019 allows that for geodetic and vertical datum only.
            formatter.setInvalidWKT(this, null);
            return name;
        }
        formatter.append(new Origin(getOrigin()));
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return formatter.shortOrLong(WKTKeywords.TDatum, WKTKeywords.TimeDatum);
    }

    /**
     * The {@code TimeOrigin[…]} element inside a {@code TimeDatum[…]}.
     */
    private static final class Origin extends FormattableObject {
        /** The value of the origin to format. */
        private final Temporal origin;

        /** Creates a new time origin with the given value. */
        Origin(final Temporal origin) {
            this.origin = origin;
        }

        /** Formats the time origin. */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(origin);
            return WKTKeywords.TimeOrigin;
        }
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
    private DefaultTemporalDatum() {
        /*
         * The origin is mandatory for SIS working. We do not verify its presence here because the verification
         * would have to be done in an `afterMarshal(…)` method and throwing an exception in that method causes
         * the whole unmarshalling to fail. But the CD_TemporalDatum adapter does some verifications.
         */
    }
}
