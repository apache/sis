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
package org.apache.sis.util.internal.shared;

import java.util.Objects;
import java.util.AbstractCollection;
import org.apache.sis.util.ArraysExt;


/**
 * A collection in which elements order does not matter (as in {@link java.util.Set})
 * but in which duplicated elements are allowed (as in {@link java.util.List}).
 * The "bag" word is used in ISO specifications for such kind of collection.
 * This base class is suitable to collection returned by {@link java.util.Map#values()};
 * it is not necessarily a good fit for all other subtypes of {@link AbstractCollection}.
 *
 * <p>This abstract class implements the {@link #equals(Object)} and {@link #hashCode()} methods.
 * Subclasses need to override at least {@link #size()} and {@link #iterator()}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <E>  type of elements in this bag.
 */
public abstract class Bag<E> extends AbstractCollection<E> {
    /**
     * Creates a new instance.
     */
    protected Bag() {
    }

    /**
     * Compares this bag with the given object for equality. This method performs comparisons only
     * with instances of {@code Bag}, and returns {@code false} for all other kinds of collection.
     * We do <strong>not</strong> compare with arbitrary collection implementations.
     *
     * <p><b>Rational:</b> {@link java.util.Collection#equals(Object)} contract explicitly forbids comparisons
     * with {@code List} and {@code Set}. The rational explained in {@code Collection} javadoc applies also to
     * other kind of {@code Collection} implementations: we cannot enforce {@code Collection.equals(Object)}
     * to be symmetric in such cases.</p>
     *
     * @param  other  the other object to compare with this bag.
     * @return {@code true} if the two bags are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Bag) {
            int size = size();
            if (size == ((Bag) other).size()) {
                final Object[] elements = toArray();
                ArraysExt.reverse(elements);
compare:        for (final Object oe : (Bag) other) {
                    for (int i=size; --i >= 0;) {
                        if (Objects.equals(elements[i], oe)) {
                            System.arraycopy(elements, i+1, elements, i, --size - i);
                            continue compare;
                        }
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this bag, ignoring element order.
     * This method computes hash code in the same way as {@link java.util.Set}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        int code = 0;
        for (final Object value : this) {
            if (value != null) {
                code += value.hashCode();
            }
        }
        return code;
    }
}
