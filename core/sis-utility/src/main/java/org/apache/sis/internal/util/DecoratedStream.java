package org.apache.sis.internal.util;

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
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class DecoratedStream<T> implements Stream<T> {

    final Stream<T> source;

    protected DecoratedStream(Stream<T> source) {
        this.source = source;
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return source.filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return source.map(mapper);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return source.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return source.mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return source.mapToDouble(mapper);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return source.flatMap(mapper);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return source.flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return source.flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return source.flatMapToDouble(mapper);
    }

    @Override
    public Stream<T> distinct() {
        return source.distinct();
    }

    @Override
    public Stream<T> sorted() {
        return source.sorted();
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return source.sorted(comparator);
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        return source.peek(action);
    }

    @Override
    public Stream<T> limit(long maxSize) {
        return source.limit(maxSize);
    }

    @Override
    public Stream<T> skip(long n) {
        return source.skip(n);
    }

/*
    @Override
    public Stream<T> takeWhile(Predicate<? super T> predicate) {
        return source.takeWhile(predicate);
    }

    @Override
    public Stream<T> dropWhile(Predicate<? super T> predicate) {
        return source.dropWhile(predicate);
    }
*/

    @Override
    public void forEach(Consumer<? super T> action) {
        source.forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        source.forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return source.toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return source.toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return source.reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return source.reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return source.reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return source.collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return source.collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return source.min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return source.max(comparator);
    }

    @Override
    public long count() {
        return source.count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return source.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return source.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return source.noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return source.findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return source.findAny();
    }

    @Override
    public Iterator<T> iterator() {
        return source.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return source.spliterator();
    }

    @Override
    public boolean isParallel() {
        return source.isParallel();
    }

    @Override
    public Stream<T> sequential() {
        return source.sequential();
    }

    @Override
    public Stream<T> parallel() {
        return source.parallel();
    }

    @Override
    public Stream<T> unordered() {
        return source.unordered();
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        return source.onClose(closeHandler);
    }

    @Override
    public void close() {
        source.close();
    }
}
