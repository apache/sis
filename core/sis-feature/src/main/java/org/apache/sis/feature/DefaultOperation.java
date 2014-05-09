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
package org.apache.sis.feature;

import java.util.Map;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.util.ArgumentChecks;


/**
 * Describes the behaviour of a feature type as a function or a method.
 * Operations can:
 *
 * <ul>
 *   <li>Compute values from the attributes.</li>
 *   <li>Perform actions that change the attribute values.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b> a mutator operation may raise the height of a dam. This changes
 * may affect other properties like the watercourse and the reservoir associated with the dam.</div>
 *
 * <div class="warning"><b>Warning:</b> this class is experimental and may change after we gained more
 * experience on this aspect of ISO 19109.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultOperation extends PropertyType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6300319108116735764L;

    /**
     * A description of the input parameters.
     */
    private final ParameterDescriptorGroup parameters;

    /**
     * The type of the result, or {@code null} if none.
     */
    private final DefaultAttributeType<?> result;

    /**
     * Constructs an attribute type from the given properties. The properties map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     *
     * @param properties The name and other properties to be given to this operation.
     * @param parameters A description of the input parameters.
     * @param result     The type of the result, or {@code null} if none.
     */
    public DefaultOperation(final Map<String,?> properties,
            final ParameterDescriptorGroup parameters, final DefaultAttributeType<?> result)
    {
        super(properties);
        ArgumentChecks.ensureNonNull("inputs", parameters);
        this.parameters = parameters;
        this.result     = result;
    }

    /**
     * Returns a description of the input parameters.
     *
     * @return Description of the input parameters.
     */
    public ParameterDescriptorGroup getParameters() {
        return parameters;
    }

    /**
     * Returns the expected result type, or {@code null} if none.
     *
     * @return The type of the result, or {@code null} if none.
     */
    public DefaultAttributeType<?> getResult() {
        return result;
    }
}
