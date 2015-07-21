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

import java.io.Serializable;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.apache.sis.referencing.IdentifiedObjects;


/**
 * Placeholder for a mandatory parameter value which has not yet been initialized.
 * {@code UninitializedParameter} are immutable and contains only the descriptor of
 * the parameter to initialize. {@code UninitializedParameter}s are replaced by the
 * actual parameter when first needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@SuppressWarnings("CloneInNonCloneableClass")
final class UninitializedParameter implements GeneralParameterValue, Serializable { // Intentionally non-Cloneable.
    /**
     * For cross-version serialization compatibility.
     */
    private static final long serialVersionUID = 4664809449434987422L;

    /**
     * The descriptor of the parameter to initialize.
     */
    private final GeneralParameterDescriptor descriptor;

    /**
     * Creates a new {@code UninitializedParameter} for the given descriptor.
     */
    UninitializedParameter(final GeneralParameterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Returns the descriptor of the parameter to initialize.
     */
    @Override
    public GeneralParameterDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns {@code this} since there is no need to clone this object.
     */
    @Override
    public GeneralParameterValue clone() {
        return this;
    }

    /**
     * Returns a string representation of this parameter.
     */
    @Override
    public String toString() {
        return "Parameter[\"" + IdentifiedObjects.toString(descriptor.getName()) + "\"]";
    }
}
