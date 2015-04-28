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
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

// Test imports
import org.apache.sis.metadata.iso.citation.HardCodedCitations;


/**
 * Skeleton for {@link MathTransformFactory} custom implementations.
 * Implementors need to override at least one method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
strictfp class MathTransformFactoryBase implements MathTransformFactory {
    /**
     * The message for all exception.
     */
    private static final String MESSAGE = "Undefined by the test suite.";

    /**
     * For subclasses constructor only.
     */
    protected MathTransformFactoryBase() {
    }

    /** Returns the Apache SIS citation. */
    @Override
    public Citation getVendor() {
        return HardCodedCitations.SIS;
    }

    /** Default implementation returns an empty set. */
    @Override
    public Set<OperationMethod> getAvailableMethods(Class<? extends SingleOperation> type) {
        return Collections.emptySet();
    }

    /** Default implementation unconditionally returns {@code null}. */
    @Override
    public OperationMethod getLastMethodUsed() {
        return null;
    }

    /** Default implementation throws an exception. */
    @Override
    public ParameterValueGroup getDefaultParameters(String method) throws NoSuchIdentifierException {
        throw new NoSuchIdentifierException(MESSAGE, method);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createBaseToDerived(CoordinateReferenceSystem baseCRS, ParameterValueGroup parameters, CoordinateSystem derivedCS) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createParameterizedTransform(ParameterValueGroup parameters) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createAffineTransform(Matrix matrix) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createConcatenatedTransform(MathTransform transform1, MathTransform transform2) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createPassThroughTransform(int firstAffectedOrdinate, MathTransform subTransform, int numTrailingOrdinates) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createFromXML(String xml) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }

    /** Default implementation throws an exception. */
    @Override
    public MathTransform createFromWKT(String wkt) throws FactoryException {
        throw new FactoryException(MESSAGE);
    }
}
