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
import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem; // For javadoc
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.internal.referencing.WKTUtilities.toFormattable;


/**
 * A 2D coordinate reference system used to approximate the shape of the earth on a planar surface.
 * It is done in such a way that the distortion that is inherent to the approximation is carefully
 * controlled and known. Distortion correction is commonly applied to calculated bearings and
 * distances to produce values that are a close match to actual field values.
 *
 * <p><b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}.
 * </p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlRootElement(name = "ProjectedCRS")
public class DefaultProjectedCRS extends AbstractDerivedCRS implements ProjectedCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4502680112031773028L;

    /**
     * Constructs a new object in which every attributes are set to a default value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultProjectedCRS() {
    }

    /**
     * Creates a projected CRS from a defining conversion.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractCRS#AbstractCRS(Map, CoordinateSystem) super-class constructor}.
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
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the new derived CRS object.
     * @param  baseCRS Coordinate reference system to base the derived CRS on.
     * @param  conversionFromBase The conversion from the base CRS to this derived CRS.
     * @param  derivedCS The coordinate system for the derived CRS. The number of axes
     *         must match the target dimension of the {@code baseToDerived} transform.
     * @throws MismatchedDimensionException if the source and target dimension of {@code baseToDerived}
     *         do not match the dimension of {@code base} and {@code derivedCS} respectively.
     */
    public DefaultProjectedCRS(final Map<String,?> properties,
                               final Conversion    conversionFromBase,
                               final GeographicCRS baseCRS,
                               final CartesianCS   derivedCS)
            throws MismatchedDimensionException
    {
        super(properties, baseCRS, conversionFromBase, derivedCS, Projection.class);
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
     * @see #castOrCopy(ProjectedCRS)
     */
    protected DefaultProjectedCRS(final ProjectedCRS crs) {
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
    public static DefaultProjectedCRS castOrCopy(final ProjectedCRS object) {
        return (object == null) || (object instanceof DefaultProjectedCRS)
                ? (DefaultProjectedCRS) object : new DefaultProjectedCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ProjectedCRS.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ProjectedCRS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with
     * their own set of interfaces.</div>
     *
     * @return {@code ProjectedCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ProjectedCRS> getInterface() {
        return ProjectedCRS.class;
    }

    /**
     * Returns the datum of the {@linkplain #getBaseCRS() base CRS}.
     *
     * @return The datum of the base CRS.
     */
    @Override
    @XmlElement(name="geodeticDatum")
    public GeodeticDatum getDatum() {
        return (GeodeticDatum) super.getDatum();
    }

    /**
     * Used by JAXB only (invoked by reflection).
     */
    private void setDatum(final GeodeticDatum datum) {
        // TODO
    }

    /**
     * Returns the base coordinate reference system, which must be geographic.
     *
     * @return The base CRS.
     */
    @Override
    public GeographicCRS getBaseCRS() {
        return (GeographicCRS) super.getBaseCRS();
    }

    /**
     * Returns the map projection from the {@linkplain #getBaseCRS() base CRS} to this CRS.
     *
     * @return The map projection from base CRS to this CRS.
     */
    @Override
    public Projection getConversionFromBase() {
        return (Projection) super.getConversionFromBase();
    }

    /**
     * Returns the coordinate system.
     */
    @Override
    @XmlElement(name="cartesianCS")
    public CartesianCS getCoordinateSystem() {
        return (CartesianCS) super.getCoordinateSystem();
    }

    /**
     * Used by JAXB only (invoked by reflection).
     */
    private void setCoordinateSystem(final CartesianCS cs) {
        setCoordinateSystem("cartesianCS", cs);
    }

    /**
     * Formats the inner part of the <cite>Well Known Text</cite> (WKT) representation of this CRS.
     *
     * @return {@code "ProjCS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, null);
        final Conversion    conversion  = getConversionFromBase();
        final GeographicCRS baseCRS     = getBaseCRS();
        final Ellipsoid     ellipsoid   = getDatum().getEllipsoid();
        final Unit<?>       unit        = ReferencingUtilities.getUnit(getCoordinateSystem());
        final Unit<Angle>   geoUnit     = ReferencingUtilities.getAngularUnit(baseCRS.getCoordinateSystem());
        final Unit<?>       linearUnit  = formatter.addContextualUnit(unit);
        final Unit<Angle>   angularUnit = formatter.addContextualUnit(geoUnit);
        final Unit<Length>  axisUnit    = ellipsoid.getAxisUnit();
        formatter.newLine();
        formatter.append(toFormattable(baseCRS));
        formatter.newLine();
        formatter.append(DefaultOperationMethod.castOrCopy(conversion));
        formatter.newLine();
        for (final GeneralParameterValue param : conversion.getParameterValues().values()) {
            final GeneralParameterDescriptor desc = param.getDescriptor();
            String name;
            if (IdentifiedObjects.isHeuristicMatchForName(desc, name = Constants.SEMI_MAJOR) ||
                IdentifiedObjects.isHeuristicMatchForName(desc, name = Constants.SEMI_MINOR))
            {
                /*
                 * Do not format semi-major and semi-minor axis length in most cases,  since those
                 * informations are provided in the ellipsoid. An exception to this rule occurs if
                 * the lengths are different from the ones declared in the datum.
                 */
                if (param instanceof ParameterValue<?>) {
                    final double value = ((ParameterValue<?>) param).doubleValue(axisUnit);
                    final double expected = (name == Constants.SEMI_MINOR)   // using '==' is okay here.
                            ? ellipsoid.getSemiMinorAxis() : ellipsoid.getSemiMajorAxis();
                    if (value == expected) {
                        continue;
                    }
                }
            }
            WKTUtilities.append(param, formatter);
        }
        formatter.append(unit);
        final CartesianCS cs = getCoordinateSystem();
        final int dimension = cs.getDimension();
        for (int i=0; i<dimension; i++) {
            formatter.newLine();
            formatter.append(toFormattable(cs.getAxis(i)));
        }
        if (unit == null) {
            formatter.setInvalidWKT(this, null);
        }
        formatter.removeContextualUnit(unit);
        formatter.removeContextualUnit(geoUnit);
        formatter.addContextualUnit(angularUnit);
        formatter.addContextualUnit(linearUnit);
        formatter.newLine(); // For writing the Id[…] element on its own line.
        return "ProjCS";
    }
}
