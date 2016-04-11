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
package org.apache.sis.internal.referencing.provider;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.operation.transform.InterpolatedMolodenskyTransform;


/**
 * An approximation of geocentric interpolations which uses {@link InterpolatedMolodenskyTransform}
 * instead than {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform}.
 *
 * <p>This operation method is not standard, and as of SIS 0.7 not yet registered in the operation methods
 * provided by {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory}.
 * This class extends {@code FranceGeocentricInterpolation} for now because the later is currently
 * the only operation performing interpolation in the geocentric domain.
 * However this class hierarchy may be revisited in any future SIS version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class MolodenskyInterpolation extends FranceGeocentricInterpolation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4265894749866901286L;

    /**
     * Constructs a provider.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public MolodenskyInterpolation() {
        super(2, 2, builder().setCodeSpace(null, Constants.SIS).addName("Molodensky interpolation")
                        .createGroupWithSameParameters(PARAMETERS), new MolodenskyInterpolation[4]);
        final ParameterDescriptorGroup parameters = super.getParameters();
        redimensioned[0] = this;
        redimensioned[1] = new MolodenskyInterpolation(2, 3, parameters, redimensioned);
        redimensioned[2] = new MolodenskyInterpolation(3, 2, parameters, redimensioned);
        redimensioned[3] = new MolodenskyInterpolation(3, 3, parameters, redimensioned);
    }

    /**
     * Constructs a provider for the given number of dimensions.
     *
     * @param sourceDimensions  number of dimensions in the source CRS of this operation method.
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param parameters        description of parameters expected by this operation.
     * @param redimensioned     providers for all combinations between 2D and 3D cases, or {@code null}.
     */
    private MolodenskyInterpolation(final int sourceDimensions,
                                    final int targetDimensions,
                                    final ParameterDescriptorGroup parameters,
                                    final GeodeticOperation[] redimensioned)
    {
        super(sourceDimensions, targetDimensions, parameters, redimensioned);
    }

    /**
     * Invoked by {@link #createMathTransform(MathTransformFactory, ParameterValueGroup)}
     * after all parameters have been processed.
     */
    @Override
    MathTransform createGeodeticTransformation(final MathTransformFactory factory,
            final Ellipsoid source, final Ellipsoid target, final boolean withHeights,
            final DatumShiftGridFile<Angle,Length> grid) throws FactoryException
    {
        return InterpolatedMolodenskyTransform.createGeodeticTransformation(
                factory, source, withHeights, target, withHeights, grid);
    }
}
