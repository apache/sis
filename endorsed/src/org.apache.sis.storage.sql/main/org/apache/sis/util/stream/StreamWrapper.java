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
package org.apache.sis.util.stream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
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
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.stream.Collector;
import org.apache.sis.filter.internal.shared.WarningEvent;


/**
 * A stream which delegates all operations to another stream.
 * This stream gives to subclasses an opportunity to redirect some operations to optimized implementations.
 *
 * <p><b>Note:</b>
 * It is sometimes necessary to defer the creation of the source stream until a terminal operation is invoked.
 * Such deferred source creation is managed by the {@link DeferredStream} subclass.</p>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <T>  the type of objects contained in the stream, as specified in {@link Stream} interface.
 *
 * @todo Add the methods that are new in JDK16.
 */
public abstract class StreamWrapper<T> extends BaseStreamWrapper<T, Stream<T>> implements Stream<T> {
    /**
     * The source of elements, or {@code null} if this {@code StreamWrapper} is no longer the active stream.
     * This is initialized at creation time and set to {@code null} after it has been determined that all
     * subsequent operations shall be done on a new stream instead of {@code this} stream.
     *
     * @see #source()
     */
    Stream<T> source;

    /**
     * An optional listener to notify of warnings that occur during the execution of filters or expressions.
     * This is always {@code null} by default and must be set explicitly if desired.
     */
    public Consumer<WarningEvent> listener;

    /**
     * Creates a new wrapper with initially no source.
     * The {@link #source} field should be initialized by subclass constructor.
     */
    StreamWrapper() {
    }

