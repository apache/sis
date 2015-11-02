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

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ArgumentChecks;


/**
 * The provider for <cite>"Abridged Molodensky transformation"</cite> (EPSG:9605).
 * This provider constructs transforms between two geographic reference systems without passing though a geocentric one.
 * This class nevertheless extends {@link GeocentricAffineBetweenGeographic} because it is an approximation of
 * {@link GeocentricTranslation3D}.
 *
 * <p>The translation terms (<var>dx</var>, <var>dy</var> and <var>dz</var>) are common to all authorities.
 * But remaining parameters are specified in different ways depending on the authority:</p>
 *
 * <ul>
 *   <li>EPSG defines <cite>"Semi-major axis length difference"</cite>
 *       and <cite>"Flattening difference"</cite> parameters.</li>
 *   <li>OGC rather defines "{@code src_semi_major}", "{@code src_semi_minor}",
 *       "{@code tgt_semi_major}", "{@code tgt_semi_minor}" and "{@code dim}" parameters.</li>
 * </ul>
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class AbridgedMolodensky extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3889456253400732280L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("9605")
                .addName("Abridged Molodensky")
                .addName(Citations.OGC, "Abridged_Molodenski")
                .createGroupWithSameParameters(Molodensky.PARAMETERS);
    }

    /**
     * The providers for all combinations between 2D and 3D cases.
     * Array length is 4. Index is build with following rule:
     * <ul>
     *   <li>Bit 1: dimension of source coordinates (0 for 2D, 1 for 3D).</li>
     *   <li>Bit 0: dimension of target coordinates (0 for 2D, 1 for 3D).</li>
     * </ul>
     */
    private final AbridgedMolodensky[] redimensioned;

    /**
     * Constructs a new provider.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public AbridgedMolodensky() {
        super(3, 3, PARAMETERS);
        redimensioned = new AbridgedMolodensky[4];
        redimensioned[0] = new AbridgedMolodensky(2, 2, redimensioned);
        redimensioned[1] = new AbridgedMolodensky(2, 3, redimensioned);
        redimensioned[2] = new AbridgedMolodensky(3, 2, redimensioned);
        redimensioned[3] = this;
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param redimensioned   Providers for all combinations between 2D and 3D cases.
     */
    private AbridgedMolodensky(final int sourceDimension, final int targetDimension, final AbridgedMolodensky[] redimensioned) {
        super(sourceDimension, targetDimension, PARAMETERS);
        this.redimensioned = redimensioned;
    }

    /**
     * Returns the same operation method, but for different number of dimensions.
     *
     * @param  sourceDimensions The desired number of input dimensions.
     * @param  targetDimensions The desired number of output dimensions.
     * @return The redimensioned operation method, or {@code this} if no change is needed.
     */
    @Override
    public OperationMethod redimension(final int sourceDimensions, final int targetDimensions) {
        ArgumentChecks.ensureBetween("sourceDimensions", 2, 3, sourceDimensions);
        ArgumentChecks.ensureBetween("targetDimensions", 2, 3, targetDimensions);
        return redimensioned[((sourceDimensions & 1) << 1) | (targetDimensions & 1)];
    }

    /**
     * While Abridged Molodensky method is an approximation of geocentric translation, this is not exactly that.
     */
    @Override
    int getType() {
        return OTHER;
    }

    /**
     * Creates an Abridged Molodensky transform from the specified group of parameter values.
     *
     * @param  factory The factory to use for creating concatenated transforms.
     * @param  values  The group of parameter values.
     * @return The created Abridged Molodensky transform.
     * @throws FactoryException if a transform can not be created.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        return Molodensky.createMathTransform(factory, Parameters.castOrWrap(values),
                getSourceDimensions(), getTargetDimensions(), true);
    }
}
