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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.jaxb.gml.UniversalTimeAdapter;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Defines the origin of a temporal coordinate reference system.
 *
 * <div class="section">Creating new temporal datum instances</div>
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
 * {@preformat java
 *     TemporalDatum datum = CommonCRS.Temporal.JULIAN.datum();
 * }
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 *
 * @see org.apache.sis.referencing.CommonCRS.Temporal#datum()
 * @see org.apache.sis.referencing.cs.DefaultTimeCS
 * @see org.apache.sis.referencing.crs.DefaultTemporalCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createTemporalDatum(String)
 */
@XmlType(name = "TemporalDatumType")
@XmlRootElement(name = "TemporalDatum")
public class DefaultTemporalDatum extends AbstractDatum implements TemporalDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3357241732140076884L;

    /**
     * The date and time origin of this temporal datum, or {@link Long#MIN_VALUE} if none.
     * This information is mandatory, but SIS is tolerant to missing value is case a XML
     * fragment was incomplete.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setOrigin(Date)}</p>
     */
    private long origin;

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
     *     <td>{@link Date}</td>
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
     * @param origin The date and time origin of this temporal datum.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createTemporalDatum(Map, Date)
     */
    public DefaultTemporalDatum(final Map<String,?> properties, final Date origin) {
        super(properties);
        ensureNonNull("origin", origin);
        this.origin = origin.getTime();
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
     * @see #castOrCopy(TemporalDatum)
     */
    protected DefaultTemporalDatum(final TemporalDatum datum) {
        super(datum);
        origin = MetadataUtilities.toMilliseconds(datum.getOrigin());
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
    public static DefaultTemporalDatum castOrCopy(final TemporalDatum object) {
        return (object == null) || (object instanceof DefaultTemporalDatum) ?
                (DefaultTemporalDatum) object : new DefaultTemporalDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code TemporalDatum.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code TemporalDatum}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
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
     * @return The date and time origin of this temporal datum.
     */
    @Override
    @XmlSchemaType(name = "dateTime")
    @XmlElement(name = "origin", required = true)
    @XmlJavaTypeAdapter(UniversalTimeAdapter.class)
    public Date getOrigin() {
        return MetadataUtilities.toDate(origin);
    }

    /**
     * Compares this temporal datum with the specified object for equality.
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
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                return origin == ((DefaultTemporalDatum) object).origin;
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
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + origin;
    }

    /**
     * Formats this datum as a <cite>Well Known Text</cite> {@code TimeDatum[…]} element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code TimeDatum} is defined in the WKT 2 specification only.</div>
     *
     * @return {@code "TimeDatum"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#90">WKT 2 specification §14.2</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
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
        private final Date origin;

        /** Creates a new time origin with the given value. */
        Origin(final Date origin) {
            this.origin = origin;
        }

        /** Formats the time origin. */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(origin);
            return WKTKeywords.TimeOrigin;
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new datum in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultTemporalDatum() {
        origin = Long.MIN_VALUE;
        /*
         * The origin is mandatory for SIS working. We do not verify its presence here because the verification
         * would have to be done in an 'afterMarshal(…)' method and throwing an exception in that method causes
         * the whole unmarshalling to fail. But the CD_TemporalDatum adapter does some verifications.
         */
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getOrigin()
     */
    private void setOrigin(final Date value) {
        if (origin == Long.MIN_VALUE) {
            origin = value.getTime();
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultTemporalDatum.class, "setOrigin", "origin");
        }
    }
}
