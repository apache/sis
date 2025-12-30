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
import java.util.Objects;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.concurrent.atomic.AtomicReference;
import java.time.temporal.Temporal;
import java.lang.reflect.Constructor;
import jakarta.xml.bind.JAXBException;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.*;
import org.apache.sis.referencing.crs.*;
import org.apache.sis.referencing.datum.*;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.internal.MergedProperties;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.io.wkt.Parser;
import org.apache.sis.xml.XML;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.referencing.legacy.DefaultImageCRS;
import org.apache.sis.referencing.legacy.DefaultImageDatum;
import org.apache.sis.referencing.legacy.DefaultUserDefinedCS;

// Specific to the main branch:
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;


/**
 * Creates Coordinate Reference System (CRS) implementations, with their Coordinate System (CS) and Datum components.
 * This factory serves two purposes:
 *
 * <ul>
 *   <li><b>For users</b>, allows the creation of complex objects that cannot be created by the authority factories,
 *       without explicit dependency to Apache SIS (when using the GeoAPI interfaces implemented by this class).</li>
 *   <li><b>For providers</b>, allows <i>inversion of control</i> by overriding methods in this class,
 *       then specifying the customized instance to other services that consume {@code CRSFactory} (for example
 *       authority factories or {@linkplain org.apache.sis.io.wkt.WKTFormat WKT parsers}).</li>
 * </ul>
 *
 * This {@code GeodeticObjectFactory} class is not easy to use directly.
 * Users are encouraged to use an authority factory instead
 * (or the {@link org.apache.sis.referencing.CRS#forCode(String)} convenience method)
 * when the CRS object to construct can be identified by a code in the namespace of an authority (typically EPSG).
 *
 * <h2>Object properties</h2>
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
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
 *     <td>{@link Identifier} or {@link String}</td>
 *     <td>{@link AbstractIdentifiedObject#getName()}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.metadata.Identifier#AUTHORITY_KEY}</td>
 *     <td>{@link String} or {@link org.opengis.metadata.citation.Citation}</td>
 *     <td>{@link NamedIdentifier#getAuthority()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.metadata.Identifier#CODE_KEY}</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getCode()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr><tr>
 *     <td>"codespace"</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getCodeSpace()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr><tr>
 *     <td>"version"</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getVersion()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr><tr>
 *     <td>"description"</td>
 *     <td>{@link String}</td>
 *     <td>{@link NamedIdentifier#getDescription()} on the {@linkplain AbstractIdentifiedObject#getName() name}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
 *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
 *     <td>{@link AbstractIdentifiedObject#getAlias()}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
 *     <td>{@link Identifier} (optionally as array)</td>
 *     <td>{@link AbstractIdentifiedObject#getIdentifiers()}</td>
 *   </tr><tr>
 *     <td>"domains"</td>
 *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
 *     <td>{@link AbstractIdentifiedObject#getDomains()}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
 *     <td>{@link Extent}</td>
 *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
 *     <td>{@link String} or {@link InternationalString}</td>
 *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getScope()}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractDatum#getAnchorDefinition()}</td>
 *   </tr><tr>
 *     <td>{@value "anchorEpoch"}</td>
 *     <td>{@link java.time.temporal.Temporal}</td>
 *     <td>{@link AbstractDatum#getAnchorEpoch()}</td>
 *   </tr><tr>
 *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractIdentifiedObject#getRemarks()}</td>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#DEPRECATED_KEY}</td>
 *     <td>{@link Boolean}</td>
 *     <td>{@link AbstractIdentifiedObject#isDeprecated()}</td>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
 *     <td>{@link Locale}</td>
 *     <td>(none)</td>
 *   </tr>
 * </table>
 *
 * <h2>Localization</h2>
 * All localizable attributes like {@code "remarks"} may have a language and country code suffix.
 * For example, the {@code "remarks_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
 * the {@code "remarks_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
 * They are convenience properties for building the {@code InternationalString} value.
 *
 * <p>The {@code "locale"} property applies only in case of exception for formatting the error message, and
 * is used only on a <em>best effort</em> basis. The locale is discarded after successful construction
 * since localizations are applied by the {@link InternationalString#toString(Locale)} method.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   0.6
 */
public class GeodeticObjectFactory extends AbstractFactory implements CRSFactory, CSFactory, DatumFactory, Parser {
    /**
     * The logger to use for reporting object creations.
     */
    private static final Logger LOGGER = Logger.getLogger(Loggers.CRS_FACTORY);

    /**
     * The constructor for WKT parsers, fetched when first needed. The WKT parser is defined in the
     * same module as this class, so we will hopefully not have security issues.  But we have to
     * use reflection because the parser class is not yet public (because we do not want to commit
     * its API yet).
     */
    private static volatile Constructor<? extends Parser> parserConstructor;

    /**
     * The default properties, or an empty map if none. This map shall not change after construction in
     * order to allow usage without synchronization in multi-thread context. But we do not need to wrap
     * in an unmodifiable map since {@code GeodeticObjectFactory} does not provide public access to it.
     */
    private final Map<String,?> defaultProperties;

    /**
     * Weak references to existing objects (CRS, CS, Datum, Ellipsoid or PrimeMeridian).
     * This set is used in order to return a pre-existing object instead of creating a new one.
     */
    private final WeakHashSet<AbstractIdentifiedObject> pool;

    /**
     * The <i>Well Known Text</i> parser for {@code CoordinateReferenceSystem} instances.
     * This parser is not thread-safe, so we need to prevent two threads from using the same instance at the same time.
     */
    private final AtomicReference<Parser> parser;

    /**
     * The default factory instance.
     */
    private static final GeodeticObjectFactory INSTANCE = new GeodeticObjectFactory();

