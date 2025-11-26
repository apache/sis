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


/**
 *
 * @author Johann Sorel (Geomatys)
 */
abstract class AbstractCursor extends AbstractTuple implements Cursor {

    private final Array array;
    protected long offset = -1;

    AbstractCursor(Array array) {
        super(array.getSampleSystem());
        this.array = array;
    }

    @Override
    public Tuple samples() {
        return this;
    }

    @Override
    public long coordinate() {
        return offset;
    }

    @Override
    public void moveTo(long coordinate) {
        if (coordinate<0 || coordinate >= array.getLength()) {
            throw new ArrayIndexOutOfBoundsException("Invalid coordinate " + coordinate + ", outside of data range [0," + array.getLength() + "]. ");
        }
        offset = coordinate;
    }

    @Override
    public boolean next() {
        offset += 1;
        return offset < array.getLength();
    }

    @Override
    public DataType getDataType() {
        return array.getDataType();
    }

    @Override
    public int getDimension() {
        return array.getDimension();
    }

    @Override
    public double get(int indice) {
        return get(offset, indice);
    }

    @Override
    public void set(int indice, double value) {
        set(offset, indice, value);
    }

    public abstract double get(long tupleIndex, int sampleIndex);

    public abstract void set(long tupleIndex, int sampleIndex, double value);

}