    /**
     * Creates a new wrapper for the given stream.
     *
     * @param  source  the stream to wrap.
     */
    protected StreamWrapper(final Stream<T> source) {
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
    protected final Stream<T> source() {
        if (source != null) {
            return source;
        }
        throw inactive();
    }

    /**
     * Same as {@link #source()} but marks this stream is inactive.
     * All subsequent operations must be done on the returned stream.
     * This is used by this class for {@code map(…)} and {@code flatMap(…)} operations.
     * May also be used by subclasses when an operation does not allow further optimizations.
     *
     * @return the stream containing actual data.
     * @throws IllegalStateException if this stream is no longer the active stream on which to apply operations.
     */
    protected final Stream<T> delegate() {
        final Stream<T> s = source();
        source = null;
        return s;
    }

    /**
     * Invoked after an intermediate operation for determining which stream is the active one.
     * If this stream is still active, then this method returns {@code this}.
     * Otherwise the given stream is returned.
     */
    private Stream<T> update(final Stream<T> result) {
        if (source == null) {
            return result;      // This stream is no longer active; result stream shall be used instead.
        } else {
            source = result;    // This stream is still active (worker not yet created).
            return this;
        }
    }

    /**
     * Executes the given action with a redirection of all warnings to the {@linkplain #listener}.
     *
     * @todo Replace by {@code ScopedValue.call(…)} when allowed to use JDK25.
     *
     * @param  <V>     the return value type of the given action.
     * @param  action  the action to execute.
     * @return the return value of the given action.
     */
    protected final <V> V execute(final Supplier<V> action) {
        if (listener == null) {
            return action.get();
        }
        final ThreadLocal<Consumer<WarningEvent>> context = WarningEvent.LISTENER;
        final Consumer<WarningEvent> old = context.get();
        try {
            context.set(listener);
            return action.get();
        } finally {
            context.set(old);
        }
    }

    /** Returns an equivalent stream that is parallel. */
    @Override public Stream<T> parallel() {
        return execute(() -> update(source().parallel()));
    }

    /** Returns an equivalent stream that is sequential. */
    @Override public Stream<T> sequential() {
        return execute(() -> update(source().sequential()));
    }

    /** Returns an equivalent stream that is unordered. */
    @Override public Stream<T> unordered() {
        return execute(() -> update(source().unordered()));
    }

    /** Returns a stream with elements of this stream that match the given predicate. */
    @Override public Stream<T> filter(Predicate<? super T> predicate) {
        return execute(() -> update(source().filter(predicate)));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return execute(() -> delegate().map(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return execute(() -> delegate().mapToInt(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return execute(() -> delegate().mapToLong(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return execute(() -> delegate().mapToDouble(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return execute(() -> delegate().flatMap(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return execute(() -> delegate().flatMapToInt(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return execute(() -> delegate().flatMapToLong(mapper));
    }

    /** Returns a stream with results of applying the given function to the elements of this stream. */
    @Override public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return execute(() -> delegate().flatMapToDouble(mapper));
    }

    /** Returns a stream with distinct elements of this stream. */
    @Override public Stream<T> distinct() {
        return execute(() -> update(source().distinct()));
    }

    /** Returns a stream with elements of this stream sorted in natural order. */
    @Override public Stream<T> sorted() {
        return execute(() -> update(source().sorted()));
    }

    /** Returns a stream with elements of this stream sorted using the given comparator. */
    @Override public Stream<T> sorted(Comparator<? super T> comparator) {
        return execute(() -> update(source().sorted(comparator)));
    }

    /** Returns a stream performing the specified action on each element when consumed. */
    @Override public Stream<T> peek(Consumer<? super T> action) {
        return execute(() -> update(source().peek(action)));
    }

    /** Returns a stream with truncated at the given number of elements. */
    @Override public Stream<T> limit(long maxSize) {
        return execute(() -> update(source().limit(maxSize)));
    }

    /** Returns a stream discarding the specified number of elements. */
    @Override public Stream<T> skip(long n) {
        return execute(() -> update(source().skip(n)));
    }

    /** Performs an action for each element of this stream. */
    @Override public void forEach(Consumer<? super T> action) {
        execute(() -> {source().forEach(action); return null;});
    }

    /** Performs an action for each element of this stream in encounter order. */
    @Override public void forEachOrdered(Consumer<? super T> action) {
        execute(() -> {source().forEachOrdered(action); return null;});
    }

    /** Performs a reduction on the elements of this stream. */
    @Override public T reduce(T identity, BinaryOperator<T> accumulator) {
        return execute(() -> source().reduce(identity, accumulator));
    }

    /** Performs a reduction on the elements of this stream. */
    @Override public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return execute(() -> source().reduce(accumulator));
    }

    /** Performs a reduction on the elements of this stream. */
    @Override public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return execute(() -> source().reduce(identity, accumulator, combiner));
    }

    /** Performs a mutable reduction on the elements of this stream. */
    @Override public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return execute(() -> source().collect(supplier, accumulator, combiner));
    }

    /** Performs a mutable reduction on the elements of this stream. */
    @Override public <R, A> R collect(Collector<? super T, A, R> collector) {
        return execute(() -> source().collect(collector));
    }

    /** Returns the minimum element of this stream according to the provided comparator. */
    @Override public Optional<T> min(Comparator<? super T> comparator) {
        return execute(() -> source().min(comparator));
    }

    /** Returns the maximum element of this stream according to the provided comparator. */
    @Override public Optional<T> max(Comparator<? super T> comparator) {
        return execute(() -> source().max(comparator));
    }

    /** Returns the number of elements in this stream. */
    @Override public long count() {
        return execute(() -> source().count());
    }

    /** Returns whether at least one element of this stream matches the provided predicate. */
    @Override public boolean anyMatch(Predicate<? super T> predicate) {
        return execute(() -> source().anyMatch(predicate));
    }

    /** Returns whether all elements of this stream match the provided predicate. */
    @Override public boolean allMatch(Predicate<? super T> predicate) {
        return execute(() -> source().allMatch(predicate));
    }

    /** Returns whether none element of this stream match the provided predicate. */
    @Override public boolean noneMatch(Predicate<? super T> predicate) {
        return execute(() -> source().noneMatch(predicate));
    }

    /** Returns the first element of this stream. */
    @Override public Optional<T> findFirst() {
        return execute(() -> source().findFirst());
    }

    /** Returns any element of this stream. */
    @Override public Optional<T> findAny() {
        return execute(() -> source().findAny());
    }

    /** Returns an iterator for the elements of this stream. */
    @Override public Iterator<T> iterator() {
        return execute(() -> source().iterator());
    }

    /** Returns a spliterator for the elements of this stream. */
    @Override public Spliterator<T> spliterator() {
        return execute(() -> source().spliterator());
    }

    /** Returns all elements in an array. */
    @Override public Object[] toArray() {
        return execute(() -> source().toArray());
    }

    /** Returns all elements in an array. */
    @Override public <A> A[] toArray(IntFunction<A[]> generator) {
        return execute(() -> source().toArray(generator));
    }

    /** Returns an equivalent stream with an additional close handler. */
    @Override public Stream<T> onClose(Runnable closeHandler) {
        return execute(() -> update(source().onClose(closeHandler)));
    }
}