    /**
     * Returns the default provider of {@code IdentifiedObject} instances.
     * This is the factory used by the Apache SIS library when no non-null
     * {@link CRSFactory}, {@link CSFactory} or {@link DatumFactory} has been explicitly specified.
     * This method can be invoked directly, or indirectly through
     * {@code ServiceLoader.load(T)} where <var>T</var> is one of above-cited interfaces.
     *
     * @return the default provider of geodetic objects.
     *
     * @see java.util.ServiceLoader
     * @since 1.4
     */
    public static GeodeticObjectFactory provider() {
        return INSTANCE;
    }

    /**
     * Constructs a factory with no default properties.
     *
     * @see #provider()
     */
    public GeodeticObjectFactory() {
        this(null);
    }

    /**
     * Constructs a factory with the given default properties.
     * {@code GeodeticObjectFactory} will fallback on the map given to this constructor for any property
     * not present in the map provided to a {@code createFoo(Map<String,?>, …)} method.
     *
     * @param  properties  the default properties, or {@code null} if none.
     */
    public GeodeticObjectFactory(final Map<String,?> properties) {
        defaultProperties = (properties != null) ? Map.copyOf(properties) : Map.of();
        pool = new WeakHashSet<>(AbstractIdentifiedObject.class);
        parser = new AtomicReference<>();
    }

    /**
     * Returns the union of the given {@code properties} map with the default properties given at
     * {@linkplain #GeodeticObjectFactory(Map) construction time}. Entries in the given properties
     * map have precedence, even if their {@linkplain java.util.Map.Entry#getValue() value} is {@code null}
     * (i.e. a null value "erase" the default property value).
     * Entries with null value after the union will be omitted.
     *
     * <p>This method is invoked by all {@code createFoo(Map<String,?>, …)} methods. Subclasses can
     * override this method if they want to add, remove or edit property values with more flexibility
     * than {@linkplain #GeodeticObjectFactory(Map) constant values specified at construction time}.</p>
     *
     * @param  properties  the properties supplied in a call to a {@code createFoo(Map, …)} method.
     * @return the union of the given properties with the default properties.
     */
    protected Map<String,?> complete(final Map<String,?> properties) {
        return new MergedProperties(Objects.requireNonNull(properties), defaultProperties) {
            /**
             * Returns the value for an "invisible" entry providing the math transform factory to use.
             * The enclosing factory handles the {@code "mtFactory"} differently than other properties
             * because a math transform factory is normally not needed for {@link GeodeticObjectFactory}.
             * However, an exception exists when creating the SIS implementation of derived or projected
             * CRS, because of the way we implemented derived CRS. But this oddity is specific to SIS.
             */
            @Override
            protected Object invisibleEntry(final Object key) {
                if (ReferencingFactoryContainer.MT_FACTORY.equals(key)) {
                    return DefaultMathTransformFactory.provider();
                } else {
                    return super.invisibleEntry(key);
                }
            }
        };
    }

