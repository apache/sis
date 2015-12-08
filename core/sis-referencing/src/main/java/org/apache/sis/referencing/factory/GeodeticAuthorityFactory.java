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
package org.apache.sis.referencing.factory;

import java.util.Set;
import java.util.Collections;
import javax.measure.unit.Unit;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;


/**
 * Creates geodetic objects from codes defined by an authority.
 * An <cite>authority</cite> is an organization that maintains definitions of authority codes.
 * An <cite>authority code</cite> is a compact string defined by an authority to reference a particular
 * spatial reference object. For example the <a href="http://www.epsg.org">EPSG geodetic dataset</a> maintains
 * a database of coordinate systems, and other spatial referencing objects, where each object has a code number ID.
 * For example, the EPSG code for a WGS84 Lat/Lon coordinate system is {@code "4326"}.
 *
 * <p>This class defines a default implementation for most methods defined in the {@link DatumAuthorityFactory},
 * {@link CSAuthorityFactory} and {@link CRSAuthorityFactory} interfaces. However, those interfaces do not appear
 * in the {@code implements} clause of this class declaration. This is up to subclasses to decide which interfaces
 * they declare to implement.</p>
 *
 * <p>The default implementation for all {@code createFoo(String)} methods ultimately invokes
 * {@link #createObject(String)}, which may be the only method that a subclass need to override.
 * However, other methods may be overridden as well for better performances.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class GeodeticAuthorityFactory extends AbstractFactory implements AuthorityFactory {
    /**
     * The factory to use for creating {@link GenericName} instances.
     */
    protected final NameFactory nameFactory;

    /**
     * Creates a new authority factory for geodetic objects.
     *
     * @param nameFactory The factory to use for creating {@link GenericName} instances.
     */
    protected GeodeticAuthorityFactory(final NameFactory nameFactory) {
        ArgumentChecks.ensureNonNull("nameFactory", nameFactory);
        this.nameFactory = nameFactory;
    }

    /**
     * Returns the organization or party responsible for definition and maintenance of the database.
     * This method may return {@code null} if it can not obtain this information, for example because
     * the connection to a database is not available.
     *
     * @return The organization responsible for definition of the database, or {@code null} if unknown.
     *
     * @see #getVendor()
     */
    @Override
    public abstract Citation getAuthority();

    /**
     * Returns a description of the underlying backing store, or {@code null} if unknown.
     * This is for example the database software used for storing the data.
     *
     * <p>The default implementation returns always {@code null}.</p>
     *
     * @return A description of the underlying backing store, or {@code null} if none.
     * @throws FactoryException if a failure occurred while fetching the backing store description.
     */
    public InternationalString getBackingStoreDescription() throws FactoryException {
        return null;
    }

    /**
     * Returns an arbitrary object from a code. The returned object will typically be an instance of {@link Datum},
     * {@link CoordinateSystem}, {@link CoordinateReferenceSystem} or {@link CoordinateOperation}.
     *
     * <p>In default {@code GeodeticAuthorityFactory} implementation, all {@code createFoo(String)} methods
     * ultimately delegate to this {@code createObject(String)} method. However subclasses are encouraged
     * to override more specific methods for efficiency.</p>
     *
     * <p>The default implementation always throw an exception. Subclasses should override this method
     * if they are capable to automatically detect the object type from its code.</p>
     *
     * @param  code Value allocated by authority.
     * @return The object for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see #createCoordinateSystem(String)
     */
    @Override
    public IdentifiedObject createObject(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        throw noSuchAuthorityCode(IdentifiedObject.class, code);
    }

    /**
     * Creates an arbitrary coordinate reference system from a code.
     * If the coordinate reference system type is known at compile time,
     * it is recommended to invoke the most precise method instead of this one (for example
     * {@link #createGeographicCRS createGeographicCRS(String)} instead of
     * <code>createCoordinateReferenceSystem(code)</code> if the caller know he is asking for a
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic coordinate reference system}).
     *
     * <p>The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.</p>
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeographicCRS(String)
     * @see #createProjectedCRS(String)
     * @see #createVerticalCRS(String)
     * @see #createTemporalCRS(String)
     */
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(CoordinateReferenceSystem.class, createObject(code), code);
    }

    /**
     * Creates a geographic coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultGeographicCRS
     */
    public GeographicCRS createGeographicCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(GeographicCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a geocentric coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createCartesianCS(String)
     * @see #createSphericalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultGeocentricCRS
     */
    public GeocentricCRS createGeocentricCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(GeocentricCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a projected coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeographicCRS(String)
     * @see #createCartesianCS(String)
     * @see org.apache.sis.referencing.crs.DefaultProjectedCRS
     */
    public ProjectedCRS createProjectedCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(ProjectedCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a vertical coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createVerticalDatum(String)
     * @see #createVerticalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultVerticalCRS
     */
    public VerticalCRS createVerticalCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(VerticalCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a temporal coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createTemporalDatum(String)
     * @see #createTimeCS(String)
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS
     */
    public TemporalCRS createTemporalCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(TemporalCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 3D or 4D coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createVerticalCRS(String)
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.crs.DefaultCompoundCRS
     */
    public CompoundCRS createCompoundCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CompoundCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a derived coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultDerivedCRS
     */
    public DerivedCRS createDerivedCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(DerivedCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates an engineering coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createEngineeringDatum(String)
     * @see org.apache.sis.referencing.crs.DefaultEngineeringCRS
     */
    public EngineeringCRS createEngineeringCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(EngineeringCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates an image coordinate reference system from a code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createImageDatum(String)
     * @see org.apache.sis.referencing.crs.DefaultImageCRS
     */
    public ImageCRS createImageCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(ImageCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Returns an arbitrary datum from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeodeticDatum(String)
     * @see #createVerticalDatum(String)
     * @see #createTemporalDatum(String)
     */
    public Datum createDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Datum.class, createObject(code), code);
    }

    /**
     * Creates a geodetic datum from a code.
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createEllipsoid(String)
     * @see #createPrimeMeridian(String)
     * @see #createGeographicCRS(String)
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     */
    public GeodeticDatum createGeodeticDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(GeodeticDatum.class, createDatum(code), code);
    }

    /**
     * Creates a vertical datum from a code.
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createVerticalCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
     */
    public VerticalDatum createVerticalDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(VerticalDatum.class, createDatum(code), code);
    }

    /**
     * Creates a temporal datum from a code.
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultTemporalDatum
     */
    public TemporalDatum createTemporalDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(TemporalDatum.class, createDatum(code), code);
    }

    /**
     * Creates a engineering datum from a code.
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createEngineeringCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultEngineeringDatum
     */
    public EngineeringDatum createEngineeringDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(EngineeringDatum.class, createDatum(code), code);
    }

    /**
     * Creates a image datum from a code.
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createImageCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultImageDatum
     */
    public ImageDatum createImageDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(ImageDatum.class, createDatum(code), code);
    }

    /**
     * Creates an ellipsoid from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The ellipsoid for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     */
    public Ellipsoid createEllipsoid(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Ellipsoid.class, createObject(code), code);
    }

    /**
     * Creates a prime meridian from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The prime meridian for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeodeticDatum(String)
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     */
    public PrimeMeridian createPrimeMeridian(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(PrimeMeridian.class, createObject(code), code);
    }

    /**
     * Creates an extent (usually an domain of validity) from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The extent for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    public Extent createExtent(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Extent.class, createObject(code), code);
    }

    /**
     * Creates an arbitrary coordinate system from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateSystemAxis(String)
     * @see #createEllipsoidalCS(String)
     * @see #createCartesianCS(String)
     */
    public CoordinateSystem createCoordinateSystem(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CoordinateSystem.class, createObject(code), code);
    }

    /**
     * Creates an ellipsoidal coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createEllipsoid(String)
     * @see #createGeodeticDatum(String)
     * @see #createGeographicCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultEllipsoidalCS
     */
    public EllipsoidalCS createEllipsoidalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(EllipsoidalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a vertical coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createVerticalDatum(String)
     * @see #createVerticalCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultVerticalCS
     */
    public VerticalCS createVerticalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(VerticalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a temporal coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createTemporalDatum(String)
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultTimeCS
     */
    public TimeCS createTimeCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(TimeCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a Cartesian coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createProjectedCRS(String)
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultCartesianCS
     */
    public CartesianCS createCartesianCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CartesianCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a spherical coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultSphericalCS
     */
    public SphericalCS createSphericalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(SphericalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a cylindrical coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultCylindricalCS
     */
    public CylindricalCS createCylindricalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CylindricalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a polar coordinate system from a code.
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultPolarCS
     */
    public PolarCS createPolarCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(PolarCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a coordinate system axis from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The axis for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateSystem(String)
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis
     */
    public CoordinateSystemAxis createCoordinateSystemAxis(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(CoordinateSystemAxis.class, createObject(code), code);
    }

    /**
     * Creates an unit of measurement from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The unit of measurement for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    public Unit<?> createUnit(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Unit.class, createObject(code), code);
    }

    /**
     * Creates a parameter descriptor from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The parameter descriptor for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.parameter.DefaultParameterDescriptor
     */
    public ParameterDescriptor<?> createParameterDescriptor(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(ParameterDescriptor.class, createObject(code), code);
    }

    /**
     * Creates an operation method from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The operation method for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod
     */
    public OperationMethod createOperationMethod(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(OperationMethod.class, createObject(code), code);
    }

    /**
     * Creates an operation from a code.
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The operation for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation
     */
    public CoordinateOperation createCoordinateOperation(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(CoordinateOperation.class, createObject(code), code);
    }

    /**
     * Creates operations from source and target coordinate reference system codes.
     * This method should only extract the information explicitely declared in a database like EPSG.
     * This method should not attempt to infer by itself operations that are not explicitely recorded in the database.
     *
     * <p>The default implementation returns an empty set.</p>
     *
     * @param  sourceCRS  Coded value of source coordinate reference system.
     * @param  targetCRS  Coded value of target coordinate reference system.
     * @return The operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws NoSuchAuthorityCodeException if a specified code was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(String sourceCRS, String targetCRS)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return Collections.emptySet();
    }

    /**
     * Returns a finder which can be used for looking up unidentified objects.
     * The finder tries to fetch a fully {@linkplain AbstractIdentifiedObject identified object}
     * from an incomplete one, for example from an object without "{@code ID[…]}" or
     * "{@code AUTHORITY[…]}" element in <cite>Well Known Text</cite>.
     *
     * <p>The {@code type} argument is a hint for optimizing the searches.
     * The specified type should be a GeoAPI interface like {@code GeographicCRS.class},
     * but this method accepts also implementation classes.
     * If the type is unknown, one can use {@code IdentifiedObject.class}.
     * However a more accurate type may help to speed up the search since it reduces the amount
     * of tables to scan in some implementations (for example the factories backed by EPSG databases).</p>
     *
     * @param  type The type of objects to look for.
     * @return A finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder can not be created.
     */
    public IdentifiedObjectFinder createIdentifiedObjectFinder(Class<? extends IdentifiedObject> type) throws FactoryException {
        return new IdentifiedObjectFinder(this, type);
    }

    /**
     * Trims the authority scope, if presents. For example if this factory is an EPSG authority factory
     * and the specified code start with the {@code "EPSG:"} prefix, then the prefix is removed.
     * Otherwise, the string is returned unchanged (except for leading and trailing spaces).
     *
     * @param  code The code to trim.
     * @return The code without the authority scope.
     */
    protected String trimAuthority(String code) {
        /*
         * IMPLEMENTATION NOTE: This method is overridden in PropertyAuthorityFactory.
         * If implementation below is modified, it is probably worth to revisit the overridden method as well.
         */
        code = code.trim();
        final GenericName name  = nameFactory.parseGenericName(null, code);
        if (name instanceof ScopedName) {
            final GenericName scope = ((ScopedName) name).path();
            if (Citations.identifierMatches(getAuthority(), null, scope.toString())) {
                return name.tip().toString().trim();
            }
        }
        return code;
    }

    /**
     * Creates an exception for an unknown authority code.
     * This convenience method is provided for implementation of {@code createXXX} methods.
     *
     * @param  type  The GeoAPI interface that was to be created (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  code  The unknown authority code.
     * @return An exception initialized with an error message built from the specified informations.
     */
    protected final NoSuchAuthorityCodeException noSuchAuthorityCode(final Class<?> type, final String code) {
        final String authority = Citations.getIdentifier(getAuthority(), false);
        return new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.NoSuchAuthorityCode_3,
                   (authority != null) ? authority : Vocabulary.formatInternational(Vocabulary.Keys.Untitled),
                   type, code), authority, trimAuthority(code), code);
    }

    /**
     * Casts the given object to the given type, or throws an exception if the object can not be casted.
     * This convenience method is provided for implementation of {@code createXXX} methods.
     *
     * @param  type   The type to return (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  object The object to cast.
     * @param  code   The authority code, used only for formatting an error message.
     * @return The object casted to the given type.
     * @throws NoSuchAuthorityCodeException if the given object is not an instance of the given type.
     */
    @SuppressWarnings("unchecked")
    private <B, T extends B> T cast(final Class<T> type, final B object, final String code)
            throws NoSuchAuthorityCodeException
    {
        if (type.isInstance(object)) {
            return (T) object;
        }
        final Class<?> actual;
        if (object instanceof AbstractIdentifiedObject) {
            actual = ((AbstractIdentifiedObject) object).getInterface();
        } else {
            actual = object.getClass();
        }
        throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.UnexpectedTypeForReference_3, code, type, actual),
                Citations.getIdentifier(getAuthority(), false), trimAuthority(code), code);
    }
}
