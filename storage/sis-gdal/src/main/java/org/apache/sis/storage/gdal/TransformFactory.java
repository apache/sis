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

import java.util.Set;
import java.util.Collections;
import java.util.MissingResourceException;

import org.opengis.util.*;
import org.opengis.parameter.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.ReferenceIdentifier;

import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * A factory for {@linkplain MathTransform Math Transform} objects created from a list of parameters.
 *
 * <p>The only supported methods are:</p>
 * <ul>
 *   <li>{@link #getAvailableMethods(Class)}</li>
 *   <li>{@link #getDefaultParameters(String)} - only partially implemented</li>
 *   <li>{@link #createParameterizedTransform(ParameterValueGroup)}</li>
 *   <li>{@link #createAffineTransform(Matrix)}</li>
 *   <li>{@link #createConcatenatedTransform(MathTransform, MathTransform)}</li>
 * </ul>
 *
 * All other methods unconditionally throw a {@link FactoryException},
 * or return {@code null} when doing so is allowed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class TransformFactory extends AbstractFactory implements MathTransformFactory {
    /**
     * Useful constant.
     */
    private static final ReferenceIdentifier WGS84 = new ImmutableIdentifier(null, null, "WGS84");

    /**
     * Creates a new coordinate operation factory.
     */
    private TransformFactory() {
    }

    /**
     * Returns the Proj.4 names of all projections supported by this class.
     */
    @Override
    public Set<OperationMethod> getAvailableMethods(Class<? extends SingleOperation> type) {
        if (type.isAssignableFrom(Projection.class)) try {
            return ResourcesLoader.getMethods();
        } catch (FactoryException e) { // Should never happen, unless there is an I/O error.
            throw new MissingResourceException(e.getLocalizedMessage(), ResourcesLoader.PROJECTIONS_FILE, "<all>");
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Unconditionally returns {@code null}, since this functionality is not supported yet.
     */
    @Override
    public OperationMethod getLastMethodUsed() {
        return null;
    }

    /**
     * Returns the parameter group for the given projection. The {@code method} argument can be the
     * Proj.4 projection name, or one of its aliases. This method does not check the validity of the
     * given argument, and the returned group does not enumerate the actual list of valid parameters,
     * because Proj.4 does not supply this information.
     */
    @Override
    public ParameterValueGroup getDefaultParameters(final String method) {
        try {
            return new ParameterGroup(new ImmutableIdentifier(null, null, method), ResourcesLoader.getAliases(method, false));
        } catch (FactoryException e) { // Should never happen, unless there is an I/O error.
            throw new MissingResourceException(e.getLocalizedMessage(), ResourcesLoader.PROJECTIONS_FILE, method);
        }
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException alway thrown.
     */
    @Override
    public MathTransform createBaseToDerived(CoordinateReferenceSystem baseCRS,
            ParameterValueGroup parameters, CoordinateSystem derivedCS) throws FactoryException
    {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Creates a math transform from the given Proj.4 parameters.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     */
    @Override
    public MathTransform createParameterizedTransform(final ParameterValueGroup parameters) throws FactoryException {
        final StringBuilder definition = new StringBuilder(200);
        definition.append("+proj=").append(ResourcesLoader.getProjName(parameters, false).substring(1));
        for (final GeneralParameterValue parameter : parameters.values()) {
            if (parameter instanceof ParameterValue) {
                final Object value = ((ParameterValue) parameter).getValue();
                if (value != null) {
                    final String pn = ResourcesLoader.getProjName(parameter, true);
                    definition.append(' ').append(pn).append('=').append(value);
                }
            }
        }
        final ReferenceIdentifier id = parameters.getDescriptor().getName();
        final CoordinateReferenceSystem targetCRS = Proj4.createCRS(id, id, definition.toString(), 2);
        final CoordinateReferenceSystem sourceCRS = (targetCRS instanceof ProjectedCRS)
                ? ((ProjectedCRS) targetCRS).getBaseCRS()
                : Proj4.createCRS(WGS84, WGS84, "+init=epsg:4326", 2);
        return Proj4.createOperation(id, sourceCRS, targetCRS).getMathTransform();
    }

    /**
     * Creates an affine transform from a matrix. If the transform input dimension is {@code M},
     * and output dimension is {@code N}, then the matrix will have size {@code [N+1][M+1]}.
     * The {@code [i][N]} element of the matrix must be 0 for <var>i</var> less than {@code M},
     * and 1 for <var>i</var> equals {@code M}.
     *
     * @param  matrix  the matrix used to define the affine transform.
     * @return the affine transform.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createAffineTransform(final Matrix matrix) throws FactoryException {
        return MathTransforms.linear(matrix);
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two
     * transforms, one after the other.
     *
     * <p>This implementation can only concatenate two affine transforms,
     * or to Proj.4 transforms. All other cases are unsupported.</p>
     *
     * @param  transform1  the first transform to apply to points.
     * @param  transform2  the second transform to apply to points.
     * @return the concatenated transform.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createConcatenatedTransform(final MathTransform transform1,
            final MathTransform transform2) throws FactoryException
    {
        final CRS sourceCRS, targetCRS;
        try {
            sourceCRS = ((Operation) transform1).source;
            targetCRS = ((Operation) transform2).target;
        } catch (ClassCastException e) {
            throw new FactoryException(e);
        }
        return new Operation(null, sourceCRS, targetCRS);
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException alway thrown.
     */
    @Override
    public MathTransform createPassThroughTransform(int firstAffectedOrdinate, MathTransform subTransform, int numTrailingOrdinates) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException alway thrown.
     */
    @Override
    public MathTransform createFromXML(String xml) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }

    /**
     * Unconditionally throws an exception, since this functionality is not supported yet.
     *
     * @throws FactoryException alway thrown.
     */
    @Override
    public MathTransform createFromWKT(String wkt) throws FactoryException {
        throw Proj4.unsupportedOperation();
    }
}
