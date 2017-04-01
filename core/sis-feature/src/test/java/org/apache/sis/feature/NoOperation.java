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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Property;


/**
 * An operation that does nothing.
 * This is used for testing purpose only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
@SuppressWarnings("serial")
final strictfp class NoOperation extends AbstractOperation {
    /**
     * A description of the input parameters.
     */
    private final ParameterDescriptorGroup parameters;

    /**
     * The type of the result, or {@code null} if none.
     */
    private final IdentifiedType result;

    /**
     * Constructs an operation from the given properties. The identification map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     *
     * @param  identification  the name and other information to be given to this operation.
     * @param  parameters      a description of the input parameters.
     * @param  result          the type of the result, or {@code null} if none.
     */
    NoOperation(final Map<String,?> identification,
            final ParameterDescriptorGroup parameters, final IdentifiedType result)
    {
        super(identification);
        this.parameters = parameters;
        this.result     = result;
    }

    /**
     * Returns a description of the input parameters.
     *
     * @return description of the input parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return parameters;
    }

    /**
     * Returns the expected result type, or {@code null} if none.
     *
     * @return the type of the result, or {@code null} if none.
     */
    @Override
    public IdentifiedType getResult() {
        return result;
    }

    /**
     * Do nothing.
     *
     * @return {@code null}
     */
    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return null;
    }
}
