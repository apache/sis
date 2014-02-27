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
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.internal.referencing.ExtentSelector;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureNonNullElement;
import static org.apache.sis.internal.referencing.WKTUtilities.toFormattable;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * Defines the location and orientation of an ellipsoid that approximates the shape of the earth.
 * Geodetic datum are used together with ellipsoidal coordinate system, and also with Cartesian
 * coordinate system centered in the ellipsoid (or sphere).
 *
 * {@section Bursa-Wolf parameters}
 * One or many {@link BursaWolfParameters} can optionally be associated to each {@code DefaultGeodeticDatum} instance.
 * This association is not part of the ISO 19111 model, but still a common practice (especially in older standards).
 * Associating Bursa-Wolf parameters to geodetic datum is known as the <cite>early-binding</cite> approach.
 * A recommended alternative, discussed below, is the <cite>late-binding</cite> approach.
 *
 * <p>There is different methods for transforming coordinates from one geodetic datum to an other datum,
 * and Bursa-Wolf parameters are used with some of them. However different set of parameters may exist
 * for the same pair of (<var>source</var>, <var>target</var>) datum, so it is often not sufficient to
 * know those datum. The (<var>source</var>, <var>target</var>) pair of CRS are often necessary,
 * sometime together with the geographic extent of the coordinates to transform.</p>
 *
 * <p>Apache SIS searches for datum shift methods (including Bursa-Wolf parameters) in the EPSG database when a
 * {@link org.opengis.referencing.operation.CoordinateOperation} or a
 * {@link org.opengis.referencing.operation.MathTransform} is requested for a pair of CRS.
 * This is known as the <cite>late-binding</cite> approach.
 * If a datum shift method is found in the database, it will have precedence over any {@code BursaWolfParameters}
 * instance associated to this {@code DefaultGeodeticDatum}. Only if no datum shift method is found in the database,
 * then the {@code BursaWolfParameters} associated to the datum may be used as a fallback.</p>
 *
 * <p>The Bursa-Wolf parameters association serves an other purpose: when a CRS is formatted in the older
 * <cite>Well Known Text</cite> (WKT 1) format, the formatted string may contain a {@code TOWGS84[…]} element
 * with the parameter values of the transformation to the WGS 84 datum. This element is provided as a help
 * for other Geographic Information Systems that support only the <cite>early-binding</cite> approach.
 * Apache SIS usually does not need the {@code TOWGS84} element, except as a fallback for datum that
 * do not exist in the EPSG database.</p>
 *
 * {@section Creating new geodetic datum instances}
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
 *   <li>Create a {@code GeodeticDatum} by invoking the {@code createGeodeticDatum(…)}
 *       method defined in the {@link org.opengis.referencing.datum.DatumFactory} interface.</li>
 *   <li>Create a {@code DefaultGeodeticDatum} by invoking the
 *       {@link #DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a <cite>World Geodetic System 1984</cite> datum:
 *
 * {@preformat java
 *     GeodeticDatum datum = CommonCRS.WGS84.datum();
 * }
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the {@link Ellipsoid} and the {@link PrimeMeridian} given to the constructor are also immutable. Unless otherwise
 * noted in the javadoc, this condition holds if all components were created using only SIS factories and static
 * constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see DefaultEllipsoid
 * @see DefaultPrimeMeridian
 * @see org.apache.sis.referencing.CommonCRS#datum()
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
    public static final String BURSA_WOLF_KEY = "bursaWolf";

    /**
     * The array to be returned by {@link #getBursaWolfParameters()} when there is no Bursa-Wolf parameters.
     */
    private static final BursaWolfParameters[] EMPTY_ARRAY = new BursaWolfParameters[0];

    /**
     * The ellipsoid.
     */
    @XmlElement
    private final Ellipsoid ellipsoid;

    /**
     * The prime meridian.
     */
    @XmlElement
    private final PrimeMeridian primeMeridian;

    /**
     * Bursa-Wolf parameters for datum shifts, or {@code null} if none.
     */
    private final BursaWolfParameters[] bursaWolf;

    /**
     * Constructs a new datum in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultGeodeticDatum() {
        ellipsoid     = null;
        primeMeridian = null;
        bursaWolf     = null;
    }

    /**
     * Creates a geodetic datum from the given properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     * In addition to the properties documented in the parent constructor,
     * the following properties are understood by this constructor:
     *
     * <table class="sis">
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
     * @param properties    The properties to be given to the identified object.
     * @param ellipsoid     The ellipsoid.
     * @param primeMeridian The prime meridian.
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
                param.verify();
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
        bursaWolf     = (datum instanceof DefaultGeodeticDatum) ?
                        ((DefaultGeodeticDatum) datum).bursaWolf : null;
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
    public Ellipsoid getEllipsoid() {
        return ellipsoid;
    }

    /**
     * Returns the prime meridian given at construction time.
     *
     * @return The prime meridian.
     */
    @Override
    public PrimeMeridian getPrimeMeridian() {
        return primeMeridian;
    }

    /**
     * Returns all Bursa-Wolf parameters specified in the {@code properties} map at construction time.
     * For a discussion about what Bursa-Wolf parameters are, see the class javadoc.
     *
     * @return The Bursa-Wolf parameters, or an empty array if none.
     */
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
     * This method performs the search in the following order:
     *
     * <ul>
     *   <li>If this {@code GeodeticDatum} contains {@code BursaWolfParameters} having the given
     *       {@linkplain BursaWolfParameters#getTargetDatum() target datum} (ignoring metadata),
     *       then the matrix will be built from those parameters.</li>
     *   <li>Otherwise if the other datum contains {@code BursaWolfParameters} having this datum
     *       as their target (ignoring metadata), then the matrix will be built from those parameters
     *       and {@linkplain MatrixSIS#inverse() inverted}.</li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * If more than one {@code BursaWolfParameters} instance is found in any of the above steps, then the one having
     * the largest intersection between its {@linkplain BursaWolfParameters#getDomainOfValidity() domain of validity}
     * and the given extent will be selected. If more than one instance have the same intersection, then the first
     * occurrence is selected.
     *
     * <p>If the given extent contains a {@linkplain org.opengis.metadata.extent.TemporalExtent temporal extent},
     * then the instant located midway between start and end time will be taken as the date where to evaluate the
     * Bursa-Wolf parameters. This apply only to {@linkplain TimeDependentBWP time-dependent parameters}.</p>
     *
     * <p>If the returned matrix is non-null, then the transformation is represented by an affine transform which can be
     * applied on <strong>geocentric</strong> coordinates. This is identified in the EPSG database as operation method
     * 1033 – <cite>Position Vector transformation (geocentric domain)</cite>, or
     * 1053 – <cite>Time-dependent Position Vector transformation</cite>.</p>
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
                return MatrixSIS.castOrCopy(createTransformation(candidate, areaOfInterest)).inverse();
            } catch (NoninvertibleMatrixException e) {
                /*
                 * Should never happen because BursaWolfParameters.getPositionVectorTransformation(Date)
                 * is defined in such a way that matrix should always be invertible. If it happen anyway,
                 * returning 'null' is allowed by this method's contract.
                 */
                Logging.unexpectedException(DefaultGeodeticDatum.class, "getPositionVectorTransformation", e);
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
                Extents.getDate(areaOfInterest, 0.5) : null); // 0.5 is for choosing midway instant.
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
            return true; // Slight optimization.
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
                 * HACK: We do not consider Bursa-Wolf parameters as a non-metadata field.
                 *       This is needed in order to get equalsIgnoreMetadata(...) to returns
                 *       'true' when comparing the WGS84 constant in this class with a WKT
                 *       DATUM element with a TOWGS84[0,0,0,0,0,0,0] element. Furthermore,
                 *       the Bursa-Wolf parameters are not part of ISO 19111 specification.
                 *       We don't want two CRS to be considered as different because one has
                 *       more of those transformation informations (which is nice, but doesn't
                 *       change the CRS itself).
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
     *      Id["EPSG", 6326, Citation["OGP"], URI["urn:ogc:def:datum:EPSG::6326"]]]
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
     * @return {@code "Datum"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.newLine();
        formatter.append(toFormattable(ellipsoid));
        if (formatter.getConvention().majorVersion() == 1) {
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
        formatter.newLine(); // For writing the ID[…] element on its own line.
        return "Datum";
    }
}
