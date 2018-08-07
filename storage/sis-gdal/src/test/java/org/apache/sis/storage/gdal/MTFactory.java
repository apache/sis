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
import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.io.wkt.WKTFormat;


/**
 * Adds to {@link Proj4Factory} the missing method for making it a {@link MathTransformFactory} implementation.
 * We do not provide those methods in the main source directory yet because we are not sure about exposing large
 * amount of methods that are not really supported by Proj.4 wrappers. If experience shows that it would be useful,
 * selected methods from this class could be refactored into {@link Proj4Factory} in future Apache SIS versions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class MTFactory extends Proj4Factory implements CoordinateOperationFactory, MathTransformFactory {
    /**
     * The WKT parser to use, created when first needed.
     *
     * @see #createFromWKT(String)
     */
    private transient WKTFormat parser;

    /**
     * Creates a new {@literal Proj.4} factory.
     *
     * @param properties  common properties for the objects to create, or {@code null} if none.
     */
    public MTFactory(final Map<String,?> properties) {
        super(properties);
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     * This implementation always uses Proj.4 for performing the coordinate operations, regardless if
     * the given CRS were created from Proj.4 definition strings or not. This method fails if it can
     * not map the given CRS to Proj.4 data structures.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws FactoryException if the given CRS are not instances recognized by this class.
     *
     * @see Proj4#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        return createOperation(sourceCRS, targetCRS, true);
    }

    /**
     * Ignores the given {@code method} argument and delegates to
     * <code>{@linkplain #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)
     * createOperation}(sourceCRS, targetCRS)</code>.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws FactoryException if the given CRS are not instances recognized by this class.
     */
    @Override
    @Deprecated
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final OperationMethod method)
            throws FactoryException
    {
        return createOperation(sourceCRS, targetCRS);
    }

    /**
     * Unsupported by the {@literal Proj.4} wrapper — delegates to the Apache SIS default factory.
     */
    @Override
    public CoordinateOperation createConcatenatedOperation(Map<String,?> properties, CoordinateOperation... operations)
            throws FactoryException
    {
        return opFactory().createConcatenatedOperation(properties, operations);
    }

    /**
     * Unsupported by the {@literal Proj.4} wrapper — delegates to the Apache SIS default factory.
     */
    @Override
    public Conversion createDefiningConversion(Map<String,?> properties, OperationMethod method, ParameterValueGroup parameters)
            throws FactoryException
    {
        return opFactory().createDefiningConversion(properties, method, parameters);
    }

    /**
     * Unsupported by the {@literal Proj.4} wrapper.
     */
    @Override
    public MathTransform createBaseToDerived(CoordinateReferenceSystem baseCRS, ParameterValueGroup parameters, CoordinateSystem derivedCS)
            throws NoSuchIdentifierException, FactoryException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported by the {@literal Proj.4} wrapper.
     */
    @Override
    public MathTransform createAffineTransform(Matrix matrix) throws FactoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not yet implemented.
     */
    @Override
    public MathTransform createConcatenatedTransform(MathTransform transform1, MathTransform transform2) throws FactoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported by the {@literal Proj.4} wrapper.
     */
    @Override
    public MathTransform createPassThroughTransform(int firstAffectedOrdinate, MathTransform subTransform, int numTrailingOrdinates)
            throws FactoryException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unconditionally returns the operation method that describes the {@literal Proj.4} {@link Transform} wrapper
     * since it is the only kind of operation created by this class.
     */
    @Override
    public OperationMethod getLastMethodUsed() {
        return Transform.METHOD;
    }

    /**
     * Parses the given Well Known Text (version 1) into a math transform.
     */
    @Override
    public synchronized MathTransform createFromWKT(final String wkt) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        if (parser == null) {
            parser = new WKTFormat(null, null);
            parser.setFactory(CRSAuthorityFactory.class, this);
            parser.setFactory(MathTransformFactory.class, this);
            parser.setFactory(CoordinateOperationFactory.class, this);
        }
        try {
            return (MathTransform) parser.parseObject(wkt);
        } catch (ParseException | ClassCastException e) {
            throw new FactoryException(e);
        }
    }

    /**
     * No XML format is defined for math transform.
     */
    @Override
    @Deprecated
    public MathTransform createFromXML(String xml) throws FactoryException {
        throw new UnsupportedOperationException();
    }
}
