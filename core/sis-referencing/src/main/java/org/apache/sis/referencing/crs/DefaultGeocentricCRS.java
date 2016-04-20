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
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.AbstractReferenceSystem;


/**
 * A 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
 * A geocentric CRS deals with the earth's curvature by taking a 3-dimensional spatial view, which obviates
 * the need to model the earth's curvature.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum Geodetic}.<br>
 * <b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS Spherical}.
 * </p>
 *
 * <div class="section">Creating new geocentric CRS instances</div>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a geocentric CRS.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code GeocentricCRS} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#geocentric()}.</li>
 *   <li>Create a {@code GeocentricCRS} from an identifier in a database by invoking
 *       {@link org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeocentricCRS(String)}.</li>
 *   <li>Create a {@code GeocentricCRS} by invoking the {@code CRSFactory.createGeocentricCRS(…)} method
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code GeocentricCRS} by invoking the
 *       {@linkplain #DefaultGeocentricCRS(Map, GeodeticDatum, CartesianCS) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a geocentric CRS using the <cite>World Geodetic System 1984</cite> datum:
 *
 * {@preformat java
 *     GeodeticDatum datum = CommonCRS.WGS84.geocentric();
 * }
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeocentricCRS(String)
 */
@XmlTransient
public class DefaultGeocentricCRS extends DefaultGeodeticCRS implements GeocentricCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6784642848287659827L;

    /**
     * For {@link #createSameType(Map, CoordinateSystem)} usage only.
     * This constructor does not verify the coordinate system type.
     */
    private DefaultGeocentricCRS(final Map<String,?>    properties,
                                 final GeodeticDatum    datum,
                                 final CoordinateSystem cs)
    {
        super(properties, datum, cs);
    }

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
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
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param datum The datum.
     * @param cs The coordinate system, which must be three-dimensional.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeocentricCRS(Map, GeodeticDatum, CartesianCS)
     */
    public DefaultGeocentricCRS(final Map<String,?> properties,
                                final GeodeticDatum datum,
                                final CartesianCS   cs)
    {
        super(properties, datum, cs);
    }

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument are the same than for the
     * {@linkplain #DefaultGeocentricCRS(Map, GeodeticDatum, CartesianCS) above constructor}.
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param datum The datum.
     * @param cs The coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeocentricCRS(Map, GeodeticDatum, SphericalCS)
     */
    public DefaultGeocentricCRS(final Map<String,?> properties,
                                final GeodeticDatum datum,
                                final SphericalCS   cs)
    {
        super(properties, datum, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param crs The coordinate reference system to copy.
     *
     * @see #castOrCopy(GeocentricCRS)
     */
    protected DefaultGeocentricCRS(final GeocentricCRS crs) {
        super(crs);
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values than the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGeocentricCRS castOrCopy(final GeocentricCRS object) {
        return (object == null) || (object instanceof DefaultGeocentricCRS)
                ? (DefaultGeocentricCRS) object : new DefaultGeocentricCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeocentricCRS.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code GeocentricCRS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code GeocentricCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends GeocentricCRS> getInterface() {
        return GeocentricCRS.class;
    }

    /**
     * Returns the geodetic datum associated to this geocentric CRS.
     * This is the datum given at construction time.
     *
     * @return The geodetic datum associated to this geocentric CRS.
     */
    @Override
    public final GeodeticDatum getDatum() {
        return super.getDatum();
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
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new DefaultGeocentricCRS(properties, super.getDatum(), cs);
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code GeodeticCRS[…]} element.
     *
     * <div class="note"><b>Example:</b> Well-Known Text (version 2)
     * of a geocentric coordinate reference system using the WGS 84 datum.
     *
     * {@preformat wkt
     *   GeodeticCRS["Geocentric",
     *     Datum["World Geodetic System 1984",
     *       Ellipsoid["WGS84", 6378137.0, 298.257223563, LengthUnit["metre", 1]]],
     *       PrimeMeridian["Greenwich", 0.0, AngleUnit["degree", 0.017453292519943295]],
     *     CS["Cartesian", 3],
     *       Axis["(X)", geocentricX],
     *       Axis["(Y)", geocentricY],
     *       Axis["(Z)", geocentricZ],
     *       LengthUnit["metre", 1]]
     * }
     *
     * <p>Same coordinate reference system using WKT 1. Note that axis directions are totally different.</p>
     *
     * {@preformat wkt
     *   GEOCCS["Geocentric",
     *     DATUM["World Geodetic System 1984",
     *       SPHEROID["WGS84", 6378137.0, 298.257223563]],
     *       PRIMEM["Greenwich", 0.0],
     *     UNIT["metre", 1],
     *     AXIS["X", OTHER],
     *     AXIS["Y", EAST],
     *     AXIS["Z", NORTH]]
     * }
     * </div>
     *
     * @return {@code "GeodeticCRS"} (WKT 2) or {@code "GeocCS"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#49">WKT 2 specification §8</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        return super.formatTo(formatter);
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultGeocentricCRS() {
    }
}
