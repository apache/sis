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
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * The base class of single parameter value or group of parameter values.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public abstract class AbstractParameterValue extends FormattableObject
        implements GeneralParameterValue, Serializable, Cloneable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8458179223988766398L;

    /**
     * The abstract definition of this parameter or group of parameters.
     */
    final GeneralParameterDescriptor descriptor;

    /**
     * Creates a parameter value from the specified descriptor.
     *
     * @param descriptor The abstract definition of this parameter or group of parameters.
     */
    protected AbstractParameterValue(final GeneralParameterDescriptor descriptor) {
        ensureNonNull("descriptor", descriptor);
        this.descriptor = descriptor;
    }

    /**
     * Creates a new instance initialized with the values from the specified parameter object.
     * This is a <em>shallow</em> copy constructor, since the values contained in the given
     * object are not cloned.
     *
     * @param parameter The parameter to copy values from.
     */
    protected AbstractParameterValue(final GeneralParameterValue parameter) {
        ensureNonNull("parameter", parameter);
        descriptor = parameter.getDescriptor();
        if (descriptor == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForProperty_1, "descriptor"));
        }
    }

    /**
     * Returns the abstract definition of this parameter or group of parameters.
     *
     * @return The abstract definition of this parameter or group of parameters.
     */
    @Override
    public GeneralParameterDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns a hash value for this parameter.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return descriptor.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Compares the given object with this parameter for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            return descriptor.equals(((AbstractParameterValue) object).descriptor);
        }
        return false;
    }

    /**
     * Returns a copy of this parameter value or group.
     *
     * @return A clone of this parameter value or group.
     */
    @Override
    public AbstractParameterValue clone() {
        try {
            return (AbstractParameterValue) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception); // Should not happen, since we are cloneable
        }
    }

    @Override
    protected String formatTo(final Formatter formatter) {
        return null;
    }
}
