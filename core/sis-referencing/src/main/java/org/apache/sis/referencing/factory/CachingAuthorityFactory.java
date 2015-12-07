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
import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.io.PrintWriter;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Loggers;


/**
 * An authority factory that caches all objects created by another factory.
 * All {@code createFoo(String)} methods first check if a previously created object exists for the given code.
 * If such object exists, it is returned. Otherwise, the object creation is delegated to another factory given
 * by {@link #createBackingStore()} and the result is cached in this factory.
 *
 * <p>Objects are cached by strong references, up to the amount of objects specified at construction time.
 * If a greater amount of objects are cached, the oldest ones will be retained through a
 * {@linkplain WeakReference weak reference} instead of a strong one.
 * This means that this caching factory will continue to return them as long as they are in use somewhere
 * else in the Java virtual machine, but will be discarded (and recreated on the fly if needed) otherwise.</p>
 *
 * <div class="section">Not for subclasses</div>
 * This abstract class does not implement any of the {@link DatumAuthorityFactory}, {@link CSAuthorityFactory},
 * {@link CRSAuthorityFactory} and {@link CoordinateOperationAuthorityFactory} interfaces.
 * Subclasses should select the interfaces that they choose to implement.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class CachingAuthorityFactory extends GeodeticAuthorityFactory implements Disposable {
    /**
     * The default value for {@link #maxStrongReferences}.
     */
    static final int DEFAULT_MAX = 100;

    /**
     * The citation for unknown authority name.
     */
    private static final Citation UNKNOWN = new SimpleCitation("Unknown");

    /**
     * The authority, cached after first requested.
     */
    private transient volatile Citation authority;

    /**
     * The pool of cached objects. The keys are instances of {@link Key} or {@link CodePair}.
     */
    private final Cache<Object,Object> cache;

    /**
     * The pool of objects identified by {@link Finder#find(IdentifiedObject)} for each comparison modes.
     * Values may be {@link NilReferencingObject} if an object has been searched but has not been found.
     *
     * <p>Every access to this pool must be synchronized on {@code findPool}.</p>
     */
    private final Map<IdentifiedObject, IdentifiedObject> findPool = new WeakHashMap<>();

    /**
     * Constructs an instance with a default number of entries to keep by strong reference.
     */
    protected CachingAuthorityFactory() {
        this(DEFAULT_MAX);
    }

    /**
     * Constructs an instance with the specified number of entries to keep by strong references.
     * In a number of object greater than {@code maxStrongReferences} are created, then the strong references
     * for the eldest ones will be replaced by weak references.
     *
     * @param maxStrongReferences The maximum number of objects to keep by strong reference.
     */
    protected CachingAuthorityFactory(final int maxStrongReferences) {
        super(DefaultFactories.forBuildin(NameFactory.class));    // TODO
        ArgumentChecks.ensurePositive("maxStrongReferences", maxStrongReferences);
        cache = new Cache<>(20, maxStrongReferences, false);
        cache.setKeyCollisionAllowed(true);
        /*
         * Key collision is usually an error. But in this case we allow them in order to enable recursivity.
         * If during the creation of an object the program asks to this CachingAuthorityFactory for the same
         * object (using the same key), then the default Cache implementation considers that situation as an
         * error unless the above property has been set to 'true'.
         */
    }

    /**
     * Returns the backing store authority factory. The returned backing store must be thread-safe.
     * This method shall be used together with {@link #release()} in a {@code try ... finally} block.
     *
     * @return The backing store to use in {@code createXXX(…)} methods.
     * @throws FactoryException if the creation of backing store failed.
     */
    private GeodeticAuthorityFactory getBackingStore() throws FactoryException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Releases the backing store previously obtained with {@link #getBackingStore}.
     */
    private void release() {
    }

    /**
     * Returns the organization or party responsible for definition and maintenance of the underlying database.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached value if it exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the backing store,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#getAuthority()} method,</li>
     *       <li>release the backing store,</li>
     *       <li>cache the result.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @return The organization responsible for definition of the database.
     */
    @Override
    public Citation getAuthority() {
        Citation c = authority;
        if (c == null) try {
            final GeodeticAuthorityFactory factory = getBackingStore();
            try {
                // Cache only in case of success. If we failed, we
                // will try again next time this method is invoked.
                authority = c = factory.getAuthority();
            } finally {
                release();
            }
        } catch (FactoryException e) {
            c = UNKNOWN;
            Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY),
                    CachingAuthorityFactory.class, "getAuthority", e);
        }
        return c;
    }

    /**
     * Returns a description of the underlying backing store, or {@code null} if unknown.
     * The default implementation performs the following steps:
     * <ol>
     *   <li>Get an instance of the backing store,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getBackingStoreDescription()} method,</li>
     *   <li>release the backing store.</li>
     * </ol>
     *
     * @return A description of the underlying backing store, or {@code null} if none.
     * @throws FactoryException if a failure occurred while fetching the backing store description.
     */
    @Override
    public InternationalString getBackingStoreDescription() throws FactoryException {
        final GeodeticAuthorityFactory factory = getBackingStore();
        try {
            return factory.getBackingStoreDescription();
        } finally {
            release();
        }
    }

    /**
     * Returns the set of authority codes for objects of the given type.
     * The default implementation performs the following steps:
     * <ol>
     *   <li>Get an instance of the backing store,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getAuthorityCodes(Class)} method,</li>
     *   <li>release the backing store.</li>
     * </ol>
     *
     * @param  type The spatial reference objects type (e.g. {@code ProjectedCRS.class}).
     * @return The set of authority codes for spatial reference objects of the given type.
     *         If this factory does not contains any object of the given type, then this method returns an empty set.
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        final GeodeticAuthorityFactory factory = getBackingStore();
        try {
            return factory.getAuthorityCodes(type);
            /*
             * In the particular case of EPSG factory, the returned Set maintains a live connection to the database.
             * But it still okay to release the factory anyway because our implementation will really close
             * the connection only when the iteration is over or the iterator has been garbage-collected.
             */
        } finally {
            release();
        }
    }

    /**
     * Gets a description of the object corresponding to a code.
     * The default implementation performs the following steps:
     * <ol>
     *   <li>Get an instance of the backing store,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getDescriptionText(String)} method,</li>
     *   <li>release the backing store.</li>
     * </ol>
     *
     * @param  code Value allocated by authority.
     * @return A description of the object, or {@code null} if the object
     *         corresponding to the specified {@code code} has no description.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the query failed for some other reason.
     */
    @Override
    public InternationalString getDescriptionText(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        final GeodeticAuthorityFactory factory = getBackingStore();
        try {
            return factory.getDescriptionText(code);
        } finally {
            release();
        }
    }

    /**
     * Returns an arbitrary object from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the backing store,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#createObject(String)} method,</li>
     *       <li>release the backing store,</li>
     *       <li>cache the result.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @return The object for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see #createCoordinateSystem(String)
     */
    @Override
    public IdentifiedObject createObject(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OBJECT, code);
    }

    /**
     * Returns an arbitrary coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateReferenceSystem(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeographicCRS(String)
     * @see #createProjectedCRS(String)
     * @see #createVerticalCRS(String)
     * @see #createTemporalCRS(String)
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CRS, code);
    }

    /**
     * Returns a geographic coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeographicCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultGeographicCRS
     */
    @Override
    public GeographicCRS createGeographicCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEOGRAPHIC_CRS, code);
    }

    /**
     * Returns a geocentric coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeocentricCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createCartesianCS(String)
     * @see #createSphericalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultGeocentricCRS
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEOCENTRIC_CRS, code);
    }

    /**
     * Returns a projected coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createProjectedCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeographicCRS(String)
     * @see #createCartesianCS(String)
     * @see org.apache.sis.referencing.crs.DefaultProjectedCRS
     */
    @Override
    public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PROJECTED_CRS, code);
    }

    /**
     * Returns a vertical coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalDatum(String)
     * @see #createVerticalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultVerticalCRS
     */
    @Override
    public VerticalCRS createVerticalCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_CRS, code);
    }

    /**
     * Returns a temporal coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTemporalCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createTemporalDatum(String)
     * @see #createTimeCS(String)
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS
     */
    @Override
    public TemporalCRS createTemporalCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TEMPORAL_CRS, code);
    }

    /**
     * Returns a 3D or 4D coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCompoundCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalCRS(String)
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.crs.DefaultCompoundCRS
     */
    @Override
    public CompoundCRS createCompoundCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COMPOUND_CRS, code);
    }

    /**
     * Returns a derived coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDerivedCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.crs.DefaultDerivedCRS
     */
    @Override
    public DerivedCRS createDerivedCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.DERIVED_CRS, code);
    }

    /**
     * Returns an engineering coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEngineeringCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEngineeringDatum(String)
     * @see org.apache.sis.referencing.crs.DefaultEngineeringCRS
     */
    @Override
    public EngineeringCRS createEngineeringCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ENGINEERING_CRS, code);
    }

    /**
     * Returns an image coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createImageCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createImageDatum(String)
     * @see org.apache.sis.referencing.crs.DefaultImageCRS
     */
    @Override
    public ImageCRS createImageCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.IMAGE_CRS, code);
    }

    /**
     * Returns an arbitrary datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createVerticalDatum(String)
     * @see #createTemporalDatum(String)
     */
    @Override
    public Datum createDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.DATUM, code);
    }

    /**
     * Returns a geodetic datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeodeticDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEllipsoid(String)
     * @see #createPrimeMeridian(String)
     * @see #createGeographicCRS(String)
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEODETIC_DATUM, code);
    }

    /**
     * Returns a vertical datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
     */
    @Override
    public VerticalDatum createVerticalDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_DATUM, code);
    }

    /**
     * Returns a temporal datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTemporalDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultTemporalDatum
     */
    @Override
    public TemporalDatum createTemporalDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TEMPORAL_DATUM, code);
    }

    /**
     * Returns an engineering datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEngineeringDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEngineeringCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultEngineeringDatum
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ENGINEERING_DATUM, code);
    }

    /**
     * Returns an image datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createImageDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createImageCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultImageDatum
     */
    @Override
    public ImageDatum createImageDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.IMAGE_DATUM, code);
    }

    /**
     * Returns an ellipsoid from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEllipsoid(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The ellipsoid for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     */
    @Override
    public Ellipsoid createEllipsoid(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ELLIPSOID, code);
    }

    /**
     * Returns a prime meridian from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createPrimeMeridian(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The prime meridian for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PRIME_MERIDIAN, code);
    }

    /**
     * Returns an extent (usually a domain of validity) from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createExtent(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The extent for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @Override
    public Extent createExtent(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.EXTENT, code);
    }

    /**
     * Returns an arbitrary coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateSystem(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateSystemAxis(String)
     * @see #createEllipsoidalCS(String)
     * @see #createCartesianCS(String)
     */
    @Override
    public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COORDINATE_SYSTEM, code);
    }

    /**
     * Returns an ellipsoidal coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEllipsoidalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEllipsoid(String)
     * @see #createGeodeticDatum(String)
     * @see #createGeographicCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultEllipsoidalCS
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ELLIPSOIDAL_CS, code);
    }

    /**
     * Returns a vertical coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalDatum(String)
     * @see #createVerticalCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultVerticalCS
     */
    @Override
    public VerticalCS createVerticalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_CS, code);
    }

    /**
     * Returns a temporal coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTimeCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createTemporalDatum(String)
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultTimeCS
     */
    @Override
    public TimeCS createTimeCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TIME_CS, code);
    }

    /**
     * Returns a Cartesian coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCartesianCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createProjectedCRS(String)
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultCartesianCS
     */
    @Override
    public CartesianCS createCartesianCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CARTESIAN_CS, code);
    }

    /**
     * Returns a spherical coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createSphericalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultSphericalCS
     */
    @Override
    public SphericalCS createSphericalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.SPHERICAL_CS, code);
    }

    /**
     * Returns a cylindrical coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCylindricalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.DefaultCylindricalCS
     */
    @Override
    public CylindricalCS createCylindricalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CYLINDRICAL_CS, code);
    }

    /**
     * Returns a polar coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createPolarCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.DefaultPolarCS
     */
    @Override
    public PolarCS createPolarCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.POLAR_CS, code);
    }

    /**
     * Returns a coordinate system axis from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateSystemAxis(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The axis for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateSystem(String)
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.AXIS, code);
    }

    /**
     * Returns an unit of measurement from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createUnit(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The unit of measurement for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Unit<?> createUnit(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.UNIT, code);
    }

    /**
     * Returns a parameter descriptor from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createParameterDescriptor(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The parameter descriptor for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.parameter.DefaultParameterDescriptor
     */
    @Override
    public ParameterDescriptor<?> createParameterDescriptor(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PARAMETER, code);
    }

    /**
     * Returns an operation method from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createOperationMethod(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The operation method for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod
     */
    @Override
    public OperationMethod createOperationMethod(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.METHOD, code);
    }

    /**
     * Returns an operation from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateOperation(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The operation for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation
     */
    @Override
    public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OPERATION, code);
    }

    /**
     * The key objects to use in the {@link CachingAuthorityFactory#cache}.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-2">GEOTK-2</a>
     */
    private static final class Key {
        /** The type of the cached object.    */ final Class<?> type;
        /** The cached object authority code. */ final String code;

        /** Creates a new key for the given type and code. */
        Key(final Class<?> type, final String code) {
            this.type = type;
            this.code = code;
        }

        /** Returns the hash code value for this key. */
        @Override public int hashCode() {
            return type.hashCode() ^ code.hashCode();
        }

        /** Compares this key with the given object for equality .*/
        @Override public boolean equals(final Object other) {
            if (other instanceof Key) {
                final Key that = (Key) other;
                return type.equals(that.type) && code.equals(that.code);
            }
            return false;
        }

        /** String representation used by {@link CacheRecord}. */
        @Override @Debug public String toString() {
            final StringBuilder buffer = new StringBuilder("Key[").append(code);
            if (buffer.length() > 15) { // Arbitrary limit in string length.
                buffer.setLength(15);
                buffer.append('…');
            }
            return buffer.append(" : ").append(type.getSimpleName()).append(']').toString();
        }
    }

    /**
     * Returns an object from a code using the given proxy. This method first checks in the cache.
     * If no object exists in the cache for the given code, then a lock is created and the object
     * creation is delegated to the {@linkplain #getBackingStore() backing store}.
     * The result is then stored in the cache and returned.
     *
     * @param  <T>   The type of the object to be returned.
     * @param  proxy The proxy to use for creating the object.
     * @param  code  The code of the object to create.
     * @return The object extracted from the cache or created.
     * @throws FactoryException If an error occurred while creating the object.
     */
    private <T> T create(final AuthorityFactoryProxy<T> proxy, final String code) throws FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        final Class<T> type = proxy.type;
        final Key key = new Key(type, trimAuthority(code));
        Object value = cache.peek(key);
        if (!type.isInstance(value)) {
            final Cache.Handler<Object> handler = cache.lock(key);
            try {
                value = handler.peek();
                if (!type.isInstance(value)) {
                    final T result;
                    final GeodeticAuthorityFactory factory = getBackingStore();
                    try {
                        result = proxy.create(factory, code);
                    } finally {
                        release();
                    }
                    value = result;     // For the finally block below.
                    return result;
                }
            } finally {
                handler.putAndUnlock(value);
            }
        }
        return type.cast(value);
    }

    /**
     * Returns operations from source and target coordinate reference system codes.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached collection for the given pair of codes if such collection already exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the backing store,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes(String, String)} method,</li>
     *       <li>release the backing store — <em>this step assumes that the collection obtained at step 2
     *           is still valid after the backing store has been released</em>,</li>
     *       <li>cache the result — <em>this step assumes that the collection obtained at step 2 is immutable</em>.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @return The operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(
            final String sourceCRS, final String targetCRS) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        final CodePair key = new CodePair(trimAuthority(sourceCRS), trimAuthority(targetCRS));
        Object value = cache.peek(key);
        if (!(value instanceof Set<?>)) {
            final Cache.Handler<Object> handler = cache.lock(key);
            try {
                value = handler.peek();
                if (!(value instanceof Set<?>)) {
                    final GeodeticAuthorityFactory factory = getBackingStore();
                    try {
                        value = factory.createFromCoordinateReferenceSystemCodes(sourceCRS, targetCRS);
                    } finally {
                        release();
                    }
                }
            } finally {
                handler.putAndUnlock(value);
            }
        }
        return (Set<CoordinateOperation>) value;
    }

    /**
     * A pair of codes for operations to cache with
     * {@link CachingAuthorityFactory#createFromCoordinateReferenceSystemCodes(String, String)}.
     */
    private static final class CodePair {
        /** Codes of source and target CRS. */
        private final String source, target;

        /** Creates a new key for the given pair of codes. */
        public CodePair(final String source, final String target) {
            this.source = source;
            this.target = target;
        }

        /** Returns the hash code value for this key. */
        @Override public int hashCode() {
            return source.hashCode() + target.hashCode() * 31;
        }

        /** Compares this key with the given object for equality .*/
        @Override public boolean equals(final Object other) {
            if (other instanceof CodePair) {
                final CodePair that = (CodePair) other;
                return source.equals(that.source) && target.equals(that.target);
            }
            return false;
        }

        /** String representation used by {@link CacheRecord}. */
        @Override @Debug public String toString() {
            return "CodePair[" + source + " → " + target + ']';
        }
    }

    /**
     * Returns a finder which can be used for looking up unidentified objects.
     * The default implementation delegates lookup to the underlying backing store and caches the result.
     *
     * @return A finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder can not be created.
     */
    @Override
    public IdentifiedObjectFinder createIdentifiedObjectFinder(final Class<? extends IdentifiedObject> type)
            throws FactoryException
    {
        return new Finder(this, type);
    }

    /**
     * An implementation of {@link IdentifiedObjectFinder} which delegates
     * the work to the underlying backing store and caches the result.
     *
     * <div class="section">Implementation note</div>
     * we will create objects using directly the underlying backing store, not using the cache.
     * This is because hundred of objects may be created during a scan while only one will be typically retained.
     * We do not want to flood the cache with every false candidates that we encounter during the scan.
     *
     * <div class="section">Synchronization note</div>
     * our public API claims that {@link IdentifiedObjectFinder}s are not thread-safe.
     * Nevertheless we synchronize this particular implementation for safety, because the consequence of misuse
     * are more dangerous than other implementations. Furthermore this is also a way to assert that no code path
     * go to the {@link #create(AuthorityFactoryProxy, String)} method from a non-overridden public method.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    private static final class Finder extends IdentifiedObjectFinder {
        /**
         * The finder on which to delegate the work. This is acquired by {@link #acquire()}
         * <strong>and must be released</strong> by call to {@link #release()} once finished.
         */
        private transient IdentifiedObjectFinder finder;

        /**
         * Number of time that {@link #acquire()} has been invoked.
         * When this count reaches zero, the {@linkplain #finder} is released.
         */
        private transient int acquireCount;

        /**
         * Creates a finder for the given type of objects.
         */
        Finder(final CachingAuthorityFactory factory, final Class<? extends IdentifiedObject> type) {
            super(factory, type);
        }

        /**
         * Acquires a new {@linkplain #finder}.
         * The {@link #release()} method must be invoked in a {@code finally} block after the call to {@code acquire}.
         * The pattern must be as below (note that the call to {@code acquire()} is inside the {@code try} block):
         *
         * {@preformat java
         *     try {
         *         acquire();
         *         (finder or proxy).doSomeStuff();
         *     } finally {
         *         release();
         *     }
         * }
         */
        private void acquire() throws FactoryException {
            assert Thread.holdsLock(this);
            assert (acquireCount == 0) == (finder == null) : acquireCount;
            if (acquireCount == 0) {
                final GeodeticAuthorityFactory delegate = ((CachingAuthorityFactory) factory).getBackingStore();
                /*
                 * Set 'acquireCount' only after we succeed in fetching the factory, and before any operation on it.
                 * The intend is to get CachingAuthorityFactory.release() invoked if and only if the getBackingStore()
                 * method succeed, no matter what happen after this point.
                 */
                acquireCount = 1;
                finder = delegate.createIdentifiedObjectFinder(getObjectType());
                finder.setWrapper(this);
            } else {
                acquireCount++;
            }
        }

        /**
         * Releases the {@linkplain #finder}.
         */
        private void release() {
            assert Thread.holdsLock(this);
            if (acquireCount == 0) {
                // May happen only if a failure occurred during getBackingStore() execution.
                return;
            }
            if (--acquireCount == 0) {
                finder = null;
                ((CachingAuthorityFactory) factory).release();
            }
        }

        /**
         * Returns the authority of the factory examined by this finder.
         */
        @Override
        public synchronized Citation getAuthority() throws FactoryException {
            try {
                acquire();
                return finder.getAuthority();
            } finally {
                release();
            }
        }

        /**
         * Returns a set of authority codes that <strong>may</strong> identify the same
         * object than the specified one. This method delegates to the backing finder.
         */
        @Override
        protected synchronized Set<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
            try {
                acquire();
                return finder.getCodeCandidates(object);
            } finally {
                release();
            }
        }

        /**
         * Looks up an object from this authority factory which is approximatively equal to the specified object.
         * The default implementation performs the same lookup than the backing store and caches the result.
         */
        @Override
        public IdentifiedObject find(final IdentifiedObject object) throws FactoryException {
            final Map<IdentifiedObject, IdentifiedObject> findPool = ((CachingAuthorityFactory) factory).findPool;
            synchronized (findPool) {
                final IdentifiedObject candidate = findPool.get(object);
                if (candidate != null) {
                    return (candidate == NilReferencingObject.INSTANCE) ? null : candidate;
                }
            }
            /*
             * Nothing has been found in the cache. Delegates the search to the backing store.
             * We must delegate to 'finder' (not to 'super') in order to take advantage of overridden methods.
             */
            final IdentifiedObject candidate;
            synchronized (this) {
                try {
                    acquire();
                    candidate = finder.find(object);
                } finally {
                    release();
                }
            }
            /*
             * If the full scan was allowed, then stores the result even if null so
             * we can remember that no object has been found for the given argument.
             */
            if (candidate != null || isFullScanAllowed()) {
                synchronized (findPool) {
                    findPool.put(object, (candidate == null) ? NilReferencingObject.INSTANCE : candidate);
                }
            }
            return candidate;
        }
    }

    /**
     * Prints the cache content to the given writer.
     * Keys are sorted by numerical order if possible, or alphabetical order otherwise.
     * This method is used for debugging purpose only.
     *
     * @param out The output printer, or {@code null} for the {@linkplain System#out standard output stream}.
     */
    @Debug
    public void printCacheContent(final PrintWriter out) {
        CacheRecord.printCacheContent(cache, out);
    }

    /**
     * Releases resources immediately instead of waiting for the garbage collector.
     * Once a factory has been disposed, further {@code createFoo(String)} method invocations
     * may throw a {@link FactoryException}. Disposing a previously-disposed factory, however, has no effect.
     */
    @Override
    public void dispose() {
        cache.clear();
        authority = null;
        synchronized (findPool) {
            findPool.clear();
        }
    }
}
