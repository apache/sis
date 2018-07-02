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


/**
 * Computes a hash code for the specified metadata. The hash code is defined as the sum of hash codes
 * of all non-empty properties, plus the hash code of the interface. This is a similar contract than
 * {@link java.util.Set#hashCode()} (except for the interface) and ensures that the hash code value
 * is insensitive to the ordering of properties.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class HashCode extends MetadataVisitor<Integer> {
    /**
     * Provider of visitor instances used by {@link MetadataStandard#hashCode(Object)}.
     */
    private static final ThreadLocal<HashCode> VISITORS = ThreadLocal.withInitial(HashCode::new);

    /**
     * The hash code value, returned by {@link MetadataStandard#hashCode(Object)} after calculation.
     */
    private int code;

    /**
     * Instantiated by {@link #VISITORS} only.
     */
    private HashCode() {
    }

    /**
     * Returns the visitor for the current thread if it already exists, or creates a new one otherwise.
     */
    static HashCode getOrCreate() {
        return VISITORS.get();
    }

    /**
     * Returns the thread-local variable that created this {@code HashCode} instance.
     */
    @Override
    final ThreadLocal<HashCode> creator() {
        return VISITORS;
    }

    /**
     * Resets the hash code to an initial value for a new metadata instance.
     * If another hash code computation was in progress, that code shall be saved before this method is invoked.
     *
     * @param  accessor  contains the standard interface of the metadata for which a hash code value will be computed.
     * @return {@link Filter#NON_EMPTY} since this visitor is not restricted to writable properties.
     */
    @Override
    Filter preVisit(final PropertyAccessor accessor) {
        code = accessor.type.hashCode();
        return Filter.NON_EMPTY;
    }

    /**
     * Adds the hash code of the given metadata property value. Invoking this method may cause recursive calls
     * to {@code HashCode} methods on this visitor instance if the given value is itself another metadata object.
     *
     * @param  value  the metadata property value for which to add hash code.
     * @return {@code null}, meaning to not modify the metadata property value.
     */
    @Override
    Object visit(final Class<?> type, final Object value) {
        if (!ValueExistencePolicy.isNullOrEmpty(value)) {
            /*
             * We make a local copy of the 'code' field in the 'c' local variable
             * because the call to 'value.hashCode()' may cause a recursive call
             * to the methods of this visitor, which would overwrite 'code'.
             */
            int c = code;
            c += value.hashCode();
            code = c;
        }
        return value;
    }

    /**
     * Returns the hash code result after visiting all elements in a metadata instance.
     */
    @Override
    Integer result() {
        return code;
    }
}
