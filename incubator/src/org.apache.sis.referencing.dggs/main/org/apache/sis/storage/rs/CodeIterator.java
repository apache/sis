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

/**
 * Referenced coverage location iterator.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface CodeIterator {

    /**
     * Returns true of the iterator is writable.
     * @return may be true
     */
    default boolean isWritable() {
        return false;
    }

    /**
     * @return current location identifier
     */
    int[] getPosition();

    /**
     * Move iterator to given zone
     *
     * @param zid searched location identifier
     */
    void moveTo(int[] zid);

    /**
     * Move to next location.
     *
     * @return true if there is a next location
     */
    boolean next();

    /**
     * @return number of bands in the coverage iterator
     */
    int getNumBands();

    /**
     * Get a cell sample.
     *
     * @param band index
     * @return band value
     */
    default int getSample(int band) {
        return (int) getSampleDouble(band);
    }

    /**
     * Get a cell sample.
     *
     * @param band index
     * @return band value
     */
    default float getSampleFloat(int band) {
        return (float) getSampleDouble(band);
    }

    /**
     * Get a cell sample.
     *
     * @param band index
     * @return band value
     */
    public abstract double getSampleDouble(int band);

    /**
     * Get cell values..
     *
     * @return all band values
     */
    default int[] getCell(int[] dest) {
        final double[] cell = getCell((double[])null);
        if (dest == null) dest = new int[cell.length];
        for (int i = 0; i < cell.length; i++) dest[i] = (int) cell[i];
        return dest;
    }

    /**
     * Get cell values..
     *
     * @return all band values
     */
    default float[] getCell(float[] dest) {
        final double[] cell = getCell((double[]) null);
        if (dest == null) dest = new float[cell.length];
        for (int i = 0; i < cell.length; i++) dest[i] = (int) cell[i];
        return dest;
    }

    /**
     * Get cell values.
     *
     * @return all band values
     */
    default double[] getCell(double[] dest) {
        if (dest == null) dest = new double[getNumBands()];
        for (int i = 0; i < dest.length; i++) dest[i] = getSampleDouble(i);
        return dest;
    }

    /**
     * Move iterator back to the starting position.
     */
    void rewind();
}
