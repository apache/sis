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
 * {@code MathTransformProvider} is the Apache SIS mechanism by which
 * {@linkplain org.apache.sis.referencing.operation.DefaultFormula formula} are concretized as Java code.
 * There is one {@code MathTransformProvider} for each map projection: one for Mercator, one for Lambert,
 * <i>etc</i>.
 *
 * <p>This interface is used by {@link DefaultMathTransformFactory} or by developers who want to plug
 * their own operation methods into Apache SIS. This interface is generally not used directly.
 * The recommended way to get a {@code MathTransform} is to first get the {@code CoordinateOperation}
 * (generally from a pair of <var>source</var> and <var>target</var> CRS), then to invoke
 * {@link org.opengis.referencing.operation.CoordinateOperation#getMathTransform()}.</p>
 *
 * {@section How to add custom coordinate operations}
 * To define a custom coordinate operation,
 * one needs to define a <strong>thread-safe</strong> class implementing <strong>both</strong> this
 * {@code MathTransformProvider} interface and the {@link org.opengis.referencing.operation.OperationMethod} one.
 * While not mandatory, we suggest to extend {@link org.apache.sis.referencing.operation.DefaultOperationMethod}.
 * Then the fully-qualified class name of that implementation should be listed in a file having this exact name:
 *
 * {@preformat text
 *     META-INF/services/org.opengis.referencing.operation.OperationMethod
 * }
 *
 * <div class="note"><b>Design note:</b>
 * this {@code MathTransformProvider} interface does not extend {@code OperationMethod} directly
 * in order to allow its usage in other contexts than coordinate operations.</div>
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see org.apache.sis.referencing.operation.DefaultOperationMethod
 * @see DefaultMathTransformFactory
 * @see AbstractMathTransform
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
