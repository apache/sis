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
package org.apache.sis.storage.dggs.internal.shared;

import java.util.AbstractList;
import java.util.List;
import java.util.function.LongFunction;


/**
 * A compressed zone identifer list.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComputedZoneIndexList extends AbstractList<Object> {

    private final long start;
    private final long step;
    private final int count;
    private final LongFunction<Object> toId;

    public ComputedZoneIndexList(long start, long step, int count, LongFunction<Object> toId) {
        this.start = start;
        this.step = step;
        this.count = count;
        this.toId = toId;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Object get(int index) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException();
        return toId.apply(start + index * step);
    }

    @Override
    public int indexOf(Object o) {
        final long value;
        if (o instanceof Integer i) {
            value = i;
        } else if (o instanceof Long l) {
            value = l;
        } else {
            return -1;
        }
        final long delta = value - start;
        if (delta % step != 0) return -1;
        final long s = delta / step;
        if (s < 0 || s >= count) return -1;
        return (int) s;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        final int nb = toIndex - fromIndex;
        return new ComputedZoneIndexList(start + step*fromIndex, step, nb, toId);
    }

}
