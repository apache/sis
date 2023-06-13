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

import java.util.Arrays;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import jakarta.xml.bind.annotation.XmlTransient;
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
 * instead of {@link org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform}.
 *
 * <p>This operation method is not standard, and as of SIS 0.7 not yet registered in the operation methods
 * provided by {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory}.
 * This class extends {@code FranceGeocentricInterpolation} for now because the latter is currently
 * the only operation performing interpolation in the geocentric domain.
 * However, this class hierarchy may be revisited in any future SIS version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.7
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-500">Deprecate (for removal) InterpolatedMolodenskyTransform</a>
 */
@XmlTransient
@Deprecated(since="1.4", forRemoval=true)
// Note: after removal, delete overrideable method in parent class.
public final class MolodenskyInterpolation extends FranceGeocentricInterpolation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4265894749866901286L;

    /**
     * The providers for all combinations between 2D and 3D cases.
     */
    private static final MolodenskyInterpolation[] REDIMENSIONED = new MolodenskyInterpolation[4];
    static {
        final ParameterDescriptorGroup parameters = builder().setCodeSpace(null, Constants.SIS)
                .addName("Molodensky interpolation").createGroupWithSameParameters(PARAMETERS);
        Arrays.setAll(REDIMENSIONED, (i) -> new MolodenskyInterpolation(parameters, i));
    }

    /**
     * Returns the provider for the specified combination of source and target dimensions.
     */
    @Override
    final GeodeticOperation redimensioned(int indexOfDim) {
        return REDIMENSIONED[indexOfDim];
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    public MolodenskyInterpolation() {
        super(REDIMENSIONED[INDEX_OF_2D]);
    }

    /**
     * Constructs a provider for the given number of dimensions.
     */
    private MolodenskyInterpolation(ParameterDescriptorGroup parameters, int indexOfDim) {
        super(parameters, indexOfDim);
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
