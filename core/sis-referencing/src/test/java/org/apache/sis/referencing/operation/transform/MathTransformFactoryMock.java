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
package org.apache.sis.referencing.operation.transform;

import java.util.Set;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.referencing.operation.DefaultOperationMethod;


/**
 * A dummy implementation of {@link MathTransformFactory}, which contains exactly one operation method.
 * The operation method shall implement the {@link MathTransformProvider} interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
public final strictfp class MathTransformFactoryMock implements MathTransformFactory {
    /**
     * The operation method.
     */
    private final DefaultOperationMethod method;

    /**
     * Creates a new mock for the given operation method.
     *
     * @param  method  the operation method to put in this factory.
     */
    public MathTransformFactoryMock(final DefaultOperationMethod method) {
        this.method = method;
    }

    /**
     * Returns the singleton method, if assignable to the given type.
     *
     * @param  type  the type of operation methods to get.
     * @return the singleton method, or an empty set.
     */
    @Override
    public Set<OperationMethod> getAvailableMethods(Class<? extends SingleOperation> type) {
        return type.isInstance(method) ? Collections.singleton(method) : Collections.emptySet();
    }

    /**
     * Returns the last method used, which can only be null or the method given at construction time.
     *
     * @return the method given at construction time.
     */
    @Override
    public OperationMethod getLastMethodUsed() {
        return method;
    }

    /**
     * Returns the parameters for the operation method.
     *
     * @param  name  shall be the operation method name.
     * @return the parameters.
     * @throws NoSuchIdentifierException if the given name is not the name
     *         of the operation method known to this factory.
     */
    @Override
    public ParameterValueGroup getDefaultParameters(final String name) throws NoSuchIdentifierException {
        if (method.isHeuristicMatchForName(name)) {
            return method.getParameters().createValue();
        }
        throw new NoSuchIdentifierException(null, name);
    }

    /**
     * Delegates to the method given at construction time.
     *
     * @param  parameters  the parameters to give to the math transform provider.
     * @return the transform created by the provider.
     * @throws FactoryException if the provider can not create the transform.
     */
    @Override
    public MathTransform createParameterizedTransform(ParameterValueGroup parameters) throws FactoryException {
        return ((MathTransformProvider) method).createMathTransform(this, parameters);
    }

    /**
     * Delegates to {@link MathTransforms}.
     *
     * @param  matrix  matrix representing the affine transform.
     * @return affine transform for the given matrix.
     */
    @Override
    public MathTransform createAffineTransform(final Matrix matrix) {
        return MathTransforms.linear(matrix);
    }

    /**
     * Delegates to {@link MathTransforms}.
     *
     * @param  transform1  first transform to concatenate.
     * @param  transform2  second transform to concatenate.
     * @return result of concatenation.
     */
    @Override
    public MathTransform createConcatenatedTransform(MathTransform transform1, MathTransform transform2) {
        return MathTransforms.concatenate(transform1, transform2);
    }

    /**
     * Unimplemented method.
     *
     * @param  firstAffectedCoordinate  ignored.
     * @param  subTransform             ignored.
     * @param  numTrailingCoordinates   ignored.
     * @return never returned.
     */
    @Override
    public MathTransform createPassThroughTransform(int firstAffectedCoordinate, MathTransform subTransform, int numTrailingCoordinates) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unimplemented method.
     *
     * @param  baseCRS     ignored.
     * @param  parameters  ignored.
     * @param  derivedCS   ignored.
     * @return never returned.
     */
    @Override
    public MathTransform createBaseToDerived(CoordinateReferenceSystem baseCRS,
            ParameterValueGroup parameters, CoordinateSystem derivedCS)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unimplemented method.
     *
     * @param  wkt  ignored.
     * @return never returned.
     */
    @Override
    public MathTransform createFromWKT(String wkt) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unimplemented method.
     *
     * @return {@code null}.
     */
    @Override
    public Citation getVendor() {
        return null;
    }
}
