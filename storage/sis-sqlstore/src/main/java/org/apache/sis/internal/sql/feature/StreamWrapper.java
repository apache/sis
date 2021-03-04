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
package org.apache.sis.internal.sql.feature;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;


/**
 * Allows delegation for a subset of behaviors of a stream to a custom component.
 * The default implementations of methods in this class delegate {@link Stream} operations to the stream provided
 * by {@link #createSource()}. Implementations in subclasses can short-circuit some methods by overriding them
 * with their own solution.
 *
 * <div class="note"><b>Example:</b>
 * if a streamable dataset whose count can be provided efficiently by a third-party tool (typically COUNT query
 * on SQL databases), a {@link StreamWrapper} subclass can override the {@link #count()} method for running
 * the count query instead of counting elements of the stream manually.
 *
 * Another example would be intermediate operations. Let's keep SQL queries as example. If a stream is executing
 * a statement on terminal operations, a subclasses can override {@link #limit(long)} and {@link #skip(long)}
 * methods to set LIMIT and OFFSET criteria in an SQL query.</div>
 *
 * For an advanced example, see the {@link org.apache.sis.internal.sql.feature.StreamSQL} class.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 *
 * @param  <T>  the type of objects contained in the stream, as specified in {@link Stream} interface.
 *
 * @since 1.1
 * @module
 */
abstract class StreamWrapper<T> extends BaseStreamWrapper<T, Stream<T>> implements Stream<T> {
    /**
     * For subclass constructor.
     */
    protected StreamWrapper() {
    }

    /**
     * Returns a stream with elements of this stream that match the given predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return getSource().filter(predicate);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return getSource().map(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return getSource().mapToInt(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return getSource().mapToLong(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return getSource().mapToDouble(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return getSource().flatMap(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return getSource().flatMapToInt(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return getSource().flatMapToLong(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return getSource().flatMapToDouble(mapper);
    }

    /**
     * Returns a stream with distinct elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> distinct() {
        return getSource().distinct();
    }

    /**
     * Returns a stream with elements of this stream sorted in natural order.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> sorted() {
        return getSource().sorted();
    }

    /**
     * Returns a stream with elements of this stream sorted using the given comparator.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return getSource().sorted(comparator);
    }

    /**
     * Returns a stream performing the specified action on each element when consumed.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        return getSource().peek(action);
    }

    /**
     * Returns a stream with truncated at the given number of elements.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> limit(long maxSize) {
        return getSource().limit(maxSize);
    }

    /**
     * Returns a stream discarding the specified number of elements.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public Stream<T> skip(long n) {
        return getSource().skip(n);
    }

    /**
     * Performs an action for each element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        getSource().forEach(action);
    }

    /**
     * Performs an action for each element of this stream in encounter order.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        getSource().forEachOrdered(action);
    }

    /**
     * Returns all elements in an array.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Object[] toArray() {
        return getSource().toArray();
    }

    /**
     * Returns all elements in an array.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return getSource().toArray(generator);
    }

    /**
     * Performs a reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return getSource().reduce(identity, accumulator);
    }

    /**
     * Performs a reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return getSource().reduce(accumulator);
    }

    /**
     * Performs a reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return getSource().reduce(identity, accumulator, combiner);
    }

    /**
     * Performs a mutable reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return getSource().collect(supplier, accumulator, combiner);
    }

    /**
     * Performs a mutable reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return getSource().collect(collector);
    }

    /**
     * Returns the minimum element of this stream according to the provided comparator.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return getSource().min(comparator);
    }

    /**
     * Returns the maximum element of this stream according to the provided comparator.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return getSource().max(comparator);
    }

    /**
     * Returns the number of elements in this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public long count() {
        return getSource().count();
    }

    /**
     * Returns whether at least one element of this stream matches the provided predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return getSource().anyMatch(predicate);
    }

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return getSource().allMatch(predicate);
    }

    /**
     * Returns whether none element of this stream match the provided predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return getSource().noneMatch(predicate);
    }

    /**
     * Returns the first element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Optional<T> findFirst() {
        return getSource().findFirst();
    }

    /**
     * Returns any element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Optional<T> findAny() {
        return getSource().findAny();
    }
}
