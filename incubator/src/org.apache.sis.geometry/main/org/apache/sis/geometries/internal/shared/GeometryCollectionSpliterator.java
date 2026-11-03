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
package org.apache.sis.geometries.internal.shared;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;


/**
 * A spliterator over the elements of a {@link GeometryCollection}, reading them by index with
 * {@link GeometryCollection#getGeometryN(int)}. Since every element can be reached directly,
 * this spliterator splits in two halves of exact size.
 *
 * <p>The number of elements is read when this spliterator is created. Adding or removing elements
 * in the collection after that point results in unspecified behavior.</p>
 *
 * <p>Elements of a collection are never null. This spliterator verifies that constraint on each
 * element it returns, so that a non-conforming implementation fails at the element at fault.</p>
 *
 * @param  <T>  type of the elements of the collection.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeometryCollectionSpliterator<T extends Geometry> implements Spliterator<T> {

    private final GeometryCollection<T> collection;

    /**
     * Index of the next element to return.
     */
    private int index;

    /**
     * Index after the last element to return.
     */
    private final int fence;

    /**
     * Creates a spliterator over all elements of the given collection.
     *
     * @param  collection  the collection to traverse, not null.
     */
    public GeometryCollectionSpliterator(final GeometryCollection<T> collection) {
        this(collection, 0, collection.getNumGeometries());
    }

    private GeometryCollectionSpliterator(final GeometryCollection<T> collection, final int index, final int fence) {
        this.collection = collection;
        this.index = index;
        this.fence = fence;
    }

    /**
     * Returns the element at the given index, verifying that it is not null.
     */
    private T element(final int i) {
        return collection.getGeometryN(i);
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (index < fence) {
            action.accept(element(index++));
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(final Consumer<? super T> action) {
        Objects.requireNonNull(action);
        while (index < fence) {
            action.accept(element(index++));
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        final int start = index;
        final int middle = (start + fence) >>> 1;
        if (middle <= start) {
            return null;
        }
        index = middle;
        return new GeometryCollectionSpliterator<>(collection, start, middle);
    }

    @Override
    public long estimateSize() {
        return fence - index;
    }

    @Override
    public int characteristics() {
        return ORDERED | SIZED | SUBSIZED | NONNULL;
    }
}
