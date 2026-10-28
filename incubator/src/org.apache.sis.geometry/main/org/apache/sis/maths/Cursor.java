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
package org.apache.sis.maths;


/**
 * A cursor allows to iterate over a tuple array without copying datas.
 * Values are written in the tuple array with the cursor set methods.
 *
 * API copied from Unlicense.science
 */
public interface Cursor {

    /**
     * Get a view on the values of the current tuple.
     * The view can not be modified, but its values change when the cursor moves
     * or when values are written with this cursor.
     * Use {@link Vector#copy()} to obtain a vector independent of the cursor.
     *
     * @return view on the current tuple values
     */
    Vector<?> samples();

    /**
     * Write a sample value of the current tuple in the array.
     *
     * @param sampleIndex sample index in the tuple
     * @param value sample value
     * @throws IndexOutOfBoundsException if index is not valid
     */
    void set(int sampleIndex, double value);

    /**
     * Write all sample values of the current tuple in the array.
     *
     * @param values values to write, of the tuple dimension
     */
    default void set(Vector<?> values) {
        for (int i = 0, n = values.getDimension(); i < n; i++) {
            set(i, values.get(i));
        }
    }

    /**
     * Get the current tuple coordinate.
     * @return Tuple, returns always the same container
     */
    long coordinate();

    /**
     * Move cursor to given coordinate.
     *
     * @param coordinate
     */
    void moveTo(long coordinate);

    /**
     * Move to next tuple.
     *
     * @return false if no more tuple
     */
    boolean next();

}
