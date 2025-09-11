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
import java.util.Objects;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.ExtentSelector;
import org.apache.sis.metadata.privy.NameToIdentifier;
import org.apache.sis.metadata.privy.ImplementationHelper;
import org.apache.sis.referencing.internal.AnnotatedMatrix;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.io.wkt.Formatter;
import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureNonNullElement;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Defines the location and orientation of an ellipsoid that approximates the shape of the earth.
 * This concept, known as <q>geodetic datum</q> in traditional geodesy,
 * is now better known as <q>geodetic reference frame</q>.
 * Geodetic reference frames are used together with ellipsoidal coordinate system,
 * and also with Cartesian coordinate system centered in the ellipsoid (or sphere).
 *
 * <h2>Bursa-Wolf parameters</h2>
 * One or many {@link BursaWolfParameters} can optionally be associated to each {@code DefaultGeodeticDatum} instance.
 * This association is not part of the ISO 19111 model, but still a common practice (especially in older standards).
 * Associating Bursa-Wolf parameters to a geodetic reference frame is known as the <i>early-binding</i> approach.
 * A recommended alternative, discussed below, is the <i>late-binding</i> approach.
 *
 * <p>The Bursa-Wolf parameters serve two purposes:</p>
 * <ol class="verbose">
 *   <li><b>Fallback for datum shifts</b><br>
 *     There is different methods for transforming coordinates from one geodetic reference frame to another frame,
 *     and Bursa-Wolf parameters are used with some of them. However, different set of parameters may exist
 *     for the same pair of (<var>source</var>, <var>target</var>) datum, so it is often not sufficient to
 *     know those datum. The (<var>source</var>, <var>target</var>) pair of CRS are often necessary,
 *     sometimes together with the geographic extent of the coordinates to transform.
 *
 *     <p>Apache SIS searches for datum shift methods (including Bursa-Wolf parameters) in the EPSG database when a
 *     {@link org.opengis.referencing.operation.CoordinateOperation} or a
 *     {@link org.opengis.referencing.operation.MathTransform} is requested for a pair of CRS.
 *     This is known as the <i>late-binding</i> approach.
 *     If a datum shift method is found in the database, it will have precedence over any {@code BursaWolfParameters}
 *     instance associated to this {@code DefaultGeodeticDatum}. Only if no datum shift method is found in the database,
 *     then the {@code BursaWolfParameters} associated to the datum may be used as a fallback.</p>
 *   </li>
 *
 *   <li><b>WKT version 1 formatting</b><br>
 *     The Bursa-Wolf parameters association serves another purpose: when a CRS is formatted in the older
 *     <i>Well Known Text</i> (WKT 1) format, the formatted string may contain a {@code TOWGS84[…]} element
 *     with the parameter values of the transformation to the WGS 84 datum. This element is provided as a help
 *     for other Geographic Information Systems that support only the <i>early-binding</i> approach.
 *     Apache SIS usually does not need the {@code TOWGS84} element, except as a fallback for datum that
 *     do not exist in the EPSG database.
 *   </li>
 * </ol>
 *
 * <h2>Creating new geodetic reference frame instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a geodetic reference frame.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code GeodeticDatum} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#datum(boolean)}.</li>
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
 * {@snippet lang="java" :
 *     GeodeticDatum datum = CommonCRS.WGS84.datum();
 *     }
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the {@link Ellipsoid} and the {@link PrimeMeridian} given to the constructor are also immutable. Unless otherwise
 * noted in the javadoc, this condition holds if all components were created using only SIS factories and static
 * constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see DefaultEllipsoid
 * @see DefaultPrimeMeridian
 * @see org.apache.sis.referencing.CommonCRS#datum(boolean)
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeodeticDatum(String)
 *
 * @since 0.4
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
     *
     * <h4>Possible evolution</h4>
     * This is a legacy parameter from the Well-Known Text (<abbr>WKT</abbr>) 1 format
     * and may be deprecated in a future Apache <abbr>SIS</abbr> version.
     * It should be replaced by the use of geodetic registries such as the <abbr>EPSG</abbr> database.
     *
     * @see #getBursaWolfParameters()
     */
    public static final String BURSA_WOLF_KEY = "bursaWolf";

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
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    private Ellipsoid ellipsoid;

    /**
     * The prime meridian.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setPrimeMeridian(PrimeMeridian)}</p>
     *
     * @see #getPrimeMeridian()
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    private PrimeMeridian primeMeridian;

    /**
     * Bursa-Wolf parameters for datum shifts, or {@code null} if none.
     */
    private final BursaWolfParameters[] bursaWolf;

    /**
     * Creates a geodetic reference frame from the given properties. The properties map is given
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
     *   </tr><tr>
     *     <td>{@value #BURSA_WOLF_KEY}</td>
     *     <td>{@link BursaWolfParameters} (optionally as array)</td>
     *     <td>{@link #getBursaWolfParameters()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Defined in parent classes (reminder)</th>
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
     * If Bursa-Wolf parameters are specified, then the prime meridian of their
     * {@linkplain BursaWolfParameters#getTargetDatum() target datum} shall be either the same as the
     * {@code primeMeridian} given to this constructor, or Greenwich. This restriction is for avoiding
     * ambiguity about whether the longitude rotation shall be applied before or after the datum shift.
     * If the target prime meridian is Greenwich, then the datum shift will be applied in a coordinate
     * system having Greenwich as the prime meridian.
     *
     * @param  properties     the properties to be given to the identified object.
     * @param  ellipsoid      the ellipsoid.
     * @param  primeMeridian  the prime meridian.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)
     */
    public DefaultGeodeticDatum(final Map<String,?> properties,
                                final Ellipsoid     ellipsoid,
                                final PrimeMeridian primeMeridian)
    {
        super(properties);
        this.ellipsoid     = Objects.requireNonNull(ellipsoid);
        this.primeMeridian = Objects.requireNonNull(primeMeridian);
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
     * Creates a new datum with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  datum  the datum to copy.
     *
     * @see #castOrCopy(GeodeticDatum)
     */
    protected DefaultGeodeticDatum(final GeodeticDatum datum) {
        super(datum);
        ellipsoid     = datum.getEllipsoid();
        primeMeridian = datum.getPrimeMeridian();
        bursaWolf     = (datum instanceof DefaultGeodeticDatum) ? ((DefaultGeodeticDatum) datum).bursaWolf : null;
        // No need to clone the `bursaWolf` array since it is read only.
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
    public static DefaultGeodeticDatum castOrCopy(final GeodeticDatum object) {
        if (object == null || object instanceof DefaultGeodeticDatum) {
            return (DefaultGeodeticDatum) object;
        }
        if (object instanceof DynamicReferenceFrame) {
            return new Dynamic(object);
        }
        return new DefaultGeodeticDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeodeticDatum.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code GeodeticDatum}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
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
     * @return the ellipsoid.
     */
    @Override
    @XmlElement(name = "ellipsoid", required = true)
    public Ellipsoid getEllipsoid() {
        return ellipsoid;
    }

    /**
     * Returns the prime meridian given at construction time.
     *
     * @return the prime meridian.
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
     * <h4>Possible evolution</h4>
     * This is a legacy parameter from the Well-Known Text (<abbr>WKT</abbr>) 1 format
     * and may be deprecated in a future Apache <abbr>SIS</abbr> version.
     * It should be replaced by the use of geodetic registries such as the <abbr>EPSG</abbr> database.
     *
     * @return the Bursa-Wolf parameters, or an empty array if none.
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
     * then it is caller's responsibility to apply longitude rotation before to use the matrix returned by this method.
     * The target prime meridian should be Greenwich (see {@linkplain #DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian)
     * constructor javadoc}), in which case the datum shift should be applied in a geocentric coordinate system having
     * Greenwich as the prime meridian.</p>
     *
     * <div class="note"><b>Note:</b>
     * in EPSG dataset version 8.9, all datum shifts that can be represented by this method use Greenwich as the
     * prime meridian, both in source and target datum.</div>
     *
     * <h4>Search criteria</h4>
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
     * <h4>Multi-occurrences resolution</h4>
     * If more than one {@code BursaWolfParameters} instance is found in any of the above steps, then the one having
     * the largest intersection between its {@linkplain BursaWolfParameters#getDomainOfValidity() domain of validity}
     * and the given extent will be selected. If more than one instance have the same intersection, then the first
     * occurrence is selected.
     *
     * <h4>Time-dependent parameters</h4>
     * If the given extent contains a {@linkplain org.opengis.metadata.extent.TemporalExtent temporal extent},
     * then the instant located midway between start and end time will be taken as the date where to evaluate the
     * Bursa-Wolf parameters. This is relevant only to {@linkplain TimeDependentBWP time-dependent parameters}.
     *
     * @param  targetDatum     the target datum.
     * @param  areaOfInterest  the geographic and temporal extent where the transformation should be valid, or {@code null}.
     * @return an affine transform from {@code this} to {@code target} in geocentric space, or {@code null} if none.
     *
     * @see BursaWolfParameters#getPositionVectorTransformation(Temporal)
     */
    @OptionalCandidate
    public Matrix getPositionVectorTransformation(final GeodeticDatum targetDatum, final Extent areaOfInterest) {
        ensureNonNull("targetDatum", targetDatum);
        final var selector = new ExtentSelector<BursaWolfParameters>(areaOfInterest);
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
                 * Should never happen because BursaWolfParameters.getPositionVectorTransformation(Temporal)
                 * is defined in such a way that matrix should always be invertible. If it happen anyway,
                 * returning `null` is allowed by this method's contract.
                 */
                Logging.unexpectedException(CoordinateOperations.LOGGER,
                        DefaultGeodeticDatum.class, "getPositionVectorTransformation", e);
            }
            /*
             * No direct tranformation found. Search for a path through an intermediate datum.
             * First, search if there is some BursaWolfParameters for the same target in both
             * `source` and `target` datum. If such an intermediate is found, ask for path:
             *
             *    source   →   [common datum]   →   target
             *
             * A consequence of such indirect path is that it may connect unrelated datums
             * if [common datum] is a world datum such as WGS84. We do not have a solution
             * for preventing that.
             */
            if (bursaWolf != null) {
                GeographicBoundingBox bbox = selector.getAreaOfInterest();
                Temporal[] timeOfInterest  = selector.getTimeOfInterest();
                boolean useAOI = true;
                do {    // Executed at most 3 times with `bbox` cleared, then `timeOfInterest` cleared.
                    for (final BursaWolfParameters toPivot : bursaWolf) {
                        if (selector.setExtentOfInterest(toPivot.getDomainOfValidity(), bbox, timeOfInterest)) {
                            candidate = ((DefaultGeodeticDatum) targetDatum).select(toPivot.getTargetDatum(), selector);
                            if (candidate != null) {
                                final Matrix step1 = createTransformation(toPivot,   areaOfInterest);
                                final Matrix step2 = createTransformation(candidate, areaOfInterest);
                                /*
                                 * MatrixSIS.multiply(MatrixSIS) is equivalent to AffineTransform.concatenate(…):
                                 * First transform by the supplied transform and then transform the result by the
                                 * original transform.
                                 */
                                try {
                                    Matrix m = MatrixSIS.castOrCopy(step2).inverse().multiply(step1);
                                    return AnnotatedMatrix.indirect(m, useAOI);
                                } catch (NoninvertibleMatrixException e) {
                                    Logging.unexpectedException(CoordinateOperations.LOGGER,
                                            DefaultGeodeticDatum.class, "getPositionVectorTransformation", e);
                                }
                            }
                        }
                    }
                    useAOI = false;
                } while (bbox != (bbox = null) || timeOfInterest != (timeOfInterest = null));
                // Clear `bbox` first, and if it was already cleared `timeOfInterest` is next.
            }
        }
        return null;
    }

    /**
     * Returns the best parameters matching the given criteria, or {@code null} if none.
     */
    private BursaWolfParameters select(final GeodeticDatum targetDatum, final ExtentSelector<BursaWolfParameters> selector) {
        if (bursaWolf == null) {
            return null;
        }
        for (final BursaWolfParameters candidate : bursaWolf) {
            if (deepEquals(targetDatum, candidate.getTargetDatum(), ComparisonMode.COMPATIBILITY)) {
                selector.evaluate(candidate.getDomainOfValidity(), candidate);
            }
        }
        return selector.best();
    }

    /**
     * Returns the position vector transformation (geocentric domain) as an affine transform.
     * If this datum is dynamic, the frame reference epoch is used.
     * Otherwise, a time is computed from the temporal area of interest.
     *
     * @see BursaWolfParameters#getPositionVectorTransformation(Temporal)
     */
    private Matrix createTransformation(final BursaWolfParameters bursaWolf, final Extent areaOfInterest) {
        Temporal epoch = null;
        /*
         * Implementation note: we know that we do not need to compute an instant if the parameters is
         * not a subclass of BursaWolfParameters. This optimisation covers the vast majority of cases.
         */
        if (bursaWolf.getClass() != BursaWolfParameters.class) {
            epoch = getFrameReferenceEpoch();
            if (epoch == null) {
                epoch = Extents.getInstant(areaOfInterest, null, 0.5).orElse(null);
                // 0.5 is for choosing the instant midway between start and end.
            }
        }
        return bursaWolf.getPositionVectorTransformation(epoch);
    }

    /**
     * A geodetic reference frame in which some of the defining parameters have time dependency.
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
    public static class Dynamic extends DefaultGeodeticDatum implements DynamicReferenceFrame {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6117199873814779662L;

        /**
         * The epoch to which the definition of the dynamic reference frame is referenced.
         */
        @SuppressWarnings("serial")                     // Standard Java implementations are serializable.
        private final Temporal frameReferenceEpoch;

        /**
         * Creates a dynamic reference frame from the given properties.
         * See super-class constructor for more information.
         *
         * @param  properties     the properties to be given to the identified object.
         * @param  ellipsoid      the ellipsoid.
         * @param  primeMeridian  the prime meridian.
         * @param  epoch          the epoch to which the definition of the dynamic reference frame is referenced.
         */
        public Dynamic(Map<String,?> properties, Ellipsoid ellipsoid, PrimeMeridian primeMeridian, Temporal epoch) {
            super(properties, ellipsoid, primeMeridian);
            frameReferenceEpoch = Objects.requireNonNull(epoch);
        }

        /**
         * Creates a new datum with the same values as the specified datum, which must be dynamic.
         *
         * @param  datum  the datum to copy.
         * @throws ClassCastException if the given datum is not an instance of {@code DynamicReferenceFrame}.
         *
         * @see #castOrCopy(GeodeticDatum)
         */
        protected Dynamic(final GeodeticDatum datum) {
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
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the given string according heuristic rules.
     * This method implements the flexibility documented in the
     * {@linkplain AbstractDatum#isHeuristicMatchForName(String) super-class}. In particular,
     * this method ignores the prime meridian name if that name is found between parenthesis in the datum name.
     * The meridian can be safely ignored in the datum name because the {@link PrimeMeridian} object is already
     * compared by the {@link #equals(Object)} method.
     *
     * <h4>Example</h4>
     * If the datum name is <q>Nouvelle Triangulation Française (Paris)</q> and the prime meridian name is
     * <q>Paris</q>, then this method compares only the <q>Nouvelle Triangulation Française</q> part.
     *
     * <h4>Future evolutions</h4>
     * This method implements heuristic rules learned from experience while trying to provide inter-operability
     * with different data producers. Those rules may be adjusted in any future SIS version according experience
     * gained while working with more data producers.
     *
     * @param  name  the name to compare.
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
     * Compares this datum with the specified object for equality.
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
            return true;                                // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final var that = (DefaultGeodeticDatum) object;
                return Objects.equals(this.ellipsoid,     that.ellipsoid)     &&
                       Objects.equals(this.primeMeridian, that.primeMeridian) &&
                        Arrays.equals(this.bursaWolf,     that.bursaWolf);
            }
            default: {
                final var that = (GeodeticDatum) object;
                return deepEquals(getEllipsoid(),     that.getEllipsoid(),     mode) &&
                       deepEquals(getPrimeMeridian(), that.getPrimeMeridian(), mode);
                /*
                 * Bursa-Wolf parameters are considered ignorable metadata. This is needed in order to get
                 * equalsIgnoreMetadata(…) to return true when comparing WGS84 datums with and without the
                 * WKT 1 "TOWGS84[0,0,0,0,0,0,0]" element. Furthermore, those Bursa-Wolf parameters are not
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
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(ellipsoid) + 31 * Objects.hashCode(primeMeridian);
    }

    /**
     * Formats this datum as a <i>Well Known Text</i> {@code Datum[…]} element.
     *
     * <h4>Example</h4>
     * Well-Known Text of a WGS 84 datum.
     *
     * {@snippet lang="wkt" :
     *   Datum["World Geodetic System 1984",
     *     Ellipsoid["WGS84", 6378137.0, 298.257223563, LengthUnit["metre", 1]],
     *   Id["EPSG", 6326, Citation["IOGP"], URI["urn:ogc:def:datum:EPSG::6326"]]]
     *   }
     *
     * <p>Same datum using WKT 1.</p>
     *
     * {@snippet lang="wkt" :
     *   DATUM["World Geodetic System 1984"
     *     SPHEROID["WGS84", 6378137.0, 298.257223563],
     *   AUTHORITY["EPSG", "6326"]]
     *   }
     *
     * Note that the {@linkplain #getPrimeMeridian() prime meridian} shall be formatted by the caller
     * as a separated element after the geodetic reference frame (for compatibility with WKT 1).
     *
     * @return {@code "Datum"} or {@code "GeodeticDatum"}.
     *         May also be {@code "Member"} if this datum is inside a <abbr>WKT</abbr> {@code Ensemble[…]} element.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final String name = super.formatTo(formatter);
        if (name != null) {
            // Member of a datum ensemble.
            return name;
        }
        formatter.newLine();
        formatter.appendFormattable(getEllipsoid(), DefaultEllipsoid::castOrCopy);
        final boolean isWKT1 = formatter.getConvention().majorVersion() == 1;
        if (isWKT1) {
            /*
             * Note that at the different of other datum (in particular vertical datum),
             * WKT of geodetic reference frame do not have a numerical code for the datum type.
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
        /*
         * For the WKT 2 case, the ANCHOR[…] element is added by Formatter itself.
         */
        formatter.newLine();                            // For writing the ID[…] element on its own line.
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
    private DefaultGeodeticDatum() {
        bursaWolf = null;
        /*
         * Ellipsoid and PrimeMeridian are mandatory for SIS working. We do not verify their presence here
         * (because the verification would have to be done in an `afterMarshal(…)` method and throwing an
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
            ImplementationHelper.propertyAlreadySet(DefaultGeodeticDatum.class, "setEllipsoid", "ellipsoid");
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
            ImplementationHelper.propertyAlreadySet(DefaultGeodeticDatum.class, "setPrimeMeridian", "primeMeridian");
        }
    }
}
