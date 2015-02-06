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

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.MathTransform;


/**
 * An object capable to create {@code MathTransform} instances from given parameter values.
 * The Apache SIS {@link DefaultMathTransformFactory} implementation checks for this interface
 * when creating a {@code MathTransform} for an
 * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation method}.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @since   0.6
 * @version 0.6
 * @module
 */
@FunctionalInterface
public interface MathTransformProvider {
    /**
     * Creates a math transform from the specified group of parameter values.
     *
     * <div class="note"><b>Implementation example:</b>
     * The following example shows how parameter values can be extracted
     * before to instantiate the transform:
     *
     * {@preformat java
     *     double semiMajor = values.parameter("semi_major").doubleValue(SI.METRE);
     *     double semiMinor = values.parameter("semi_minor").doubleValue(SI.METRE);
     *     // etc...
     *     return new MyTransform(semiMajor, semiMinor, ...);
     * }
     * </div>
     *
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws InvalidParameterNameException if the values contains an unknown parameter.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws InvalidParameterValueException if a parameter has an invalid value.
     * @throws FactoryException if the math transform can not be created for some other reason
     *         (for example a required file was not found).
     */
    MathTransform createMathTransform(ParameterValueGroup values)
            throws InvalidParameterNameException, ParameterNotFoundException,
                   InvalidParameterValueException, FactoryException;
}
