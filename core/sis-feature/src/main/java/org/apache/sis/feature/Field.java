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

import java.util.Collection;
import java.util.Iterator;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of property that can be stored in a {@link AbstractFeature} instance.
 * This include {@code Attribute} and {@code Association}, but not {@code Operation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
abstract class Field<V> extends Property {
    /**
     * For subclass constructors.
     */
    Field() {
    }

    /**
     * Returns {@code true} if an attribute type or association role having the given
     * maximum number of occurrences should be treated as a singleton.
     *
     * This method gives us a simple keyword to search for every places in the code
     * where a decision regarding "singleton versus multi-valued" is made.
     */
    static boolean isSingleton(final int maximumOccurs) {
        return maximumOccurs <= 1;
    }

    /**
     * Returns the field feature or attribute value, or {@code null} if none.
     *
     * @return The feature or attribute value (may be {@code null}).
     * @throws IllegalStateException if this field contains more than one value.
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    @Override
    public abstract V getValue() throws IllegalStateException;

    /**
     * Returns all features or attribute values, or an empty collection if none.
     * The returned collection is <cite>live</cite>: changes in the returned collection
     * will be reflected immediately in this {@code Field} instance, and conversely.
     *
     * @return The features or attribute values in a <cite>live</cite> collection.
     */
    public Collection<V> getValues() {
        return new PropertySingleton<V>(this);
    }

    /**
     * Sets the feature or attribute value. All previous values are replaced by the given singleton.
     *
     * @param value The new value, or {@code null} for removing all values from this field.
     *
     * @see AbstractFeature#setPropertyValue(String, Object)
     */
    public abstract void setValue(final V value);

    /**
     * Sets the features or attribute values. All previous values are replaced by the given collection.
     *
     * <p>The default implementation ensures that the given collection contains at most one element,
     * then delegates to {@link #setValue(Object)}.</p>
     *
     * @param values The new values.
     * @throws IllegalArgumentException if the given collection contains too many elements.
     */
    public void setValues(final Collection<? extends V> values) throws IllegalArgumentException {
        V value = null;
        ArgumentChecks.ensureNonNull("values", values);
        final Iterator<? extends V> it = values.iterator();
        if (it.hasNext()) {
            value = it.next();
            if (it.hasNext()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.TooManyOccurrences_2, 1, getName()));
            }
        }
        setValue(value);
    }
}
