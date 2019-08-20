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
package org.apache.sis.internal.util;

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
 * Allows for delegation for a subset of behaviors of a stream to a custom component.
 * How it works: This class is simply delegating all {@link Stream} interface operations to the one provided by
 * {@link #createDecoratedStream()} implementation. It allows implementations to short-circuit a certain number of
 * methods to provide their own solution. For example, if you've got a streamable dataset whose count can be provided
 * efficiently by a third-party tool (typically COUNT query on SQL databases), you can create a {@link StreamDecoration}
 * with an overrided count method running the count query instead of counting elements of the stream manually.
 *
 * Another exemple would be intermediate operations. Let's keep SQL queries as example. If you create a stream executing
 * a statement on terminal operations, you could override {@link Stream#limit(long)} and {@link Stream#skip(long)}
 * methods to set LIMIT and OFFSET criterias in an SQL query.
 *
 * For an advanced example, you can look at {@link org.apache.sis.internal.sql.feature.StreamSQL} class.
 *
 * @param <T> The type of objects contained in the stream, as specified in {@link Stream} interface.
 *
 * @since 1.0
 *
 * @author Alexis Manin (Geomatys)
 */
public abstract class StreamDecoration<T> extends BaseStreamDecoration<T, Stream<T>> implements Stream<T> {

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return getOrCreate().filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return getOrCreate().map(mapper);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return getOrCreate().mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return getOrCreate().mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return getOrCreate().mapToDouble(mapper);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return getOrCreate().flatMap(mapper);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return getOrCreate().flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return getOrCreate().flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return getOrCreate().flatMapToDouble(mapper);
    }

    @Override
    public Stream<T> distinct() {
        return getOrCreate().distinct();
    }

    @Override
    public Stream<T> sorted() {
        return getOrCreate().sorted();
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return getOrCreate().sorted(comparator);
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        return getOrCreate().peek(action);
    }

    @Override
    public Stream<T> limit(long maxSize) {
        return getOrCreate().limit(maxSize);
    }

    @Override
    public Stream<T> skip(long n) {
        return getOrCreate().skip(n);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        getOrCreate().forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        getOrCreate().forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return getOrCreate().toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return getOrCreate().toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return getOrCreate().reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return getOrCreate().reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return getOrCreate().reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return getOrCreate().collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return getOrCreate().collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return getOrCreate().min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return getOrCreate().max(comparator);
    }

    @Override
    public long count() {
        return getOrCreate().count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return getOrCreate().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return getOrCreate().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return getOrCreate().noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return getOrCreate().findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return getOrCreate().findAny();
    }
}
