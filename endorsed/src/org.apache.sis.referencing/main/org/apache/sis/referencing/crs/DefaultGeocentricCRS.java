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
package org.apache.sis.referencing.crs;

import java.util.Map;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.io.wkt.Formatter;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * A 2- or 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
 * This class is used for geodetic CRS that are not geographic, and is named "geocentric" for historical reasons.
 * Note that ISO 19111 does not define a {@code GeocentricCRS} interface.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum Geodetic}.<br>
 * <b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS Spherical}.
 * </p>
 *
 * <h2>Creating new geocentric CRS instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a geocentric CRS.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code GeodeticCRS} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#geocentric()}.</li>
 *   <li>Create a {@code GeodeticCRS} from an identifier in a database by invoking
 *       {@link org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeodeticCRS(String)}.</li>
 *   <li>Create a {@code GeodeticCRS} by invoking the {@code CRSFactory.createGeodeticCRS(…)} method
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code GeodeticCRS} by invoking the
 *       {@linkplain #DefaultGeocentricCRS(Map, GeodeticDatum, CartesianCS) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a geocentric CRS using the <cite>World Geodetic System 1984</cite> datum:
 *
 * {@snippet lang="java" :
 *     GeodeticDatum datum = CommonCRS.WGS84.geocentric();
 *     }
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeodeticCRS(String)
 *
 * @since 0.4
 */
@XmlTransient
public class DefaultGeocentricCRS extends DefaultGeodeticCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6784642848287659827L;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
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
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system, which must be three-dimensional.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeodeticCRS(Map, GeodeticDatum, DatumEnsemble, CartesianCS)
     *
     * @since 1.5
     */
    public DefaultGeocentricCRS(final Map<String,?> properties,
                                final GeodeticDatum datum,
                                final DatumEnsemble<GeodeticDatum> ensemble,
                                final CartesianCS cs)
    {
        super(properties, datum, ensemble, cs);
        checkDimension(3, 3, cs);
    }

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The properties given in argument are the same as for the
     * {@linkplain #DefaultGeocentricCRS(Map, GeodeticDatum, CartesianCS) above constructor}.
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeodeticCRS(Map, GeodeticDatum, DatumEnsemble, SphericalCS)
     *
     * @since 1.5
     */
    public DefaultGeocentricCRS(final Map<String,?> properties,
                                final GeodeticDatum datum,
                                final DatumEnsemble<GeodeticDatum> ensemble,
                                final SphericalCS cs)
    {
        super(properties, datum, ensemble, cs);
        checkDimension(2, 3, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     * This constructor does not verify the coordinate system type.
     */
    private DefaultGeocentricCRS(final DefaultGeocentricCRS original, final AbstractCS cs) {
        super(original, null, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     * @throws IllegalArgumentException if the coordinate system of the given CRS is not Cartesian or spherical.
     *
     * @see #castOrCopy(GeodeticCRS)
     */
    @SuppressWarnings("this-escape")
    protected DefaultGeocentricCRS(final GeodeticCRS crs) {
        super(crs);
        CoordinateSystem cs = super.getCoordinateSystem();
        if (cs instanceof CartesianCS) {
            ArgumentChecks.ensureDimensionMatches("crs.coordinateSystem", 3, cs);
        } else if (!(cs instanceof SphericalCS)) {
            throw illegalCoordinateSystemType(cs);
        }
        checkDimension(2, 3, cs);
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values as the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     * @throws IllegalArgumentException if the coordinate system of the given CRS is not Cartesian or spherical.
     */
    public static DefaultGeocentricCRS castOrCopy(final GeodeticCRS object) {
        return (object == null) || (object instanceof DefaultGeocentricCRS)
                ? (DefaultGeocentricCRS) object : new DefaultGeocentricCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeodeticCRS.class}.
     *
     * @return {@code GeodeticCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends GeodeticCRS> getInterface() {
        return super.getInterface();
    }

    /**
     * Returns the geodetic reference frame associated to this geocentric CRS.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     *
     * @return the geodetic reference frame, or {@code null} if this <abbr>CRS</abbr> is related to
     *         an object identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     */
    @Override
    public final GeodeticDatum getDatum() {
        return super.getDatum();
    }

    /**
     * Returns a collection of datums which, for low accuracy requirements,
     * may be considered to be insignificantly different from each other.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a single {@linkplain #getDatum() datum}.
     *
     * @return the datum ensemble, or {@code null} if this <abbr>CRS</abbr> is related
     *         to an object identified only by a single {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DatumEnsemble<GeodeticDatum> getDatumEnsemble() {
        return super.getDatumEnsemble();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultGeocentricCRS forConvention(final AxesConvention convention) {
        return (DefaultGeocentricCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultGeocentricCRS(this, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code GeodeticCRS[…]} element.
     *
     * <h4>Example</h4>
     * Well-Known Text (version 2)
     * of a geocentric coordinate reference system using the WGS 84 datum.
     *
     * {@snippet lang="wkt" :
     *   GeodeticCRS["Geocentric",
     *     Datum["World Geodetic System 1984",
     *       Ellipsoid["WGS84", 6378137.0, 298.257223563, LengthUnit["metre", 1]]],
     *       PrimeMeridian["Greenwich", 0.0, AngleUnit["degree", 0.017453292519943295]],
     *     CS["Cartesian", 3],
     *       Axis["(X)", geocentricX],
     *       Axis["(Y)", geocentricY],
     *       Axis["(Z)", geocentricZ],
     *       LengthUnit["metre", 1]]
     *   }
     *
     * <p>Same coordinate reference system using WKT 1. Note that axis directions are totally different.</p>
     *
     * {@snippet lang="wkt" :
     *   GEOCCS["Geocentric",
     *     DATUM["World Geodetic System 1984",
     *       SPHEROID["WGS84", 6378137.0, 298.257223563]],
     *       PRIMEM["Greenwich", 0.0],
     *     UNIT["metre", 1],
     *     AXIS["X", OTHER],
     *     AXIS["Y", EAST],
     *     AXIS["Z", NORTH]]
     *   }
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "GeodeticCRS"} (WKT 2) or {@code "GeocCS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        return super.formatTo(formatter);
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultGeocentricCRS() {
    }
}
