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
package org.apache.sis.referencing.operation.provider;

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.FactoryException;


/**
 * The three-dimensional counter-part of a map projection.
 * This is the same as two-dimensional map projections with only the ellipsoidal height which pass through.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
final class MapProjection3D extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6089942320273982171L;

    /**
     * The two-dimensional counterpart of this three-dimensional map projection.
     */
    private final MapProjection redimensioned;

    /**
     * Constructs a three-dimensional map projection for the given two-dimensional projection.
     */
    MapProjection3D(final MapProjection proj) {
        super(proj.getOperationType(), proj.getParameters(),
              proj.sourceCSType, proj.sourceOnEllipsoid,
              proj.targetCSType, proj.targetOnEllipsoid);
        redimensioned = proj;
    }

    /**
     * Returns the number of source dimensions of the transforms created by this provider.
     */
    @Override
    @SuppressWarnings("deprecation")
    public final Integer getSourceDimensions() {
        return 3;
    }

    /**
     * Returns the number of target dimensions of the transforms created by this provider.
     */
    @Override
    @SuppressWarnings("deprecation")
    public final Integer getTargetDimensions() {
        return 3;
    }

    /**
     * Returns this operation method with the specified number of dimensions.
     * The number of dimensions can be only 2 or 3, and must be the same for source and target CRS.
     */
    @Override
    public AbstractProvider redimension(final int sourceDimensions, final int targetDimensions) {
        if (sourceDimensions == 2 && targetDimensions == 2) {
            return redimensioned;
        }
        return super.redimension(sourceDimensions, targetDimensions);
    }

    /**
     * Creates a three-dimensional map projections for the given parameters.
     * The ellipsoidal height is assumed to be in the third dimension.
     */
    @Override
    public MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup parameters) throws FactoryException {
        return factory.createPassThroughTransform(0, redimensioned.createMathTransform(factory, parameters), 1);
    }
}
