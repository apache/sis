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

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;


/**
 * A specialization of {@link BaseStreamWrapper} for {@linkplain DoubleStream streams of double value}.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class DoubleStreamWrapper extends BaseStreamWrapper<Double, DoubleStream> implements DoubleStream {
    /**
     * For subclass constructors.
     */
    protected DoubleStreamWrapper() {
    }

    /**
     * Returns a stream with elements of this stream that match the given predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        return getSource().filter(predicate);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        return getSource().map(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return getSource().mapToObj(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        return getSource().mapToInt(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        return getSource().mapToLong(mapper);
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return getSource().flatMap(mapper);
    }

    /**
     * Returns a stream with distinct elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream distinct() {
        return getSource().distinct();
    }

    /**
     * Returns a stream with elements of this stream sorted in ascending order.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream sorted() {
        return getSource().sorted();
    }

    /**
     * Returns a stream performing the specified action on each element when consumed.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream peek(DoubleConsumer action) {
        return getSource().peek(action);
    }

    /**
     * Returns a stream with truncated at the given number of elements.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream limit(long maxSize) {
        return getSource().limit(maxSize);
    }

    /**
     * Returns a stream discarding the specified number of elements.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public DoubleStream skip(long n) {
        return getSource().skip(n);
    }

    /**
     * Performs an action for each element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public void forEach(DoubleConsumer action) {
        getSource().forEach(action);
    }

    /**
     * Performs an action for each element of this stream in encounter order.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public void forEachOrdered(DoubleConsumer action) {
        getSource().forEachOrdered(action);
    }

    /**
     * Returns all elements in an array.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public double[] toArray() {
        return getSource().toArray();
    }

    /**
     * Performs a reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        return getSource().reduce(identity, op);
    }

    /**
     * Performs a reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        return getSource().reduce(op);
    }

    /**
     * Performs a mutable reduction on the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return getSource().collect(supplier, accumulator, combiner);
    }

    /**
     * Returns the sum of elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public double sum() {
        return getSource().sum();
    }

    /**
     * Returns the minimum element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public OptionalDouble min() {
        return getSource().min();
    }

    /**
     * Returns the maximum element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public OptionalDouble max() {
        return getSource().max();
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
     * Returns the average of elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public OptionalDouble average() {
        return getSource().average();
    }

    /**
     * Returns statistics about elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        return getSource().summaryStatistics();
    }

    /**
     * Returns whether at least one element of this stream matches the provided predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        return getSource().anyMatch(predicate);
    }

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean allMatch(DoublePredicate predicate) {
        return getSource().allMatch(predicate);
    }

    /**
     * Returns whether none element of this stream match the provided predicate.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return getSource().noneMatch(predicate);
    }

    /**
     * Returns the first element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public OptionalDouble findFirst() {
        return getSource().findFirst();
    }

    /**
     * Returns any element of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public OptionalDouble findAny() {
        return getSource().findAny();
    }

    /**
     * Returns the stream elements as {@link Double} elements.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Stream<Double> boxed() {
        return getSource().boxed();
    }

    /**
     * Returns an iterator for the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return getSource().iterator();
    }

    /**
     * Returns a spliterator for the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Spliterator.OfDouble spliterator() {
        return getSource().spliterator();
    }
}
