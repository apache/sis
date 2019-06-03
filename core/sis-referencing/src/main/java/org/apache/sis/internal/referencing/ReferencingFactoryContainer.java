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

import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.CRS;

// Branch-dependent imports
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;


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
    private DefaultCoordinateOperationFactory operationFactory;

    /**
     * The {@linkplain org.opengis.referencing.operation.MathTransform math transform} factory.
     * If null, then a default factory will be created only when first needed.
     */
    private MathTransformFactory mtFactory;

    /**
     * Creates a new instance for the default factories.
     */
    public ReferencingFactoryContainer() {
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
    public final DefaultCoordinateOperationFactory getCoordinateOperationFactory() {
        if (operationFactory == null) {
            operationFactory = CoordinateOperations.factory();
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
