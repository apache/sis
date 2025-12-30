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

import java.util.List;
import java.util.Collection;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.CloneAccess;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.feature.internal.Resources;


/**
 * An instance of an {@linkplain DefaultAttributeType attribute type} containing an arbitrary number of values.
 *
 * <div class="note"><b>Note:</b> in the common case where the {@linkplain DefaultAttributeType attribute type}
 * restricts the cardinality to [0 … 1], the {@link SingletonAttribute} implementation consumes less memory.</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code MultiValuedAttribute} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 *   <li><b>Cloning:</b> this class support <em>shallow</em> cloning only:
 *       the attribute is cloned, but not the {@linkplain #getValues() value} elements.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <V>  the type of the attribute values.
 *
 * @see DefaultAttributeType
 */
final class MultiValuedAttribute<V> extends AbstractAttribute<V> implements CloneAccess {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7824265855672575215L;

    /**
     * The attribute values.
     */
    @SuppressWarnings("serial")     // Should be an instance of `CheckedArrayList`.
    private List<V> values;

    /**
     * Creates a new attribute of the given type initialized to the
     * {@linkplain DefaultAttributeType#getDefaultValue() default value}.
     *
     * @param  type  information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    public MultiValuedAttribute(final DefaultAttributeType<V> type) {
        super(type);
        values = Containers.newCheckedList(null, type.getValueClass());
        final V value = type.getDefaultValue();
        if (value != null) {
            values.add(value);
        }
    }

    /**
     * Creates a new attribute of the given type initialized to the given values.
     * Note that a {@code null} value may not be the same as the default value.
     *
     * @param  type    information about the attribute (base Java class, domain of values, <i>etc.</i>).
     * @param  values  the initial values, or {@code null} for initializing to an empty list.
     */
    @SuppressWarnings("unchecked")
    MultiValuedAttribute(final DefaultAttributeType<V> type, final Object values) {
        super(type);
        final Class<V> valueClass = type.getValueClass();
        if (values == null) {
            this.values = Containers.newCheckedList(null, valueClass);
        } else {
            final Class<?> actual = ((CheckedContainer<?>) values).getElementType();
            if (actual == valueClass) {
                this.values = (List<V>) values;
            } else {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_3, "values", valueClass, actual));
            }
        }
    }

    /**
     * Returns the attribute value, or {@code null} if none.
     *
     * @return the attribute value (may be {@code null}).
     * @throws IllegalStateException if this attribute contains more than one value.
     */
    @Override
    public V getValue() {
        switch (values.size()) {
            case 0:  return null;
            case 1:  return values.get(0);
            default: throw new IllegalStateException(Resources.format(Resources.Keys.NotASingleton_1, getName()));
        }
    }

    /**
     * Returns all attribute values, or an empty collection if none.
     * The returned collection is <em>live</em>: changes in the returned collection
     * will be reflected immediately in this {@code Attribute} instance, and conversely.
     *
     * @return the attribute values in a <em>live</em> collection.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<V> getValues() {
        return values;      // Intentionally modifiable
    }

    /**
     * Sets the attribute value.
     *
     * @param  value  the new value, or {@code null} for removing all values from this attribute.
     */
    @Override
    public void setValue(final V value) {
        values.clear();
        if (value != null) {
            values.add(value);
        }
    }

    /**
     * Sets the attribute values. All previous values are replaced by the given collection.
     *
     * @param  newValues  the new values.
     */
    @Override
    public void setValues(final Collection<? extends V> newValues) {
        if (newValues != values) {
            ArgumentChecks.ensureNonNull("values", newValues);      // The parameter name in public API is "values".
            values.clear();
            values.addAll(newValues);
        }
    }

    /**
     * Returns a copy of this attribute.
     * This implementation returns a <em>shallow</em> copy:
     * the attribute {@linkplain #getValues() values} are <strong>not</strong> cloned.
     *
     * @return a clone of this attribute.
     * @throws CloneNotSupportedException if this attribute cannot be cloned.
     */
    @Override
    @SuppressWarnings("unchecked")
    public AbstractAttribute<V> clone() throws CloneNotSupportedException {
        final var clone = (MultiValuedAttribute<V>) super.clone();
        clone.values = (List<V>) ((CloneAccess) clone.values).clone();
        return clone;
    }

    /**
     * Returns a hash code value for this attribute.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + values.hashCode() + characteristicsReadOnly().hashCode();
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
            return type.equals(that.type) && values.equals(that.values) &&
                   characteristicsReadOnly().equals(that.characteristicsReadOnly());
        }
        return false;
    }
}
