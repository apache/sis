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
package org.apache.sis.storage.rs.internal.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.Cursor;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.WritableCodeIterator;


/**
 * Referenced Coverage backed by a list of samples stored in TupleArrays.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ArrayCodedCoverage extends AbstractCodedCoverage{

    private final List<Array> samples;

    public ArrayCodedCoverage(final GenericName name, CodedGeometry gridGeometry, SampleDimension ... sampleDimensions) throws FactoryException {
        super(name, gridGeometry, List.of(sampleDimensions));

        final int nbCell = Math.toIntExact(gridGeometry.getExtent().getLatticePointCount());

        samples = new ArrayList<>();
        final double[] nans = new double[sampleDimensions.length];
        for (int i = 0; i < nans.length; i++) {
            final SampleSystem ss = new SampleSystem(DataType.DOUBLE, sampleDimensions[i]);
            final double[] datas = new double[nbCell];
            Arrays.fill(datas, Double.NaN);
            samples.add(NDArrays.of(ss, datas));
            nans[i] = Double.NaN;
        }
    }

    public ArrayCodedCoverage(final GenericName name, CodedGeometry gridGeometry, List<Array> samples) throws FactoryException {
        super(name, gridGeometry, toSds(samples));

        this.samples = samples;
        final long nbCell = extent.getLatticePointCount();
        for (Array ta : samples) {
            if (ta.getLength() != nbCell) {
                throw new IllegalArgumentException("Number of samples do not match number of cells");
            }
            if (ta.getDimension() != 1) {
                throw new IllegalArgumentException("Samples tuple arrays must have a dimension of 1");
            }
        }
    }

    private static List<SampleDimension> toSds(List<Array> sampleDimensions) {
        final List<SampleDimension> dims = new ArrayList<>();
        for (Array ta : sampleDimensions) {
            dims.add(ta.getSampleSystem().getSampleDimensions().get(0));
        }
        return dims;
    }

    public List<Array> getSamples() {
        return samples;
    }

    @Override
    public CodeIterator createIterator() {
        return createWritableIterator();
    }

    @Override
    public WritableCodeIterator createWritableIterator() {
        return new Iterator();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * super.hashCode() + Objects.hashCode(this.samples);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ArrayCodedCoverage other = (ArrayCodedCoverage) obj;
        return super.equals(obj) && Objects.equals(this.samples, other.samples);
    }

    private final class Iterator implements WritableCodeIterator {

        private long linearPosition = -1;
        private final long nbCell;

        private final Cursor[] cursors;

        public Iterator() {
            nbCell = extent.getLatticePointCount();

            cursors = new Cursor[samples.size()];
            for (int i = 0; i < cursors.length; i++) {
                cursors[i] = samples.get(i).cursor();
            }
        }

        private long toLinearPosition(int[] pos) {
            long p = 0;
            for (int i = 0; i < dimension; i++) {
                p += pos[i] * dimStep[i];
            }
            return p;
        }

        @Override
        public void setSample(int band, double value) {
            cursors[band].moveTo(Math.toIntExact(linearPosition));
            cursors[band].samples().set(0, value);
        }

        @Override
        public int getNumBands() {
            return cursors.length;
        }

        @Override
        public int[] getPosition() {
            long remain = linearPosition;
            final int[] pos = new int[dimension];
            for (int i = 0; i < pos.length; i++) {
                long k = remain / dimStep[i];
                pos[i] = Math.toIntExact(dimOffsets[i] + k);
                remain -= k * dimStep[i];
            }
            return pos;
        }

        @Override
        public void moveTo(int[] pos) {
            final long lp = toLinearPosition(pos);
            if (lp < 0 || lp >= nbCell) {
                throw new IllegalArgumentException("Position " + Arrays.toString(pos) +" is not part of this coverage");
            }
            linearPosition = lp;
        }

        @Override
        public boolean next() {
            if (linearPosition < nbCell-1) {
                linearPosition ++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public double getSampleDouble(int band) {
            cursors[band].moveTo(Math.toIntExact(linearPosition));
            return cursors[band].samples().get(0);
        }

        @Override
        public void rewind() {
            linearPosition = -1;
        }

        @Override
        public void close() throws DataStoreException {
        }

    }

}
