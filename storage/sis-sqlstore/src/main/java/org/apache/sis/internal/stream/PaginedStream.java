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

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;


/**
 * A stream on which {@code skip} and {@code limit} operations are delegated to a root stream.
 * This is useful when the root stream is capable of some optimizations, for example using SQL.
 * The redirection stops as soon as an operation may change the elements order or filtering.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <T>  the type of objects contained in the stream, as specified in {@link Stream} interface.
 *
 * @since 1.1
 * @module
 */
public final class PaginedStream<T> extends StreamWrapper<T> {
    /**
     * The root stream on which to delegate {@code skip} and {@code limit} operations.
     */
    private Stream<?> root;

    /**
     * Creates a new mapped stream.
     *
     * @param  source  the stream to wrap.
     * @param  root    the stream on which to delegate {@code skip} and {@code limit} operations.
     */
    public PaginedStream(final Stream<T> source, final Stream<?> root) {
        super(source);
        this.root = root;
    }

    /**
     * Discards the specified number of elements.
     * This method delegates the operation to the root stream.
     *
     * @param  n  number of elements to skip.
     * @return {@code this}.
     */
    @Override
    public Stream<T> skip(final long n) {
        root = root.skip(n);
        return this;
    }

    /**
     * Truncates this stream to the given number of elements.
     * This method delegates the operation to the root stream.
     *
     * @param  maxSize  number of elements to keep.
     * @return {@code this}.
     */
    @Override
    public Stream<T> limit(final long maxSize) {
        root = root.limit(maxSize);
        return this;
    }

    /**
     * Returns the number of elements in this stream.
     * This method delegates the operation to the root stream.
     *
     * @return number of elements in this stream.
     */
    @Override
    public long count() {
        return root.count();
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     *
     * @param mapper function to apply to each element.
     * @return the new stream.
     */
    @Override
    public DoubleStream mapToDouble​(ToDoubleFunction<? super T> mapper) {
        return new PaginedDoubleStream(super.mapToDouble(mapper), root);
    }

    /*
     * All intermediate methods that may change elements order or count must be listed below.
     */
    @Override public Stream<T> distinct()                               {return delegate().distinct();}
    @Override public Stream<T> unordered()                              {return delegate().unordered();}
    @Override public Stream<T> sorted()                                 {return delegate().sorted();}
    @Override public Stream<T> sorted(Comparator<? super T> comparator) {return delegate().sorted(comparator);}
    @Override public Stream<T> filter(Predicate<? super T> predicate)   {return delegate().filter(predicate);}
}
