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
package org.apache.sis.storage.base;

import java.util.Map;
import java.util.Arrays;


/**
 * Workaround for the use of {@code long[]} arrays as keys in a hash map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ArrayOfLongs {
    /**
     * The array.
     */
    private final long[] array;

    /**
     * Creates a new key.
     *
     * @param array the array.
     */
    public ArrayOfLongs(final long[] array) {
        this.array = array;
    }

    /**
     * Returns a unique instance of the array wrapped by this key.
     *
     * @param  sharedInstances  a map containing shared instances of arrays.
     * @return a shared instance of the array wrapped by this key.
     * @throws ClassCastException if this key is associated in given map to an object other than a {@code long[]}.
     */
    public long[] unique(final Map<? super ArrayOfLongs, ? super long[]> sharedInstances) {
        Object existing = sharedInstances.putIfAbsent(this, array);
        return (existing != null) ? (long[]) existing : array;
    }

    /**
     * Returns whether the given object is a key wrapping
     * an array equals to the array wrapped by this key.
     *
     * @param  other  the other object to compare with this key.
     * @return whether the two objects are wrapping equal arrays.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof ArrayOfLongs) && Arrays.equals(array, ((ArrayOfLongs) other).array);
    }

    /**
     * Returns a hash code value for this key.
     *
     * @return the array hash code.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    /**
     * Returns a string representation of the wrapped array.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Arrays.toString(array);
    }
}
