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
package org.apache.sis.referencing.internal.shared;

import java.util.Map;
import java.util.Locale;
import org.opengis.util.FactoryException;
import org.opengis.util.NameFactory;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Localized;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;


/**
 * A container of factories frequently used together.
 * Provides also some utility methods working with factories.
 *
 * <p>This class is not thread safe. Synchronization, if needed, is caller's responsibility.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public class ReferencingFactoryContainer implements Localized {
    /**
     * The key for specifying a {@link NameFactory} instance to use for geodetic object constructions.
     */
    public static final String NAME_FACTORY = "nameFactory";

    /**
     * The key for specifying a {@link DatumFactory} instance to use for geodetic object constructions.
     */
    public static final String DATUM_FACTORY = "datumFactory";

    /**
     * The key for specifying a {@link CSFactory} instance to use for geodetic object constructions.
     */
    public static final String CS_FACTORY = "csFactory";

    /**
     * The key for specifying a {@link CRSFactory} instance to use for geodetic object constructions.
     */
    public static final String CRS_FACTORY = "crsFactory";

    /**
     * The key for specifying a {@link CoordinateOperationFactory} instance to use for geodetic object constructions.
     */
    public static final String OPERATION_FACTORY = "copFactory";

    /**
     * The key for specifying a {@link MathTransformFactory} instance to use for geodetic object constructions.
     * This is usually not needed for CRS construction, except in the special case of a derived CRS created
     * from a defining conversion.
     */
    public static final String MT_FACTORY = "mtFactory";

    /**
     * The factory for creating coordinate reference systems from authority codes.
     * If null, then a default factory will be created only when first needed.
     */
    private CRSAuthorityFactory crsAuthorityFactory;

    /**
     * The {@linkplain org.opengis.util.GenericName name} factory.
     * If null, then a default factory will be created only when first needed.
     */
    private NameFactory nameFactory;

    /**
     * The {@linkplain org.opengis.referencing.datum.Datum datum} factory.
     * If null, then a default factory will be created only when first needed.
     */
    private DatumFactory datumFactory;

    /**
     * The {@linkplain org.opengis.referencing.cs.CoordinateSystem coordinate system} factory.
     * If null, then a default factory will be created only when first needed.
     */
    private CSFactory csFactory;

    /**
     * The {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem coordinate reference system} factory.
     * If null, then a default factory will be created only when first needed.
     */
    private CRSFactory crsFactory;

    /**
     * Factory for fetching operation methods and creating defining conversions.
     * This is needed only for user-defined projected coordinate reference system.
     */
    private DefaultCoordinateOperationFactory operationFactory;

    /**
     * The {@linkplain org.opengis.referencing.operation.MathTransform math transform} factory.
     * If null, then a default factory will be created only when first needed.
     */
    private MathTransformFactory mtFactory;

    /**
     * The default properties, or an empty map if none. This map shall not be modified since its
     * reference may be shared without cloning. This field will be cleared when no longer needed.
     */
    private Map<String,?> defaultProperties;

    /**
     * Creates a new instance for the default factories.
     */
    public ReferencingFactoryContainer() {
    }

    /**
     * Creates a new instance with factories fetched from the given map of properties.
     * Factories that are not present in the map will be left to their default value.
     * This method recognizes the keys declared as static {@link String} constants in this class.
     * Other entries are ignored.
     *
     * @param  properties  the factories.
     */
    public ReferencingFactoryContainer(final Map<String,?> properties) {
        nameFactory      = (NameFactory)                properties.get(NAME_FACTORY);
        datumFactory     = (DatumFactory)               properties.get(DATUM_FACTORY);
        csFactory        = (CSFactory)                  properties.get(CS_FACTORY);
        crsFactory       = (CRSFactory)                 properties.get(CRS_FACTORY);
        operationFactory = (DefaultCoordinateOperationFactory) properties.get(OPERATION_FACTORY);
        mtFactory        = (MathTransformFactory)       properties.get(MT_FACTORY);
    }

    /**
     * Creates a new instance which will use the given factories.
     * Any factory given in argument may be {@code null} if lazy instantiation is desired.
     *
     * @param  defaultProperties  default properties to give to the objects to create (will not be cloned).
     * @param  crsFactory         the factory to use for creating coordinate reference systems.
     * @param  csFactory          the factory to use for creating coordinate systems.
     * @param  datumFactory       the factory to use for creating datum.
     * @param  operationFactory   the factory to use for creating (defining) conversions.
     * @param  mtFactory          the factory to use for creating transform objects.
     */
    public ReferencingFactoryContainer(final Map<String,?>              defaultProperties,
                                       final CRSFactory                 crsFactory,
                                       final CSFactory                  csFactory,
                                       final DatumFactory               datumFactory,
                                       final CoordinateOperationFactory operationFactory,
                                       final MathTransformFactory       mtFactory)
    {
        this.defaultProperties = defaultProperties;
        this.crsFactory        = crsFactory;
        this.csFactory         = csFactory;
        this.datumFactory      = datumFactory;
        this.operationFactory  = (DefaultCoordinateOperationFactory) operationFactory;      // Because of GeoAPI 3.0 limitation.
        this.mtFactory         = mtFactory;
    }

    /**
     * Sets one of the factories managed by this container.
     * The given {@code type} argument can be one of the following values:
     *
     * <ul>
     *   <li><code>{@linkplain NameFactory}.class</code></li>
     *   <li><code>{@linkplain DatumFactory}.class</code></li>
     *   <li><code>{@linkplain CSFactory}.class</code></li>
     *   <li><code>{@linkplain CRSFactory}.class</code></li>
     *   <li><code>{@linkplain CoordinateOperationFactory}.class</code></li>
     *   <li><code>{@linkplain MathTransformFactory}.class</code></li>
     * </ul>
     *
     * Note that authority factories are not yet handled by this method
     * for consistency with {@link #getFactory(Class)}.
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  type     the factory type.
     * @param  factory  the factory to use for the given type, or {@code null} for the default.
     * @return {@code true} if the factory changed as a result of this method call.
     * @throws IllegalArgumentException if the {@code type} argument is not one of the valid values.
     */
    public final <T> boolean setFactory(final Class<T> type, final T factory) {
        if (type == NameFactory.class)                return nameFactory      != (nameFactory      = (NameFactory)                factory);
        if (type == DatumFactory.class)               return datumFactory     != (datumFactory     = (DatumFactory)               factory);
        if (type == CSFactory.class)                  return csFactory        != (csFactory        = (CSFactory)                  factory);
        if (type == CRSFactory.class)                 return crsFactory       != (crsFactory       = (CRSFactory)                 factory);
        if (type == CoordinateOperationFactory.class) return operationFactory != (operationFactory = (DefaultCoordinateOperationFactory) factory);
        if (type == MathTransformFactory.class)       return mtFactory        != (mtFactory        = (MathTransformFactory)       factory);
        throw new IllegalArgumentException(Errors.forLocale(getLocale()).getString(Errors.Keys.IllegalArgumentValue_2, "type", type));
    }

    /**
     * Returns one of the factories managed by this container.
     * The given {@code type} argument can be one of the following values:
     *
     * <ul>
     *   <li><code>{@linkplain NameFactory}.class</code></li>
     *   <li><code>{@linkplain DatumFactory}.class</code></li>
     *   <li><code>{@linkplain CSFactory}.class</code></li>
     *   <li><code>{@linkplain CRSFactory}.class</code></li>
     *   <li><code>{@linkplain CoordinateOperationFactory}.class</code></li>
     *   <li><code>{@linkplain MathTransformFactory}.class</code></li>
     * </ul>
     *
     * Note that authority factories are not yet handled by this method, since we would have to expose
     * the current restriction to EPSG geodetic dataset and to handle {@link FactoryException}.
     *
     * @param  <T>   the compile-time type of the {@code type} argument.
     * @param  type  the factory type.
     * @return the factory for the given type.
     * @throws IllegalArgumentException if the {@code type} argument is not one of the valid values.
     */
    public final <T> T getFactory(final Class<T> type) {
        final Object f;
             if (type == NameFactory.class)                f = getNameFactory();
        else if (type == DatumFactory.class)               f = getDatumFactory();
        else if (type == CSFactory.class)                  f = getCSFactory();
        else if (type == CRSFactory.class)                 f = getCRSFactory();
        else if (type == CoordinateOperationFactory.class) f = getCoordinateOperationFactory();
        else if (type == MathTransformFactory.class)       f = getMathTransformFactory();
        else {
            throw new IllegalArgumentException(Errors.forLocale(getLocale())
                    .getString(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
        return type.cast(f);
    }

    /**
     * Returns the factory for creating generic name.
     *
     * @return the Generic Name factory (never {@code null}).
     */
    public final NameFactory getNameFactory() {
        if (nameFactory == null) {
            nameFactory = DefaultNameFactory.provider();
        }
        return nameFactory;
    }

    /**
     * Returns the factory for creating datum, prime meridians and ellipsoids.
     *
     * @return the Datum factory (never {@code null}).
     */
    public final DatumFactory getDatumFactory() {
        if (datumFactory == null) {
            datumFactory = GeodeticObjectFactory.provider();
        }
        return datumFactory;
    }

    /**
     * Returns the factory for creating coordinate systems and their axes.
     *
     * @return the Coordinate System factory (never {@code null}).
     */
    public final CSFactory getCSFactory() {
        if (csFactory == null) {
            csFactory = GeodeticObjectFactory.provider();
        }
        return csFactory;
    }

    /**
     * Returns the factory for creating coordinate reference systems.
     *
     * @return the Coordinate Reference System factory (never {@code null}).
     */
    public final CRSFactory getCRSFactory() {
        if (crsFactory == null) {
            crsFactory = GeodeticObjectFactory.provider();
        }
        return crsFactory;
    }

    /**
     * Returns the factory for fetching operation methods and creating defining conversions.
     * This is needed only for user-defined projected coordinate reference system.
     * The factory is fetched when first needed.
     *
     * @return the Coordinate Operation factory (never {@code null}).
     */
    public final DefaultCoordinateOperationFactory getCoordinateOperationFactory() {
        if (operationFactory == null) {
            CoordinateOperationFactory op = CoordinateOperations.getCoordinateOperationFactory(defaultProperties, mtFactory, crsFactory, csFactory);
            defaultProperties = null;       // Not needed anymore.
            if (op instanceof DefaultCoordinateOperationFactory) {
                operationFactory = (DefaultCoordinateOperationFactory) op;
            } else {
                operationFactory = DefaultCoordinateOperationFactory.provider();
            }
        }
        return operationFactory;
    }

    /**
     * Returns the factory for creating parameterized transforms.
     *
     * @return the Math Transform factory (never {@code null}).
     */
    public final MathTransformFactory getMathTransformFactory() {
        if (mtFactory == null) {
            mtFactory = DefaultMathTransformFactory.provider();
        }
        return mtFactory;
    }

    /**
     * Returns the factory for creating datum from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Datum authority factory (never {@code null}).
     * @throws FactoryException if the authority factory cannot be obtained.
     */
    public final DatumAuthorityFactory getDatumAuthorityFactory() throws FactoryException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory();
        if (factory instanceof DatumAuthorityFactory) {                 // This is the case for SIS implementation.
            return (DatumAuthorityFactory) factory;
        }
        throw new NoSuchAuthorityFactoryException(null, Constants.EPSG);
    }

    /**
     * Returns the factory for creating coordinate systems from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Coordinate System authority factory (never {@code null}).
     * @throws FactoryException if the authority factory cannot be obtained.
     */
    public final CSAuthorityFactory getCSAuthorityFactory() throws FactoryException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory();
        if (factory instanceof CSAuthorityFactory) {                    // This is the case for SIS implementation.
            return (CSAuthorityFactory) factory;
        }
        throw new NoSuchAuthorityFactoryException(null, Constants.EPSG);
    }

    /**
     * Returns the factory for creating coordinate reference systems from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Coordinate Reference System authority factory (never {@code null}).
     * @throws FactoryException if the authority factory cannot be obtained.
     */
    public final CRSAuthorityFactory getCRSAuthorityFactory() throws FactoryException {
        if (crsAuthorityFactory == null) {
            crsAuthorityFactory = CRS.getAuthorityFactory(Constants.EPSG);
        }
        return crsAuthorityFactory;
    }

    /**
     * Returns the factory for creating coordinate operations from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Coordinate Operation authority factory (never {@code null}).
     * @throws FactoryException if the authority factory cannot be obtained.
     */
    public final CoordinateOperationAuthorityFactory getCoordinateOperationAuthorityFactory() throws FactoryException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory();
        if (factory instanceof CoordinateOperationAuthorityFactory) {       // This is the case for SIS implementation.
            return (CoordinateOperationAuthorityFactory) factory;
        }
        throw new NoSuchAuthorityFactoryException(null, Constants.EPSG);
    }

    /**
     * Returns the a coordinate system for map projections with (easting, northing) axes in metres.
     * EPSG::4400 — Cartesian 2D CS. Axes: easting, northing (E,N). Orientations: east, north. UoM: m.
     *
     * @return a coordinate system with (easting, northing) axes in metres.
     * @throws FactoryException if an error occurred while creating the coordinate system.
     */
    public final CartesianCS getStandardProjectedCS() throws FactoryException {
        return getCSAuthorityFactory().createCartesianCS("4400");
    }

    /**
     * Returns the locale to use for error messages, or {@code null} if unspecified.
     * In the latter case, the platform default locale will be used.
     * Subclasses should override if a locale is known.
     *
     * @return the locale to use for error messages, or {@code null} if unspecified.
     */
    @Override
    public Locale getLocale() {
        return Resources.getLocale(defaultProperties);
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either
     * a method name (e.g. <q>Transverse Mercator</q>) or one of its identifiers (e.g. {@code "EPSG:9807"}).
     * The search is case-insensitive and comparisons against method names can be
     * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.
     *
     * <p>If more than one method match the given name, then the first (according iteration order)
     * non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  name  the name of the operation method to fetch.
     * @return the operation method of the given name.
     * @throws NoSuchIdentifierException if the requested operation method cannot be found.
     */
    public final OperationMethod findOperationMethod(String name) throws NoSuchIdentifierException {
        ArgumentChecks.ensureNonEmpty("name", name = name.strip());
        return CoordinateOperations.findMethod(getMathTransformFactory(), name);
    }
}
