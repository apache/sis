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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.FactoryException;

import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.metadata.iso.ImmutableIdentifier;


/**
 * A factory for {@linkplain CoordinateOperation Coordinate Operation} objects created from source and target CRS.
 * Current implementation accepts only CRS objects created by a {@link Proj4}.
 *
 * <p>The only supported methods are:</p>
 * <ul>
 *   <li>{@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)}</li>
 *   <li>{@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, OperationMethod)}</li>
 * </ul>
 *
 * All other methods unconditionally throw a {@link FactoryException}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class OperationFactory extends AbstractFactory implements CoordinateOperationFactory {
    /**
     * Creates a new coordinate operation factory.
     */
    private OperationFactory() {
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     * This given source and target CRS must be instances created by {@link Proj4} or {@link EPSGFactory}.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws FactoryException if the given CRS are not instances recognized by this class.
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        ReferenceIdentifier id;
        String src=null, tgt=null, space=null;
        if ((id = sourceCRS.getName()) != null) {
            src = id.getCode();
            space = id.getCodeSpace();
        }
        if ((id = targetCRS.getName()) != null) {
            tgt = id.getCode();
            if (space == null) {
                space = id.getCodeSpace();
            }
        }
        id = null;
        if (src != null || tgt != null) {
            final StringBuilder buffer = new StringBuilder();
            if (src != null) buffer.append("From ").append(src);
            if (tgt != null) buffer.append(buffer.length() == 0 ? "To " : " to ").append(tgt);
            id = new ImmutableIdentifier(null, space, buffer.toString());
        }
        try {
            return Proj4.createOperation(id, sourceCRS, targetCRS);
        } catch (ClassCastException e) {
            throw new FactoryException("The CRS must be instances created by PJFactory.", e);
        }
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
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final OperationMethod method)
            throws FactoryException
    {
        return createOperation(sourceCRS, targetCRS);
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException alway thrown.
     */
    @Override
    public CoordinateOperation createConcatenatedOperation(Map<String,?> properties,
            CoordinateOperation... operations) throws FactoryException
    {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException alway thrown.
     */
    @Override
    public Conversion createDefiningConversion(Map<String,?> properties,
            OperationMethod method, ParameterValueGroup parameters) throws FactoryException
    {
        throw Proj4.unsupportedOperation();
    }
}
