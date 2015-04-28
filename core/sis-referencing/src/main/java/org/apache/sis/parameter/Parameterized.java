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
package org.apache.sis.parameter;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * An object which can supply its parameters in a {@link ParameterValueGroup}.
 * All Apache SIS implementations of {@link org.opengis.referencing.operation.MathTransform}
 * implement this interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public interface Parameterized {
    /**
     * Returns the parameter descriptors for this parameterized object, or {@code null} if unknown.
     *
     * @return The parameter descriptors for this object, or {@code null}.
     */
    ParameterDescriptorGroup getParameterDescriptors();

    /**
     * Returns the parameter values for this parameterized object, or {@code null} if unknown.
     *
     * <div class="section">Modifying parameter values</div>
     * Unless explicitely allowed by the implementation class, callers should not modify the values
     * returned by this method. Implementors are encouraged to protect their internal data by returning
     * an unmodifiable view or a copy of their parameters. If the caller wishes to edit parameter values,
     * then (s)he should {@linkplain DefaultParameterValueGroup#clone() clone} the parameters before to
     * modify them, then use the modified parameters for creating a new {@code Parameterized} object.
     *
     * @return The parameter values for this object, or {@code null} if unknown.
     */
    ParameterValueGroup getParameterValues();
}
