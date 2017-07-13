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
package org.apache.sis.storage.gdal;

import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Angle;

import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.ReferenceIdentifier;

import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.measure.Units;


/**
 * A factory for {@linkplain CoordinateReferenceSystem Coordinate Reference System} objects
 * created from property maps.
 *
 * <p>The supported methods in this class are:</p>
 *
 * <ul>
 *   <li>{@link #createGeocentricCRS(Map, GeodeticDatum, CartesianCS)}</li>
 *   <li>{@link #createGeographicCRS(Map, GeodeticDatum, EllipsoidalCS)}</li>
 *   <li>{@link #createProjectedCRS(Map, GeographicCRS, Conversion, CartesianCS)}</li>
 * </ul>
 *
 * All other methods throw a {@link FactoryException}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class ReferencingFactory extends AbstractFactory implements CRSFactory {
    /**
     * The unique instance.
     */
    static final ReferencingFactory INSTANCE = new ReferencingFactory();

    /**
     * Creates a new factory.
     */
    private ReferencingFactory() {
    }

    /**
     * Appends the prime meridian to the given definition string buffer.
     *
     * @param def  the definition string buffer.
     * @param pm   the prime meridian, or {@code null} if none.
     */
    private static void appendPrimeMeridian(final StringBuilder def, final PrimeMeridian pm) {
        if (pm != null) {
            double lon = pm.getGreenwichLongitude();
            final Unit<Angle> unit = pm.getAngularUnit();
            if (unit != null) {
                lon = unit.getConverterTo(Units.DEGREE).convert(lon);
            }
            def.append(" +pm=").append(lon);
        }
    }

    /**
     * Appends the axis directions in the given definition string buffer.
     *
     * @param  def        the definition string buffer.
     * @param  cs         the coordinate system.
     * @param  dimension  the number of dimension to format (may be lower than the CS dimension).
     * @throws FactoryException if an axis direction is not supported.
     */
    private static void appendAxisDirections(final StringBuilder def, final CoordinateSystem cs,
            final int dimension) throws FactoryException
    {
        for (int i=0; i<dimension; i++) {
            final AxisDirection dir = cs.getAxis(i).getDirection();
            final char c;
                 if (dir == AxisDirection.EAST ) c = 'e';
            else if (dir == AxisDirection.WEST ) c = 'w';
            else if (dir == AxisDirection.NORTH) c = 'n';
            else if (dir == AxisDirection.SOUTH) c = 's';
            else if (dir == AxisDirection.UP   ) c = 'u';
            else if (dir == AxisDirection.DOWN ) c = 'd';
            else throw new FactoryException("Unsupported axis direction: " + dir);
            def.append(c);
        }
    }

    /**
     * Creates a geographic or geocentric coordinate reference system.
     *
     * @param  type        {@code "latlon"} or {@code "geocent"}.
     * @param  properties  name to give to the new object.
     * @param  datum       geodetic datum to use in created CRS.
     * @param  cs          the ellipsoidal coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    private CoordinateReferenceSystem createGeodeticCRS(final String type, final Map<String,?> properties,
            final GeodeticDatum datum, final CoordinateSystem cs) throws FactoryException
    {
        final int           dimension  = cs.getDimension();
        final ReferenceIdentifier name = new ImmutableIdentifier(properties);
        final Ellipsoid     ellipsoid  = datum.getEllipsoid();
        final StringBuilder definition = new StringBuilder(100);
        definition.append("+proj=").append(type)
                .append(" +a=").append(ellipsoid.getSemiMajorAxis())
                .append(" +b=").append(ellipsoid.getSemiMinorAxis());
        appendPrimeMeridian(definition, datum.getPrimeMeridian());
        appendAxisDirections(definition.append(' ').append(Proj4.AXIS_ORDER_PARAM), cs, Math.min(dimension, 3));
        try {
            return Proj4.createCRS(name, datum.getName(), definition.toString(), dimension);
        } catch (IllegalArgumentException e) {
            throw new FactoryException(e.getMessage(), e);
        }
    }

    /**
     * Creates a geographic coordinate reference system.
     * It can be <var>Latitude</var>/<var>Longitude</var> or <var>Longitude</var>/<var>Latitude</var>.
     *
     * @param  properties  name to give to the new object.
     * @param  datum       geodetic datum to use in created CRS.
     * @param  cs          the ellipsoidal coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeographicCRS createGeographicCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final EllipsoidalCS cs) throws FactoryException
    {
        return (GeographicCRS) createGeodeticCRS("latlon", properties, datum, cs);
    }

    /**
     * Creates a geocentric coordinate reference system.
     *
     * @param  properties  name to give to the new object.
     * @param  datum       geodetic datum to use in created CRS.
     * @param  cs          the coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final CartesianCS cs) throws FactoryException
    {
        return (GeocentricCRS) createGeodeticCRS("geocent", properties, datum, cs);
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public GeocentricCRS createGeocentricCRS (Map<String,?> properties, GeodeticDatum datum, SphericalCS cs) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Creates a projected coordinate reference system from a defining conversion.
     * The projection and parameter names in the {@code conversionFromBase} can be
     * Proj.4 names, OGC names, EPSG names or GeoTIFF names.
     *
     * @param  properties          name to give to the new object.
     * @param  baseCRS             geographic coordinate reference system to base the projection on.
     * @param  conversionFromBase  the defining conversion.
     * @param  derivedCS           the coordinate system for the projected CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ProjectedCRS createProjectedCRS(final Map<String,?> properties, final GeographicCRS baseCRS,
            final Conversion conversionFromBase, final CartesianCS derivedCS) throws FactoryException
    {
        final int                 dimension  = derivedCS.getDimension();
        final ReferenceIdentifier name       = new ImmutableIdentifier(properties);
        final EllipsoidalCS       baseCS     = baseCRS.getCoordinateSystem();
        final GeodeticDatum       datum      = baseCRS.getDatum();
        final Ellipsoid           ellipsoid  = datum.getEllipsoid();
        final ParameterValueGroup parameters = conversionFromBase.getParameterValues();
        final StringBuilder       definition = new StringBuilder(200);
        definition.append("+proj=").append(ResourcesLoader.getProjName(parameters, false).substring(1));
        boolean hasSemiMajor = false;
        boolean hasSemiMinor = false;
        for (final GeneralParameterValue parameter : parameters.values()) {
            if (parameter instanceof ParameterValue) {
                final Object value = ((ParameterValue) parameter).getValue();
                if (value != null) {
                    final String pn = ResourcesLoader.getProjName(parameter, true);
                    if (pn.equals("+a")) hasSemiMajor = true;
                    if (pn.equals("+b")) hasSemiMinor = true;
                    definition.append(' ').append(pn).append('=').append(value);
                }
            }
        }
        if (!hasSemiMajor) definition.append(" +a=").append(ellipsoid.getSemiMajorAxis());
        if (!hasSemiMinor) definition.append(" +b=").append(ellipsoid.getSemiMinorAxis());
        appendPrimeMeridian (definition, datum.getPrimeMeridian());
        appendAxisDirections(definition.append(' ').append(Proj4.AXIS_ORDER_PARAM), derivedCS, Math.min(dimension, 3));
        appendAxisDirections(definition.append(Proj4.AXIS_ORDER_SEPARATOR), baseCS, Math.min(baseCS.getDimension(), 3));
        final CRS.Projected crs;
        try {
            crs = (CRS.Projected) Proj4.createCRS(name, datum.getName(), definition.toString(), dimension);
        } catch (IllegalArgumentException e) {
            throw new FactoryException(e.getMessage(), e);
        }
        if (baseCRS instanceof CRS.Geographic) {
            crs.baseCRS = (CRS.Geographic) baseCRS;
        }
        return crs;
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public VerticalCRS createVerticalCRS(Map<String,?> properties, VerticalDatum datum, VerticalCS cs) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public TemporalCRS createTemporalCRS(Map<String, ?> properties, TemporalDatum datum, TimeCS cs) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public ImageCRS createImageCRS(Map<String, ?> properties, ImageDatum datum, AffineCS cs) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public EngineeringCRS createEngineeringCRS(Map<String, ?> properties, EngineeringDatum datum, CoordinateSystem cs) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public DerivedCRS createDerivedCRS(Map<String, ?> properties, CoordinateReferenceSystem baseCRS, Conversion conversionFromBase, CoordinateSystem derivedCS) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public CompoundCRS createCompoundCRS(Map<String, ?> properties, CoordinateReferenceSystem... elements) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public CoordinateReferenceSystem createFromXML(String xml) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException always thrown.
     */
    @Override
    public CoordinateReferenceSystem createFromWKT(String wkt) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }
}
