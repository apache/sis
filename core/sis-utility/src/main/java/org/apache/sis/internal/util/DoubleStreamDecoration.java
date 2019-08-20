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
 * A specialization of {@link StreamDecoration} for {@link DoubleStream streams of double value}.
 *
 * @since 1.0
 *
 * @author Alexis Manin (Geomatys)
 */
public abstract class DoubleStreamDecoration extends BaseStreamDecoration<Double, DoubleStream> implements DoubleStream {

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        return getOrCreate().filter(predicate);
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        return getOrCreate().map(mapper);
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return getOrCreate().mapToObj(mapper);
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        return getOrCreate().mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        return getOrCreate().mapToLong(mapper);
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return getOrCreate().flatMap(mapper);
    }

    @Override
    public DoubleStream distinct() {
        return getOrCreate().distinct();
    }

    @Override
    public DoubleStream sorted() {
        return getOrCreate().sorted();
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        return getOrCreate().peek(action);
    }

    @Override
    public DoubleStream limit(long maxSize) {
        return getOrCreate().limit(maxSize);
    }

    @Override
    public DoubleStream skip(long n) {
        return getOrCreate().skip(n);
    }

    @Override
    public void forEach(DoubleConsumer action) {
        getOrCreate().forEach(action);
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        getOrCreate().forEachOrdered(action);
    }

    @Override
    public double[] toArray() {
        return getOrCreate().toArray();
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        return getOrCreate().reduce(identity, op);
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        return getOrCreate().reduce(op);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return getOrCreate().collect(supplier, accumulator, combiner);
    }

    @Override
    public double sum() {
        return getOrCreate().sum();
    }

    @Override
    public OptionalDouble min() {
        return getOrCreate().min();
    }

    @Override
    public OptionalDouble max() {
        return getOrCreate().max();
    }

    @Override
    public long count() {
        return getOrCreate().count();
    }

    @Override
    public OptionalDouble average() {
        return getOrCreate().average();
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        return getOrCreate().summaryStatistics();
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        return getOrCreate().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        return getOrCreate().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return getOrCreate().noneMatch(predicate);
    }

    @Override
    public OptionalDouble findFirst() {
        return getOrCreate().findFirst();
    }

    @Override
    public OptionalDouble findAny() {
        return getOrCreate().findAny();
    }

    @Override
    public Stream<Double> boxed() {
        return getOrCreate().boxed();
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return getOrCreate().iterator();
    }

    @Override
    public Spliterator.OfDouble spliterator() {
        return getOrCreate().spliterator();
    }
}
