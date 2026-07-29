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
package org.apache.sis.storage.rs;

import org.apache.sis.storage.DataStoreException;

/**
 * Writable RS coverage location iterator.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface WritableCodeIterator extends CodeIterator, AutoCloseable {

    /**
     * @return always true
     */
    @Override
    default boolean isWritable() {
        return true;
    }

    /**
     * @param band coverage band index
     * @param value cell band new value
     */
    default void setSample(int band, int value) {
        setSample(band, (double)value);
    }

    /**
     * @param band coverage band index
     * @param value cell band new value
     */
    default void setSample(int band, float value) {
        setSample(band, (double)value);
    }

    /**
     * @param band coverage band index
     * @param value cell band new value
     */
    void setSample(int band, double value);

    /**
     * @param values cell bands new values
     */
    default void setCell(int[] values) {
        final double[] cell = new double[values.length];
        for (int i = 0; i < cell.length; i++) cell[i] = values[i];
        setCell(cell);
    }

    /**
     * @param values cell bands new values
     */
    default void setCell(float[] values) {
        final double[] cell = new double[values.length];
        for (int i = 0; i < cell.length; i++) cell[i] = values[i];
        setCell(cell);
    }

    /**
     * @param values cell bands new values
     */
    default void setCell(double[] values) {
        for (int i = 0; i < values.length; i++) {
            setSample(i, values[i]);
        }
    }

    /**
     * Release any resource attached to the writer.
     */
    @Override
    public void close() throws DataStoreException;

}