    /**
     * Returns a unique instance of the given object. If this method recycles an existing object,
     * then the existing instance is returned silently. Otherwise this method logs a message at
     * {@link Level#FINE} or {@link Level#FINER} telling that a new object has been created.
     * The finer level is used if the object has been creating during an operation that creates
     * a lot of candidates in search for a CRS matching some criterion.
     */
    private <T extends AbstractIdentifiedObject> T unique(final String caller, final T object) {
        final T c = pool.unique(object);
        if (c == object) {
            final Level level = Semaphores.FINER_LOG_LEVEL_FOR_OBJECTS_CREATION.getLogLevel(Level.FINE);
            if (LOGGER.isLoggable(level)) {
                final String id = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(c, null));
                final LogRecord record = Messages.forLocale(null).createLogRecord(level,
                        (id != null) ? Messages.Keys.CreatedIdentifiedObject_3
                                     : Messages.Keys.CreatedNamedObject_2,
                        c.getInterface(), c.getName().getCode(), id);

                Logging.completeAndLog(LOGGER, GeodeticObjectFactory.class, caller, record);
            }
        }
        return c;
    }

    /**
     * Creates a geocentric coordinate reference system from a Cartesian coordinate system.
     * Geocentric CRS have their origin at the approximate centre of mass of the earth.
     * An {@linkplain #createGeocentricCRS(Map, GeodeticDatum, SphericalCS) alternate method} allows creation of the
     * same kind of CRS with spherical coordinate system instead of a Cartesian one.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed to the
     * {@link GeodeticCRS} parent interface. This is because ISO 19111 does not defines specific interface
     * for the geocentric case. Users should assign the return value to a {@code GeodeticCRS} type.</div>
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     *   <li>{@link #createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}</li>
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultGeocentricCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       geodetic reference frame, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the three-dimensional Cartesian coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeocentricCRS#DefaultGeocentricCRS(Map, GeodeticDatum, DefaultDatumEnsemble, CartesianCS)
     * @see GeodeticAuthorityFactory#createGeodeticCRS(String)
     *
     * @since 1.5
     */
    public GeodeticCRS createGeodeticCRS(final Map<String,?> properties,
                                         final GeodeticDatum datum,
                                         final DefaultDatumEnsemble<GeodeticDatum> ensemble,
                                         final CartesianCS cs)
            throws FactoryException
    {
        final DefaultGeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeodeticCRS", crs);
    }

    /**
     * Creates a geocentric coordinate reference system from a Cartesian coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the geodetic datum to use in created CRS.
     * @param  cs          the three-dimensional Cartesian coordinate system for the created CRS.
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
            crs = new DefaultGeocentricCRS(complete(properties), datum, null, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeocentricCRS", crs);
    }

    /**
     * Creates a three-dimensional Cartesian coordinate system from the given set of axis.
     * This coordinate system can be used with geocentric, engineering and derived CRS.
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultCartesianCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis (e.g. “Geocentric X”).
     * @param  axis1       the second axis (e.g. “Geocentric Y”).
     * @param  axis2       the third  axis (e.g. “Geocentric Z”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCartesianCS#DefaultCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createCartesianCS(String)
     */
    @Override
    public CartesianCS createCartesianCS(final Map<String,?> properties,
                                         final CoordinateSystemAxis axis0,
                                         final CoordinateSystemAxis axis1,
                                         final CoordinateSystemAxis axis2)
            throws FactoryException
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
     * Creates a geocentric coordinate reference system from a spherical coordinate system.
     * Geocentric CRS have their origin at the approximate centre of mass of the earth.
     * An {@linkplain #createGeocentricCRS(Map, GeodeticDatum, CartesianCS) alternate method} allows creation of the
     * same kind of CRS with Cartesian coordinate system instead of a spherical one.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed to the
     * {@link GeodeticCRS} parent interface. This is because ISO 19111 does not defines specific interface
     * for the geocentric case. Users should assign the return value to a {@code GeodeticCRS} type.</div>
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createSphericalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)}</li>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     *   <li>{@link #createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}</li>
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultGeocentricCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       geodetic reference frame, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the spherical coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeocentricCRS#DefaultGeocentricCRS(Map, GeodeticDatum, DefaultDatumEnsemble, SphericalCS)
     * @see GeodeticAuthorityFactory#createGeodeticCRS(String)
     *
     * @since 1.5
     */
    public GeodeticCRS createGeodeticCRS(final Map<String,?> properties,
                                         final GeodeticDatum datum,
                                         final DefaultDatumEnsemble<GeodeticDatum> ensemble,
                                         final SphericalCS cs)
            throws FactoryException
    {
        final DefaultGeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeodeticCRS", crs);
    }

    /**
     * Creates a geocentric coordinate reference system from a spherical coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the geodetic datum to use in created CRS.
     * @param  cs          the three-dimensional Cartesian coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated ISO 19111:2019 does not define an explicit class for geocentric CRS.
     * Use {@link #createGeodeticCRS(Map, GeodeticDatum, SphericalCS)} instead.
     */
    @Override
    @Deprecated
    public GeocentricCRS createGeocentricCRS(final Map<String,?> properties,
            final GeodeticDatum datum, final SphericalCS cs) throws FactoryException
    {
        final DefaultGeocentricCRS crs;
        try {
            crs = new DefaultGeocentricCRS(complete(properties), datum, null, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeocentricCRS", crs);
    }

    /**
     * Creates a spherical coordinate system from the given set of axis.
     * This coordinate system can be used with geocentric, engineering and derived CRS.
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultSphericalCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis (e.g. “Spherical latitude”).
     * @param  axis1       the second axis (e.g. “Spherical longitude”).
     * @param  axis2       the third  axis (e.g. “Geocentric radius”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultSphericalCS#DefaultSphericalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createSphericalCS(String)
     */
    @Override
    public SphericalCS createSphericalCS(final Map<String,?> properties,
                                         final CoordinateSystemAxis axis0,
                                         final CoordinateSystemAxis axis1,
                                         final CoordinateSystemAxis axis2)
            throws FactoryException
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
     * Creates a spherical coordinate system without radius.
     * This coordinate system can be used with geocentric, engineering and derived CRS.
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultSphericalCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis (e.g. “Spherical latitude”).
     * @param  axis1       the second axis (e.g. “Spherical longitude”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultSphericalCS#DefaultSphericalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     *
     * @since 1.4
     */
    public SphericalCS createSphericalCS(final Map<String,?> properties,
                                         final CoordinateSystemAxis axis0,
                                         final CoordinateSystemAxis axis1)
            throws FactoryException
    {
        final DefaultSphericalCS cs;
        try {
            cs = new DefaultSphericalCS(complete(properties), axis0, axis1);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createSphericalCS", cs);
    }

    /**
     * Creates a geographic coordinate reference system. It can be (<var>latitude</var>, <var>longitude</var>)
     * or (<var>longitude</var>, <var>latitude</var>), optionally with an ellipsoidal height.
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
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
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultGeographicCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       geodetic reference frame, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the two- or three-dimensional ellipsoidal coordinate system for the created <abbr>CRS</abbr>.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeographicCRS#DefaultGeographicCRS(Map, GeodeticDatum, DefaultDatumEnsemble, EllipsoidalCS)
     * @see GeodeticAuthorityFactory#createGeographicCRS(String)
     *
     * @since 1.5
     */
    public GeographicCRS createGeographicCRS(final Map<String,?> properties,
                                             final GeodeticDatum datum,
                                             final DefaultDatumEnsemble<GeodeticDatum> ensemble,
                                             final EllipsoidalCS cs)
            throws FactoryException
    {
        final DefaultGeographicCRS crs;
        try {
            crs = new DefaultGeographicCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createGeographicCRS", crs);
    }

    /**
     * Creates a geographic <abbr>CRS</abbr> with a datum that may be a datum ensemble.
     * If the given {@code datum} argument is a {@linkplain DefaultDatumEnsemble datum ensemble
     * viewed as a pseudo-datum}, then it is used as the {@code ensemble} argument of the above
     * constructor.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the geodetic reference frame or datum ensemble viewed as a pseudo-datum.
     * @param  cs          the two- or three-dimensional ellipsoidal coordinate system for the created <abbr>CRS</abbr>.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeographicCRS createGeographicCRS(Map<String,?> properties, GeodeticDatum datum, EllipsoidalCS cs)
            throws FactoryException
    {
        DefaultDatumEnsemble<GeodeticDatum> ensemble = DatumOrEnsemble.asEnsemble(datum).orElse(null);
        if (ensemble != null) datum = null;
        return createGeographicCRS(properties, datum, ensemble, cs);
    }

    /**
     * Creates a datum ensemble from a collection of members and an ensemble accuracy.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code DatumEnsemble} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).</div>
     *
     * @param  <D>         the type of datum contained in the ensemble.
     * @param  properties  name and other properties to give to the new object.
     * @param  members     datum or reference frames which are members of the datum ensemble.
     * @param  accuracy    inaccuracy introduced through use of the given collection of datums.
     * @return the datum ensemble for the given properties.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.5
     */
    public <D extends Datum> DefaultDatumEnsemble<D> createDatumEnsemble(final Map<String,?> properties,
                                                                  final Collection<? extends D> members,
                                                                  final PositionalAccuracy accuracy)
            throws FactoryException
    {
        final DefaultDatumEnsemble<D> ensemble;
        try {
            ensemble = DefaultDatumEnsemble.create(complete(properties), null, members, accuracy);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createDatumEnsemble", ensemble);
    }

    /**
     * Creates a static geodetic reference frame from ellipsoid and (optionally) Bursa-Wolf parameters.
     * Geodetic reference frame defines the location and orientation of an ellipsoid that approximates the shape of the earth.
     * This datum can be used with geographic and geocentric <abbr>CRS</abbr>.
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>One of:<ul>
     *     <li>{@link #createEllipsoid(Map, double, double, Unit)}</li>
     *     <li>{@link #createFlattenedSphere(Map, double, double, Unit)}</li>
     *   </ul></li>
     *   <li>{@link #createPrimeMeridian(Map, double, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultGeodeticDatum} instance.
     *
     * @param  properties     name and other properties to give to the new object.
     * @param  ellipsoid      the ellipsoid to use in new geodetic reference frame.
     * @param  primeMeridian  the prime meridian to use in new geodetic reference frame.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeodeticDatum#DefaultGeodeticDatum(Map, Ellipsoid, PrimeMeridian)
     * @see GeodeticAuthorityFactory#createGeodeticDatum(String)
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final Map<String,?> properties,
                                             final Ellipsoid     ellipsoid,
                                             final PrimeMeridian primeMeridian)
            throws FactoryException
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
     * Creates a dynamic geodetic reference frame from ellipsoid and frame reference epoch.
     * The arguments are the same as for the {@linkplain #createGeodeticDatum(Map, Ellipsoid,
     * PrimeMeridian) static datum}, with the addition of a mandatory frame reference epoch.
     *
     * @param  properties     name and other properties to give to the new object.
     * @param  ellipsoid      the ellipsoid to use in new geodetic reference frame.
     * @param  primeMeridian  the prime meridian to use in new geodetic reference frame.
     * @param  epoch          the epoch to which the definition of the dynamic reference frame is referenced.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultGeodeticDatum.Dynamic#Dynamic(Map, Ellipsoid, PrimeMeridian, Temporal)
     * @see GeodeticAuthorityFactory#createGeodeticDatum(String)
     *
     * @since 1.5
     */
    public GeodeticDatum createGeodeticDatum(final Map<String,?> properties,
                                             final Ellipsoid     ellipsoid,
                                             final PrimeMeridian primeMeridian,
                                             final Temporal      epoch)
            throws FactoryException
    {
        final DefaultGeodeticDatum datum;
        try {
            datum = new DefaultGeodeticDatum.Dynamic(complete(properties), ellipsoid, primeMeridian, epoch);
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
     * @param  properties   name and other properties to give to the new object.
     * @param  longitude    the longitude of prime meridian in supplied angular units East of Greenwich.
     * @param  angularUnit  the angular units of longitude.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultPrimeMeridian#DefaultPrimeMeridian(Map, double, Unit)
     * @see GeodeticAuthorityFactory#createPrimeMeridian(String)
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final Map<String,?> properties,
                                             final double        longitude,
                                             final Unit<Angle>   angularUnit)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultEllipsoidalCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis (e.g. “Geodetic latitude”).
     * @param  axis1       the second axis (e.g. “Geodetic longitude”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoidalCS#DefaultEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createEllipsoidalCS(String)
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final Map<String,?> properties,
                                             final CoordinateSystemAxis axis0,
                                             final CoordinateSystemAxis axis1)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultEllipsoidalCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis (e.g. “Geodetic latitude”).
     * @param  axis1       the second axis (e.g. “Geodetic longitude”).
     * @param  axis2       the third  axis (e.g. “Ellipsoidal height”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoidalCS#DefaultEllipsoidalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createEllipsoidalCS(String)
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final Map<String,?> properties,
                                             final CoordinateSystemAxis axis0,
                                             final CoordinateSystemAxis axis1,
                                             final CoordinateSystemAxis axis2)
            throws FactoryException
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
     * @param  properties     name and other properties to give to the new object.
     * @param  semiMajorAxis  the equatorial radius in supplied linear units.
     * @param  semiMinorAxis  the polar radius in supplied linear units.
     * @param  unit           the linear units of ellipsoid axes.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoid#createEllipsoid(Map, double, double, Unit)
     * @see GeodeticAuthorityFactory#createEllipsoid(String)
     */
    @Override
    public Ellipsoid createEllipsoid(final Map<String,?> properties,
                                     final double semiMajorAxis,
                                     final double semiMinorAxis,
                                     final Unit<Length> unit)
            throws FactoryException
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
     * @param  properties         name and other properties to give to the new object.
     * @param  semiMajorAxis      the equatorial radius in supplied linear units.
     * @param  inverseFlattening  the eccentricity of ellipsoid.
     * @param  unit               the linear units of major axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEllipsoid#createFlattenedSphere(Map, double, double, Unit)
     * @see GeodeticAuthorityFactory#createEllipsoid(String)
     */
    @Override
    public Ellipsoid createFlattenedSphere(final Map<String,?> properties,
                                           final double semiMajorAxis,
                                           final double inverseFlattening,
                                           final Unit<Length> unit)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
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
     * </ol>
     *
     * The supplied {@code conversion} argument shall <strong>not</strong> includes the operation steps
     * for performing {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes unit
     * conversions and change of axis order} since those operations will be inferred by this constructor.
     *
     * <p>The default implementation creates a {@link DefaultProjectedCRS} instance.</p>
     *
     * @param  properties     name and other properties to give to the new object.
     * @param  baseCRS        the geographic coordinate reference system to base projection on.
     * @param  baseToDerived  the defining conversion from a {@linkplain AxesConvention#NORMALIZED normalized} base to a normalized derived CRS.
     * @param  derivedCS      the coordinate system for the projected CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultProjectedCRS#DefaultProjectedCRS(Map, GeographicCRS, Conversion, CartesianCS)
     * @see GeodeticAuthorityFactory#createProjectedCRS(String)
     */
    @Override
    public ProjectedCRS createProjectedCRS(final Map<String,?> properties,
                                           final GeographicCRS baseCRS,
                                           final Conversion    baseToDerived,
                                           final CartesianCS   derivedCS)
            throws FactoryException
    {
        final DefaultProjectedCRS crs;
        try {
            crs = new DefaultProjectedCRS(complete(properties), baseCRS, baseToDerived, derivedCS);
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultCartesianCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis (e.g. “Easting”).
     * @param  axis1       the second axis (e.g. “Northing”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCartesianCS#DefaultCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createCartesianCS(String)
     */
    @Override
    public CartesianCS createCartesianCS(final Map<String,?> properties,
                                         final CoordinateSystemAxis axis0,
                                         final CoordinateSystemAxis axis1)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>A {@code createFooCS(…)} method for Cartesian, spherical, ellipsoidal, vertical, temporal, linear, affine, polar, cylindrical or user-defined CS.</li>
     *   <li>Another {@code createFooCRS(…)} method for geocentric, geographic, vertical, temporal or engineering CRS.</li>
     *   <li>{@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#createDefiningConversion(Map, OperationMethod, ParameterValueGroup)}</li>
     * </ol>
     *
     * The supplied {@code conversion} argument shall <strong>not</strong> includes the operation steps
     * for performing {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes unit
     * conversions and change of axis order} since those operations will be inferred by this constructor.
     *
     * <p>The default implementation creates a {@link DefaultDerivedCRS} instance.</p>
     *
     * @param  properties     name and other properties to give to the new object.
     * @param  baseCRS        the coordinate reference system to base projection on. Shall be an instance of {@link SingleCRS}.
     * @param  baseToDerived  the defining conversion from a {@linkplain AxesConvention#NORMALIZED normalized} base to a normalized derived CRS.
     * @param  derivedCS      the coordinate system for the derived CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultDerivedCRS#create(Map, SingleCRS, Conversion, CoordinateSystem)
     * @see GeodeticAuthorityFactory#createDerivedCRS(String)
     */
    @Override
    public DerivedCRS createDerivedCRS(final Map<String,?> properties,
                                       final CoordinateReferenceSystem baseCRS,
                                       final Conversion baseToDerived,
                                       final CoordinateSystem derivedCS)
            throws FactoryException
    {
        ArgumentChecks.ensureCanCast("baseCRS", SingleCRS.class, baseCRS);
        final DefaultDerivedCRS crs;
        try {
            crs = DefaultDerivedCRS.create(complete(properties), (SingleCRS) baseCRS, baseToDerived, derivedCS);
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createVerticalCS(Map, CoordinateSystemAxis)}</li>
     *   <li>{@link #createVerticalDatum(Map, VerticalDatumType)}</li>
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultVerticalCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       vertical reference frame, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the vertical coordinate system for the created <abbr>CRS</abbr>.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalCRS#DefaultVerticalCRS(Map, VerticalDatum, DefaultDatumEnsemble, VerticalCS)
     * @see GeodeticAuthorityFactory#createVerticalCRS(String)
     *
     * @since 1.5
     */
    public VerticalCRS createVerticalCRS(final Map<String,?> properties,
                                         final VerticalDatum datum,
                                         final DefaultDatumEnsemble<VerticalDatum> ensemble,
                                         final VerticalCS cs)
            throws FactoryException
    {
        final DefaultVerticalCRS crs;
        try {
            crs = new DefaultVerticalCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createVerticalCRS", crs);
    }

    /**
     * Creates a vertical <abbr>CRS</abbr> with a datum that may be a datum ensemble.
     * If the given {@code datum} argument is a {@linkplain DefaultDatumEnsemble datum ensemble
     * viewed as a pseudo-datum}, then it is used as the {@code ensemble} argument of the above
     * constructor.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the vertical reference frame or datum ensemble viewed as a pseudo-datum.
     * @param  cs          the vertical coordinate system for the created <abbr>CRS</abbr>.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCRS createVerticalCRS(Map<String,?> properties, VerticalDatum datum, VerticalCS cs)
            throws FactoryException
    {
        DefaultDatumEnsemble<VerticalDatum> ensemble = DatumOrEnsemble.asEnsemble(datum).orElse(null);
        if (ensemble != null) datum = null;
        return createVerticalCRS(properties, datum, ensemble, cs);
    }

    /**
     * Creates a vertical datum from an enumerated type value.
     * The default implementation creates a {@link DefaultVerticalDatum} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  type        the type of this vertical datum (often geoidal).
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
     * Creates a dynamic vertical datum from an enumerated type value and a frame reference epoch.
     * The arguments are the same as for the {@linkplain #createVerticalDatum(Map, VerticalDatumType)
     * static datum}, with the addition of a mandatory frame reference epoch.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  type        the type of this vertical datum (often geoidal).
     * @param  epoch       the epoch to which the definition of the dynamic reference frame is referenced.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalDatum.Dynamic#Dynamic(Map, VerticalDatumType, Temporal)
     * @see GeodeticAuthorityFactory#createVerticalDatum(String)
     *
     * @since 1.5
     */
    public VerticalDatum createVerticalDatum(final Map<String,?> properties,
                                             final VerticalDatumType type,
                                             final Temporal epoch)
            throws FactoryException
    {
        final DefaultVerticalDatum datum;
        try {
            datum = new DefaultVerticalDatum.Dynamic(complete(properties), type, epoch);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createVerticalDatum", datum);
    }

    /**
     * Creates a vertical coordinate system.
     * This coordinate system can be used with vertical and derived CRS.
     *
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultVerticalCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the single axis (e.g. “height” or “depth”).
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultVerticalCS#DefaultVerticalCS(Map, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createVerticalCS(String)
     */
    @Override
    public VerticalCS createVerticalCS(final Map<String,?> properties,
                                       final CoordinateSystemAxis axis)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createTimeCS(Map, CoordinateSystemAxis)}</li>
     *   <li>{@link #createTemporalDatum(Map, Date)}</li>
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultTemporalCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       temporal datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of datum which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the temporal coordinate system for the created <abbr>CRS</abbr>.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTemporalCRS#DefaultTemporalCRS(Map, TemporalDatum, DefaultDatumEnsemble, TimeCS)
     * @see GeodeticAuthorityFactory#createTemporalCRS(String)
     *
     * @since 1.5
     */
    public TemporalCRS createTemporalCRS(final Map<String,?> properties,
                                         final TemporalDatum datum,
                                         final DefaultDatumEnsemble<TemporalDatum> ensemble,
                                         final TimeCS cs) throws FactoryException
    {
        final DefaultTemporalCRS crs;
        try {
            crs = new DefaultTemporalCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createTemporalCRS", crs);
    }

    /**
     * Creates a temporal <abbr>CRS</abbr> with a datum that may be a datum ensemble.
     * If the given {@code datum} argument is a {@linkplain DefaultDatumEnsemble datum ensemble
     * viewed as a pseudo-datum}, then it is used as the {@code ensemble} argument of the above
     * constructor.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the temporal datum or datum ensemble viewed as a pseudo-datum.
     * @param  cs          the temporal coordinate system for the created <abbr>CRS</abbr>.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalCRS createTemporalCRS(Map<String,?> properties, TemporalDatum datum, TimeCS cs)
            throws FactoryException
    {
        DefaultDatumEnsemble<TemporalDatum> ensemble = DatumOrEnsemble.asEnsemble(datum).orElse(null);
        if (ensemble != null) datum = null;
        return createTemporalCRS(properties, datum, ensemble, cs);
    }

    /**
     * Creates a temporal datum from an enumerated type value.
     * The default implementation creates a {@link DefaultTemporalDatum} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  origin      the date and time origin of this temporal datum.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTemporalDatum#DefaultTemporalDatum(Map, Date)
     * @see GeodeticAuthorityFactory#createTemporalDatum(String)
     */
    @Override
    public TemporalDatum createTemporalDatum(final Map<String,?> properties,
                                             final Date origin)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultTimeCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultTimeCS#DefaultTimeCS(Map, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createTimeCS(String)
     */
    @Override
    public TimeCS createTimeCS(final Map<String,?> properties,
                               final CoordinateSystemAxis axis)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>{@link #createParametricCS(Map, CoordinateSystemAxis)}</li>
     *   <li>{@link #createParametricDatum(Map)}</li>
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultParametricCRS} instance.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter types may be changed to
     * {@code org.opengis.referencing.datum.ParametricDatum} and {@code org.opengis.referencing.cs.ParametricCS},
     * and the return type may be changed to {@code org.opengis.referencing.crs.ParametricCRS}.
     * Those change are pending GeoAPI revision.</div>
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       parametric datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of datum which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the parametric coordinate system for the created <abbr>CRS</abbr>.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultParametricCRS#DefaultParametricCRS(Map, DefaultParametricDatum, DefaultDatumEnsemble, ParametricCS)
     * @see GeodeticAuthorityFactory#createParametricCRS(String)
     *
     * @since 1.5
     */
    public DefaultParametricCRS createParametricCRS(final Map<String,?> properties,
                                             final DefaultParametricDatum datum,
                                             final DefaultDatumEnsemble<DefaultParametricDatum> ensemble,
                                             final DefaultParametricCS cs)
            throws FactoryException
    {
        final DefaultParametricCRS crs;
        try {
            crs = new DefaultParametricCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createParametricCRS", crs);
    }

    /**
     * Creates a parametric <abbr>CRS</abbr> from a datum.
     * This is a shortcut for the {@linkplain #createParametricCRS(Map, ParametricDatum, DefaultDatumEnsemble, ParametricCS)
     * more generic method} without datum ensemble.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the parametric datum or datum ensemble viewed as a pseudo-datum.
     * @param  cs          the parametric coordinate system for the created <abbr>CRS</abbr>.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public DefaultParametricCRS createParametricCRS(final Map<String,?> properties,
                                             final DefaultParametricDatum datum,
                                             final DefaultParametricCS cs)
            throws FactoryException
    {
        return createParametricCRS(properties, datum, null, cs);
    }

    /**
     * Creates a parametric datum.
     * The default implementation creates a {@link DefaultParametricDatum} instance.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.datum.ParametricDatum}. This change is pending GeoAPI revision.</div>
     *
     * @param  properties  name and other properties to give to the new object.
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultParametricCS} instance.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.cs.ParametricCS}. This change is pending GeoAPI revision.</div>
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultParametricCS#DefaultParametricCS(Map, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createParametricCS(String)
     */
    public DefaultParametricCS createParametricCS(final Map<String, ?> properties,
                                           final CoordinateSystemAxis axis)
            throws FactoryException
    {
        final DefaultParametricCS cs;
        try {
            cs = new DefaultParametricCS(complete(properties), axis);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createParametricCS", cs);
    }

    /**
     * Creates a compound coordinate reference system from an ordered list of CRS components.
     * Apache SIS is permissive on the order of components that can be used in a compound CRS.
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
     * @param  properties  name and other properties to give to the new object.
     * @param  components  the sequence of coordinate reference systems making the compound CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCompoundCRS#DefaultCompoundCRS(Map, CoordinateReferenceSystem...)
     * @see GeodeticAuthorityFactory#createCompoundCRS(String)
     * @see org.apache.sis.referencing.CRS#compound(CoordinateReferenceSystem...)
     */
    @Override
    public CompoundCRS createCompoundCRS(final Map<String,?> properties,
                                         final CoordinateReferenceSystem... components)
            throws FactoryException
    {
        final DefaultCompoundCRS crs;
        try {
            crs = new DefaultCompoundCRS(complete(properties), components);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createCompoundCRS", crs);
    }

    /**
     * Creates an image coordinate reference system.
     * The default implementation creates a {@link DefaultImageCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the image datum to use in created CRS.
     * @param  cs          the Cartesian or oblique Cartesian coordinate system for the created CRS.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultImageCRS#DefaultImageCRS(Map, ImageDatum, AffineCS)
     * @see GeodeticAuthorityFactory#createImageCRS(String)
     *
     * @deprecated The {@code ImageCRS} class has been removed in ISO 19111:2019.
     *             It is replaced by {@code EngineeringCRS}.
     */
    @Override
    @Deprecated(since = "1.5")
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
     *
     * @deprecated The {@code ImageDatum} class has been removed in ISO 19111:2019.
     *             It is replaced by {@code EngineeringDatum}.
     */
    @Override
    @Deprecated(since = "1.5")
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultAffineCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultAffineCS#DefaultAffineCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    @Override
    public AffineCS createAffineCS(final Map<String,?> properties,
                                   final CoordinateSystemAxis axis0,
                                   final CoordinateSystemAxis axis1)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     *   <li>A {@code createFooCS(…)} method for Cartesian, spherical, linear, affine, polar, cylindrical or user-defined CS.</li>
     *   <li>{@link #createEngineeringDatum(Map)}</li>
     *   <li>{@link #createDatumEnsemble(Map, Collection, PositionalAccuracy)} (optional)</li>
     * </ol>
     *
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The default implementation creates a {@link DefaultEngineeringCRS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       engineering datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of datum which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system for the created <abbr>CRS</abbr>.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultEngineeringCRS#DefaultEngineeringCRS(Map, EngineeringDatum, CoordinateSystem)
     * @see GeodeticAuthorityFactory#createEngineeringCRS(String)
     *
     * @since 1.5
     */
    public EngineeringCRS createEngineeringCRS(final Map<String,?> properties,
                                               final EngineeringDatum datum,
                                               final DefaultDatumEnsemble<EngineeringDatum> ensemble,
                                               final CoordinateSystem cs)
            throws FactoryException
    {
        final DefaultEngineeringCRS crs;
        try {
            crs = new DefaultEngineeringCRS(complete(properties), datum, ensemble, cs);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception);
        }
        return unique("createEngineeringCRS", crs);
    }

    /**
     * Creates a engineering <abbr>CRS</abbr> with a datum that may be a datum ensemble.
     * If the given {@code datum} argument is a {@linkplain DefaultDatumEnsemble datum ensemble
     * viewed as a pseudo-datum}, then it is used as the {@code ensemble} argument of the above
     * constructor.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       the engineering datum or datum ensemble viewed as a pseudo-datum.
     * @param  cs          the coordinate system for the created <abbr>CRS</abbr>.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringCRS createEngineeringCRS(Map<String,?> properties, EngineeringDatum datum, CoordinateSystem cs)
            throws FactoryException
    {
        DefaultDatumEnsemble<EngineeringDatum> ensemble = DatumOrEnsemble.asEnsemble(datum).orElse(null);
        if (ensemble != null) datum = null;
        return createEngineeringCRS(properties, datum, ensemble, cs);
    }

    /**
     * Creates an engineering datum.
     * The default implementation creates a {@link DefaultEngineeringDatum} instance.
     *
     * @param  properties  name and other properties to give to the new object.
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultAffineCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultAffineCS#DefaultAffineCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    @Override
    public AffineCS createAffineCS(final Map<String,?> properties,
                                   final CoordinateSystemAxis axis0,
                                   final CoordinateSystemAxis axis1,
                                   final CoordinateSystemAxis axis2)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultCylindricalCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCylindricalCS#DefaultCylindricalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createCylindricalCS(String)
     */
    @Override
    public CylindricalCS createCylindricalCS(final Map<String,?> properties,
                                             final CoordinateSystemAxis axis0,
                                             final CoordinateSystemAxis axis1,
                                             final CoordinateSystemAxis axis2)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultPolarCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultPolarCS#DefaultPolarCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     * @see GeodeticAuthorityFactory#createPolarCS(String)
     */
    @Override
    public PolarCS createPolarCS(final Map<String,?> properties,
                                 final CoordinateSystemAxis axis0,
                                 final CoordinateSystemAxis axis1)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultLinearCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the single axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultLinearCS#DefaultLinearCS(Map, CoordinateSystemAxis)
     */
    @Override
    public LinearCS createLinearCS(final Map<String,?> properties,
                                   final CoordinateSystemAxis axis)
            throws FactoryException
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultUserDefinedCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultUserDefinedCS#DefaultUserDefinedCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     *
     * @deprecated The {@code UserDefinedCS} class has been removed from ISO 19111:2019.
     */
    @Override
    @Deprecated(since = "1.5")
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
     * <h4>Dependencies</h4>
     * The components needed by this method can be created by the following methods:
     * <ol>
     *   <li>{@link #createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}</li>
     * </ol>
     *
     * The default implementation creates a {@link DefaultUserDefinedCS} instance.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultUserDefinedCS#DefaultUserDefinedCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     *
     * @deprecated The {@code UserDefinedCS} class has been removed from ISO 19111:2019.
     */
    @Override
    @Deprecated(since = "1.5")
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
     * @param  properties    name and other properties to give to the new object.
     * @param  abbreviation  the coordinate axis abbreviation.
     * @param  direction     the axis direction.
     * @param  unit          the coordinate axis unit.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultCoordinateSystemAxis#DefaultCoordinateSystemAxis(Map, String, AxisDirection, Unit)
     * @see GeodeticAuthorityFactory#createCoordinateSystemAxis(String)
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final Map<String,?> properties,
                                                           final String abbreviation,
                                                           final AxisDirection direction,
                                                           final Unit<?> unit)
            throws FactoryException
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
     * @param  xml  coordinate reference system encoded in XML format.
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
            /*
             * The JAXB exception if often a wrapper around other exceptions, sometimes InvocationTargetException.
             * The exception cause is called "linked exception" by JAXB, presumably because it predates standard
             * chained exception mechanism introduced in Java 1.4. The JAXB linked exceptions do not propagate the
             * error message, so we have to take it from the cause, skipping InvocationTargetException since they
             * are wrapper for other causes. If the cause is a JAXBException, we will keep it as the declared cause
             * for simplifying the stack trace.
             */
            String message = e.getLocalizedMessage();
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                cause = Exceptions.unwrap((Exception) cause);
                if (cause instanceof JAXBException) {
                    e = (JAXBException) cause;
                }
                if (message == null) {
                    message = cause.getLocalizedMessage();
                }
            }
            throw new FactoryException(message, e);
        }
        if (object instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) object;
        } else {
            throw new FactoryException(Errors.forProperties(defaultProperties).getString(
                    Errors.Keys.IllegalClass_2, CoordinateReferenceSystem.class, object.getClass()));
        }
    }

    /**
     * Creates a Coordinate Reference System object from a <i>Well Known Text</i> (WKT).
     * This method understands both version 1 (a.k.a. OGC 01-009) and version 2 (a.k.a. ISO 19162)
     * of the WKT format.
     *
     * <h4>Example</h4>
     * Below is a slightly simplified WKT 2 string for a Mercator projection.
     * For making this example smaller, some optional {@code UNIT[…]} and {@code ORDER[…]} elements have been omitted.
     *
     * {@snippet lang="wkt" :
     *   ProjectedCRS["SIRGAS 2000 / Brazil Mercator",
     *     BaseGeogCRS["SIRGAS 2000",
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
     *   }
     *
     * <h4>Logging</h4>
     * If the given text contains non-fatal anomalies
     * (unknown or unsupported WKT elements, inconsistent unit definitions, unparsable axis abbreviations, <i>etc.</i>),
     * warnings may be reported in a {@linkplain java.util.logging.Logger logger} named {@code "org.apache.sis.io.wkt"}.
     * However, this parser does not verify if the overall parsed object matches the EPSG (or other authority) definition,
     * since this geodetic object factory is not an {@linkplain GeodeticAuthorityFactory authority factory}.
     * For such verification, see the {@link org.apache.sis.referencing.CRS#fromWKT(String)} convenience method.
     *
     * <h4>Usage and performance considerations</h4>
     * The default implementation uses a shared instance of {@link org.apache.sis.io.wkt.WKTFormat}
     * with the addition of thread-safety. This is okay for occasional use,
     * but is sub-optimal if this method is extensively used in a multi-thread environment.
     * Furthermore, this method offers no control on the WKT {@linkplain org.apache.sis.io.wkt.Convention conventions}
     * in use and on the handling of {@linkplain org.apache.sis.io.wkt.Warnings warnings}.
     * Applications which need to parse a large number of WKT strings should consider to use
     * the {@link org.apache.sis.io.wkt.WKTFormat} class instead of this method.
     *
     * @param  wkt  coordinate system encoded in Well-Known Text format (version 1 or 2).
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.io.wkt
     * @see org.apache.sis.referencing.CRS#fromWKT(String)
     */
    @Override
    public CoordinateReferenceSystem createFromWKT(final String wkt) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        Parser p = parser.getAndSet(null);
        if (p == null) try {
            Constructor<? extends Parser> c = parserConstructor;
            if (c == null) {
                c = Class.forName("org.apache.sis.io.wkt.GeodeticObjectParser").asSubclass(Parser.class)
                         .getConstructor(Map.class, ObjectFactory.class, MathTransformFactory.class);
                c.setAccessible(true);
                parserConstructor = c;
            }
            p = c.newInstance(defaultProperties, this, DefaultMathTransformFactory.provider());
        } catch (ReflectiveOperationException e) {
            throw new FactoryException(e);
        }
        final Object object;
        try {
            object = p.createFromWKT(wkt);
        } catch (FactoryException e) {
            /*
             * In the case of map projection, the parsing may fail because a projection parameter is not known to SIS.
             * If this happen, replace the generic exception thrown be the parser (which is `FactoryException`) by a
             * more specific one. Note that `InvalidGeodeticParameterException` is defined only in this referencing
             * module, so we could not throw it from the `org.apache.sis.metadata` module that contain the parser.
             */
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ParameterNotFoundException) {
                    throw new InvalidGeodeticParameterException(e.getLocalizedMessage(), cause);
                }
                cause = cause.getCause();
            }
            throw e;
        }
        parser.set(p);
        if (object instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) object;
        } else {
            throw new FactoryException(Errors.forProperties(defaultProperties).getString(
                    Errors.Keys.IllegalClass_2, CoordinateReferenceSystem.class, object.getClass()));
        }
    }
}
