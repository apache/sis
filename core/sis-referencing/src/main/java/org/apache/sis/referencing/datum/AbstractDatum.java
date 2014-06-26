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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.internal.metadata.MetadataUtilities;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.collection.Containers.property;
import static org.apache.sis.internal.metadata.MetadataUtilities.canSetProperty;

// Branch-dependent imports
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
 * {@section Immutability and thread safety}
 * This base class is immutable if the property <em>values</em> (not necessarily the map itself) given to the
 * constructor are also immutable. Most SIS subclasses and related classes are immutable under similar conditions.
 * This means that unless otherwise noted in the javadoc, {@code Datum} instances created using only SIS factories
 * and static constants can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.cs.AbstractCS
 * @see org.apache.sis.referencing.crs.AbstractCRS
 */
@XmlType(name = "AbstractDatumType", propOrder = {
    "domainOfValidity",
    "scope",
    "anchorDefinition",
    "realizationEpoch"
})
@XmlRootElement(name = "AbstractDatum")
@XmlSeeAlso({
    DefaultGeodeticDatum.class,
    DefaultVerticalDatum.class,
    DefaultTemporalDatum.class,
    DefaultEngineeringDatum.class,
    DefaultImageDatum.class
})
public class AbstractDatum extends AbstractIdentifiedObject implements Datum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4894180465652474930L;

    /**
     * The prefix used by ESRI at the beginning of datum names.
     */
    private static final String ESRI_PREFIX = "D_";

    /**
     * Description, possibly including coordinates, of the point or points used to anchor the datum
     * to the Earth. Also known as the "origin", especially for Engineering and Image Datums.
     */
    @XmlElement
    private final InternationalString anchorDefinition;

    /**
     * The time after which this datum definition is valid. This time may be precise
     * (e.g. 1997 for IRTF97) or merely a year (e.g. 1983 for NAD83). If the time is
     * not defined, then the value is {@link Long#MIN_VALUE}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setRealizationEpoch(Date)}</p>
     */
    private long realizationEpoch;

    /**
     * Area or region in which this datum object is valid.
     */
    @XmlElement
    private final Extent domainOfValidity;

    /**
     * Description of domain of usage, or limitations of usage, for which this datum object is valid.
     */
    @XmlElement
    private final InternationalString scope;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractDatum() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        anchorDefinition = null;
        realizationEpoch = Long.MIN_VALUE;
        domainOfValidity = null;
        scope            = null;
    }

    /**
     * Creates a datum from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
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
        anchorDefinition = Types.toInternationalString(properties, ANCHOR_POINT_KEY);
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
        anchorDefinition = datum.getAnchorPoint();
    }

    /**
     * Returns a SIS datum implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
     *       {@link org.opengis.referencing.datum.GeodeticDatum},
     *       {@link org.opengis.referencing.datum.VerticalDatum},
     *       {@link org.opengis.referencing.datum.TemporalDatum},
     *       {@link org.opengis.referencing.datum.EngineeringDatum} or
     *       {@link org.opengis.referencing.datum.ImageDatum},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractDatum}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractDatum} instance is created using the
     *       {@linkplain #AbstractDatum(Datum) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractDatum castOrCopy(final Datum object) {
        return SubTypes.castOrCopy(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code Datum.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The datum interface implemented by this class.
     */
    @Override
    public Class<? extends Datum> getInterface() {
        return Datum.class;
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
        return anchorDefinition;
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
    @XmlElement(name = "realizationEpoch")
    public Date getRealizationEpoch() {
        return MetadataUtilities.toDate(realizationEpoch);
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     */
    private void setRealizationEpoch(final Date value) {
        if (value != null && canSetProperty("realizationEpoch", realizationEpoch != Long.MIN_VALUE)) {
            realizationEpoch = value.getTime();
        }
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
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the given string according heuristic rules.
     * This method performs the comparison documented in the
     * {@link AbstractIdentifiedObject#isHeuristicMatchForName(String) super-class},
     * with the following additional flexibility:
     *
     * <ul>
     *   <li>The {@code "D_"} prefix (used in ESRI datum names), if presents in the given name or in this datum name,
     *       is ignored.</li>
     * </ul>
     *
     * {@section Future evolutions}
     * This method implements heuristic rules learned from experience while trying to provide inter-operability
     * with different data producers. Those rules may be adjusted in any future SIS version according experience
     * gained while working with more data producers.
     *
     * @param  name The name to compare.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     */
    @Override
    public boolean isHeuristicMatchForName(final String name) {
        if (name.startsWith((ESRI_PREFIX))) {
            if (super.isHeuristicMatchForName(name.substring(ESRI_PREFIX.length()))) {
                return true;
            }
        } else if (getName().getCode().startsWith(ESRI_PREFIX)) {
            if (super.isHeuristicMatchForName(ESRI_PREFIX.concat(name))) {
                return true;
            }
        }
        return super.isHeuristicMatchForName(name);
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
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final AbstractDatum that = (AbstractDatum) object;
                return this.realizationEpoch == that.realizationEpoch &&
                       Objects.equals(this.domainOfValidity, that.domainOfValidity) &&
                       Objects.equals(this.anchorDefinition, that.anchorDefinition) &&
                       Objects.equals(this.scope,            that.scope);
            }
            case BY_CONTRACT: {
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
                final Datum that = (Datum) object;
                return isHeuristicMatchForName(that. getName().getCode()) ||
                       IdentifiedObjects.isHeuristicMatchForName(that, getName().getCode());
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
        return super.computeHashCode() + Objects.hash(anchorDefinition, realizationEpoch, domainOfValidity, scope);
    }
}
