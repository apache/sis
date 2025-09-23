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

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import org.apache.sis.util.ConditionallySafe;


/**
 * A central place where to put unsafe methods. Those method should be invoked only in contexts
 * where the warning cannot be fully resolved. The use of this class allow maintainers to trace
 * the Apache SIS codes where potentially unsafe operations still exist despite
 * {@code @SuppressWarnings("unchecked")} annotation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Unsafe {
    /**
     * Do not allow instantiation of this class.
     */
    private Unsafe() {
    }

    /**
     * Adds all elements from one collection to the other. This method bypasses the compiler type safety checks.
     * This method should be invoked only when, while not sure, the caller thinks that there is reasonably good
     * reasons to assume that the element types have been or will be verified. For example, it may be a context
     * where Apache SIS uses {@linkplain java.util.Collections#checkedCollection checked collection} by default
     * but cannot guarantee that the users didn't override with a non-checked collection.
     *
     * @param  target    the collection where to add elements. Should preferably be a checked collection.
     * @param  elements  the elements to add.
     * @return whether {@code target} changed as a result of this operation.
     */
    @ConditionallySafe
    @SuppressWarnings("unchecked")
    public static boolean addAll(Collection<?> target, Collection<?> elements) {
        return ((Collection) target).addAll(elements);
    }

    /**
     * Sets the element at the given index in the given list. This method bypasses the compiler type safety checks.
     * This method should be invoked only when the caller has done its best effort for ensuring that the element is
     * an instance of the required type, or when the target list has good chances to be a checked collection.
     *
     * @param  target   the list where to set the element. Should preferably be a checked collection.
     * @param  index    index of the element to set.
     * @param  element  the element to set in the list.
     * @return the previous element at the given index.
     */
    @ConditionallySafe
    @SuppressWarnings("unchecked")
    public static Object set(List<?> target, int index, Object element) {
        return ((List) target).set(index, element);
    }

    /**
     * Merges the value associated to the given key. This method bypasses the compiler type safety checks.
     * This method should be invoked only when the caller has done its best effort for ensuring that the
     * value and the merge function have compatible types.
     *
     * @param  target  the map where to merge a value.
     * @param  key     key of the value to merge.
     * @param  value   value to merge.
     * @param  remappingFunction the function to apply for merging the given value with the existing one.
     * @return the new value associated to the key.
     */
    @ConditionallySafe
    @SuppressWarnings("unchecked")
    public static Object merge(Map<?,?> target, Object key, Object value, BiFunction<?,?,?> remappingFunction) {
        return ((Map) target).merge(key, value, remappingFunction);
    }

    /**
     * Sets the value of a map entry. This method bypasses the compiler type safety checks.
     * This method should be invoked only when the caller has done its best effort for ensuring
     * that the value is an instance of the expected type, or if the map is a checked collection.
     *
     * @param  target  the entry where to set a value. Should be an entry from a checked collection.
     * @param  value   the value to set.
     * @return the previous value.
     */
    @ConditionallySafe
    @SuppressWarnings("unchecked")
    public static Object setValue(Map.Entry<?,?> target, Object value) {
        return ((Map.Entry) target).setValue(value);
    }
}
