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
package org.apache.sis.internal.stream;

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
 * A stream which delegates all operations to another stream of floating point values.
 * This is equivalent to {@link StreamWrapper} but specialized for {@code double} primitive type.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 *
 * @todo Add the methods that are new in JDK16.
 */
public abstract class DoubleStreamWrapper extends BaseStreamWrapper<Double, DoubleStream> implements DoubleStream {
    /**
     * The source of elements, or {@code null} if this {@code DoubleStreamWrapper} is no longer the active stream.
     * This is initialized at creation time and set to {@code null} after it has been determined that all subsequent
     * operations shall be done on a new stream instead of {@code this} stream.
     */
    private DoubleStream source;

    /**
     * Creates a new wrapper for the given stream.
     *
     * @param  source  the stream to wrap.
     */
    protected DoubleStreamWrapper(final DoubleStream source) {
        super(source);
        this.source = source;
    }

    /**
     * Verifies that this stream is still the active one, then returns the source of this stream.
     *
     * @return the stream containing actual data.
     * @throws IllegalStateException if this stream is no longer the active stream on which to apply operations.
     */
    @Override
    final DoubleStream source() {
        if (source != null) {
            return source;
        }
        throw inactive();
    }

    /**
     * Same as {@link #source()} but marks this stream is inactive.
     * All subsequent operations must be done on the returned stream.
     * This is used for {@code map(…)} and {@code flatMap(…)} operations.
     *
     * @return the stream containing actual data.
     * @throws IllegalStateException if this stream is no longer the active stream on which to apply operations.
     */
    final DoubleStream delegate() {
        final DoubleStream s = source();
        source = null;
        return s;
    }

    /**
     * Invoked after an intermediate operation for determining which stream is the active one.
     * If this stream is still active, then this method returns {@code this}.
     * Otherwise the given stream is returned.
     */
    private DoubleStream update(final DoubleStream result) {
        if (source == null) {
            return result;      // This stream is no longer active; result stream shall be used instead.
        } else {
            source = result;    // This stream is still active.
            return this;
        }
    }

    /** Returns an equivalent stream that is parallel. */
    @Override public DoubleStream parallel() {
        return update(source().parallel());
    }

    /** Returns an equivalent stream that is sequential. */
    @Override public DoubleStream sequential() {
        return update(source().sequential());
    }

    /** Returns an equivalent stream that is unordered. */
    @Override public DoubleStream unordered() {
        return update(source().unordered());
    }

    /** Returns a stream with elements of this stream that match the given predicate. */
    @Override public DoubleStream filter(DoublePredicate predicate) {
        return update(source().filter(predicate));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public DoubleStream map(DoubleUnaryOperator mapper) {
        return update(source().map(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return delegate().mapToObj(mapper);
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public IntStream mapToInt(DoubleToIntFunction mapper) {
        return delegate().mapToInt(mapper);
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public LongStream mapToLong(DoubleToLongFunction mapper) {
        return delegate().mapToLong(mapper);
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return update(source().flatMap(mapper));
    }

    /** Returns a stream with distinct elements of this stream. */
    @Override public DoubleStream distinct() {
        return update(source().distinct());
    }

    /** Returns a stream with elements of this stream sorted in ascending order. */
    @Override public DoubleStream sorted() {
        return update(source().sorted());
    }

    /** Returns a stream performing the specified action on each element when consumed. */
    @Override public DoubleStream peek(DoubleConsumer action) {
        return update(source().peek(action));
    }

    /** Returns a stream with truncated at the given number of elements. */
    @Override public DoubleStream limit(long maxSize) {
        return update(source().limit(maxSize));
    }

    /** Returns a stream discarding the specified number of elements. */
    @Override public DoubleStream skip(long n) {
        return update(source().skip(n));
    }

    /** Performs an action for each element of this stream. */
    @Override public void forEach(DoubleConsumer action) {
        source().forEach(action);
    }

    /** Performs an action for each element of this stream in encounter order. */
    @Override public void forEachOrdered(DoubleConsumer action) {
        source().forEachOrdered(action);
    }

    /** Performs a reduction on the elements of this stream. */
    @Override public double reduce(double identity, DoubleBinaryOperator op) {
        return source().reduce(identity, op);
    }

    /** Performs a reduction on the elements of this stream. */
    @Override public OptionalDouble reduce(DoubleBinaryOperator op) {
        return source().reduce(op);
    }

    /** Performs a mutable reduction on the elements of this stream. */
    @Override public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return source().collect(supplier, accumulator, combiner);
    }

    /** Returns the sum of elements of this stream. */
    @Override public double sum() {
        return source().sum();
    }

    /** Returns the minimum element of this stream. */
    @Override public OptionalDouble min() {
        return source().min();
    }

    /** Returns the maximum element of this stream. */
    @Override public OptionalDouble max() {
        return source().max();
    }

    /** Returns the number of elements in this stream. */
    @Override public long count() {
        return source().count();
    }

    /** Returns the average of elements of this stream. */
    @Override public OptionalDouble average() {
        return source().average();
    }

    /** Returns statistics about elements of this stream. */
    @Override public DoubleSummaryStatistics summaryStatistics() {
        return source().summaryStatistics();
    }

    /** Returns whether at least one element of this stream matches the provided predicate. */
    @Override public boolean anyMatch(DoublePredicate predicate) {
        return source().anyMatch(predicate);
    }

    /** Returns whether all elements of this stream match the provided predicate. */
    @Override public boolean allMatch(DoublePredicate predicate) {
        return source().allMatch(predicate);
    }

    /** Returns whether none element of this stream match the provided predicate. */
    @Override public boolean noneMatch(DoublePredicate predicate) {
        return source().noneMatch(predicate);
    }

    /** Returns the first element of this stream. */
    @Override public OptionalDouble findFirst() {
        return source().findFirst();
    }

    /** Returns any element of this stream. */
    @Override public OptionalDouble findAny() {
        return source().findAny();
    }

    /** Returns the stream elements as {@link Double} elements. */
    @Override public Stream<Double> boxed() {
        return delegate().boxed();
    }

    /** Returns an iterator for the elements of this stream. */
    @Override public PrimitiveIterator.OfDouble iterator() {
        return source().iterator();
    }

    /** Returns a spliterator for the elements of this stream. */
    @Override public Spliterator.OfDouble spliterator() {
        return source().spliterator();
    }

    /** Returns all elements in an array. */
    @Override public double[] toArray() {
        return source().toArray();
    }

    /** Returns an equivalent stream with an additional close handler. */
    @Override public DoubleStream onClose(Runnable closeHandler) {
        return update(source().onClose(closeHandler));
    }
}
