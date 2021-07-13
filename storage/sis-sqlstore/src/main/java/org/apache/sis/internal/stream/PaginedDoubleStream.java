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

import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
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
 * @since   1.1
 * @module
 */
public final class PaginedDoubleStream extends DoubleStreamWrapper {
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
    public PaginedDoubleStream(final DoubleStream source, final Stream<?> root) {
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
    public DoubleStream skip(final long n) {
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
    public DoubleStream limit(final long maxSize) {
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

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        return new PaginedDoubleStream(super.map(mapper), root);
    }

    /*
     * All intermediate methods that may change elements order or count must be listed below.
     */
    @Override public DoubleStream distinct()                        {return delegate().distinct();}
    @Override public DoubleStream unordered()                       {return delegate().unordered();}
    @Override public DoubleStream sorted()                          {return delegate().sorted();}
    @Override public DoubleStream filter(DoublePredicate predicate) {return delegate().filter(predicate);}
}
