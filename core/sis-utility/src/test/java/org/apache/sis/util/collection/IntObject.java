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
package org.apache.sis.util.collection;


/**
 * A wrapper for {@code int} value as a Java object (not a value class) and without caching.
 * This is used for tests that use {@link java.lang.ref.WeakReference}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class IntObject {
    /**
     * The value.
     */
    final int value;

    /**
     * Creates a new wrapper for the given value.
     */
    IntObject(final int value) {
        this.value = value;
    }

    /**
     * Returns a hash code based on the value.
     */
    @Override
    public int hashCode() {
        return value;
    }

    /**
     * Compares the given object with this object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof IntObject) && ((IntObject) other).value == value;
    }

    /**
     * Returns a string representation of the integer value.
     */
    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
