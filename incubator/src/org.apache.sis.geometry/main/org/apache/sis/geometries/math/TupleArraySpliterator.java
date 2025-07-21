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
package org.apache.sis.geometries.math;

import java.util.Spliterator;
import java.util.function.Consumer;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class TupleArraySpliterator implements Spliterator<Tuple>{

    private final TupleArray array;
    /**
     * Inclusive
     */
    private int rangeStart;
    /**
     * Exclusive
     */
    private int rangeEnd;

    private TupleArrayCursor cursor;

    public TupleArraySpliterator(TupleArray array) {
        this(array,0, array.getLength());
    }

    public TupleArraySpliterator(TupleArray array, int rangeStart, int rangeEnd) {
        if (rangeEnd <= rangeStart) {
            throw new IllegalArgumentException("Range end must be superior to range start");
        }
        this.array = array;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Tuple> action) {
        if (rangeStart >= rangeEnd) return false;
        if (cursor == null) cursor = array.cursor();
        cursor.moveTo(rangeStart);
        rangeStart++;
        action.accept(cursor.samples());
        return true;
    }

    @Override
    public Spliterator<Tuple> trySplit() {
        int remaining = rangeEnd - rangeStart;
        if (remaining < 5) {
            //too few elements to split it
            return null;
        }

        final int half = rangeStart + remaining / 2;
        final TupleArraySpliterator split = new TupleArraySpliterator(array, half, rangeEnd);
        rangeEnd = half;
        return split;
    }

    @Override
    public long estimateSize() {
        return rangeEnd - rangeStart;
    }

    @Override
    public int characteristics() {
        return SIZED | NONNULL;
    }

}
