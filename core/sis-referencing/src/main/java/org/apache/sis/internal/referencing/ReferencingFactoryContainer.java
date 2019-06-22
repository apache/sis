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
package org.apache.sis.internal.referencing;

import java.util.Map;
import org.opengis.util.Factory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Errors;


/**
 * A container of factories frequently used together.
 * Provides also some utility methods working with factories.
 *
 * This class may be temporary until we choose a dependency injection framework
 * See <a href="https://issues.apache.org/jira/browse/SIS-102">SIS-102</a>.
 *
 * <p>This class is not thread safe. Synchronization, if needed, is caller's responsibility.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-102">SIS-102</a>
 *
 * @since 1.0
 * @module
 */
public class ReferencingFactoryContainer {
    /**
     * The key for specifying a {@link MathTransformFactory} instance to use for geodetic object constructions.
     * This is usually not needed for CRS construction, except in the special case of a derived CRS created
     * from a defining conversion.
     */
    public static final String MT_FACTORY = "mtFactory";

    /**
     * The key for specifying a {@link CRSFactory} instance to use for geodetic object constructions.
     */
    public static final String CRS_FACTORY = "crsFactory";

    /**
     * The key for specifying a {@link CSFactory} instance to use for geodetic object constructions.
     */
    public static final String CS_FACTORY = "csFactory";

    /**
     * The key for specifying a {@link DatumFactory} instance to use for geodetic object constructions.
     */
    public static final String DATUM_FACTORY = "datumFactory";

    /**
     * The factory for creating coordinate reference systems from authority codes.
     * If null, then a default factory will be created only when first needed.
     */
    private CRSAuthorityFactory crsAuthorityFactory;

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
    private CoordinateOperationFactory operationFactory;

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
        this.operationFactory  = operationFactory;
        this.mtFactory         = mtFactory;
    }

    /**
     * Sets one of the factories managed by this container.
     * The given {@code type} argument can be one of the following values:
     *
     * <ul>
     *   <li><code>{@linkplain CRSFactory}.class</code></li>
     *   <li><code>{@linkplain CSFactory}.class</code></li>
     *   <li><code>{@linkplain DatumFactory}.class</code></li>
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
    public final <T extends Factory> boolean setFactory(final Class<T> type, final T factory) {
        if (type == CRSFactory.class)                 return crsFactory       != (crsFactory       = (CRSFactory)                 factory);
        if (type == CSFactory.class)                  return csFactory        != (csFactory        = (CSFactory)                  factory);
        if (type == DatumFactory.class)               return datumFactory     != (datumFactory     = (DatumFactory)               factory);
        if (type == CoordinateOperationFactory.class) return operationFactory != (operationFactory = (CoordinateOperationFactory) factory);
        if (type == MathTransformFactory.class)       return mtFactory        != (mtFactory        = (MathTransformFactory)       factory);
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
    }

    /**
     * Returns one of the factories managed by this container.
     * The given {@code type} argument can be one of the following values:
     *
     * <ul>
     *   <li><code>{@linkplain CRSFactory}.class</code></li>
     *   <li><code>{@linkplain CSFactory}.class</code></li>
     *   <li><code>{@linkplain DatumFactory}.class</code></li>
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
    public final <T extends Factory> T getFactory(final Class<T> type) {
        final Factory f;
             if (type == CRSFactory.class)                 f = getCRSFactory();
        else if (type == CSFactory.class)                  f = getCSFactory();
        else if (type == DatumFactory.class)               f = getDatumFactory();
        else if (type == CoordinateOperationFactory.class) f = getCoordinateOperationFactory();
        else if (type == MathTransformFactory.class)       f = getMathTransformFactory();
        else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
        return type.cast(f);
    }

    /**
     * Returns the factory for creating coordinate reference systems from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Coordinate Reference System authority factory (never {@code null}).
     * @throws FactoryException if the authority factory can not be obtained.
     */
    public final CRSAuthorityFactory getCRSAuthorityFactory() throws FactoryException {
        if (crsAuthorityFactory == null) {
            crsAuthorityFactory = CRS.getAuthorityFactory(Constants.EPSG);
        }
        return crsAuthorityFactory;
    }

    /**
     * Returns the factory for creating coordinate systems from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Coordinate System authority factory (never {@code null}).
     * @throws FactoryException if the authority factory can not be obtained.
     */
    public final CSAuthorityFactory getCSAuthorityFactory() throws FactoryException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory();
        if (factory instanceof CSAuthorityFactory) {                    // This is the case for SIS implementation.
            return (CSAuthorityFactory) factory;
        }
        throw new NoSuchAuthorityFactoryException(null, Constants.EPSG);
    }

    /**
     * Returns the factory for creating datum from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Datum authority factory (never {@code null}).
     * @throws FactoryException if the authority factory can not be obtained.
     */
    public final DatumAuthorityFactory getDatumAuthorityFactory() throws FactoryException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory();
        if (factory instanceof DatumAuthorityFactory) {                 // This is the case for SIS implementation.
            return (DatumAuthorityFactory) factory;
        }
        throw new NoSuchAuthorityFactoryException(null, Constants.EPSG);
    }

    /**
     * Returns the factory for creating coordinate operations from authority codes.
     * Currently only EPSG codes are supported.
     *
     * @return the Coordinate Operation authority factory (never {@code null}).
     * @throws FactoryException if the authority factory can not be obtained.
     */
    public final CoordinateOperationAuthorityFactory getCoordinateOperationAuthorityFactory() throws FactoryException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory();
        if (factory instanceof CoordinateOperationAuthorityFactory) {       // This is the case for SIS implementation.
            return (CoordinateOperationAuthorityFactory) factory;
        }
        throw new NoSuchAuthorityFactoryException(null, Constants.EPSG);
    }

    /**
     * Returns the factory for creating coordinate reference systems.
     *
     * @return the Coordinate Reference System factory (never {@code null}).
     */
    public final CRSFactory getCRSFactory() {
        if (crsFactory == null) {
            crsFactory = DefaultFactories.forBuildin(CRSFactory.class);
        }
        return crsFactory;
    }

    /**
     * Returns the factory for creating coordinate systems and their axes.
     *
     * @return the Coordinate System factory (never {@code null}).
     */
    public final CSFactory getCSFactory() {
        if (csFactory == null) {
            csFactory = DefaultFactories.forBuildin(CSFactory.class);
        }
        return csFactory;
    }

    /**
     * Returns the factory for creating datum, prime meridians and ellipsoids.
     *
     * @return the Datum factory (never {@code null}).
     */
    public final DatumFactory getDatumFactory() {
        if (datumFactory == null) {
            datumFactory = DefaultFactories.forBuildin(DatumFactory.class);
        }
        return datumFactory;
    }

    /**
     * Returns the factory for fetching operation methods and creating defining conversions.
     * This is needed only for user-defined projected coordinate reference system.
     * The factory is fetched when first needed.
     *
     * @return the Coordinate Operation factory (never {@code null}).
     */
    public final CoordinateOperationFactory getCoordinateOperationFactory() {
        if (operationFactory == null) {
            operationFactory = CoordinateOperations.getCoordinateOperationFactory(defaultProperties, mtFactory, crsFactory, csFactory);
            defaultProperties = null;       // Not needed anymore.
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
            mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class);
        }
        return mtFactory;
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
}
