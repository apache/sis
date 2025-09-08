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
import java.util.Arrays;
import java.util.NoSuchElementException;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.GeodeticException;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Longitude;
import static org.apache.sis.util.privy.Constants.CRS;
import static org.apache.sis.util.privy.Constants.EPSG;
import static org.apache.sis.util.privy.Constants.CRS27;
import static org.apache.sis.util.privy.Constants.CRS83;
import static org.apache.sis.util.privy.Constants.CRS84;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;


/**
 * A 2- or 3-dimensional coordinate reference system based on an ellipsoidal approximation of the geoid.
 * This provides an accurate representation of the geometry of geographic features
 * for a large portion of the earth's surface.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum Geodetic}.<br>
 * <b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}.
 * </p>
 *
 * <h2>Creating new geographic CRS instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * Choice 1 in the following list is the easiest but most restrictive way to get a geographic CRS.
 * The other choices provide more freedom.
 *
 * <ol>
 *   <li>Create a {@code GeographicCRS} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#geographic()} or
 *       {@link org.apache.sis.referencing.CommonCRS#geographic3D()}.</li>
 *   <li>Create a {@code GeographicCRS} from an identifier in a database by invoking
 *       {@link org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeographicCRS(String)}.</li>
 *   <li>Create a {@code GeographicCRS} by invoking the {@code CRSFactory.createGeographicCRS(…)} method
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code GeographicCRS} by invoking the
 *       {@linkplain #DefaultGeographicCRS(Map, GeodeticDatum, EllipsoidalCS) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets a two-dimensional geographic CRS
 * using the <cite>World Geodetic System 1984</cite> datum:
 *
 * {@snippet lang="java" :
 *     GeodeticDatum datum = CommonCRS.WGS84.geographic();
 *     }
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createGeographicCRS(String)
 *
 * @since 0.4
 */
@XmlTransient
public class DefaultGeographicCRS extends DefaultGeodeticCRS implements GeographicCRS {
    /**
     * Some codes in the EPSG namespace, in ascending order.
     * Used for mapping to {@link #CRS_CODES}.
     */
    private static final short[] EPSG_CODES = {4267, 4269, 4326};

    /**
     * Codes in the CRS namespace for each code listed in the {@link #EPSG_CODES} list.
     */
    private static final byte[] CRS_CODES = {CRS27, CRS83, CRS84};

    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 861224913438092335L;

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
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>"domains"</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
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
     * @param  cs          the two- or three-dimensional coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createGeographicCRS(Map, GeodeticDatum, DefaultDatumEnsemble, EllipsoidalCS)
     *
     * @since 1.5
     */
    public DefaultGeographicCRS(final Map<String,?> properties,
                                final GeodeticDatum datum,
                                final DefaultDatumEnsemble<GeodeticDatum> ensemble,
                                final EllipsoidalCS cs)
    {
        super(properties, datum, ensemble, cs);
        checkDimension(2, 3, cs);
    }

