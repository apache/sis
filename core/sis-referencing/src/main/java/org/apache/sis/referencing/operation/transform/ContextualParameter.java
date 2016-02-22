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

import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterValue;


/**
 * A parameter value stored in {@link ContextualParameters} before they are made {@linkplain #unmodifiable unmodifiable}.
 * This temporary {@code ParameterValue} bypasses the validity check normally performed by {@link DefaultParameterValue}.
 *
 * <div class="note"><b>Rational:</b>
 * The intend is to skip the parameter value verification done by {@link DefaultParameterValue#setValue(Object, Unit)}
 * on the assumption that the value has already been verified when the user created his {@code ParameterValueGroup}.
 * Even if the user's {@code ParameterValue} implementation did not performed any verification, there is chances that
 * {@link DefaultMathTransformFactory} {@linkplain org.apache.sis.parameter.Parameters#copy copied} the parameters in
 * instances of the {@link org.apache.sis.parameter} package that do the checks.
 *
 * <p>Skipping redundant verifications allows us to avoid redundant logging messages when
 * the {@link org.apache.sis.internal.system.Semaphores#SUSPEND_PARAMETER_CHECK} flag is set.
 * Furthermore it is a little bit late for checking parameter validity here; that verification should have been done
 * at {@link MathTransform} construction time or even before, and the job of the {@link ContextualParameters} class
 * is just to record what have been used.</p></div>
 *
 * Note that the {@link ContextualParameters#freeze()} method will replace all {@code ParameterValue} instances by
 * {@code UnmodifiableParameterValue} instances anyway. So no matter which temporary instance we used, we will end
 * with the same objects in memory anyway.
 *
 * @param <T> The type of the value stored in this parameter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class ContextualParameter<T> extends DefaultParameterValue<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2886149929114558478L;

    /**
     * Creates a parameter value from the specified descriptor.
     * The value will be initialized to the default value, if any.
     *
     * @param descriptor The abstract definition of this parameter.
     */
    ContextualParameter(final ParameterDescriptor<T> descriptor) {
        super(descriptor);
    }

    /**
     * Sets the parameter value and its associated unit without any verification of parameter validity
     * (except value type).
     *
     * @param  value The parameter value, or {@code null} to restore the default.
     * @param  unit  The unit associated to the new parameter value, or {@code null}.
     */
    @Override
    protected void setValue(final Object value, final Unit<?> unit) {
        if (value != null) {
            this.value = getDescriptor().getValueClass().cast(value);
        } else {
            this.value = getDescriptor().getDefaultValue();
        }
        this.unit = unit;
    }
}
