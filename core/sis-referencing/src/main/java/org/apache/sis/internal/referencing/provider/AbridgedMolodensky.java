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
import javax.measure.unit.SI;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <cite>"Abridged Molodensky transformation"</cite> (EPSG:9605).
 * This provider constructs transforms between two geographic reference systems,
 * without passing though a geocentric one.
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
abstract class AbridgedMolodensky extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3889456253400732280L;

    /**
     * The default value for geographic source and target dimensions.
     * We have to provide a default value because the {@link #DIM} parameter is not an EPSG parameter.
     * The default value is set to 3 because this family of operations is implicitly three-dimensional
     * in the EPSG database.
     *
     * <div class="note"><b>Maintenance note:</b>
     * if this default value is modified, then the handling of the two- and three-dimensional cases in
     * {@link AbridgedMolodenskyTransform} must be adjusted accordingly.</div>
     */
    private static final int DEFAULT_DIMENSION = 3;

    /**
     * The operation parameter descriptor for the number of source and target geographic dimensions (2 or 3).
     * This is an OGC-specific parameter.
     */
    private static final ParameterDescriptor<Integer> DIM;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    private static final ParameterDescriptor<Double> SRC_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    private static final ParameterDescriptor<Double> SRC_SEMI_MINOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    private static final ParameterDescriptor<Double> TGT_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    private static final ParameterDescriptor<Double> TGT_SEMI_MINOR;

    static {
        final ParameterBuilder builder = builder();
        /*
         * OGC parameters not defined in EPSG database.
         */
        builder.setCodeSpace(Citations.OGC, Constants.OGC).setRequired(false);
        DIM = builder.addName("dim").createBounded(2, 3, DEFAULT_DIMENSION);
        SRC_SEMI_MAJOR = builder.addName("src_semi_major").createStrictlyPositive(Double.NaN, SI.METRE);
        SRC_SEMI_MINOR = builder.addName("src_semi_minor").createStrictlyPositive(Double.NaN, SI.METRE);
        TGT_SEMI_MAJOR = builder.addName("tgt_semi_major").createStrictlyPositive(Double.NaN, SI.METRE);
        TGT_SEMI_MINOR = builder.addName("tgt_semi_minor").createStrictlyPositive(Double.NaN, SI.METRE);
    }

    private AbridgedMolodensky() {
        super(2, 2, null);
    }
}
