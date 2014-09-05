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
package org.apache.sis.metadata;

import java.util.Set;
import java.util.HashSet;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;


/**
 * A pair of objects in process of being compared by the {@code MetadataStandard.equals(…)} method.
 * We have to remember those pairs for avoiding infinite recursivity when comparing metadata objects
 * having cyclic associations. The objects are compared using the identity comparison.
 * Object order is not significant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class ObjectPair {
    /**
     * The set of objects currently in process of being compared.
     */
    static final ThreadLocal<Set<ObjectPair>> CURRENT = new ThreadLocal<Set<ObjectPair>>() {
        @Override protected Set<ObjectPair> initialValue() {
            return new HashSet<ObjectPair>();
        }
    };

    /**
     * The pair of objects in process of being compared.
     */
    private final Object o1, o2;

    /**
     * Creates a new pair of objects being compared.
     */
    ObjectPair(final Object o1, final Object o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    /**
     * Returns a hash code value for this pair of objects.
     * The hash code value shall be insensitive to the objects order.
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(o1) ^ System.identityHashCode(o2);
    }

    /**
     * Compares the given object with this pair for equality.
     * The comparison shall be insensitive to the objects order.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ObjectPair) {
            final ObjectPair that = (ObjectPair) other;
            return (o1 == that.o1 && o2 == that.o2) ||
                   (o1 == that.o2 && o2 == that.o1);
        }
        return false;
    }

    /**
     * Returns a string representation of the object pair for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return '(' + Classes.getShortClassName(o1) + ", " + Classes.getShortClassName(o2) + ')';
    }
}
