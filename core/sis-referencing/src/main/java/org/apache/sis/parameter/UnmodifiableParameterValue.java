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

import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValue;
import org.apache.sis.internal.util.Cloner;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.resources.Errors;


/**
 * A parameter value which can not be modified. This implementation shall be used only with:
 *
 * <ul>
 *   <li>immutable {@linkplain #getDescriptor() descriptor},</li>
 *   <li>immutable or null {@linkplain #getUnit() unit}, and</li>
 *   <li>immutable or {@linkplain Cloneable cloneable} parameter {@linkplain #getValue() value}.</li>
 * </ul>
 *
 * If the parameter value implements the {@link Cloneable} interface and has a public {@code clone()} method,
 * then that value will be cloned every time the {@link #getValue()} method is invoked.
 * The value is not cloned by this method however; it is caller's responsibility to not modify the value of
 * the given {@code parameter} instance after this method call.
 *
 * <div class="section">Instances sharing</div>
 * If the {@link #create(ParameterValue)} method is invoked more than once with equal descriptor, value and unit,
 * then the method will return the same {@code UnmodifiableParameterValue} instance on a <cite>best effort</cite>
 * basis.
 *
 * <div class="note"><b>Rational:</b>
 * the same parameter value is often used in many different coordinate operations. For example all <cite>Universal
 * Transverse Mercator</cite> (UTM) projections use the same scale factor (0.9996) and false easting (500000 metres).
 * </div>
 *
 * @param <T> The type of the value stored in this parameter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
final class UnmodifiableParameterValue<T> extends DefaultParameterValue<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4760030766220872555L;

    /**
     * Pool of parameter instances created in this running JVM.
     * See class javadoc for a rational about why we use a pool.
     */
    @SuppressWarnings("rawtypes")
    private static final WeakHashSet<UnmodifiableParameterValue> POOL =
            new WeakHashSet<UnmodifiableParameterValue>(UnmodifiableParameterValue.class);

    /**
     * Creates a new parameter with the same value than the given one.
     */
    private UnmodifiableParameterValue(final ParameterValue<T> value) {
        super(value);
    }

    /**
     * Returns an unmodifiable implementation of the given parameter value.
     * See class javadoc for more information.
     *
     * @param  <T> The type of the value stored in the given parameter.
     * @param  parameter The parameter to make unmodifiable, or {@code null}.
     * @return An unmodifiable implementation of the given parameter, or {@code null} if the given parameter was null.
     */
    static <T> UnmodifiableParameterValue<T> create(final ParameterValue<T> parameter) {
        if (parameter == null || parameter instanceof UnmodifiableParameterValue<?>) {
            return (UnmodifiableParameterValue<T>) parameter;
        } else {
            return POOL.unique(new UnmodifiableParameterValue<T>(parameter));
        }
    }

    /**
     * If the value is cloneable, clones it before to return it.
     */
    @Override
    public T getValue() {
        T value = super.getValue();
        if (value instanceof Cloneable) try {
            value = getDescriptor().getValueClass().cast(Cloner.cloneIfPublic(value));
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.CloneNotSupported_1, value.getClass()), e);
        }
        return value;
    }

    /**
     * Do not allow modification of the parameter value.
     */
    @Override
    protected void setValue(final Object value, final Unit<?> unit) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, getClass()));
    }

    /**
     * Returns a modifiable copy of this parameter.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public DefaultParameterValue<T> clone() {
        return new DefaultParameterValue<T>(this);
    }
}
