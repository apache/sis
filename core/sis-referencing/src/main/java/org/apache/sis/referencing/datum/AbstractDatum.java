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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.jaxb.gco.DateAsLongAdapter;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.collection.Containers.property;

// Related to JDK7
import java.util.Objects;


/**
 * Specifies the relationship of a coordinate system to the earth.
 * A datum can be defined as a set of real points on the earth that have coordinates.
 * Each datum subtype can be associated with only specific types of
 * {@linkplain org.apache.sis.referencing.cs.AbstractCS coordinate systems}, thus creating specific types of
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS coordinate reference system}.
 *
 * {@section Instantiation}
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.cs.AbstractCS
 * @see org.apache.sis.referencing.crs.AbstractCRS
 */
@Immutable
@XmlType(name="AbstractDatumType")
public class AbstractDatum extends AbstractIdentifiedObject implements Datum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4894180465652474930L;

    /**
     * Description, possibly including coordinates, of the point or points used to anchor the datum
     * to the Earth. Also known as the "origin", especially for Engineering and Image Datums.
     */
    @XmlElement
    private final InternationalString anchorPoint;

    /**
     * The time after which this datum definition is valid. This time may be precise
     * (e.g. 1997 for IRTF97) or merely a year (e.g. 1983 for NAD83). If the time is
     * not defined, then the value is {@link Long#MIN_VALUE}.
     */
    @XmlElement
    @XmlJavaTypeAdapter(value=DateAsLongAdapter.class, type=long.class)
    private final long realizationEpoch;

    /**
     * Area or region in which this datum object is valid.
     */
    @XmlElement(name = "validArea")
    private final Extent domainOfValidity;

    /**
     * Description of domain of usage, or limitations of usage, for which this datum object is valid.
     */
    @XmlElement
    private final InternationalString scope;

    /**
     * Creates a datum from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
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
     *     <td>{@link Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
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
     * </table>
     *
     * @param properties The properties to be given to the identified object.
     */
    public AbstractDatum(final Map<String,?> properties) {
        super(properties);
        realizationEpoch = MetadataUtilities.toMilliseconds(property(properties, REALIZATION_EPOCH_KEY, Date.class));
        domainOfValidity = property(properties, DOMAIN_OF_VALIDITY_KEY, Extent.class);
        anchorPoint      = Types.toInternationalString(properties, ANCHOR_POINT_KEY);
        scope            = Types.toInternationalString(properties, SCOPE_KEY);
    }

    /**
     * Creates a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     */
    protected AbstractDatum(final Datum datum) {
        super(datum);
        realizationEpoch = MetadataUtilities.toMilliseconds(datum.getRealizationEpoch());
        domainOfValidity = datum.getDomainOfValidity();
        scope            = datum.getScope();
        anchorPoint      = datum.getAnchorPoint();
    }

    /**
     * Returns a description of the point(s) used to anchor the datum to the Earth.
     * Also known as the "origin", especially for Engineering and Image Datums.
     *
     * <ul>
     *   <li>For a {@linkplain DefaultGeodeticDatum geodetic datum}, the anchor may be the point(s) where the
     *       relationship between geoid and ellipsoid is defined.</li>
     *
     *   <li>For an {@linkplain DefaultEngineeringDatum engineering datum}, the anchor may be an identified
     *       physical point with the orientation defined relative to the object.</li>
     *
     *   <li>For an {@linkplain DefaultImageDatum image datum}, the anchor point may be the centre or the corner
     *       of the image.</li>
     *
     *   <li>For a {@linkplain DefaultTemporalDatum temporal datum}, see their
     *       {@linkplain DefaultTemporalDatum#getOrigin() origin} instead.</li>
     * </ul>
     *
     * @return Description, possibly including coordinates, of the point or points used to anchor the datum
     *         to the Earth.
     */
    @Override
    public InternationalString getAnchorPoint() {
        return anchorPoint;
    }

    /**
     * The time after which this datum definition is valid.
     * This time may be precise or merely a year.
     *
     * <p>If an old datum is superseded by a new datum, then the realization epoch for the new datum
     * defines the upper limit for the validity of the old datum.</p>
     *
     * @return The time after which this datum definition is valid, or {@code null} if none.
     */
    @Override
    public Date getRealizationEpoch() {
        return MetadataUtilities.toDate(realizationEpoch);
    }

    /**
     * Returns the region or timeframe in which this datum is valid, or {@code null} if unspecified.
     *
     * @return Area or region or timeframe in which this datum is valid, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @Override
    public Extent getDomainOfValidity() {
        return domainOfValidity;
    }

    /**
     * Returns the domain or limitations of usage, or {@code null} if unspecified.
     *
     * @return Description of domain of usage, or limitations of usage, for which this datum object is valid.
     */
    @Override
    public InternationalString getScope() {
        return scope;
    }

    /**
     * Gets the type of the datum as an enumerated code. Datum type was provided for all kind of datum
     * in the legacy OGC 01-009 specification. In the new OGC 03-73 (ISO 19111) specification,
     * datum type is provided only for vertical datum. Nevertheless, we keep this method around
     * since it is needed for WKT formatting. Note that we return the datum type ordinal value,
     * not the code list object.
     */
    int getLegacyDatumType() {
        return 0;
    }

    /**
     * Compares the specified object with this datum for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties are compared including the
     * {@linkplain #getAnchorPoint() anchor point}, {@linkplain #getRealizationEpoch() realization epoch},
     * {@linkplain #getDomainOfValidity() domain of validity} and the {@linkplain #getScope() scope}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final AbstractDatum that = (AbstractDatum) object;
                    return this.realizationEpoch == that.realizationEpoch &&
                           Objects.equals(this.domainOfValidity, that.domainOfValidity) &&
                           Objects.equals(this.anchorPoint,      that.anchorPoint) &&
                           Objects.equals(this.scope,            that.scope);
                }
                case BY_CONTRACT: {
                    if (!(object instanceof Datum)) break;
                    final Datum that = (Datum) object;
                    return deepEquals(getRealizationEpoch(), that.getRealizationEpoch(), mode) &&
                           deepEquals(getDomainOfValidity(), that.getDomainOfValidity(), mode) &&
                           deepEquals(getAnchorPoint(),      that.getAnchorPoint(),      mode) &&
                           deepEquals(getScope(),            that.getScope(),            mode);
                }
                default: {
                    /*
                     * Tests for name, since datum with different name have completely
                     * different meaning. We don't perform this comparison if the user
                     * asked for metadata comparison, because in such case the names
                     * have already been compared by the subclass.
                     */
                    if (!(object instanceof Datum)) break;
                    final Datum that = (Datum) object;
                    return nameMatches(that. getName().getCode()) ||
                           IdentifiedObjects.nameMatches(that, getName().getCode());
                }
            }
        }
        return false;
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     * If the given argument is {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}, then the
     * {@linkplain #getAnchorPoint() anchor point}, {@linkplain #getRealizationEpoch() realization epoch},
     * {@linkplain #getDomainOfValidity() domain of validity} and the {@linkplain #getScope() scope}
     * properties are ignored.
     *
     * @return The hash code value for the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        /*
         * The "^ (int) serialVersionUID" is an arbitrary change applied to the hash code value in order to
         * differentiate this Datum implementation from implementations of other GeoAPI interfaces.
         */
        int code = super.hashCode(mode) ^ (int) serialVersionUID;
        switch (mode) {
            case STRICT: {
                code += Objects.hash(anchorPoint, realizationEpoch, domainOfValidity, scope);
                break;
            }
            case BY_CONTRACT: {
                code += Objects.hash(getAnchorPoint(), getRealizationEpoch(), getDomainOfValidity(), getScope());
                break;
            }
            /*
             * The name is significant for all modes, but we nevertheless ignore it because
             * of the way the name is compared in the equals(Object, ComparisonMode) method
             * which make hash code computation impractical in the IGNORE_METADATA case.
             */
        }
        return code;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT)</a> element.
     *
     * {@note All subclasses will override this method, but only <code>DefaultGeodeticDatum</code>
     *        will <strong>not</strong> invoke this parent method, because horizontal datum do not
     *        write the datum type.}
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name.
     */
    @Override
    public String formatTo(final Formatter formatter) {
        formatter.append(getLegacyDatumType());
        return Classes.getShortClassName(this);
    }
}
