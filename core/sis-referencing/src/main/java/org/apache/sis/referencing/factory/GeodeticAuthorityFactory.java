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
import org.opengis.metadata.Identifier;
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
import org.apache.sis.util.iso.SimpleInternationalString;
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
     * The factory to use for parsing authority code as {@link GenericName} instances.
     */
    protected final NameFactory nameFactory;

    /**
     * Creates a new authority factory for geodetic objects.
     *
     * @param nameFactory The factory to use for parsing authority codes as {@link GenericName} instances.
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
     * <div class="note"><b>Example</b>
     * A factory that create coordinate reference system objects from EPSG codes could return
     * a citation like below:
     *
     * {@preformat text
     *   Citation
     *   ├─ Title ……………………………………………………… EPSG Geodetic Parameter Dataset
     *   ├─ Identifier ………………………………………… EPSG
     *   ├─ Online resource (1 of 2)
     *   │  ├─ Linkage ………………………………………… http://epsg-registry.org/
     *   │  └─ Function ……………………………………… Browse
     *   └─ Online resource (2 of 2)
     *      ├─ Linkage ………………………………………… jdbc:derby:/my/path/to/SIS_DATA/Metadata
     *      ├─ Description ……………………………… EPSG dataset version 8.8 on “Apache Derby Embedded JDBC Driver” version 10.12.
     *      └─ Function ……………………………………… Connection
     * }
     *
     * The online resource description with a “Connection” function is a SIS extension.</div>
     *
     * @return The organization responsible for definition of the database, or {@code null} if unknown.
     *
     * @see #getVendor()
     */
    @Override
    public abstract Citation getAuthority();

    /**
     * Returns a description of the object corresponding to a code.
     * The description can be used for example in a combo box in a graphical user interface.
     *
     * <div class="section">Default implementation</div>
     * The default implementation invokes {@link #createObject(String)} for the given code
     * and returns the {@linkplain AbstractIdentifiedObject#getName() object name}.
     * This may be costly since it involves a full object creation.
     * Subclasses are encouraged to provide a more efficient implementation if they can.
     *
     * @throws FactoryException if an error occurred while fetching the description.
     */
    @Override
    public InternationalString getDescriptionText(final String code) throws FactoryException {
        return new SimpleInternationalString(createObject(code).getName().getCode());
    }

    /**
     * Returns an arbitrary object from a code. The returned object will typically be an instance of {@link Datum},
     * {@link CoordinateSystem}, {@link CoordinateReferenceSystem} or {@link CoordinateOperation}.
     * This method may be used when the type of the object to create is unknown.
     * But it is recommended to invoke the most specific {@code createFoo(String)} method when
     * the desired type is known, both for performance reason and for avoiding ambiguity.
     *
     * <div class="section">Note for subclasses</div>
     * In default {@code GeodeticAuthorityFactory} implementation, all {@code createFoo(String)} methods ultimately
     * delegate to this {@code createObject(String)} method and verify if the created object is of the desired type.
     * Overriding this method is sufficient for supporting the more specific {@code createFoo(String)} methods,
     * but subclasses are encouraged to override the later for efficiency.
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
    public abstract IdentifiedObject createObject(String code) throws NoSuchAuthorityCodeException, FactoryException;

    /**
     * Creates an arbitrary coordinate reference system from a code.
     * The returned object will typically be an instance of {@link GeographicCRS}, {@link ProjectedCRS},
     * {@link VerticalCRS} or {@link CompoundCRS}.
     * If the coordinate reference system type is known at compile time,
     * it is recommended to invoke the most precise method instead of this one (for example
     * {@link #createGeographicCRS createGeographicCRS(String)} instead of
     * <code>createCoordinateReferenceSystem(code)</code> if the caller know he is asking for a
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic coordinate reference system}).
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
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
     * Creates a 2- or 3-dimensional coordinate reference system based on an ellipsoidal approximation of the geoid.
     * This provides an accurate representation of the geometry of geographic features
     * for a large portion of the earth's surface.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
     * A geocentric CRS deals with the earth's curvature by taking a 3-dimensional spatial view, which obviates
     * the need to model the earth's curvature.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 2-dimensional coordinate reference system used to approximate the shape of the earth on a planar surface.
     * It is done in such a way that the distortion that is inherent to the approximation is carefully controlled and known.
     * Distortion correction is commonly applied to calculated bearings and distances to produce values
     * that are a close match to actual field values.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 1-dimensional coordinate reference system used for recording heights or depths.
     * Vertical CRSs make use of the direction of gravity to define the concept of height or depth,
     * but the relationship with gravity may not be straightforward.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 1-dimensional coordinate reference system used for the recording of time.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a CRS describing the position of points through two or more independent coordinate reference systems.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a CRS that is defined by its coordinate conversion from another CRS (not by a datum).
     * {@code DerivedCRS} can not be {@code ProjectedCRS} themselves, but may be derived from a projected CRS.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 1-, 2- or 3-dimensional contextually local coordinate reference system.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 2-dimensional engineering coordinate reference system applied to locations in images.
     * Image coordinate reference systems are treated as a separate sub-type because a separate
     * user community exists for images with its own terms of reference.
     *
     * <div class="section">Default implementation</div>
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
     * Creates an arbitrary datum from a code. The returned object will typically be an
     * instance of {@link GeodeticDatum}, {@link VerticalDatum} or {@link TemporalDatum}.
     * If the datum is known at compile time, it is recommended to invoke the most precise method instead of this one.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a datum defining the location and orientation of an ellipsoid that approximates the shape of the earth.
     * Geodetic datum are used together with ellipsoidal coordinate system, and also with Cartesian coordinate system
     * centered in the ellipsoid (or sphere).
     *
     * <div class="section">Default implementation</div>
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
     * Creates a datum identifying a particular reference level surface used as a zero-height surface.
     * There are several types of vertical datums, and each may place constraints on the axis with which
     * it is combined to create a vertical CRS.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a datum defining the origin of a temporal coordinate reference system.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a datum defining the origin of an engineering coordinate reference system.
     * An engineering datum is used in a region around that origin.
     * This origin can be fixed with respect to the earth or be a defined point on a moving vehicle.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a datum defining the origin of an image coordinate reference system.
     * An image datum is used in a local context only.
     * For an image datum, the anchor point is usually either the centre of the image or the corner of the image.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a geometric figure that can be used to describe the approximate shape of the earth.
     * In mathematical terms, it is a surface formed by the rotation of an ellipse about its minor axis.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a prime meridian defining the origin from which longitude values are determined.
     *
     * <div class="section">Default implementation</div>
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
     * Creates information about spatial, vertical, and temporal extent (usually a domain of validity) from a code.
     *
     * <div class="section">Default implementation</div>
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
     * Creates an arbitrary coordinate system from a code. The returned object will typically be an
     * instance of {@link EllipsoidalCS}, {@link CartesianCS}, {@link VerticalCS} or {@link TimeCS}.
     * If the coordinate system is known at compile time, it is recommended to invoke the most precise
     * method instead of this one.
     *
     * <div class="section">Default implementation</div>
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
     * Creates 2- or 3-dimensional coordinate system for geodetic latitude and longitude,
     * sometime with ellipsoidal height.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 1-dimensional coordinate system for heights or depths of points.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 1-dimensional coordinate system for time elapsed in the specified time units
     * from a specified time origin.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 2- or 3-dimensional Cartesian coordinate system made of straight orthogonal axes.
     * All axes shall have the same linear unit of measure.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 3-dimensional coordinate system with one distance measured from the origin and two angular coordinates.
     * Not to be confused with an ellipsoidal coordinate system based on an ellipsoid "degenerated" into a sphere.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 3-dimensional coordinate system made of a polar coordinate system
     * extended by a straight perpendicular axis.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a 2-dimensional coordinate system for coordinates represented by a distance from the origin
     * and an angle from a fixed direction.
     *
     * <div class="section">Default implementation</div>
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
     * Creates a coordinate system axis with name, direction, unit and range of values.
     *
     * <div class="section">Default implementation</div>
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
     *
     * <div class="section">Default implementation</div>
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
     * Creates a definition of a single parameter used by an operation method.
     *
     * <div class="section">Default implementation</div>
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
     * Creates  description of the algorithm and parameters used to perform a coordinate operation.
     * An {@code OperationMethod} is a kind of metadata: it does not perform any coordinate operation
     * (e.g. map projection) by itself, but tells us what is needed in order to perform such operation.
     *
     * <div class="section">Default implementation</div>
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
     * Creates an operation for transforming coordinates in the source CRS to coordinates in the target CRS.
     * Coordinate operations contain a {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform
     * math transform}, which does the actual work of transforming coordinates.
     *
     * <div class="section">Default implementation</div>
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
     * <div class="section">Default implementation</div>
     * The default implementation returns an empty set.
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
    final String trimAuthority(String code) {
        return trimAuthority(code, null);
    }

    /**
     * Implementation of {@link #trimAuthority(String)}, but with an authority which may be already known.
     * If the given {@code authority} is null, then it will be fetched by a call to {@link #getAuthority()}
     * if needed.
     */
    private String trimAuthority(String code, Citation authority) {
        code = code.trim();
        final GenericName name = nameFactory.parseGenericName(null, code);
        if (name instanceof ScopedName) {
            final GenericName scope = ((ScopedName) name).path();
            if (authority == null) {
                authority = getAuthority();     // Costly operation for EPSGFactory.
            }
            if (Citations.identifierMatches(authority, null, scope.toString())) {
                return name.tip().toString().trim();
            }
        }
        return code;
    }

    /**
     * Creates an exception for an unknown authority code.
     * This convenience method is provided for implementation of {@code createFoo(String)} methods in subclasses.
     *
     * @param  type  The GeoAPI interface that was to be created (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  code  The unknown authority code.
     * @return An exception initialized with an error message built from the specified informations.
     */
    final NoSuchAuthorityCodeException noSuchAuthorityCode(final Class<?> type, final String code) {
        final Citation authority = getAuthority();
        final String name = Citations.getIdentifier(authority, false);
        return new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.NoSuchAuthorityCode_3,
                   (name != null) ? name : Vocabulary.formatInternational(Vocabulary.Keys.Untitled),
                   type, code), name, trimAuthority(code, authority), code);
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
        /*
         * Get the actual type of the object. Returns the GeoAPI type if possible,
         * or fallback on the implementation class otherwise.
         */
        final Class<?> actual;
        if (object instanceof AbstractIdentifiedObject) {
            actual = ((AbstractIdentifiedObject) object).getInterface();
        } else {
            actual = object.getClass();
        }
        /*
         * Get the authority from the object if possible, in order to avoid a call
         * to the potentially costly (for EPSGFactory) getAuthority() method.
         */
        Citation authority = null;
        if (object instanceof IdentifiedObject) {
            final Identifier id = ((IdentifiedObject) object).getName();
            if (id != null) {
                authority = id.getAuthority();
            }
        }
        if (authority == null) {
            authority = getAuthority();
        }
        throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.UnexpectedTypeForReference_3, code, type, actual),
                Citations.getIdentifier(authority, false), trimAuthority(code, authority), code);
    }
}
