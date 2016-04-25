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

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.xml.bind.JAXBException;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.referencing.cs.*;
import org.apache.sis.referencing.crs.*;
import org.apache.sis.referencing.datum.*;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.MergedProperties;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.io.wkt.Parser;
import org.apache.sis.xml.XML;


/**
 * Creates {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} (CRS) implementations,
 * with their {@linkplain org.apache.sis.referencing.cs.AbstractCS Coordinate System} (CS)
 * and {@linkplain org.apache.sis.referencing.datum.AbstractDatum Datum} components.
 * This factory serves two purposes:
 *
 * <ul>
 *   <li><b>For users</b>, allows the creation of complex objects that can not be created by the authority factories,
 *       without explicit dependency to Apache SIS (when using the GeoAPI interfaces implemented by this class).</li>
 *   <li><b>For providers</b>, allows <cite>inversion of control</cite> by overriding methods in this class,
 *       then specifying the customized instance to other services that consume {@code CRSFactory} (for example
 *       authority factories or {@linkplain org.apache.sis.io.wkt.WKTFormat WKT parsers}).</li>
 * </ul>
 *
 * This {@code GeodeticObjectFactory} class is not easy to use directly.
 * Users are encouraged to use an authority factory instead
 * (or the {@link org.apache.sis.referencing.CRS#forCode(String)} convenience method)
 * when the CRS object to construct can be identified by a code in the namespace of an authority (typically EPSG).
 *
 * <div class="section">Object properties</div>
 * Most factory methods expect a {@link Map Map&lt;String,?&gt;} argument, often followed by explicit arguments.
 * Unless otherwise noticed, information provided in the {@code properties} map are considered ignorable metadata
 * while information provided in explicit arguments have an impact on coordinate transformation results.
 *
 * <p>The following table lists the keys recognized by the {@code GeodeticObjectFactory} default implementation,
 * together with the type of values associated to those keys.
 * A value for the {@code "name"} key is mandatory for all objects, while all other properties are optional.
 * {@code GeodeticObjectFactory} methods ignore all unknown properties.</p>
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
 *     <td>{@link Identifier} or {@link String}</td>
 *     <td>{@link AbstractIdentifiedObject#getName()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.metadata.Identifier#AUTHORITY_KEY}</td>
 *     <td>{@link String} or {@link Citation}</td>
 *     <td>{@link NamedIdentifier#getAuthority()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.metadata.Identifier#CODE_KEY}</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getCode()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.metadata.Identifier#CODESPACE_KEY}</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getCodeSpace()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.metadata.Identifier#VERSION_KEY}</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getVersion()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.metadata.Identifier#DESCRIPTION_KEY}</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getDescription()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
 *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
 *     <td>{@link AbstractIdentifiedObject#getAlias()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
 *     <td>{@link Identifier} (optionally as array)</td>
 *     <td>{@link AbstractIdentifiedObject#getIdentifiers()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
 *     <td>{@link Extent}</td>
 *     <td>{@link AbstractReferenceSystem#getDomainOfValidity()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
 *     <td>{@link String} or {@link InternationalString}</td>
 *     <td>{@link AbstractReferenceSystem#getScope()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractDatum#getAnchorPoint()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.datum.Datum#REALIZATION_EPOCH_KEY}</td>
 *     <td>{@link Date}</td>
 *     <td>{@link AbstractDatum#getRealizationEpoch()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractIdentifiedObject#getRemarks()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#DEPRECATED_KEY}</td>
 *     <td>{@link Boolean}</td>
 *     <td>{@link AbstractIdentifiedObject#isDeprecated()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
 *     <td>{@link Locale}</td>
 *     <td>(none)</td>
 *   </tr>
 * </table>
 *
 * <div class="section">Localization</div>
 * All localizable attributes like {@code "remarks"} may have a language and country code suffix.
 * For example the {@code "remarks_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
 * the {@code "remarks_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
 * They are convenience properties for building the {@code InternationalString} value.
 *
 * <p>The {@code "locale"} property applies only in case of exception for formatting the error message, and
 * is used only on a <cite>best effort</cite> basis. The locale is discarded after successful construction
 * since localizations are applied by the {@link InternationalString#toString(Locale)} method.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public class GeodeticObjectFactory extends AbstractFactory implements CRSFactory, CSFactory, DatumFactory, Parser {
    /**
     * The logger to use for reporting object creations.
     */
    private static final Logger LOGGER = Logging.getLogger(Loggers.CRS_FACTORY);

    /**
     * The constructor for WKT parsers, fetched when first needed. The WKT parser is defined in the
     * same module than this class, so we will hopefully not have security issues.  But we have to
     * use reflection because the parser class is not yet public (because we do not want to commit
     * its API yet).
     */
    private static volatile Constructor<? extends Parser> parserConstructor;

    /**
     * The default properties, or an empty map if none. This map shall not change after construction in
     * order to allow usage without synchronization in multi-thread context. But we do not need to wrap
     * in a unmodifiable map since {@code GeodeticObjectFactory} does not provide public access to it.
     */
    private final Map<String,?> defaultProperties;

    /**
     * The math transform factory. Will be created only when first needed.
     * This is normally not needed by this factory, except when constructing derived and projected CRS.
     *
     * @see #getMathTransformFactory()
     */
    private volatile MathTransformFactory mtFactory;

    /**
     * Weak references to existing objects (CRS, CS, Datum, Ellipsoid or PrimeMeridian).
     * This set is used in order to return a pre-existing object instead of creating a new one.
     */
    private final WeakHashSet<AbstractIdentifiedObject> pool;

    /**
     * The <cite>Well Known Text</cite> parser for {@code CoordinateReferenceSystem} instances.
     * This parser is not thread-safe, so we need to prevent two threads from using the same instance in same time.
     */
    private final AtomicReference<Parser> parser;

    /**
     * Constructs a factory with no default properties.
     */
    public GeodeticObjectFactory() {
        this(null);
    }

    /**
     * Constructs a factory with the given default properties.
     * {@code GeodeticObjectFactory} will fallback on the map given to this constructor for any property
     * not present in the map provided to a {@code createFoo(Map<String,?>, …)} method.
     *
     * @param properties The default properties, or {@code null} if none.
     */
    public GeodeticObjectFactory(Map<String,?> properties) {
        if (properties == null || properties.isEmpty()) {
            properties = Collections.emptyMap();
        } else {
            properties = CollectionsExt.compact(new HashMap<String,Object>(properties));
        }
        defaultProperties = properties;
        pool = new WeakHashSet<AbstractIdentifiedObject>(AbstractIdentifiedObject.class);
        parser = new AtomicReference<Parser>();
    }

    /**
     * Returns the union of the given {@code properties} map with the default properties given at
     * {@linkplain #GeodeticObjectFactory(Map) construction time}. Entries in the given properties
     * map have precedence, even if their {@linkplain java.util.Map.Entry#getValue() value} is {@code null}
     * (i.e. a null value "erase" the default property value).
     * Entries with null value after the union will be omitted.
     *
     * <p>This method is invoked by all {@code createFoo(Map<String,?>, …)} methods.</p>
     *
     * @param  properties The user-supplied properties.
     * @return The union of the given properties with the default properties.
     */
    protected Map<String,?> complete(final Map<String,?> properties) {
        ArgumentChecks.ensureNonNull("properties", properties);
        return new MergedProperties(properties, defaultProperties) {
            /**
             * Handles the {@code "mtFactory"} key in a special way since this is normally not needed for
             * {@link GeodeticObjectFactory}, except when creating the SIS implementation of derived or
             * projected CRS (because of the way we implemented derived CRS, but this is specific to SIS).
             */
            @Override
            protected Object invisibleEntry(final Object key) {
                if (ReferencingServices.MT_FACTORY.equals(key)) {
                    return getMathTransformFactory();
                } else {
                    return super.invisibleEntry(key);
                }
            }
        };
    }

    /**
     * Returns the math transform factory for internal usage only.
     * The {@code MathTransformFactory} is normally not needed by {@code GeodeticObjectFactory},
     * except when constructing the Apache SIS implementation of derived and projected CRS.
     * For this reason, we will fetch this dependency only if really requested.
     */
    final MathTransformFactory getMathTransformFactory() {
        MathTransformFactory factory = mtFactory;
        if (factory == null) {
            mtFactory = factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        }
        return factory;
    }

    /**
     * Returns a unique instance of the given object. If this method recycles an existing object,
     * then the existing instance is returned silently. Otherwise this method logs a message at
     * {@link Level#FINE} telling that a new object has been created.
     */
    private <T extends AbstractIdentifiedObject> T unique(final String caller, final T object) {
        final T c = pool.unique(object);
        if (c == object && LOGGER.isLoggable(Level.FINE)) {
            final String id = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(c, null));
            final LogRecord record = Messages.getResources(null).getLogRecord(Level.FINE,
                    (id != null) ? Messages.Keys.CreatedIdentifiedObject_3
                                 : Messages.Keys.CreatedNamedObject_2,
                    c.getInterface(), c.getName().getCode(), id);
            record.setSourceClassName(GeodeticObjectFactory.class.getCanonicalName());
            record.setSourceMethodName(caller);
            record.setLoggerName(LOGGER.getName());
            LOGGER.log(record);
        }
        return c;
    }

    /**
     * Creates a geocentric coordinate reference system from a {@linkplain CartesianCS Cartesian coordinate system}.
     * Geocentric CRS have their origin at the approximate centre of mass of the earth.
     * An {@linkplain #createGeocentricCRS(Map, GeodeticDatum, SphericalCS) alternate method} allows creation of the
     * same kind of CRS with spherical coordinate system instead than a Cartesian one.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     *   <li>{@link #createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultGeocentricCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The geodetic datum to use in created CRS.
     * @param  cs         The three-dimensional Cartesian coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see GeodeticAuthorityFactory#createGeocentricCRS(String)
     * @see DefaultGeocentricCRS#DefaultGeocentricCRS(Map, GeodeticDatum, CartesianCS)
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final CartesianCS cs) throws FactoryException
    {
        final DefaultGeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeocentricCRS", crs);
    }

    /**
     * Creates a three-dimensional Cartesian coordinate system from the given set of axis.
     * This coordinate system can be used with geocentric, engineering and derived CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultCartesianCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis (e.g. “Geocentric X”).
     * @param  axis1 The second axis (e.g. “Geocentric Y”).
     * @param  axis2 The third  axis (e.g. “Geocentric Z”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCartesianCS#DefaultCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createCartesianCS(String)
     */
    @Override
    public CartesianCS createCartesianCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final DefaultCartesianCS cs;
        try {
            cs = new DefaultCartesianCS(complete(properties), axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createCartesianCS", cs);
    }

    /**
     * Creates a geocentric coordinate reference system from a {@linkplain SphericalCS spherical coordinate system}.
     * Geocentric CRS have their origin at the approximate centre of mass of the earth.
     * An {@linkplain #createGeocentricCRS(Map, GeodeticDatum, CartesianCS) alternate method} allows creation of the
     * same kind of CRS with Cartesian coordinate system instead than a spherical one.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createSphericalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     *   <li>{@link #createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultGeocentricCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      Geodetic datum to use in created CRS.
     * @param  cs         The spherical coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeocentricCRS#DefaultGeocentricCRS(Map, GeodeticDatum, SphericalCS)
     * @see GeodeticAuthorityFactory#createGeocentricCRS(String)
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final SphericalCS cs) throws FactoryException
    {
        final DefaultGeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeocentricCRS", crs);
    }

    /**
     * Creates a spherical coordinate system from the given set of axis.
     * This coordinate system can be used with geocentric, engineering and derived CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultSphericalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis (e.g. “Spherical latitude”).
     * @param  axis1 The second axis (e.g. “Spherical longitude”).
     * @param  axis2 The third  axis (e.g. “Geocentric radius”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultSphericalCS#DefaultSphericalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createSphericalCS(String)
     */
    @Override
    public SphericalCS createSphericalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final DefaultSphericalCS cs;
        try {
            cs = new DefaultSphericalCS(complete(properties), axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createSphericalCS", cs);
    }

    /**
     * Creates a geographic coordinate reference system. It can be (<var>latitude</var>, <var>longitude</var>)
     * or (<var>longitude</var>, <var>latitude</var>), optionally with an ellipsoidal height.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *     <li>{@link #createEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   </ul></li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     *   <li>{@link #createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultGeographicCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      Geodetic datum to use in created CRS.
     * @param  cs         The two- or three-dimensional ellipsoidal coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeographicCRS#DefaultGeographicCRS(Map, GeodeticDatum, EllipsoidalCS)
     * @see GeodeticAuthorityFactory#createGeographicCRS(String)
     */
    @Override
    public GeographicCRS createGeographicCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final EllipsoidalCS cs) throws FactoryException
    {
        final DefaultGeographicCRS crs;
        try {
            crs = new DefaultGeographicCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeographicCRS", crs);
    }

    /**
     * Creates geodetic datum from ellipsoid and (optionally) Bursa-Wolf parameters.
     * Geodetic datum defines the location and orientation of an ellipsoid that approximates the shape of the earth.
     * This datum can be used with geographic, geocentric and engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultGeodeticDatum} instance.
     *
     * @param  properties    Name and other properties to give to the new object.
     * @param  ellipsoid     The ellipsoid to use in new geodetic datum.
     * @param  primeMeridian The prime meridian to use in new geodetic datum.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeodeticDatum#DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian)
     * @see GeodeticAuthorityFactory#createGeodeticDatum(String)
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final Map<String,?> properties,
            final Ellipsoid ellipsoid, final PrimeMeridian primeMeridian) throws FactoryException
    {
        final DefaultGeodeticDatum datum;
        try {
            datum = new DefaultGeodeticDatum(complete(properties), ellipsoid, primeMeridian);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeodeticDatum", datum);
    }

    /**
     * Creates a prime meridian, relative to Greenwich.
     * Defines the origin from which longitude values are determined.
     *
     * <p>The default implementation creates a {@link DefaultPrimeMeridian} instance.</p>
     *
     * @param  properties  Name and other properties to give to the new object.
     * @param  longitude   The longitude of prime meridian in supplied angular units East of Greenwich.
     * @param  angularUnit The angular units of longitude.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultPrimeMeridian#DefaultPrimeMeridian(Map, double, Unit)
     * @see GeodeticAuthorityFactory#createPrimeMeridian(String)
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final Map<String,?> properties,
            final double longitude, final Unit<Angle> angularUnit) throws FactoryException
    {
        final DefaultPrimeMeridian meridian;
        try {
            meridian = new DefaultPrimeMeridian(complete(properties), longitude, angularUnit);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createPrimeMeridian", meridian);
    }

    /**
     * Creates an ellipsoidal coordinate system without ellipsoidal height.
     * It can be (<var>latitude</var>, <var>longitude</var>) or (<var>longitude</var>, <var>latitude</var>).
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultEllipsoidalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis (e.g. “Geodetic latitude”).
     * @param  axis1 The second axis (e.g. “Geodetic longitude”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoidalCS#DefaultEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createEllipsoidalCS(String)
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final DefaultEllipsoidalCS cs;
        try {
            cs = new DefaultEllipsoidalCS(complete(properties), axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createEllipsoidalCS", cs);
    }

    /**
     * Creates an ellipsoidal coordinate system with ellipsoidal height.
     * It can be (<var>latitude</var>, <var>longitude</var>, <var>height</var>)
     * or (<var>longitude</var>, <var>latitude</var>, <var>height</var>).
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultEllipsoidalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis (e.g. “Geodetic latitude”).
     * @param  axis1 The second axis (e.g. “Geodetic longitude”).
     * @param  axis2 The third  axis (e.g. “Ellipsoidal height”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoidalCS#DefaultEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createEllipsoidalCS(String)
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final DefaultEllipsoidalCS cs;
        try {
            cs = new DefaultEllipsoidalCS(complete(properties), axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createEllipsoidalCS", cs);
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
     * @see GeodeticAuthorityFactory#createEllipsoid(String)
     */
    @Override
    public Ellipsoid createEllipsoid(final Map<String,?> properties,
            final double semiMajorAxis, final double semiMinorAxis,
            final Unit<Length> unit) throws FactoryException
    {
        final DefaultEllipsoid ellipsoid;
        try {
            ellipsoid = DefaultEllipsoid.createEllipsoid(complete(properties), semiMajorAxis, semiMinorAxis, unit);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createEllipsoid", ellipsoid);
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
     * @see GeodeticAuthorityFactory#createEllipsoid(String)
     */
    @Override
    public Ellipsoid createFlattenedSphere(final Map<String,?> properties,
            final double semiMajorAxis, final double inverseFlattening,
            final Unit<Length> unit) throws FactoryException
    {
        final DefaultEllipsoid ellipsoid;
        try {
            ellipsoid = DefaultEllipsoid.createFlattenedSphere(complete(properties), semiMajorAxis, inverseFlattening, unit);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createFlattenedSphere", ellipsoid);
    }

    /**
     * Creates a projected coordinate reference system from a conversion.
     * Projected CRS are used to approximate the shape of the earth on a planar surface in such a way
     * that the distortion that is inherent to the approximation is controlled and known.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   <li>{@link #createEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     *   <li>{@link #createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}</li>
     *   <li>{@link #createGeographicCRS(Map, GeodeticDatum, EllipsoidalCS)}</li>
     *   <li>{@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#createDefiningConversion(Map, OperationMethod, ParameterValueGroup)}</li>
     * </ol></div>
     *
     * The supplied {@code conversion} argument shall <strong>not</strong> includes the operation steps
     * for performing {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes unit
     * conversions and change of axis order} since those operations will be inferred by this constructor.
     *
     * <p>The default implementation creates a {@link DefaultProjectedCRS} instance.</p>
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  baseCRS    The geographic coordinate reference system to base projection on.
     * @param  conversion The defining conversion from a {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED
     *                    normalized} base to a normalized derived CRS.
     * @param  derivedCS  The coordinate system for the projected CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultProjectedCRS#DefaultProjectedCRS(Map, GeographicCRS, Conversion, CartesianCS)
     * @see GeodeticAuthorityFactory#createProjectedCRS(String)
     */
    @Override
    public ProjectedCRS createProjectedCRS(final Map<String,?> properties,
            final GeographicCRS baseCRS, final Conversion conversion,
            final CartesianCS derivedCS) throws FactoryException
    {
        final DefaultProjectedCRS crs;
        try {
            crs = new DefaultProjectedCRS(complete(properties), baseCRS, conversion, derivedCS);
        } catch (IllegalArgumentException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof FactoryException) {
                throw (FactoryException) cause;         // Must be propagated for allowing caller to catch NoSuchIdentifierException.
            }
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createProjectedCRS", crs);
    }

    /**
     * Creates a two-dimensional Cartesian coordinate system from the given pair of axis.
     * This coordinate system can be used with projected, engineering and derived CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultCartesianCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis (e.g. “Easting”).
     * @param  axis1 The second axis (e.g. “Northing”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCartesianCS#DefaultCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createCartesianCS(String)
     */
    @Override
    public CartesianCS createCartesianCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final DefaultCartesianCS cs;
        try {
            cs = new DefaultCartesianCS(complete(properties), axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createCartesianCS", cs);
    }

    /**
     * Creates a derived coordinate reference system from a conversion.
     * The derived CRS returned by this method may also implement the {@link GeodeticCRS}, {@link VerticalCRS},
     * {@link TemporalCRS} or {@link EngineeringCRS} interface depending on the type of the base CRS and the
     * coordinate system.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>A {@code createFooCS(…)} method for Cartesian, spherical, ellipsoidal, vertical, temporal, linear, affine, polar, cylindrical or user-defined CS.</li>
     *   <li>An other {@code createFooCRS(…)} method for geocentric, geographic, vertical, temporal or engineering CRS.</li>
     *   <li>{@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#createDefiningConversion(Map, OperationMethod, ParameterValueGroup)}</li>
     * </ol></div>
     *
     * The supplied {@code conversion} argument shall <strong>not</strong> includes the operation steps
     * for performing {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes unit
     * conversions and change of axis order} since those operations will be inferred by this constructor.
     *
     * <p>The default implementation creates a {@link DefaultDerivedCRS} instance.</p>
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  baseCRS    The coordinate reference system to base projection on. Shall be an instance of {@link SingleCRS}.
     * @param  conversion The defining conversion from a {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED
     *                    normalized} base to a normalized derived CRS.
     * @param  derivedCS  The coordinate system for the derived CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultDerivedCRS#create(Map, SingleCRS, Conversion, CoordinateSystem)
     * @see GeodeticAuthorityFactory#createDerivedCRS(String)
     */
    @Override
    public DerivedCRS createDerivedCRS(final Map<String,?> properties,
            final CoordinateReferenceSystem baseCRS, final Conversion conversion,
            final CoordinateSystem derivedCS) throws FactoryException
    {
        ArgumentChecks.ensureCanCast("baseCRS", SingleCRS.class, baseCRS);
        final DefaultDerivedCRS crs;
        try {
            crs = DefaultDerivedCRS.create(complete(properties), (SingleCRS) baseCRS, conversion, derivedCS);
        } catch (IllegalArgumentException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof FactoryException) {
                throw (FactoryException) cause;         // Must be propagated for allowing caller to catch NoSuchIdentifierException.
            }
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createDerivedCRS", crs);
    }

    /**
     * Creates a vertical coordinate reference system.
     * Vertical CRSs make use of the direction of gravity to define the concept of height or depth,
     * but the relationship with gravity may not be straightforward.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createVerticalCS(Map, CoordinateSystemAxis)}</li>
     *   <li>{@link #createVerticalDatum(Map, VerticalDatumType)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultVerticalCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The vertical datum to use in created CRS.
     * @param  cs         The vertical coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalCRS#DefaultVerticalCRS(Map, VerticalDatum, VerticalCS)
     * @see GeodeticAuthorityFactory#createVerticalCRS(String)
     */
    @Override
    public VerticalCRS createVerticalCRS(final Map<String,?> properties,
            final VerticalDatum datum, final VerticalCS cs) throws FactoryException
    {
        final DefaultVerticalCRS crs;
        try {
            crs = new DefaultVerticalCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createVerticalCRS", crs);
    }

    /**
     * Creates a vertical datum from an enumerated type value.
     * The default implementation creates a {@link DefaultVerticalDatum} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  type       The type of this vertical datum (often geoidal).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalDatum#DefaultVerticalDatum(Map, VerticalDatumType)
     * @see GeodeticAuthorityFactory#createVerticalDatum(String)
     */
    @Override
    public VerticalDatum createVerticalDatum(final Map<String,?> properties,
            final VerticalDatumType type) throws FactoryException
    {
        final DefaultVerticalDatum datum;
        try {
            datum = new DefaultVerticalDatum(complete(properties), type);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createVerticalDatum", datum);
    }

    /**
     * Creates a vertical coordinate system.
     * This coordinate system can be used with vertical and derived CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultVerticalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis The single axis (e.g. “height” or “depth”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalCS#DefaultVerticalCS(Map, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createVerticalCS(String)
     */
    @Override
    public VerticalCS createVerticalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        final DefaultVerticalCS cs;
        try {
            cs = new DefaultVerticalCS(complete(properties), axis);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createVerticalCS", cs);
    }

    /**
     * Creates a temporal coordinate reference system.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createTimeCS(Map, CoordinateSystemAxis)}</li>
     *   <li>{@link #createTemporalDatum(Map, Date)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultTemporalCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The temporal datum to use in created CRS.
     * @param  cs         The temporal coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTemporalCRS#DefaultTemporalCRS(Map, TemporalDatum, TimeCS)
     * @see GeodeticAuthorityFactory#createTemporalCRS(String)
     */
    @Override
    public TemporalCRS createTemporalCRS(final Map<String,?> properties,
            final TemporalDatum datum, final TimeCS cs) throws FactoryException
    {
        final DefaultTemporalCRS crs;
        try {
            crs = new DefaultTemporalCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createTemporalCRS", crs);
    }

    /**
     * Creates a temporal datum from an enumerated type value.
     * The default implementation creates a {@link DefaultTemporalDatum} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  origin     The date and time origin of this temporal datum.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTemporalDatum#DefaultTemporalDatum(Map, Date)
     * @see GeodeticAuthorityFactory#createTemporalDatum(String)
     */
    @Override
    public TemporalDatum createTemporalDatum(final Map<String,?> properties,
            final Date origin) throws FactoryException
    {
        final DefaultTemporalDatum datum;
        try {
            datum = new DefaultTemporalDatum(complete(properties), origin);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createTemporalDatum", datum);
    }

    /**
     * Creates a temporal coordinate system.
     * This coordinate system can be used with temporal and derived CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultTimeCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis The single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTimeCS#DefaultTimeCS(Map, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createTimeCS(String)
     */
    @Override
    public TimeCS createTimeCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        final DefaultTimeCS cs;
        try {
            cs = new DefaultTimeCS(complete(properties), axis);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createTimeCS", cs);
    }

    /**
     * Creates a parametric coordinate reference system.
     * Parametric CRS can be used for physical properties or functions that vary monotonically with height.
     * A typical example is the pressure in meteorological applications.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createParametricCS(Map, CoordinateSystemAxis)}</li>
     *   <li>{@link #createParametricDatum(Map)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultParametricCRS} instance.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter types may be changed to
     * {@code org.opengis.referencing.datum.ParametricDatum} and {@code org.opengis.referencing.cs.ParametricCS},
     * and the return type may be changed to {@code org.opengis.referencing.crs.ParametricCRS}.
     * Those change are pending GeoAPI revision.</div>
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The parametric datum to use in created CRS.
     * @param  cs         The parametric coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultParametricCRS#DefaultParametricCRS(Map, DefaultParametricDatum, DefaultParametricCS)
     * @see GeodeticAuthorityFactory#createParametricCRS(String)
     */
    public DefaultParametricCRS createParametricCRS(final Map<String,?> properties,
            final DefaultParametricDatum datum, final DefaultParametricCS cs) throws FactoryException
    {
        final DefaultParametricCRS crs;
        try {
            crs = new DefaultParametricCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createParametricCRS", crs);
    }

    /**
     * Creates a parametric datum.
     * The default implementation creates a {@link DefaultParametricDatum} instance.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.datum.ParametricDatum}. This change is pending GeoAPI revision.</div>
     *
     * @param  properties Name and other properties to give to the new object.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultParametricDatum#DefaultParametricDatum(Map)
     * @see GeodeticAuthorityFactory#createParametricDatum(String)
     */
    public DefaultParametricDatum createParametricDatum(final Map<String,?> properties)
            throws FactoryException
    {
        final DefaultParametricDatum datum;
        try {
            datum = new DefaultParametricDatum(complete(properties));
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createParametricDatum", datum);
    }

    /**
     * Creates a parametric coordinate system.
     * This coordinate system can be used only with parametric CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultParametricCS} instance.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.cs.ParametricCS}. This change is pending GeoAPI revision.</div>
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis The single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultParametricCS#DefaultParametricCS(Map, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createParametricCS(String)
     */
    public DefaultParametricCS createParametricCS(Map<String, ?> properties, CoordinateSystemAxis axis) throws FactoryException {
        final DefaultParametricCS cs;
        try {
            cs = new DefaultParametricCS(complete(properties), axis);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createParametricCS", cs);
    }

    /**
     * Creates a compound coordinate reference system from an ordered list of {@code CoordinateReferenceSystem} objects.
     * Apache SIS puts no restriction on the components that can be used in a compound CRS.
     * However for better inter-operability, users are encouraged to follow the order mandated by ISO 19162:
     *
     * <ol>
     *   <li>A mandatory horizontal CRS (only one of two-dimensional {@code GeographicCRS} or {@code ProjectedCRS} or {@code EngineeringCRS}).</li>
     *   <li>Optionally followed by a {@code VerticalCRS} or a {@code ParametricCRS} (but not both).</li>
     *   <li>Optionally followed by a {@code TemporalCRS}.</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultCompoundCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  elements   Ordered array of {@code CoordinateReferenceSystem} objects.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCompoundCRS#DefaultCompoundCRS(Map, CoordinateReferenceSystem...)
     * @see GeodeticAuthorityFactory#createCompoundCRS(String)
     */
    @Override
    public CompoundCRS createCompoundCRS(final Map<String,?> properties,
            final CoordinateReferenceSystem... elements) throws FactoryException
    {
        final DefaultCompoundCRS crs;
        try {
            crs = new DefaultCompoundCRS(complete(properties), elements);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createCompoundCRS", crs);
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
     * @see DefaultImageCRS#DefaultImageCRS(Map, ImageDatum, AffineCS)
     * @see GeodeticAuthorityFactory#createImageCRS(String)
     */
    @Override
    public ImageCRS createImageCRS(final Map<String,?> properties,
            final ImageDatum datum, final AffineCS cs) throws FactoryException
    {
        final DefaultImageCRS crs;
        try {
            crs = new DefaultImageCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createImageCRS", crs);
    }

    /**
     * Creates an image datum.
     * The default implementation creates a {@link DefaultImageDatum} instance.
     *
     * @param  properties  Name and other properties to give to the new object.
     * @param  pixelInCell Specification of the way the image grid is associated with the image data attributes.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultImageDatum#DefaultImageDatum(Map, PixelInCell)
     * @see GeodeticAuthorityFactory#createImageDatum(String)
     */
    @Override
    public ImageDatum createImageDatum(final Map<String,?> properties,
            final PixelInCell pixelInCell) throws FactoryException
    {
        final DefaultImageDatum datum;
        try {
            datum = new DefaultImageDatum(complete(properties), pixelInCell);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createImageDatum", datum);
    }

    /**
     * Creates a two-dimensional affine coordinate system from the given pair of axis.
     * This coordinate system can be used with image and engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultAffineCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis.
     * @param  axis1 The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultAffineCS#DefaultAffineCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    @Override
    public AffineCS createAffineCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final DefaultAffineCS cs;
        try {
            cs = new DefaultAffineCS(complete(properties), axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createAffineCS", cs);
    }

    /**
     * Creates a engineering coordinate reference system.
     * Engineering CRS can be divided into two broad categories:
     *
     * <ul>
     *   <li>earth-fixed systems applied to engineering activities on or near the surface of the earth;</li>
     *   <li>CRSs on moving platforms such as road vehicles, vessels, aircraft, or spacecraft.</li>
     * </ul>
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>A {@code createFooCS(…)} method for Cartesian, spherical, linear, affine, polar, cylindrical or user-defined CS.</li>
     *   <li>{@link #createEngineeringDatum(Map)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultEngineeringCRS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  datum      The engineering datum to use in created CRS.
     * @param  cs         The coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEngineeringCRS#DefaultEngineeringCRS(Map, EngineeringDatum, CoordinateSystem)
     * @see GeodeticAuthorityFactory#createEngineeringCRS(String)
     */
    @Override
    public EngineeringCRS createEngineeringCRS(final Map<String,?> properties,
            final EngineeringDatum datum, final CoordinateSystem cs) throws FactoryException
    {
        final DefaultEngineeringCRS crs;
        try {
            crs = new DefaultEngineeringCRS(complete(properties), datum, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createEngineeringCRS", crs);
    }

    /**
     * Creates an engineering datum.
     * The default implementation creates a {@link DefaultEngineeringDatum} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEngineeringDatum#DefaultEngineeringDatum(Map)
     * @see GeodeticAuthorityFactory#createEngineeringDatum(String)
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final Map<String,?> properties)
            throws FactoryException
    {
        final DefaultEngineeringDatum datum;
        try {
            datum = new DefaultEngineeringDatum(complete(properties));
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createEngineeringDatum", datum);
    }

    /**
     * Creates a three-dimensional affine coordinate system from the given set of axis.
     * This coordinate system can be used with engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultAffineCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis.
     * @param  axis1 The second axis.
     * @param  axis2 The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultAffineCS#DefaultAffineCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    @Override
    public AffineCS createAffineCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final DefaultAffineCS cs;
        try {
            cs = new DefaultAffineCS(complete(properties), axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createAffineCS", cs);
    }

    /**
     * Creates a cylindrical coordinate system from the given set of axis.
     * This coordinate system can be used with engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultCylindricalCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis.
     * @param  axis1 The second axis.
     * @param  axis2 The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCylindricalCS#DefaultCylindricalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createCylindricalCS(String)
     */
    @Override
    public CylindricalCS createCylindricalCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final DefaultCylindricalCS cs;
        try {
            cs = new DefaultCylindricalCS(complete(properties), axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createCylindricalCS", cs);
    }

    /**
     * Creates a polar coordinate system from the given pair of axis.
     * This coordinate system can be used with engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultPolarCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis.
     * @param  axis1 The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultPolarCS#DefaultPolarCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createPolarCS(String)
     */
    @Override
    public PolarCS createPolarCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final DefaultPolarCS cs;
        try {
            cs = new DefaultPolarCS(complete(properties), axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createPolarCS", cs);
    }

    /**
     * Creates a linear coordinate system.
     * This coordinate system can be used with engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultLinearCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis The single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultLinearCS#DefaultLinearCS(Map, CoordinateSystemAxis)
     */
    @Override
    public LinearCS createLinearCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        final DefaultLinearCS cs;
        try {
            cs = new DefaultLinearCS(complete(properties), axis);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createLinearCS", cs);
    }

    /**
     * Creates a two-dimensional user defined coordinate system from the given pair of axis.
     * This coordinate system can be used with engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultUserDefinedCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis.
     * @param  axis1 The second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultUserDefinedCS#DefaultUserDefinedCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    @Override
    public UserDefinedCS createUserDefinedCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        final DefaultUserDefinedCS cs;
        try {
            cs = new DefaultUserDefinedCS(complete(properties), axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createUserDefinedCS", cs);
    }

    /**
     * Creates a three-dimensional user defined coordinate system from the given set of axis.
     * This coordinate system can be used with engineering CRS.
     *
     * <div class="note"><b>Dependencies:</b>
     * the components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol></div>
     *
     * The default implementation creates a {@link DefaultUserDefinedCS} instance.
     *
     * @param  properties Name and other properties to give to the new object.
     * @param  axis0 The first  axis.
     * @param  axis1 The second axis.
     * @param  axis2 The third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultUserDefinedCS#DefaultUserDefinedCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    @Override
    public UserDefinedCS createUserDefinedCS(final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        final DefaultUserDefinedCS cs;
        try {
            cs = new DefaultUserDefinedCS(complete(properties), axis0, axis1, axis2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createUserDefinedCS", cs);
    }

    /**
     * Creates a coordinate system axis from an abbreviation and a unit.
     * Note that the axis name is constrained by ISO 19111 depending on the coordinate reference system type.
     * See the GeoAPI {@link CoordinateSystemAxis} javadoc for more information.
     *
     * <p>The default implementation creates a {@link DefaultCoordinateSystemAxis} instance.</p>
     *
     * @param  properties   Name and other properties to give to the new object.
     * @param  abbreviation The coordinate axis abbreviation.
     * @param  direction    The axis direction.
     * @param  unit         The coordinate axis unit.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCoordinateSystemAxis#DefaultCoordinateSystemAxis(Map, String, AxisDirection, Unit)
     * @see GeodeticAuthorityFactory#createCoordinateSystemAxis(String)
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final Map<String,?> properties,
            final String abbreviation, final AxisDirection direction,
            final Unit<?> unit) throws FactoryException
    {
        final DefaultCoordinateSystemAxis axis;
        try {
            axis = new DefaultCoordinateSystemAxis(complete(properties), abbreviation, direction, unit);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createCoordinateSystemAxis", axis);
    }

    /**
     * Creates a coordinate reference system object from a XML string.
     * Note that the given argument is the XML document itself,
     * <strong>not</strong> a URL to a XML document.
     *
     * <p>The default implementation delegates to {@link XML#unmarshal(String)}</p>
     *
     * @param  xml Coordinate reference system encoded in XML format.
     * @throws FactoryException if the object creation failed.
     *
     * @see XML#unmarshal(String)
     * @see org.apache.sis.referencing.CRS#fromXML(String)
     */
    @Override
    public CoordinateReferenceSystem createFromXML(final String xml) throws FactoryException {
        final Object object;
        try {
            object = XML.unmarshal(xml);
        } catch (JAXBException e) {
            throw new FactoryException(e.getLocalizedMessage(), e);
        }
        if (object instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) object;
        } else {
            throw new FactoryException(Errors.getResources(defaultProperties).getString(
                    Errors.Keys.IllegalClass_2, CoordinateReferenceSystem.class, object.getClass()));
        }
    }

    /**
     * Creates a Coordinate Reference System object from a <cite>Well Known Text</cite> (WKT).
     * This method understands both version 1 (a.k.a. OGC 01-009) and version 2 (a.k.a. ISO 19162)
     * of the WKT format.
     *
     * <div class="note"><b>Example:</b> below is a slightly simplified WKT 2 string for a Mercator projection.
     * For making this example smaller, some optional {@code UNIT[…]} and {@code ORDER[…]} elements have been omitted.
     *
     * {@preformat wkt
     *   ProjectedCRS["SIRGAS 2000 / Brazil Mercator",
     *     BaseGeodCRS["SIRGAS 2000",
     *       Datum["Sistema de Referencia Geocentrico para las Americas 2000",
     *         Ellipsoid["GRS 1980", 6378137, 298.257222101]]],
     *     Conversion["Petrobras Mercator",
     *       Method["Mercator (variant B)", Id["EPSG",9805]],
     *       Parameter["Latitude of 1st standard parallel", -2],
     *       Parameter["Longitude of natural origin", -43],
     *       Parameter["False easting", 5000000],
     *       Parameter["False northing", 10000000]],
     *     CS[cartesian,2],
     *       Axis["easting (E)", east],
     *       Axis["northing (N)", north],
     *       LengthUnit["metre", 1],
     *     Id["EPSG",5641]]
     * }
     * </div>
     *
     * <div class="section">Usage and performance considerations</div>
     * The default implementation uses a shared instance of {@link org.apache.sis.io.wkt.WKTFormat}
     * with the addition of thread-safety. This is okay for occasional use,
     * but is sub-optimal if this method is extensively used in a multi-thread environment.
     * Furthermore this method offers no control on the WKT {@linkplain org.apache.sis.io.wkt.Convention conventions}
     * in use and on the handling of {@linkplain org.apache.sis.io.wkt.Warnings warnings}.
     * Applications which need to parse a large amount of WKT strings should consider to use
     * the {@link org.apache.sis.io.wkt.WKTFormat} class instead than this method.
     *
     * @param  text Coordinate system encoded in Well-Known Text format (version 1 or 2).
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.io.wkt
     * @see org.apache.sis.referencing.CRS#fromWKT(String)
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
     * @see <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Legacy WKT 1</a>
     */
    @Override
    public CoordinateReferenceSystem createFromWKT(final String text) throws FactoryException {
        Parser p = parser.getAndSet(null);
        if (p == null) try {
            Constructor<? extends Parser> c = parserConstructor;
            if (c == null) {
                c = Class.forName("org.apache.sis.io.wkt.GeodeticObjectParser").asSubclass(Parser.class)
                         .getConstructor(Map.class, ObjectFactory.class, MathTransformFactory.class);
                final Constructor<?> cp = c;     // For allowing use in inner class or lambda expression.
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override public Void run() {
                        cp.setAccessible(true);
                        return null;
                    }
                });
                parserConstructor = c;
            }
            p = c.newInstance(defaultProperties, this, getMathTransformFactory());
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
            throw new FactoryException(e);
        }
        final Object object;
        try {
            object = p.createFromWKT(text);
        } catch (FactoryException e) {
            /*
             * In the case of map projection, the parsing may fail because a projection parameter is not known to SIS.
             * If this happen, replace the generic exception thrown be the parser (which is FactoryException) by a
             * more specific one. Note that InvalidGeodeticParameterException is defined only in this sis-referencing
             * module, so we could not throw it from the sis-metadata module that contain the parser.
             */
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ParameterNotFoundException) {
                    throw new InvalidGeodeticParameterException(e.getMessage(), cause);     // More accurate exception.
                }
                cause = cause.getCause();
            }
            throw e;
        }
        parser.set(p);
        if (object instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) object;
        } else {
            throw new FactoryException(Errors.getResources(defaultProperties).getString(
                    Errors.Keys.IllegalClass_2, CoordinateReferenceSystem.class, object.getClass()));
        }
    }
}
