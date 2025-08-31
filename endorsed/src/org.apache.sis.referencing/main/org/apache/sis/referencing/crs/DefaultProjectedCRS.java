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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;                 // For javadoc
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.Projection;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * A 2-dimensional coordinate reference system used to approximate the shape of the earth on a planar surface.
 * It is done in such a way that the distortion that is inherent to the approximation is carefully controlled and known.
 * Distortion correction is commonly applied to calculated bearings and distances to produce values
 * that are a close match to actual field values.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum Geodetic}.<br>
 * <b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}.
 * </p>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createProjectedCRS(String)
 *
 * @since 0.6
 */
@XmlType(name = "ProjectedCRSType", propOrder = {
    "baseCRS",
    "coordinateSystem"
})
@XmlRootElement(name = "ProjectedCRS")
public class DefaultProjectedCRS extends AbstractDerivedCRS<Projection> implements ProjectedCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4502680112031773028L;

    /**
     * Creates a projected CRS from a defining conversion.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractCRS#AbstractCRS(Map, CoordinateSystem) super-class constructor}.
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
     * The supplied {@code conversion} argument shall <strong>not</strong> includes the operation steps
     * for performing {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes unit
     * conversions and change of axis order} since those operations will be inferred by this constructor.
     *
     * @param  properties     the properties to be given to the new derived CRS object.
     * @param  baseCRS        coordinate reference system to base the derived CRS on.
     * @param  baseToDerived  the defining conversion from a {@linkplain AxesConvention#NORMALIZED normalized}
     *                        base to a normalized derived CRS.
     * @param  derivedCS      the coordinate system for the derived CRS. The number of axes must match
     *                        the target dimension of the {@code baseToDerived} transform.
     * @throws MismatchedDimensionException if the source and target dimensions of {@code baseToDerived}
     *         do not match the dimensions of {@code base} and {@code derivedCS} respectively.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createProjectedCRS(Map, GeographicCRS, Conversion, CartesianCS)
     */
    public DefaultProjectedCRS(final Map<String,?> properties,
                               final GeographicCRS baseCRS,
                               final Conversion    baseToDerived,
                               final CartesianCS   derivedCS)
            throws MismatchedDimensionException
    {
        super(properties, checkDimensions(baseCRS, derivedCS), baseToDerived, derivedCS);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static GeodeticCRS checkDimensions(final GeodeticCRS baseCRS, final CartesianCS derivedCS) {
        int n = ReferencingUtilities.getDimension(baseCRS);
        if (derivedCS != null) {
            n = Math.max(n, derivedCS.getDimension());
        }
        n = Math.max(2, Math.min(3, n));
        ArgumentChecks.ensureDimensionMatches("baseCRS",   n, baseCRS);
        ArgumentChecks.ensureDimensionMatches("derivedCS", n, derivedCS);
        return baseCRS;
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    private DefaultProjectedCRS(final DefaultProjectedCRS original, final AbstractCS derivedCS) {
        super(original, derivedCS);
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
     * @see #castOrCopy(ProjectedCRS)
     */
    protected DefaultProjectedCRS(final ProjectedCRS crs) {
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
    public static DefaultProjectedCRS castOrCopy(final ProjectedCRS object) {
        return (object == null) || (object instanceof DefaultProjectedCRS)
                ? (DefaultProjectedCRS) object : new DefaultProjectedCRS(object);
    }

    /**
     * Returns the type of conversion associated to this {@code DefaultProjectedCRS}.
     * Must be a hard-coded, constant value (not dependent on object state).
     */
    @Override
    final Class<Projection> getConversionType() {
        return Projection.class;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ProjectedCRS.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ProjectedCRS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with
     * their own set of interfaces.
     *
     * @return {@code ProjectedCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ProjectedCRS> getInterface() {
        return ProjectedCRS.class;
    }

    /**
     * Returns the datum of the base <abbr>CRS</abbr>.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     *
     * @return the datum of the {@linkplain #getBaseCRS() base CRS}, or {@code null} if this <abbr>CRS</abbr>
     *         is related to an object identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     */
    @Override
    public GeodeticDatum getDatum() {
        return getBaseCRS().getDatum();
    }

    /**
     * Returns the datum ensemble of the base <abbr>CRS</abbr>.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatum() reference frame}.
     *
     * @return the datum ensemble of the {@linkplain #getBaseCRS() base CRS}, or {@code null} if this
     *         <abbr>CRS</abbr> is related to an object identified only by a {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DatumEnsemble<GeodeticDatum> getDatumEnsemble() {
        return getBaseCRS().getDatumEnsemble();
    }

    /**
     * Returns the datum or a view of the ensemble as a datum.
     * The {@code legacy} argument tells whether this method is invoked for formatting in a legacy <abbr>WKT</abbr> format.
     */
    @Override
    final GeodeticDatum getDatumOrEnsemble(final boolean legacy) {
        return legacy ? DatumOrEnsemble.asDatum(getBaseCRS()) : getDatum();
    }

    /**
     * Returns the geographic CRS on which the map projection is applied.
     * This CRS defines the {@linkplain #getDatum() datum} of this CRS and (at least implicitly)
     * the {@linkplain org.apache.sis.referencing.operation.DefaultConversion#getSourceCRS() source}
     * of the {@linkplain #getConversionFromBase() conversion from base}.
     *
     * @return the base coordinate reference system, which must be geographic.
     */
    @Override
    @XmlElement(name = "baseGeodeticCRS", required = true)        // Note: older GML version used "baseGeographicCRS".
    public GeographicCRS getBaseCRS() {
        final Projection projection = super.getConversionFromBase();
        return (projection != null) ? (GeographicCRS) projection.getSourceCRS() : null;
    }

    /**
     * Returns the map projection from the {@linkplain #getBaseCRS() base CRS} to this CRS.
     * In Apache SIS, the conversion source and target CRS are set to the following values:
     *
     * <ul>
     *   <li>The conversion {@linkplain org.apache.sis.referencing.operation.DefaultConversion#getSourceCRS()
     *       source CRS} is the {@linkplain #getBaseCRS() base CRS} of {@code this} CRS.</li>
     *   <li>The conversion {@linkplain org.apache.sis.referencing.operation.DefaultConversion#getTargetCRS()
     *       target CRS} is {@code this} CRS.
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This is different than ISO 19111, which allows source and target CRS to be {@code null}.</div>
     *
     * @return the map projection from base CRS to this CRS.
     */
    @Override
    public Projection getConversionFromBase() {
        return super.getConversionFromBase();
    }

    /**
     * Returns the coordinate system.
     */
    @Override
    @XmlElement(name = "cartesianCS", required = true)
    public final CartesianCS getCoordinateSystem() {
        /*
         * See AbstractDerivedCRS.createConversionFromBase(…) for
         * an explanation about why this method is declared final.
         */
        return (CartesianCS) super.getCoordinateSystem();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultProjectedCRS forConvention(final AxesConvention convention) {
        return (DefaultProjectedCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultProjectedCRS(this, cs);
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     * In addition to the metadata documented in the
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#equals(Object, ComparisonMode) parent class},
     * this method considers coordinate system axes of the {@linkplain #getBaseCRS() base CRS} as metadata.
     * This means that if the given {@code ComparisonMode} is {@code IGNORE_METADATA} or more permissive,
     * then axis order of the base geodetic <abbr>CRS</abbr> is ignored
     * (but <strong>not</strong> axis order of <strong>this</strong> projected CRS).
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return super.equals(object, mode);
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode();
    }

    /**
     * Formats the inner part of the <i>Well Known Text</i> (WKT) representation of this CRS.
     *
     * <h4>Example</h4>
     * Well-Known Text (version 2)
     * of a projected coordinate reference system using the Lambert Conformal method.
     *
     * {@snippet lang="wkt" :
     *   ProjectedCRS[“NTF (Paris) / Lambert zone II”,
     *     BaseGeodCRS[“NTF (Paris)”,
     *       Datum[“Nouvelle Triangulation Francaise”,
     *         Ellipsoid[“NTF”, 6378249.2, 293.4660212936269, LengthUnit[“metre”, 1]]],
     *         PrimeMeridian[“Paris”, 2.5969213, AngleUnit[“grad”, 0.015707963267948967]]],
     *     Conversion[“Lambert zone II”,
     *       Method[“Lambert Conic Conformal (1SP)”, Id[“EPSG”, 9801, Citation[“IOGP”]]],
     *       Parameter[“Latitude of natural origin”, 52.0, AngleUnit[“grad”, 0.015707963267948967], Id[“EPSG”, 8801]],
     *       Parameter[“Longitude of natural origin”, 0.0, AngleUnit[“degree”, 0.017453292519943295], Id[“EPSG”, 8802]],
     *       Parameter[“Scale factor at natural origin”, 0.99987742, ScaleUnit[“unity”, 1], Id[“EPSG”, 8805]],
     *       Parameter[“False easting”, 600000.0, LengthUnit[“metre”, 1], Id[“EPSG”, 8806]],
     *       Parameter[“False northing”, 2200000.0, LengthUnit[“metre”, 1], Id[“EPSG”, 8807]]],
     *     CS[“Cartesian”, 2],
     *       Axis[“Easting (E)”, east, Order[1]],
     *       Axis[“Northing (N)”, north, Order[2]],
     *       LengthUnit[“metre”, 1],
     *     Id[“EPSG”, 27572, Citation[“IOGP”], URI[“urn:ogc:def:crs:EPSG::27572”]]]
     *   }
     *
     * <p>Same coordinate reference system using WKT 1.</p>
     *
     * {@snippet lang="wkt" :
     *   PROJCS[“NTF (Paris) / Lambert zone II”,
     *     GEOGCS[“NTF (Paris)”,
     *       DATUM[“Nouvelle Triangulation Francaise”,
     *         SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],
     *         PRIMEM[“Paris”, 2.33722917],
     *       UNIT[“degree”, 0.017453292519943295],
     *       AXIS[“Longitude”, EAST],
     *       AXIS[“Latitude”, NORTH]],
     *     PROJECTION[“Lambert_Conformal_Conic_1SP”, AUTHORITY[“EPSG”, “9801”]],
     *     PARAMETER[“latitude_of_origin”, 46.8],
     *     PARAMETER[“central_meridian”, 0.0],
     *     PARAMETER[“scale_factor”, 0.99987742],
     *     PARAMETER[“false_easting”, 600000.0],
     *     PARAMETER[“false_northing”, 2200000.0],
     *     UNIT[“metre”, 1],
     *     AXIS[“Easting”, EAST],
     *     AXIS[“Northing”, NORTH],
     *     AUTHORITY[“EPSG”, “27572”]]
     *   }
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "ProjectedCRS"} (WKT 2) or {@code "ProjCS"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#57">WKT 2 specification §9</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        if (super.getConversionFromBase() == null) {
            /*
             * Should never happen except temporarily at construction time, or if the user invoked the copy constructor
             * with an invalid Conversion. Delegates to the super-class method for avoiding a NullPointerException.
             * That method returns 'null', which will cause the WKT to be declared invalid.
             */
            return super.formatTo(formatter);
        }
        WKTUtilities.appendName(this, formatter, null);
        final Convention  convention  = formatter.getConvention();
        final boolean     isWKT1      = (convention.majorVersion() == 1);
        final CartesianCS cs          = getCoordinateSystem();
        final GeodeticCRS baseCRS     = getBaseCRS();
        final Unit<?>     lengthUnit  = ReferencingUtilities.getUnit(cs);
        final Unit<Angle> angularUnit = AxisDirections.getAngularUnit(baseCRS.getCoordinateSystem(), null);
        final Unit<Angle> oldAngle    = formatter.addContextualUnit(angularUnit);
        final Unit<?>     oldLength   = formatter.addContextualUnit(lengthUnit);
        /*
         * Format the enclosing base CRS. Note that WKT 1 formats a full GeographicCRS while WKT 2 formats only
         * the datum with the prime meridian (no coordinate system) and uses a different keyword ("BaseGeodCRS"
         * instead of "GeodeticCRS"). The DefaultGeodeticCRS.formatTo(Formatter) method detects when the CRS to
         * format is part of an enclosing ProjectedCRS and will adapt accordingly.
         */
        formatter.newLine();
        formatter.append(WKTUtilities.toFormattable(baseCRS));
        formatter.newLine();
        final ExplicitParameters p = new ExplicitParameters(this, WKTKeywords.Conversion);
        final boolean isBaseCRS;
        if (isWKT1) {
            p.append(formatter);                        // Format outside of any "Conversion" element.
            isBaseCRS = false;
        } else {
            formatter.append(p);                        // Format inside a "Conversion" element.
            isBaseCRS = isBaseCRS(formatter);
        }
        /*
         * In WKT 2 format, the coordinate system axes are written only if this projected CRS is not the base CRS
         * of another derived CRS.
         */
        if (!isBaseCRS || convention == Convention.INTERNAL) {
            formatCS(formatter, cs, lengthUnit, isWKT1);
        }
        formatter.restoreContextualUnit(lengthUnit, oldLength);
        formatter.restoreContextualUnit(angularUnit, oldAngle);
        return isWKT1 ? WKTKeywords.ProjCS : isBaseCRS ? WKTKeywords.BaseProjCRS
                : formatter.shortOrLong(WKTKeywords.ProjCRS, WKTKeywords.ProjectedCRS);
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
     * Constructs a new object in which every attributes are set to a default value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultProjectedCRS() {
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getBaseCRS()
     */
    private void setBaseCRS(final GeographicCRS crs) {
        setBaseCRS("baseGeodeticCRS", crs);
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCoordinateSystem()
     */
    private void setCoordinateSystem(final CartesianCS cs) {
        setCoordinateSystem("cartesianCS", cs);
    }
}