    /**
     * @deprecated A {@code DefaultDatumEnsemble} argument has been added.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public DefaultGeographicCRS(final Map<String,?> properties,
                                final GeodeticDatum datum,
                                final EllipsoidalCS cs)
    {
        this(properties, datum, null, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    private DefaultGeographicCRS(final DefaultGeographicCRS original, final ReferenceIdentifier id, final AbstractCS cs) {
        super(original, id, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified geodetic CRS.
     * This constructor is needed because GML does not have {@code GeographicCRS} or {@code GeocentricCRS} types.
     * Instead, the unmarshalling process will give us a {@code GeodeticCRS} object with the constraint that the
     * coordinate system shall be ellipsoidal. This constructor may be invoked for converting such
     * {@code GeodeticCRS} instance to a {@code GeographicCRS} instance.
     */
    DefaultGeographicCRS(final GeodeticCRS crs) {
        super(crs);
        final CoordinateSystem cs = super.getCoordinateSystem();
        if (!(cs instanceof EllipsoidalCS)) {
            throw illegalCoordinateSystemType(cs);
        }
        checkDimension(2, 3, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     *
     * @see #castOrCopy(GeographicCRS)
     */
    protected DefaultGeographicCRS(final GeographicCRS crs) {
        super(crs);
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
     */
    public static DefaultGeographicCRS castOrCopy(final GeographicCRS object) {
        return (object == null) || (object instanceof DefaultGeographicCRS)
                ? (DefaultGeographicCRS) object : new DefaultGeographicCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeographicCRS.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code GeographicCRS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code GeographicCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends GeographicCRS> getInterface() {
        return GeographicCRS.class;
    }

    /**
     * Returns the prime meridian which is indirectly (through a datum) associated to this <abbr>CRS</abbr>.
     * If the {@linkplain #getDatum() datum} is non-null, then this method returns the datum prime meridian.
     * Otherwise, if all members of the {@linkplain #getDatumEnsemble() datum ensemble} use the same prime meridian,
     * then this method returns that meridian.
     *
     * @return the prime meridian indirectly associated to this <abbr>CRS</abbr>.
     * @throws NoSuchElementException if there is no datum and the ensemble does not contain at least one member.
     * @throws GeodeticException if the prime meridian is not the same for all members of the datum ensemble.
     *
     * @since 1.5
     */
    public PrimeMeridian getPrimeMeridian() {
        return DatumOrEnsemble.asDatum(this).getPrimeMeridian();
    }

    /**
     * Returns the ellipsoid which is indirectly (through a datum) associated to this <abbr>CRS</abbr>.
     * If the {@linkplain #getDatum() datum} is non-null, then this method returns the datum ellipsoid.
     * Otherwise, if all members of the {@linkplain #getDatumEnsemble() datum ensemble} use the same ellipsoid,
     * then this method returns that ellipsoid.
     *
     * @return the ellipsoid indirectly associated to this <abbr>CRS</abbr>.
     * @throws NoSuchElementException if there is no datum and the ensemble does not contain at least one member.
     * @throws GeodeticException if the ellipsoid is not the same for all members of the datum ensemble.
     *
     * @since 1.5
     */
    public Ellipsoid getEllipsoid() {
        return DatumOrEnsemble.asDatum(this).getEllipsoid();
    }

    /**
     * Returns the geodetic reference frame associated to this geographic CRS.
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
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may
     * be changed to the {@code org.opengis.referencing.datum.DatumEnsemble} interface.
     * This change is pending GeoAPI revision.</div>
     *
     * @return the datum ensemble, or {@code null} if this <abbr>CRS</abbr> is related
     *         to an object identified only by a single {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DefaultDatumEnsemble<GeodeticDatum> getDatumEnsemble() {
        return super.getDatumEnsemble();
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
     */
    @Override
    public EllipsoidalCS getCoordinateSystem() {
        return (EllipsoidalCS) super.getCoordinateSystem();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultGeographicCRS forConvention(final AxesConvention convention) {
        return (DefaultGeographicCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     *
     * <h4>Special case</h4>
     * If the first axis is the longitude in the [-180 … +180]° range and the identifier is EPSG:4267,
     * EPSG:4269 or EPSG:4326, then this method magically add the CRS:27, CRS:83 or CRS:84 identifier.
     * Without this special case, the normal behavior would be no identifier. The expected behavior is
     * that {@code CommonCRS.WGS84.normalizedGeographic()} returns a CRS having the "CRS:84" identifier.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        ReferenceIdentifier id = null;
        final CoordinateSystemAxis axis = cs.getAxis(0);
        if (axis.getMinimumValue() == Longitude.MIN_VALUE &&
            axis.getMaximumValue() == Longitude.MAX_VALUE)    // For excluding the AxesConvention.POSITIVE_RANGE case.
        {
            for (final ReferenceIdentifier identifier : super.getIdentifiers()) {
                if (EPSG.equals(identifier.getCodeSpace())) try {
                    final int i = Arrays.binarySearch(EPSG_CODES, Short.parseShort(identifier.getCode()));
                    if (i >= 0) {
                        id = new ImmutableIdentifier(Citations.WMS, CRS, Short.toString(CRS_CODES[i]));
                        break;
                    }
                } catch (NumberFormatException e) {
                    // Okay to igore, because it is not the purpose of this method to disallow non-numeric codes.
                }
            }
        }
        return new DefaultGeographicCRS(this, id, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code GeographicCRS[…]} element.
     *
     * <h4>Example</h4>
     * Well-Known Text (version 2)
     * of a geographic coordinate reference system using the WGS 84 datum.
     *
     * {@snippet lang="wkt" :
     *   GeographicCRS["WGS 84",
     *      Datum["World Geodetic System 1984",
     *        Ellipsoid["WGS84", 6378137.0, 298.257223563, LengthUnit["metre", 1]]],
     *        PrimeMeridian["Greenwich", 0.0, AngleUnit["degree", 0.017453292519943295]],
     *      CS["ellipsoidal", 2],
     *        Axis["Latitude", north],
     *        Axis["Longitude", east],
     *        AngleUnit["degree", 0.017453292519943295],
     *      Area["World"],
     *      BBox[-90.00, -180.00, 90.00, 180.00],
     *      Scope["Used by GPS satellite navigation system."],
     *      Id["EPSG", 4326, Citation["IOGP"], URI["urn:ogc:def:crs:EPSG::4326"]]]
     *   }
     *
     * <p>Same coordinate reference system using WKT 1.</p>
     *
     * {@snippet lang="wkt" :
     *   GEOGCS["WGS 84",
     *      DATUM["World Geodetic System 1984",
     *        SPHEROID["WGS84", 6378137.0, 298.257223563]],
     *        PRIMEM["Greenwich", 0.0],
     *      UNIT["degree", 0.017453292519943295],
     *      AXIS["Latitude", NORTH],
     *      AXIS["Longitude", EAST],
     *      AUTHORITY["EPSG", "4326"]]
     *   }
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "GeodeticCRS"} (WKT 2) or {@code "GeogCS"} (WKT 1).
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
    private DefaultGeographicCRS() {
    }
}
