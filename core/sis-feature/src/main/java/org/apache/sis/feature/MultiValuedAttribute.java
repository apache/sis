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

import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Field;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * An instance of an {@linkplain DefaultAttributeType attribute type} containing an arbitrary amount of values.
 *
 * <div class="note"><b>Note:</b> in the common case where the {@linkplain DefaultAttributeType attribute type}
 * restricts the cardinality to [0 … 1], the {@link SingletonAttribute} implementation consumes less memory.</div>
 *
 * {@section Limitations}
 * <ul>
 *   <li><b>Multi-threading:</b> {@code MultiValuedAttribute} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 * </ul>
 *
 * @param <T> The type of the attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultAttributeType
 */
final class MultiValuedAttribute<T> extends AbstractAttribute<T> implements Cloneable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7824265855672575215L;

    /**
     * The attribute values.
     */
    private final ArrayList<T> values;

    /**
     * Creates a new attribute of the given type initialized to the
     * {@linkplain DefaultAttributeType#getDefaultValue() default value}.
     *
     * @param type Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    public MultiValuedAttribute(final DefaultAttributeType<T> type) {
        super(type);
        values = new ArrayList<>();
        final T value = type.getDefaultValue();
        if (value != null) {
            values.add(value);
        }
    }

    /**
     * Returns the attribute value, or {@code null} if none.
     *
     * @return The attribute value (may be {@code null}).
     * @throws IllegalStateException if this attribute contains more than one value.
     */
    @Override
    public T getValue() {
        switch (values.size()) {
            case 0:  return null;
            case 1:  return values.get(0);
            default: throw new IllegalStateException(Errors.format(Errors.Keys.NotASingleton_1, getName()));
        }
    }

    /**
     * Returns all attribute values, or an empty collection if none.
     * The returned collection is <cite>live</cite>: changes in the returned collection
     * will be reflected immediately in this {@code Attribute} instance, and conversely.
     *
     * @return The attribute values in a <cite>live</cite> collection.
     */
    @Override
    public Collection<T> getValues() {
        return values;
    }

    /**
     * Sets the attribute value.
     *
     * @param value The new value, or {@code null} for removing all values from this attribute.
     */
    @Override
    public void setValue(final T value) {
        values.clear();
        if (value != null) {
            values.add(value);
        }
    }

    /**
     * Set the attribute values. All previous values are replaced by the given collection.
     *
     * @param values The new values.
     */
    @Override
    public void setValues(final Collection<? extends T> values) {
        ArgumentChecks.ensureNonNull("values", values);
        this.values.clear();
        this.values.addAll(values);
    }

    /**
     * Returns a copy of this attribute.
     * The default implementation returns a <em>shallow</em> copy:
     * the attribute {@linkplain #getValues() values} are <strong>not</strong> cloned.
     * However subclasses may choose to do otherwise.
     *
     * @return A clone of this attribute.
     * @throws CloneNotSupportedException if this attribute can not be cloned.
     *         The default implementation never throw this exception. However subclasses may throw it,
     *         for example on attempt to clone the attribute values.
     */
    @Override
    @SuppressWarnings("unchecked")
    public MultiValuedAttribute<T> clone() throws CloneNotSupportedException {
        final MultiValuedAttribute<T> clone = (MultiValuedAttribute<T>) super.clone();
        try {
            final Field field = MultiValuedAttribute.class.getDeclaredField("values");
            field.setAccessible(true);
            field.set(clone, clone.values.clone());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return clone;
    }

    /**
     * Returns a hash code value for this attribute type.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + values.hashCode();
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
        if (obj instanceof MultiValuedAttribute<?>) {
            final MultiValuedAttribute<?> that = (MultiValuedAttribute<?>) obj;
            return type.equals(that.type) && values.equals(that.values);
        }
        return false;
    }
}
