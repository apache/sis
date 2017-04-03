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
package org.apache.sis.math;

import org.apache.sis.util.Numbers;
import org.apache.sis.util.collection.IntegerList;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * A vector of integer values backed by an {@link IntegerList}.
 * This offers a compressed storage using only the minimal amount of bits per value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class PackedVector extends ArrayVector<Long> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2025113345434607526L;

    /**
     * The compressed list of integer values. This list can store values from 0 to {@code delta} inclusive.
     */
    private final IntegerList data;

    /**
     * The offset to add to the {@link #data} in order to get the values to return.
     */
    private final long offset;

    /**
     * Creates a new compressed vector initialized to a copy of the data provided by the given vector.
     *
     * @param  source  the vector to copy.
     * @param  offset  the minimal value in the source vector.
     * @param  delta   the maximal value in the source vector minus {@code offset}.
     */
    PackedVector(final Vector source, final long offset, final int delta) {
        this.offset = offset;
        final int length = source.size();
        data = new IntegerList(length, delta, true);
        for (int i=0; i<length; i++) {
            data.setInt(i, JDK8.toIntExact(source.longValue(i) - offset));
        }
    }

    /**
     * Type of elements fixed to {@code Long} even if the actual storage used by this class is more compact.
     * The reason for the {@code Long} type is that this class can return any value in the {@code Long} range,
     * because of the {@link #offset}.
     */
    @Override
    public Class<Long> getElementType() {
        return Long.class;
    }

    /**
     * Returns the number of elements in this vector.
     */
    @Override
    public int size() {
        return data.size();
    }

    /**
     * Returns the value at the given index as a {@code double} primitive type.
     */
    @Override
    public double doubleValue(final int index) {
        return longValue(index);
    }

    /**
     * Returns the value at the given index as a {@code float} primitive type.
     */
    @Override
    public float floatValue(final int index) {
        return longValue(index);
    }

    /**
     * Returns the value at the given index as a {@code long} primitive type.
     */
    @Override
    public long longValue(final int index) {
        return data.getInt(index) + offset;
    }

    /**
     * Returns the string representation of the value at the given index.
     */
    @Override
    public String stringValue(final int index) {
        return Long.toString(longValue(index));
    }

    /**
     * Returns the value at the given index wrapped in a {@link Long} instance.
     */
    @Override
    public Number get(final int index) {
        return longValue(index);
    }

    /**
     * Sets the value at the given index at returns the previous value.
     */
    @Override
    public Number set(final int index, final Number value) {
        verifyType(value.getClass(), Numbers.LONG);
        final Number old = get(index);
        data.setInt(index, JDK8.toIntExact(JDK8.subtractExact(value.longValue(), offset)));
        return old;
    }
}
