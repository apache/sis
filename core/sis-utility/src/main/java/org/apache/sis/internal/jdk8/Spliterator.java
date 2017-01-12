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
package org.apache.sis.internal.jdk8;


/**
 * Placeholder for the {@link java.util.Spliterator} interface.
 *
 * @param <T> type of values on which to iterate.
 */
public abstract interface Spliterator<T> {
    /**
     * Flag for iterators that return the values in some specified order.
     */
    public static final int ORDERED = 0x10;

    /**
     * Flag for iterators that do not return null values.
     */
    public static final int NONNULL = 0x100;

    /**
     * Flag telling that no element addition, replacement or removal will happen during iteration.
     */
    public static final int IMMUTABLE  = 0x400;

    /**
     * Performs the given action on it on the next element, if it exists.
     *
     * @param  action  the action to execute.
     * @return whether an element existed.
     */
    public abstract boolean tryAdvance(Consumer<? super T> action);

    /**
     * Performs the given action on all remaining elements.
     *
     * @param  action  the action to execute.
     */
    public abstract void forEachRemaining(Consumer<? super T> action);

    /**
     * Partitions this iterator if possible.
     *
     * @return an iterator over part of the elements, or {@code null}.
     */
    public abstract Spliterator<T> trySplit();

    /**
     * Returns an estimation of the amount of remaining elements.
     *
     * @return remaining elements count estimation.
     */
    public abstract long estimateSize();

    /**
     * Returns a mask about whether the iterator is concurrent, etc.
     *
     * @return mask of the flags defined above.
     */
    public abstract int characteristics();
}
