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

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An instance of an {@linkplain DefaultAttributeType attribute type} containing at most one value.
 * The majority of features types contain attributes restricted to such [0 … 1] cardinality.
 * While {@link MultiValuedAttribute} would be suitable to all cases, this {@code SingletonAttribute}
 * consumes less memory.
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code SingletonAttribute} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 * </ul>
 *
 * @param <V> The type of the attribute value.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultAttributeType
 */
final class SingletonAttribute<V> extends AbstractAttribute<V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2236273725166545505L;

    /**
     * The attribute value, or {@code null} if none.
     */
    private V value;

    /**
     * Creates a new attribute of the given type initialized to the
     * {@linkplain DefaultAttributeType#getDefaultValue() default value}.
     *
     * @param type Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    public SingletonAttribute(final DefaultAttributeType<V> type) {
        super(type);
        assert isSingleton(type.getMaximumOccurs());
        value = type.getDefaultValue();
    }

    /**
     * Creates a new attribute of the given type initialized to the given value.
     * Note that a {@code null} value may not be the same as the default value.
     *
     * @param type  Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     * @param value The initial value (may be {@code null}).
     */
    SingletonAttribute(final DefaultAttributeType<V> type, final Object value) {
        super(type);
        assert isSingleton(type.getMaximumOccurs());
        this.value = type.getValueClass().cast(value);
    }

    /**
     * Returns the attribute value.
     *
     * @return The attribute value (may be {@code null}).
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Sets the attribute value.
     *
     * @param value The new value.
     */
    @Override
    public void setValue(final V value) {
        this.value = value;
    }

    /**
     * Returns a hash code value for this attribute.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + Objects.hashCode(value) + characteristicsReadOnly().hashCode();
    }

    /**
     * Compares this attribute with the given object for equality.
     *
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SingletonAttribute<?>) {
            final SingletonAttribute<?> that = (SingletonAttribute<?>) obj;
            return type.equals(that.type) && Objects.equals(value, that.value) &&
                   characteristicsReadOnly().equals(that.characteristicsReadOnly());
        }
        return false;
    }
}
