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
package org.apache.sis.internal.util;

import java.util.Map;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import java.util.Objects;


/**
 * Provides default implementations of {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()}
 * for a map entry.
 *
 * @param <K> The type of keys maintained by the map.
 * @param <V> The type of mapped values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class AbstractMapEntry<K,V> implements Map.Entry<K,V> {
    /**
     * For subclasses constructors.
     */
    protected AbstractMapEntry() {
    }

    /**
     * Compares the specified object with this entry for equality.
     * Criterion are specified by the {@link Map.Entry} contract.
     *
     * @param object The other object to compare with this entry.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof Map.Entry<?,?>) {
            final Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
            return Objects.equals(getKey(),   entry.getKey()) &&
                   Objects.equals(getValue(), entry.getValue());
        }
        return false;
    }

    /**
     * Returns the hash code value for this map entry.
     * The formula is specified by the {@link Map.Entry} contract.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
    }

    /**
     * Returns a string representation of this entry. If the string representation
     * of the value uses more than one line, then only the first line is shown.
     * This method is mostly for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        String value = String.valueOf(getValue());
        value = value.substring(0, CharSequences.indexOfLineStart(value, 1, 0));
        return String.valueOf(getKey()) + '=' + value;
    }
}
