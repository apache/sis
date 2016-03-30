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
import java.util.Arrays;
import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.ExtentSelector;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.CharSequences;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureNonNullElement;
import static org.apache.sis.internal.referencing.WKTUtilities.toFormattable;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Defines the location and orientation of an ellipsoid that approximates the shape of the earth.
 * Geodetic datum are used together with ellipsoidal coordinate system, and also with Cartesian
 * coordinate system centered in the ellipsoid (or sphere).
 *
 * <div class="section">Bursa-Wolf parameters</div>
 * One or many {@link BursaWolfParameters} can optionally be associated to each {@code DefaultGeodeticDatum} instance.
 * This association is not part of the ISO 19111 model, but still a common practice (especially in older standards).
 * Associating Bursa-Wolf parameters to geodetic datum is known as the <cite>early-binding</cite> approach.
 * A recommended alternative, discussed below, is the <cite>late-binding</cite> approach.
 *
 * <p>The Bursa-Wolf parameters serve two purposes:</p>
 * <ol class="verbose">
 *   <li><b>Fallback for datum shifts</b><br>
 *     There is different methods for transforming coordinates from one geodetic datum to an other datum,
 *     and Bursa-Wolf parameters are used with some of them. However different set of parameters may exist
 *     for the same pair of (<var>source</var>, <var>target</var>) datum, so it is often not sufficient to
 *     know those datum. The (<var>source</var>, <var>target</var>) pair of CRS are often necessary,
 *     sometime together with the geographic extent of the coordinates to transform.
 *
 *     <p>Apache SIS searches for datum shift methods (including Bursa-Wolf parameters) in the EPSG database when a
 *     {@link org.opengis.referencing.operation.CoordinateOperation} or a
 *     {@link org.opengis.referencing.operation.MathTransform} is requested for a pair of CRS.
 *     This is known as the <cite>late-binding</cite> approach.
 *     If a datum shift method is found in the database, it will have precedence over any {@code BursaWolfParameters}
 *     instance associated to this {@code DefaultGeodeticDatum}. Only if no datum shift method is found in the database,
 *     then the {@code BursaWolfParameters} associated to the datum may be used as a fallback.</p>
 *   </li>
 *
 *   <li><b>WKT version 1 formatting</b><br>
 *     The Bursa-Wolf parameters association serves an other purpose: when a CRS is formatted in the older
 *     <cite>Well Known Text</cite> (WKT 1) format, the formatted string may contain a {@code TOWGS84[…]} element
 *     with the parameter values of the transformation to the WGS 84 datum. This element is provided as a help
 *     for other Geographic Information Systems that support only the <cite>early-binding</cite> approach.
 *     Apache SIS usually does not need the {@code TOWGS84} element, except as a fallback for datum that
 *     do not exist in the EPSG database.
 *   </li>
 * </ol>
 *
 * <div class="section">Creating new geodetic datum instances</div>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a geodetic datum.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code GeodeticDatum} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#datum()}.</li>
 *   <li>Create a {@code GeodeticDatum} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createGeodeticDatum(String)}.</li>
 *   <li>Create a {@code GeodeticDatum} by invoking the {@code DatumFactory.createGeodeticDatum(…)} method
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code DefaultGeodeticDatum} by invoking the
 *       {@linkplain #DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a <cite>World Geodetic System 1984</cite> datum:
 *
 * {@preformat java
 *     GeodeticDatum datum = CommonCRS.WGS84.datum();
 * }
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the {@link Ellipsoid} and the {@link PrimeMeridian} given to the constructor are also immutable. Unless otherwise
 * noted in the javadoc, this condition holds if all components were created using only SIS factories and static
 * constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see DefaultEllipsoid
 * @see DefaultPrimeMeridian
 * @see org.apache.sis.referencing.CommonCRS#datum()
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeodeticDatum(String)
 */
@XmlType(name = "GeodeticDatumType", propOrder = {
    "primeMeridian",
    "ellipsoid"
})
@XmlRootElement(name = "GeodeticDatum")
public class DefaultGeodeticDatum extends AbstractDatum implements GeodeticDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8832100095648302943L;

    /**
     * The <code>{@value #BURSA_WOLF_KEY}</code> property for
     * {@linkplain #getBursaWolfParameters() Bursa-Wolf parameters}.
     */
    public static final String BURSA_WOLF_KEY = ReferencingServices.BURSA_WOLF_KEY;

    /**
     * The array to be returned by {@link #getBursaWolfParameters()} when there is no Bursa-Wolf parameters.
     */
    private static final BursaWolfParameters[] EMPTY_ARRAY = new BursaWolfParameters[0];

    /**
     * The ellipsoid.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setEllipsoid(Ellipsoid)}</p>
     *
     * @see #getEllipsoid()
     */
    private Ellipsoid ellipsoid;

    /**
     * The prime meridian.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setPrimeMeridian(PrimeMeridian)}</p>
     *
     * @see #getPrimeMeridian()
     */
    private PrimeMeridian primeMeridian;

    /**
     * Bursa-Wolf parameters for datum shifts, or {@code null} if none.
     */
    private final BursaWolfParameters[] bursaWolf;

    /**
     * Creates a geodetic datum from the given properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     * In addition to the properties documented in the parent constructor,
     * the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value #BURSA_WOLF_KEY}</td>
     *     <td>{@link BursaWolfParameters} (optionally as array)</td>
     *     <td>{@link #getBursaWolfParameters()}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent classes (reminder)</th>
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
     *     <td>{@link Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * If Bursa-Wolf parameters are specified, then the prime meridian of their
     * {@linkplain BursaWolfParameters#getTargetDatum() target datum} shall be either the same than the
     * {@code primeMeridian} given to this constructor, or Greenwich. This restriction is for avoiding
     * ambiguity about whether the longitude rotation shall be applied before or after the datum shift.
     * If the target prime meridian is Greenwich, then the datum shift will be applied in a coordinate
     * system having Greenwich as the prime meridian.
     *
     * @param properties    The properties to be given to the identified object.
     * @param ellipsoid     The ellipsoid.
     * @param primeMeridian The prime meridian.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)
     */
    public DefaultGeodeticDatum(final Map<String,?> properties,
                                final Ellipsoid     ellipsoid,
                                final PrimeMeridian primeMeridian)
    {
        super(properties);
        ensureNonNull("ellipsoid",     ellipsoid);
        ensureNonNull("primeMeridian", primeMeridian);
        this.ellipsoid     = ellipsoid;
        this.primeMeridian = primeMeridian;
        bursaWolf = CollectionsExt.nonEmpty(CollectionsExt.nonNullArraySet(
                BURSA_WOLF_KEY, properties.get(BURSA_WOLF_KEY), EMPTY_ARRAY));
        if (bursaWolf != null) {
            for (int i=0; i<bursaWolf.length; i++) {
                BursaWolfParameters param = bursaWolf[i];
                ensureNonNullElement("bursaWolf", i, param);
                param = param.clone();
                param.verify(primeMeridian);
                bursaWolf[i] = param;
            }
        }
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
     * @see #castOrCopy(GeodeticDatum)
     */
    protected DefaultGeodeticDatum(final GeodeticDatum datum) {
        super(datum);
        ellipsoid     = datum.getEllipsoid();
        primeMeridian = datum.getPrimeMeridian();
        bursaWolf     = (datum instanceof DefaultGeodeticDatum) ? ((DefaultGeodeticDatum) datum).bursaWolf : null;
        // No need to clone the 'bursaWolf' array since it is read only.
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
    public static DefaultGeodeticDatum castOrCopy(final GeodeticDatum object) {
        return (object == null) || (object instanceof DefaultGeodeticDatum)
                ? (DefaultGeodeticDatum) object : new DefaultGeodeticDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeodeticDatum.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code GeodeticDatum}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code GeodeticDatum.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends GeodeticDatum> getInterface() {
        return GeodeticDatum.class;
    }

    /**
     * Returns the ellipsoid given at construction time.
     *
     * @return The ellipsoid.
     */
    @Override
    @XmlElement(name = "ellipsoid", required = true)
    public Ellipsoid getEllipsoid() {
        return ellipsoid;
    }

    /**
     * Returns the prime meridian given at construction time.
     *
     * @return The prime meridian.
     */
    @Override
    @XmlElement(name = "primeMeridian", required = true)
    public PrimeMeridian getPrimeMeridian() {
        return primeMeridian;
    }

    /**
     * Returns all Bursa-Wolf parameters specified in the {@code properties} map at construction time.
     * See class javadoc for a discussion about Bursa-Wolf parameters.
     *
     * @return The Bursa-Wolf parameters, or an empty array if none.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public BursaWolfParameters[] getBursaWolfParameters() {
        if (bursaWolf == null) {
            return EMPTY_ARRAY;
        }
        final BursaWolfParameters[] copy = bursaWolf.clone();
        for (int i=0; i<copy.length; i++) {
            copy[i] = copy[i].clone();
        }
        return copy;
    }

    /**
     * Returns the position vector transformation (geocentric domain) to the specified datum.
     * If the returned matrix is non-null, then the transformation is represented by an affine transform which can be
     * applied on <strong>geocentric</strong> coordinates. This is identified in the EPSG database as operation method
     * 1033 – <cite>Position Vector transformation (geocentric domain)</cite>, or
     * 1053 – <cite>Time-dependent Position Vector transformation</cite>.
     *
     * <p>If this datum and the given {@code targetDatum} do not use the same {@linkplain #getPrimeMeridian() prime meridian},
     * then it is caller's responsibility to to apply longitude rotation before to use the matrix returned by this method.
     * The target prime meridian should be Greenwich (see {@linkplain #DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian)
     * constructor javadoc}), in which case the datum shift should be applied in a geocentric coordinate system having
     * Greenwich as the prime meridian.</p>
     *
     * <div class="note"><b>Note:</b>
     * in EPSG dataset version 8.9, all datum shifts that can be represented by this method use Greenwich as the
     * prime meridian, both in source and target datum.</div>
     *
     * <div class="section">Search criterion</div>
     * If the given {@code areaOfInterest} is non-null and contains at least one geographic bounding box, then this
     * method ignores any Bursa-Wolf parameters having a {@linkplain BursaWolfParameters#getDomainOfValidity() domain
     * of validity} that does not intersect the given geographic extent.
     * This method performs the search among the remaining parameters in the following order:
     * <ol>
     *   <li>If this {@code GeodeticDatum} contains {@code BursaWolfParameters} having the given
     *       {@linkplain BursaWolfParameters#getTargetDatum() target datum} (ignoring metadata),
     *       then the matrix will be built from those parameters.</li>
     *   <li>Otherwise if the other datum contains {@code BursaWolfParameters} having this datum
     *       as their target (ignoring metadata), then the matrix will be built from those parameters
     *       and {@linkplain org.apache.sis.referencing.operation.matrix.MatrixSIS#inverse() inverted}.</li>
     * </ol>
     *
     * <div class="section">Multi-occurrences resolution</div>
     * If more than one {@code BursaWolfParameters} instance is found in any of the above steps, then the one having
     * the largest intersection between its {@linkplain BursaWolfParameters#getDomainOfValidity() domain of validity}
     * and the given extent will be selected. If more than one instance have the same intersection, then the first
     * occurrence is selected.
     *
     * <div class="section">Time-dependent parameters</div>
     * If the given extent contains a {@linkplain org.opengis.metadata.extent.TemporalExtent temporal extent},
     * then the instant located midway between start and end time will be taken as the date where to evaluate the
     * Bursa-Wolf parameters. This is relevant only to {@linkplain TimeDependentBWP time-dependent parameters}.
     *
     * @param  targetDatum The target datum.
     * @param  areaOfInterest The geographic and temporal extent where the transformation should be valid, or {@code null}.
     * @return An affine transform from {@code this} to {@code target} in geocentric space, or {@code null} if none.
     *
     * @see BursaWolfParameters#getPositionVectorTransformation(Date)
     */
    public Matrix getPositionVectorTransformation(final GeodeticDatum targetDatum, final Extent areaOfInterest) {
        ensureNonNull("targetDatum", targetDatum);
        final ExtentSelector<BursaWolfParameters> selector = new ExtentSelector<BursaWolfParameters>(areaOfInterest);
        BursaWolfParameters candidate = select(targetDatum, selector);
        if (candidate != null) {
            return createTransformation(candidate, areaOfInterest);
        }
        /*
         * Found no suitable BursaWolfParameters associated to this instance.
         * Search in the BursaWolfParameters associated to the other instance.
         */
        if (targetDatum instanceof DefaultGeodeticDatum) {
            candidate = ((DefaultGeodeticDatum) targetDatum).select(this, selector);
            if (candidate != null) try {
                return Matrices.inverse(createTransformation(candidate, areaOfInterest));
            } catch (NoninvertibleMatrixException e) {
                /*
                 * Should never happen because BursaWolfParameters.getPositionVectorTransformation(Date)
                 * is defined in such a way that matrix should always be invertible. If it happen anyway,
                 * returning 'null' is allowed by this method's contract.
                 */
                Logging.unexpectedException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                        DefaultGeodeticDatum.class, "getPositionVectorTransformation", e);
            }
        }
        /*
         * In a previous version (Geotk), we were used to search for a transformation path through a common datum:
         *
         *     source   →   [common datum]   →   target
         *
         * This has been removed, because it was dangerous (many paths may be possible - we are better to rely on
         * the EPSG database, which do define some transformation paths explicitely). Especially since our javadoc
         * now said that associating BursaWolfParameters to GeodeticDatum is not recommended except in a few special
         * cases, this method does not have a picture complete enough for attempting anything else than a direct path.
         */
        return null;
    }

    /**
     * Invokes {@link BursaWolfParameters#getPositionVectorTransformation(Date)} for a date calculated from
     * the temporal elements on the given extent.  This method chooses an instant located midway between the
     * start and end time.
     */
    private static Matrix createTransformation(final BursaWolfParameters bursaWolf, final Extent areaOfInterest) {
        /*
         * Implementation note: we know that we do not need to compute an instant if the parameters is
         * not a subclass of BursaWolfParameters. This optimisation covers the vast majority of cases.
         */
        return bursaWolf.getPositionVectorTransformation(bursaWolf.getClass() != BursaWolfParameters.class ?
                Extents.getDate(areaOfInterest, 0.5) : null);       // 0.5 is for choosing midway instant.
    }

    /**
     * Returns the best parameters matching the given criteria, or {@code null} if none.
     */
    private BursaWolfParameters select(final GeodeticDatum targetDatum, final ExtentSelector<BursaWolfParameters> selector) {
        if (bursaWolf == null) {
            return null;
        }
        for (final BursaWolfParameters candidate : bursaWolf) {
            if (deepEquals(targetDatum, candidate.getTargetDatum(), ComparisonMode.IGNORE_METADATA)) {
                selector.evaluate(candidate.getDomainOfValidity(), candidate);
            }
        }
        return selector.best();
    }

    /**
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the given string according heuristic rules.
     * This method implements the flexibility documented in the
     * {@linkplain AbstractDatum#isHeuristicMatchForName(String) super-class}. In particular,
     * this method ignores the prime meridian name if that name is found between parenthesis in the datum name.
     * The meridian can be safely ignored in the datum name because the {@link PrimeMeridian} object is already
     * compared by the {@link #equals(Object)} method.
     *
     * <div class="note"><b>Example:</b>
     * if the datum name is <cite>"Nouvelle Triangulation Française (Paris)"</cite> and the prime meridian name is
     * <cite>"Paris"</cite>, then this method compares only the <cite>"Nouvelle Triangulation Française"</cite> part.
     * </div>
     *
     * <div class="section">Future evolutions</div>
     * This method implements heuristic rules learned from experience while trying to provide inter-operability
     * with different data producers. Those rules may be adjusted in any future SIS version according experience
     * gained while working with more data producers.
     *
     * @param  name The name to compare.
     * @return {@code true} if the primary name or at least one alias matches the specified {@code name}.
     *
     * @since 0.7
     */
    @Override
    public boolean isHeuristicMatchForName(final String name) {
        final String meridian = primeMeridian.getName().getCode();
        return NameToIdentifier.isHeuristicMatchForName(super.getName(), super.getAlias(), name, new Simplifier() {
            @Override protected CharSequence apply(CharSequence name) {
                name = super.apply(name);
                int lower = CharSequences.indexOf(name, meridian, 0, name.length()) - 1;
                if (lower >= 0 && name.charAt(lower) == '(') {
                    int upper = lower + meridian.length() + 1;
                    if (upper < name.length() && name.charAt(upper) == ')') {
                        lower = CharSequences.skipTrailingWhitespaces(name, 0, lower);
                        while (lower > 0) {
                            final int c = Character.codePointBefore(name, lower);
                            if (Character.isLetterOrDigit(c)) {
                                // Remove the meridian name only if it is not at the beginning of the name.
                                name = new StringBuilder(name).delete(lower, upper+1).toString();
                                break;
                            }
                            lower -= Character.charCount(c);
                        }
                    }
                }
                return name;
            }
        });
    }

    /**
     * Compare this datum with the specified object for equality.
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
            return true;                                // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final DefaultGeodeticDatum that = (DefaultGeodeticDatum) object;
                return Objects.equals(this.ellipsoid,     that.ellipsoid)     &&
                       Objects.equals(this.primeMeridian, that.primeMeridian) &&
                        Arrays.equals(this.bursaWolf,     that.bursaWolf);
            }
            default: {
                final GeodeticDatum that = (GeodeticDatum) object;
                return deepEquals(getEllipsoid(),     that.getEllipsoid(),     mode) &&
                       deepEquals(getPrimeMeridian(), that.getPrimeMeridian(), mode);
                /*
                 * Bursa-Wolf parameters are considered ignorable metadata. This is needed in order to get
                 * equalsIgnoreMetadata(…) to return true when comparing WGS84 datums with and without the
                 * WKT 1 "TOWGS84[0,0,0,0,0,0,0]" element. Furthermore those Bursa-Wolf parameters are not
                 * part of ISO 19111 specification.
                 */
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
        return super.computeHashCode() + Objects.hashCode(ellipsoid) + 31 * Objects.hashCode(primeMeridian);
    }

    /**
     * Formats this datum as a <cite>Well Known Text</cite> {@code Datum[…]} element.
     *
     * <div class="note"><b>Example:</b> Well-Known Text of a WGS 84 datum.
     *
     * {@preformat wkt
     *      Datum["World Geodetic System 1984",
     *        Ellipsoid["WGS84", 6378137.0, 298.257223563, LengthUnit["metre", 1]],
     *      Id["EPSG", 6326, Citation["IOGP"], URI["urn:ogc:def:datum:EPSG::6326"]]]
     * }
     *
     * <p>Same datum using WKT 1.</p>
     *
     * {@preformat wkt
     *      DATUM["World Geodetic System 1984"
     *        SPHEROID["WGS84", 6378137.0, 298.257223563],
     *      AUTHORITY["EPSG", "6326"]]
     * }
     * </div>
     *
     * Note that the {@linkplain #getPrimeMeridian() prime meridian} shall be formatted by the caller
     * as a separated element after the geodetic datum (for compatibility with WKT 1).
     *
     * @return {@code "Datum"} or {@code "GeodeticDatum"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#51">WKT 2 specification §8.2</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.newLine();
        formatter.append(toFormattable(getEllipsoid()));
        final boolean isWKT1 = formatter.getConvention().majorVersion() == 1;
        if (isWKT1) {
            /*
             * Note that at the different of other datum (in particular vertical datum),
             * WKT of geodetic datum do not have a numerical code for the datum type.
             */
            if (bursaWolf != null) {
                for (final BursaWolfParameters candidate : bursaWolf) {
                    if (candidate.isToWGS84()) {
                        formatter.newLine();
                        formatter.append(candidate);
                        break;
                    }
                }
            }
        }
        // For the WKT 2 case, the ANCHOR[…] element is added by Formatter itself.

        formatter.newLine(); // For writing the ID[…] element on its own line.
        if (!isWKT1) {
            /*
             * In WKT 2, both "Datum" and "GeodeticDatum" keywords are permitted. The standard recommends
             * to use "Datum" for simplicity. We will follow this advice when the Datum element is inside
             * a GeodeticCRS element since the "Geodetic" aspect is more obvious in such case. But if the
             * Datum appears in another context, then we will use "GeodeticDatum" for clarity.
             */
            if (!(formatter.getEnclosingElement(1) instanceof GeodeticCRS)) {
                return formatter.shortOrLong(WKTKeywords.Datum, WKTKeywords.GeodeticDatum);
            }
        }
        return WKTKeywords.Datum;
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
    private DefaultGeodeticDatum() {
        bursaWolf = null;
        /*
         * Ellipsoid and PrimeMeridian are mandatory for SIS working. We do not verify their presence here
         * (because the verification would have to be done in an 'afterMarshal(…)' method and throwing an
         * exception in that method causes the whole unmarshalling to fail). But the CD_GeodeticDatum
         * adapter does some verifications.
         */
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getEllipsoid()
     */
    private void setEllipsoid(final Ellipsoid value) {
        if (ellipsoid == null) {
            ellipsoid = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultGeodeticDatum.class, "setEllipsoid", "ellipsoid");
        }
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getPrimeMeridian()
     */
    private void setPrimeMeridian(final PrimeMeridian value) {
        if (primeMeridian == null) {
            primeMeridian = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultGeodeticDatum.class, "setPrimeMeridian", "primeMeridian");
        }
    }
}
