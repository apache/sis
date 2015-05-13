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
package org.apache.sis.referencing;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.apache.sis.referencing.cs.*;
import org.apache.sis.referencing.crs.*;
import org.apache.sis.referencing.datum.*;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.ArgumentChecks;


/**
 * Creates implementations of {@link CoordinateReferenceSystem}, {@link CoordinateSystem} and {@link Datum} objects.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class GeodeticObjectFactory extends AbstractFactory implements CSFactory, DatumFactory, CRSFactory {
    /**
     * The math transform factory. Will be created only when first needed.
     */
    private volatile MathTransformFactory mtFactory;

    /**
     * Weak references to existing objects (identifiers, CRS, Datum, whatever).
     * This set is used in order to return a pre-existing object instead of creating a new one.
     */
    private final WeakHashSet<IdentifiedObject> pool;

    /**
     * Constructs a default factory.
     */
    public GeodeticObjectFactory() {
        pool = new WeakHashSet<>(IdentifiedObject.class);
    }

    /**
     * Returns the math transform factory for internal usage only.
     */
    private MathTransformFactory getMathTransformFactory() {
        MathTransformFactory factory = mtFactory;
        if (factory == null) {
            mtFactory = factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        }
        return factory;
    }

    /**
     * Creates a geocentric coordinate reference system from a {@linkplain CartesianCS Cartesian coordinate system}.
     * The default implementation creates a {@link DefaultGeocentricCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The geodetic datum to use in created CRS.
     * @param  cs         The Cartesian coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeocentricCRS
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final CartesianCS cs) throws FactoryException
    {
        final GeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates a three-dimensional Cartesian coordinate system from the given set of axis.
     * The default implementation creates a {@link DefaultCartesianCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @param  axis2      The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCartesianCS
     */
    @Override
    public CartesianCS createCartesianCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final CartesianCS cs;
        try {
            cs = new DefaultCartesianCS(properties, axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a geocentric coordinate reference system from a {@linkplain SphericalCS spherical coordinate system}.
     * The default implementation creates a {@link DefaultGeocentricCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      Geodetic datum to use in created CRS.
     * @param  cs         The spherical coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeocentricCRS
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final SphericalCS cs) throws FactoryException
    {
        final GeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates a spherical coordinate system from the given set of axis.
     * The default implementation creates a {@link DefaultSphericalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @param  axis2      The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultSphericalCS
     */
    @Override
    public SphericalCS createSphericalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final SphericalCS cs;
        try {
            cs = new DefaultSphericalCS(properties, axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a geographic coordinate reference system.
     * It can be <var>latitude</var>/<var>longitude</var> or <var>longitude</var>/<var>latitude</var>.
     * The default implementation creates a {@link DefaultGeographicCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      Geodetic datum to use in created CRS.
     * @param  cs         The ellipsoidal coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeographicCRS
     */
    @Override
    public GeographicCRS createGeographicCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final EllipsoidalCS cs) throws FactoryException
    {
        final GeographicCRS crs;
        try {
            crs = new DefaultGeographicCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates geodetic datum from ellipsoid and (optionally) Bursa-Wolf parameters.
     * The default implementation creates a {@link DefaultGeodeticDatum} instance.
     *
     * @param  properties    Name and other properties to give to the new object.
     * @param  ellipsoid     The ellipsoid to use in new geodetic datum.
     * @param  primeMeridian The prime meridian to use in new geodetic datum.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeodeticDatum
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final Map<String,?> properties,
            final Ellipsoid ellipsoid, final PrimeMeridian primeMeridian) throws FactoryException
    {
        final GeodeticDatum datum;
        try {
            datum = new DefaultGeodeticDatum(properties, ellipsoid, primeMeridian);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(datum);
    }

    /**
     * Creates a prime meridian, relative to Greenwich.
     * The default implementation creates a {@link DefaultPrimeMeridian} instance.
     *
     * @param  properties  Name and other properties to give to the new object.
     * @param  longitude   The longitude of prime meridian in supplied angular units East of Greenwich.
     * @param  angularUnit The angular units of longitude.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultPrimeMeridian
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final Map<String,?> properties,
            final double longitude, final Unit<Angle> angularUnit) throws FactoryException
    {
        final PrimeMeridian meridian;
        try {
            meridian = new DefaultPrimeMeridian(properties, longitude, angularUnit);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(meridian);
    }

    /**
     * Creates an ellipsoidal coordinate system without ellipsoidal height.
     * The default implementation creates a {@link DefaultEllipsoidalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoidalCS
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final EllipsoidalCS cs;
        try {
            cs = new DefaultEllipsoidalCS(properties, axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates an ellipsoidal coordinate system with ellipsoidal height.
     * The default implementation creates a {@link DefaultEllipsoidalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @param  axis2      The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoidalCS
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final EllipsoidalCS cs;
        try {
            cs = new DefaultEllipsoidalCS(properties, axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates an ellipsoid from semi-axis length values.
     * The default implementation creates a {@link DefaultEllipsoid} instance.
     *
     * @param  properties    Name and other properties to give to the new object.
     * @param  semiMajorAxis The equatorial radius in supplied linear units.
     * @param  semiMinorAxis The polar radius in supplied linear units.
     * @param  unit          The linear units of ellipsoid axes.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoid#createEllipsoid(Map, double, double, Unit)
     */
    @Override
    public Ellipsoid createEllipsoid(final Map<String,?> properties,
            final double semiMajorAxis, final double semiMinorAxis,
            final Unit<Length> unit) throws FactoryException
    {
        final Ellipsoid ellipsoid;
        try {
            ellipsoid = DefaultEllipsoid.createEllipsoid(properties, semiMajorAxis, semiMinorAxis, unit);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(ellipsoid);
    }

    /**
     * Creates an ellipsoid from a major semi-axis length and inverse flattening.
     * The default implementation creates a {@link DefaultEllipsoid} instance.
     *
     * @param  properties        Name and other properties to give to the new object.
     * @param  semiMajorAxis     The equatorial radius in supplied linear units.
     * @param  inverseFlattening The eccentricity of ellipsoid.
     * @param  unit              The linear units of major axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoid#createFlattenedSphere(Map, double, double, Unit)
     */
    @Override
    public Ellipsoid createFlattenedSphere(final Map<String,?> properties,
            final double semiMajorAxis, final double inverseFlattening,
            final Unit<Length> unit) throws FactoryException
    {
        final Ellipsoid ellipsoid;
        try {
            ellipsoid = DefaultEllipsoid.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, unit);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(ellipsoid);
    }

    /**
     * Creates a projected coordinate reference system from a conversion.
     * The supplied conversion should <strong>not</strong> includes the operation steps for performing
     * {@linkplain AbstractCS#swapAndScaleAxis unit conversions and change of axis order} since those
     * operations will be inferred by this constructor.
     *
     * <p>The default implementation creates a {@link DefaultProjectedCRS} instance.</p>
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  baseCRS    The geographic coordinate reference system to base projection on.
     * @param  conversion The defining conversion from a normalized base.
     * @param  derivedCS  The coordinate system for the projected CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultProjectedCRS
     */
    @Override
    public ProjectedCRS createProjectedCRS(Map<String,?> properties,
            final GeographicCRS baseCRS, Conversion conversion,
            final CartesianCS derivedCS) throws FactoryException
    {
        if (!properties.containsKey(OperationMethods.MT_FACTORY)) {
            final Map<String,Object> copy = new HashMap<>(properties);
            copy.put(OperationMethods.MT_FACTORY, getMathTransformFactory());
            properties = copy;
        }
        final ProjectedCRS crs;
        try {
            crs = new DefaultProjectedCRS(properties, baseCRS, conversion, derivedCS);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates a two-dimensional Cartesian coordinate system from the given pair of axis.
     * The default implementation creates a {@link DefaultCartesianCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCartesianCS
     */
    @Override
    public CartesianCS createCartesianCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final CartesianCS cs;
        try {
            cs = new DefaultCartesianCS(properties, axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a derived coordinate reference system from a conversion.
     *
     * <p>The default implementation creates a {@link DefaultDerivedCRS} instance.</p>
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  baseCRS    The coordinate reference system to base projection on.
     * @param  conversion The defining conversion from a normalized base.
     * @param  derivedCS  The coordinate system for the derived CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultDerivedCRS
     */
    @Override
    public DerivedCRS createDerivedCRS(final Map<String,?> properties,
            final CoordinateReferenceSystem baseCRS, final Conversion conversion,
            final CoordinateSystem derivedCS) throws FactoryException
    {
        ArgumentChecks.ensureCanCast("baseCRS", SingleCRS.class, baseCRS);
        final DerivedCRS crs;
        try {
            crs = new DefaultDerivedCRS(properties, (SingleCRS) baseCRS, conversion, derivedCS);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates a vertical coordinate reference system.
     * The default implementation creates a {@link DefaultVerticalCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The vertical datum to use in created CRS.
     * @param  cs         The vertical coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalCRS
     */
    @Override
    public VerticalCRS createVerticalCRS(final Map<String,?> properties,
            final VerticalDatum datum, final VerticalCS cs) throws FactoryException
    {
        final VerticalCRS crs;
        try {
            crs = new DefaultVerticalCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates a vertical datum from an enumerated type value.
     * The default implementation creates a {@link DefaultVerticalDatum} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  type       The type of this vertical datum (often geoidal).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalDatum
     */
    @Override
    public VerticalDatum createVerticalDatum(final Map<String,?> properties,
            final VerticalDatumType type) throws FactoryException
    {
        final VerticalDatum datum;
        try {
            datum = new DefaultVerticalDatum(properties, type);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(datum);
    }

    /**
     * Creates a vertical coordinate system.
     * The default implementation creates a {@link DefaultVerticalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis       The single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalCS
     */
    @Override
    public VerticalCS createVerticalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        final VerticalCS cs;
        try {
            cs = new DefaultVerticalCS(properties, axis);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a temporal coordinate reference system.
     * The default implementation creates a {@link DefaultTemporalCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The temporal datum to use in created CRS.
     * @param  cs         The temporal coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTemporalCRS
     */
    @Override
    public TemporalCRS createTemporalCRS(final Map<String,?> properties,
            final TemporalDatum datum, final TimeCS cs) throws FactoryException
    {
        final TemporalCRS crs;
        try {
            crs = new DefaultTemporalCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates a temporal datum from an enumerated type value.
     * The default implementation creates a {@link DefaultTemporalDatum} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  origin     The date and time origin of this temporal datum.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTemporalDatum
     */
    @Override
    public TemporalDatum createTemporalDatum(final Map<String,?> properties,
            final Date origin) throws FactoryException
    {
        final TemporalDatum datum;
        try {
            datum = new DefaultTemporalDatum(properties, origin);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(datum);
    }

    /**
     * Creates a temporal coordinate system.
     * The default implementation creates a {@link DefaultTimeCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis       The single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTimeCS
     */
    @Override
    public TimeCS createTimeCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        final TimeCS cs;
        try {
            cs = new DefaultTimeCS(properties, axis);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a compound coordinate reference system from an ordered list of {@code CoordinateReferenceSystem} objects.
     * The default implementation creates a {@link DefaultCompoundCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  elements   Ordered array of {@code CoordinateReferenceSystem} objects.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCompoundCRS
     */
    @Override
    public CompoundCRS createCompoundCRS(final Map<String,?> properties,
            final CoordinateReferenceSystem... elements) throws FactoryException
    {
        final CompoundCRS crs;
        try {
            crs = new DefaultCompoundCRS(properties, elements);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates an image coordinate reference system.
     * The default implementation creates a {@link DefaultImageCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The image datum to use in created CRS.
     * @param  cs         The Cartesian or oblique Cartesian coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultImageCRS
     */
    @Override
    public ImageCRS createImageCRS(final Map<String,?> properties,
            final ImageDatum datum, final AffineCS cs) throws FactoryException
    {
        final ImageCRS crs;
        try {
            crs = new DefaultImageCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates an image datum.
     * The default implementation creates a {@link DefaultImageDatum} instance.
     *
     * @param  properties  Name and other properties to give to the new object.
     * @param  pixelInCell Specification of the way the image grid is associated with the image data attributes.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultImageDatum
     */
    @Override
    public ImageDatum createImageDatum(final Map<String,?> properties,
            final PixelInCell pixelInCell) throws FactoryException
    {
        final ImageDatum datum;
        try {
            datum = new DefaultImageDatum(properties, pixelInCell);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(datum);
    }

    /**
     * Creates a two-dimensional affine coordinate system from the given pair of axis.
     * The default implementation creates a {@link DefaultAffineCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultAffineCS
     */
    @Override
    public AffineCS createAffineCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final AffineCS cs;
        try {
            cs = new DefaultAffineCS(properties, axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a engineering coordinate reference system.
     * The default implementation creates a {@link DefaultEngineeringCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The engineering datum to use in created CRS.
     * @param  cs         The coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEngineeringCRS
     */
    @Override
    public EngineeringCRS createEngineeringCRS(final Map<String,?> properties,
            final EngineeringDatum datum, final CoordinateSystem cs) throws FactoryException
    {
        final EngineeringCRS crs;
        try {
            crs = new DefaultEngineeringCRS(properties, datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(crs);
    }

    /**
     * Creates an engineering datum.
     * The default implementation creates a {@link DefaultEngineeringDatum} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEngineeringDatum
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final Map<String,?> properties)
            throws FactoryException
    {
        final EngineeringDatum datum;
        try {
            datum = new DefaultEngineeringDatum(properties);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(datum);
    }

    /**
     * Creates a three-dimensional affine coordinate system from the given set of axis.
     * The default implementation creates a {@link DefaultAffineCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @param  axis2      The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultAffineCS
     */
    @Override
    public AffineCS createAffineCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final AffineCS cs;
        try {
            cs = new DefaultAffineCS(properties, axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a cylindrical coordinate system from the given set of axis.
     * The default implementation creates a {@link DefaultCylindricalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @param  axis2      The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCylindricalCS
     */
    @Override
    public CylindricalCS createCylindricalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final CylindricalCS cs;
        try {
            cs = new DefaultCylindricalCS(properties, axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a polar coordinate system from the given pair of axis.
     * The default implementation creates a {@link DefaultPolarCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultPolarCS
     */
    @Override
    public PolarCS createPolarCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final PolarCS cs;
        try {
            cs = new DefaultPolarCS(properties, axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a linear coordinate system.
     * The default implementation creates a {@link DefaultLinearCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis       The single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultLinearCS
     */
    @Override
    public LinearCS createLinearCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        final LinearCS cs;
        try {
            cs = new DefaultLinearCS(properties, axis);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a two-dimensional user defined coordinate system from the given pair of axis.
     * The default implementation creates a {@link DefaultUserDefinedCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultUserDefinedCS
     */
    @Override
    public UserDefinedCS createUserDefinedCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final UserDefinedCS cs;
        try {
            cs = new DefaultUserDefinedCS(properties, axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a three-dimensional user defined coordinate system from the given set of axis.
     * The default implementation creates a {@link DefaultUserDefinedCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0      The first  axis.
     * @param  axis1      The second axis.
     * @param  axis2      The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultUserDefinedCS
     */
    @Override
    public UserDefinedCS createUserDefinedCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final UserDefinedCS cs;
        try {
            cs = new DefaultUserDefinedCS(properties, axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(cs);
    }

    /**
     * Creates a coordinate system axis from an abbreviation and a unit.
     * The default implementation creates a {@link DefaultCoordinateSystemAxis} instance.
     *
     * @param  properties   Name and other properties to give to the new object.
     * @param  abbreviation The coordinate axis abbreviation.
     * @param  direction    The axis direction.
     * @param  unit         The coordinate axis unit.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCoordinateSystemAxis
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final Map<String,?> properties,
            final String abbreviation, final AxisDirection direction,
            final Unit<?> unit) throws FactoryException
    {
        final CoordinateSystemAxis axis;
        try {
            axis = new DefaultCoordinateSystemAxis(properties, abbreviation, direction, unit);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(axis);
    }

    /**
     * Creates a coordinate reference system object from a XML string.
     *
     * @param  xml Coordinate reference system encoded in XML format.
     * @throws FactoryException if the object creation failed.
     *
     * @todo Not yet implemented.
     */
    @Override
    public CoordinateReferenceSystem createFromXML(final String xml) throws FactoryException {
        throw new FactoryException("Not yet implemented");
    }

    /**
     * Creates a coordinate reference system object from a string.
     *
     * @param  wkt Coordinate system encoded in Well-Known Text format.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createFromWKT(final String wkt) throws FactoryException {
        throw new FactoryException("Not yet implemented");
    }
}
