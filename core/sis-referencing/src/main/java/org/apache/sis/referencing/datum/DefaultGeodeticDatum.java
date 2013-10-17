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
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.GeodeticObjects;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import java.util.Objects;


/**
 * Defines the location and precise orientation in 3-dimensional space of a defined ellipsoid
 * (or sphere) that approximates the shape of the earth. Used also for Cartesian coordinate
 * system centered in this ellipsoid (or sphere).
 *
 * {@section Creating new geodetic datum instances}
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a geodetic datum.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code GeodeticDatum} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.GeodeticObjects#datum()}.</li>
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
 *     GeodeticDatum datum = GeodeticObjects.WGS84.datum();
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see DefaultEllipsoid
 * @see DefaultPrimeMeridian
 * @see org.apache.sis.referencing.GeodeticObjects#datum()
 */
@Immutable
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
     * {@link #getAffineTransform(GeodeticDatum) datum shifts}.
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
     * Bursa Wolf parameters for datum shifts, or {@code null} if none.
     */
    private final BursaWolfParameters[] bursaWolf;

    /**
     * Creates a geodetic datum using the Greenwich prime meridian. This is a convenience constructor for
     * {@link #DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian) DefaultGeodeticDatum(Map, …)}
     * with a map containing only the {@value org.opengis.referencing.IdentifiedObject#NAME_KEY} property
     * and the {@link #getPrimeMeridian() prime meridian} fixed to Greenwich.
     *
     * @param name      The datum name.
     * @param ellipsoid The ellipsoid.
     */
    public DefaultGeodeticDatum(final String name, final Ellipsoid ellipsoid) {
        this(Collections.singletonMap(NAME_KEY, name), ellipsoid, GeodeticObjects.WGS84.primeMeridian());
    }

    /**
     * Creates a geodetic datum from the given properties. The properties map is given
     * unchanged to the {@link AbstractDatum#AbstractDatum(Map) super-class constructor}.
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
     *     <td>{@link BursaWolfParameters} or {@code BursaWolfParameters[]}</td>
     *     <td>{@link #getBursaWolfParameters()}</td>
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
     * Returns all Bursa Wolf parameters specified in the {@code properties} map at construction time.
     *
     * @return The Bursa Wolf parameters, or an empty array if none.
     */
    public BursaWolfParameters[] getBursaWolfParameters() {
        return (bursaWolf != null) ? bursaWolf.clone() : EMPTY_ARRAY;
    }

    /**
     * Returns Bursa Wolf parameters for a datum shift toward the specified target, or {@code null} if none.
     * This method searches only for Bursa-Wolf parameters explicitly specified in the {@code properties} map
     * given at construction time. This method doesn't try to infer a set of parameters from indirect informations.
     * For example it does not try to inverse the parameters specified in the {@code target} datum if none were found
     * in this datum. If a more elaborated search is wanted, use {@link #getAffineTransform(GeodeticDatum)} instead.
     *
     * @param  target The target geodetic datum.
     * @return Bursa Wolf parameters from this datum to the given target datum, or {@code null} if none.
     */
    public BursaWolfParameters getBursaWolfParameters(final GeodeticDatum target) {
        if (bursaWolf != null) {
            for (final BursaWolfParameters candidate : bursaWolf) {
                if (deepEquals(target, candidate.targetDatum, ComparisonMode.IGNORE_METADATA)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns a direct reference to the {@link #bursaWolf} of the given datum if it exists,
     * or {@code null} otherwise. This method does not clone the array - do not modify!
     */
    private static BursaWolfParameters[] bursaWolf(final GeodeticDatum datum) {
        return (datum instanceof DefaultGeodeticDatum) ? ((DefaultGeodeticDatum) datum).bursaWolf : null;
    }

    /**
     * Returns a matrix that can be used to define a transformation to the specified datum.
     * If no transformation path is found, then this method returns {@code null}.
     *
     * @param  targetDatum The target datum.
     * @return An affine transform from {@code this} to {@code target}, or {@code null} if none.
     *
     * @see BursaWolfParameters#getAffineTransform()
     */
    public Matrix getAffineTransform(final GeodeticDatum targetDatum) {
        ensureNonNull("targetDatum", targetDatum);
        try {
            return getAffineTransform(this, targetDatum, null);
        } catch (NoninvertibleMatrixException e) {
            /*
             * Should never happen, unless the user has overriden BursaWolfParameters.getAffineTransform()
             * and create an invalid matrix. Returning 'null' is compliant with this method contract.
             */
            Logging.unexpectedException(DefaultGeodeticDatum.class, "getAffineTransform", e);
            return null;
        }
    }

    /**
     * Returns a matrix that can be used to define a transformation to the specified datum.
     * If no transformation path is found, then this method returns {@code null}.
     *
     * @param  source The source datum, or {@code null}.
     * @param  target The target datum, or {@code null}.
     * @param  exclusion The set of datum to exclude from the search, or {@code null}.
     *         This is used in order to avoid never-ending recursivity.
     * @return An affine transform from {@code source} to {@code target}, or {@code null} if none.
     */
    private static Matrix getAffineTransform(final GeodeticDatum source, final GeodeticDatum target,
            Set<GeodeticDatum> exclusion) throws NoninvertibleMatrixException
    {
        final BursaWolfParameters[] sourceParam = bursaWolf(source);
        if (sourceParam != null) {
            for (final BursaWolfParameters candidate : sourceParam) {
                if (deepEquals(target, candidate.targetDatum, ComparisonMode.IGNORE_METADATA)) {
                    return candidate.getAffineTransform();
                }
            }
        }
        /*
         * No transformation found to the specified target datum.
         * Search if a transform exists in the opposite direction.
         */
        final BursaWolfParameters[] targetParam = bursaWolf(target);
        if (targetParam != null) {
            for (final BursaWolfParameters candidate : targetParam) {
                if (deepEquals(source, candidate.targetDatum, ComparisonMode.IGNORE_METADATA)) {
                    return MatrixSIS.castOrCopy(candidate.getAffineTransform()).inverse();
                }
            }
        }
        /*
         * No direct tranformation found. Search for a path through some intermediate datum.
         * First, search if there is some BursaWolfParameters for the same target in both
         * 'source' and 'target' datum. If such an intermediate is found, ask for a path as below:
         *
         *    source   →   [common datum]   →   target
         */
        if (sourceParam != null && targetParam != null) {
            for (int i=0; i<sourceParam.length; i++) {
                final GeodeticDatum sourceStep = sourceParam[i].targetDatum;
                for (int j=0; j<targetParam.length; j++) {
                    final GeodeticDatum targetStep = targetParam[j].targetDatum;
                    if (deepEquals(sourceStep, targetStep, ComparisonMode.IGNORE_METADATA)) {
                        if (exclusion == null) {
                            exclusion = new HashSet<>();
                        }
                        if (exclusion.add(source)) {
                            if (exclusion.add(target)) {
                                final Matrix step1 = getAffineTransform(source, sourceStep, exclusion);
                                if (step1 != null) {
                                    final Matrix step2 = getAffineTransform(targetStep, target, exclusion);
                                    if (step2 != null) {
                                        /*
                                         * MatrixSIS.multiply(MatrixSIS) is equivalent to AffineTransform.concatenate(…):
                                         * First transform by the supplied transform and then transform the result
                                         * by the original transform.
                                         */
                                        return MatrixSIS.castOrCopy(step2).multiply(step1);
                                    }
                                }
                                exclusion.remove(target);
                            }
                            exclusion.remove(source);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Compare this datum with the specified object for equality.
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
                     * HACK: We do not consider Bursa Wolf parameters as a non-metadata field.
                     *       This is needed in order to get equalsIgnoreMetadata(...) to returns
                     *       'true' when comparing the WGS84 constant in this class with a WKT
                     *       DATUM element with a TOWGS84[0,0,0,0,0,0,0] element. Furthermore,
                     *       the Bursa Wolf parameters are not part of ISO 19111 specification.
                     *       We don't want two CRS to be considered as different because one has
                     *       more of those transformation informations (which is nice, but doesn't
                     *       change the CRS itself).
                     */
                }
            }
        }
        return false;
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     *
     * @return The hash code value for the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        return (Objects.hashCode(ellipsoid) * 31 + Objects.hashCode(primeMeridian)) * 31 + super.hashCode(mode);
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT) element.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "DATUM"}.
     */
    @Override
    public String formatTo(final Formatter formatter) {
        // Do NOT invokes the super-class method, because
        // horizontal datum do not write the datum type.
        formatter.append(ellipsoid);
        if (bursaWolf != null) {
            for (final BursaWolfParameters candidate : bursaWolf) {
                if (candidate.isToWGS84()) {
                    formatter.append(candidate);
                    break;
                }
            }
        }
        return "DATUM";
    }
}
