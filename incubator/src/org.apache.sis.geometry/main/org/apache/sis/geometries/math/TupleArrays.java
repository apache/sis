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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.RecursiveAction;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TupleArrays extends Static {

    public static TupleArray of(List<? extends Tuple> vectors, int dimension, DataType dataType) {
        return of(vectors, SampleSystem.ofSize(dimension), dataType);
    }

    public static TupleArray of(List<? extends Tuple> vectors, SampleSystem type, DataType dataType) {
        final int dimension = type.getSize();
        final TupleArray array;
        switch (dataType) {
            case BYTE : array = new TupleArrayND.Byte(type, new byte[vectors.size() * dimension]); break;
            case UBYTE : array = new TupleArrayND.UByte(type, new byte[vectors.size() * dimension]); break;
            case SHORT : array = new TupleArrayND.Short(type, new short[vectors.size() * dimension]); break;
            case USHORT : array = new TupleArrayND.UShort(type, new short[vectors.size() * dimension]); break;
            case INT : array = new TupleArrayND.Int(type, new int[vectors.size() * dimension]); break;
            case UINT : array = new TupleArrayND.UInt(type, new int[vectors.size() * dimension]); break;
            case LONG : array = new TupleArrayND.Long(type, new long[vectors.size() * dimension]); break;
            case FLOAT : array = new TupleArrayND.Float(type, new float[vectors.size() * dimension]); break;
            case DOUBLE : array = new TupleArrayND.Double(type, new double[vectors.size() * dimension]); break;
            default : throw new IllegalArgumentException("Unexpected data type " + dataType);
        }

        for (int i = 0, n = vectors.size(); i < n; i++) {
            array.set(i, vectors.get(i));
        }

        return array;
    }

    public static TupleArray of(SampleSystem type, DataType dataType, int nbTuple) {
        final int dimension = type.getSize();
        final int size = nbTuple * dimension;
        final TupleArray array;
        switch (dataType) {
            case BYTE : array = new TupleArrayND.Byte(type, new byte[size]); break;
            case UBYTE : array = new TupleArrayND.UByte(type, new byte[size]); break;
            case SHORT : array = new TupleArrayND.Short(type, new short[size]); break;
            case USHORT : array = new TupleArrayND.UShort(type, new short[size]); break;
            case INT : array = new TupleArrayND.Int(type, new int[size]); break;
            case UINT : array = new TupleArrayND.UInt(type, new int[size]); break;
            case LONG : array = new TupleArrayND.Long(type, new long[size]); break;
            case FLOAT : array = new TupleArrayND.Float(type, new float[size]); break;
            case DOUBLE : array = new TupleArrayND.Double(type, new double[size]); break;
            default : throw new IllegalArgumentException("Unexpected data type " + dataType);
        }
        return array;
    }

    public static TupleArray of(int dimension, byte ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray of(int dimension, short ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray of(int dimension, int ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray of(int dimension, long ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray of(int dimension, float ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray of(int dimension, double ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray ofUnsigned(int dimension, byte ... values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray ofUnsigned(int dimension, short ... values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray ofUnsigned(int dimension, int ... values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray ofUnsigned(int dimension, List<Integer> values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static TupleArray of(CoordinateReferenceSystem crs, byte ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static TupleArray of(SampleSystem type, byte ... values) {
        return new TupleArrayND.Byte(type, values);
    }

    public static TupleArray of(CoordinateReferenceSystem crs, int ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static TupleArray of(SampleSystem type, int ... values) {
        return new TupleArrayND.Int(type, values);
    }

    public static TupleArray of(CoordinateReferenceSystem crs, short ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static TupleArray of(SampleSystem type, short ... values) {
        return new TupleArrayND.Short(type, values);
    }

    public static TupleArray of(CoordinateReferenceSystem crs, long ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static TupleArray of(SampleSystem type, long ... values) {
        return new TupleArrayND.Long(type, values);
    }

    public static TupleArray of(CoordinateReferenceSystem crs, float ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static TupleArray of(SampleSystem type, float ... values) {
        return new TupleArrayND.Float(type, values);
    }

    public static TupleArray of(CoordinateReferenceSystem crs, double ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static TupleArray of(SampleSystem type, double ... values) {
        return new TupleArrayND.Double(type, values);
    }

    public static TupleArray ofUnsigned(SampleSystem type, byte ... values) {
        return new TupleArrayND.UByte(type, values);
    }

    public static TupleArray ofUnsigned(SampleSystem type, short ... values) {
        return new TupleArrayND.UShort(type, values);
    }

    public static TupleArray ofUnsigned(SampleSystem type, int ... values) {
        return new TupleArrayND.UInt(type, values);
    }

    public static TupleArray ofUnsigned(SampleSystem type, List<Integer> values) {
        return new TupleArrayND.UInt(type, values);
    }

    /**
     * Compute the range of the given array.
     *
     * @param array to compute bbox from
     * @return vectors with the same crs as the array, lower and upper bounds.
     */
    public static BBox computeRange(TupleArray array) {
        final CoordinateReferenceSystem crs = array.getCoordinateReferenceSystem();
        final BBox bbox = crs == null ? new BBox(array.getDimension()) : new BBox(crs);
        bbox.setToNaN();
        boolean first = true;
        final TupleArrayCursor cursor = array.cursor();
        while (cursor.next()) {
            final Tuple samples = cursor.samples();
            if (first) {
                bbox.getLower().set(samples);
                bbox.getUpper().set(samples);
                first = false;
            } else {
                bbox.add(samples);
            }
        }
        return bbox;
    }

    /**
     * Try to compress array to a smaller data type.
     * The algorithm works for long, int or short types only.
     * The minimum and maximum values are extracted and if a small data type exist
     * values will be repacked in a new array of this type.
     *
     * @param array, not null
     * @return same or repacked array
     */
    public static TupleArray packIntegerDataType(TupleArray array) {
        final DataType dataType = array.getDataType();

        if (  DataType.LONG.equals(dataType)
           || DataType.INT.equals(dataType)
           || DataType.UINT.equals(dataType)
           || DataType.SHORT.equals(dataType)
           || DataType.USHORT.equals(dataType)) {
            DataType dt;
            if (array.isEmpty()) {
                dt = DataType.UBYTE;
            } else {
                BBox range = computeRange(array);
                dt = bestIntegerDataType(range);
            }
            if (dataType != dt) {
                //copy to a more efficiant data type
                TupleArray cp = TupleArrays.of(array.getSampleSystem(), dt, array.getLength());
                cp.set(0, array, 0, array.getLength());
                array = cp;
            }
        }
        return array;
    }

    /**
     * Create an unmodifiable view of the given array.
     *
     * @param array not null
     * @return unmodifiable view of the array.
     */
    public static TupleArray unmodifiable(TupleArray array) {
        return new TupleArrayUnmodifiable(array);
    }

    /**
     * Create an unmodifiable view of the given cursor.
     *
     * @param cursor not null
     * @return unmodifiable view of the array cursor.
     */
    public static TupleArrayCursor unmodifiable(TupleArrayCursor cursor) {
        return new TupleArrayCursorUnmodifiable(cursor);
    }

    /**
     * Create an concatenated view of the given arrays.
     *
     * @param arrays not null
     * @return unmodifiable view of the array cursor.
     */
    public static TupleArray concatenate(TupleArray ... arrays) {
        return new TupleArrayConcatenated(arrays);
    }

    /**
     * Group each tuple in given arrays.
     * Returns an array of same length and sum of all dimensions.
     *
     * @param arrays not null or empty
     * @return never null, if arrays size is one, then a copy is returned
     */
    public static TupleArray group(TupleArray ... arrays) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("arrays", arrays);
        final int length = arrays[0].getLength();
        if (length == 1) return arrays[0].copy();
        final DataType dataType = arrays[0].getDataType();
        final CoordinateReferenceSystem[] crs = new CoordinateReferenceSystem[arrays.length];
        final List<SampleDimension> allDims = new ArrayList<>();
        crs[0] = arrays[0].getCoordinateReferenceSystem();
        boolean undefined = (crs[0] == null || Geometries.isUndefined(crs[0]));
        allDims.addAll(arrays[0].getSampleSystem().getSampleDimensions());
        for (int i = 1; i <arrays.length; i++) {
            if (arrays[i].getLength() != length) {
                throw new IllegalArgumentException("All arrays must have the same length");
            }
            if (arrays[i].getDataType() != dataType) {
                throw new IllegalArgumentException("All arrays must have the same data type");
            }
            crs[i] = arrays[i].getCoordinateReferenceSystem();
            undefined |= (crs[i] == null || Geometries.isUndefined(crs[i]));
            allDims.addAll(arrays[i].getSampleSystem().getSampleDimensions());
        }

        final TupleArray result;
        if (undefined) {
            SampleSystem sampleSystem = new SampleSystem(dataType, allDims.toArray(new SampleDimension[0]));
            result = TupleArrays.of(sampleSystem, dataType, length);
        } else {
            result = TupleArrays.of(SampleSystem.of(CRS.compound(crs)), dataType, length);
        }

        if (length == 0) return result;

        final TupleArrayCursor resultCursor = result.cursor();
        int offset = 0;
        for (int a = 0; a < arrays.length; a++) {
            final int dim = arrays[a].getDimension();
            TupleArrayCursor cursor = arrays[a].cursor();
            cursor.moveTo(0);
            resultCursor.moveTo(0);
            for (int i = 0; i < length; i++, cursor.next(), resultCursor.next()) {
                Tuple source = cursor.samples();
                Tuple target = resultCursor.samples();
                for (int s = 0; s < dim; s++) {
                    target.set(s+offset, source.get(s));
                }
            }
            offset += dim;
        }
        return result;
    }

    /**
     * Create a view of only the given selected index tuples in the array.
     */
    public static TupleArray subset(TupleArray array, int ... selection) {
        return new Subset(array, selection);
    }

    /**
     * View TupleArray as a list.
     */
    public static List<Tuple> asList(TupleArray array) {
        return new AbstractList<Tuple>() {
            @Override
            public Tuple get(int index) {
                return array.get(index);
            }

            @Override
            public int size() {
                return array.getLength();
            }
        };
    }

    /**
     * Shuffle tuples in the array
     *
     * https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
     *
     * @param array to shuffle
     */
    public static void shuffle(TupleArray array) {
        final Random random = new Random();
        for (int i = array.getLength() - 1; i >= 1; i--) {
            array.swap(i, random.nextInt(i));
        }
    }

    /**
     * Sort tuple array, in place, using quick sort algorithm.
     * Inspired by : https://www.geeksforgeeks.org/quick-sort-algorithm/
     */
    public static void quickSort(TupleArray array, Comparator<Tuple> comparator) {
        quickSort(array, array.cursor(), array.cursor(), comparator, 0, array.getLength() - 1);
    }

    /**
     * Sort tuple array, in place, using quick sort algorithm.
     * Inspired by : https://www.geeksforgeeks.org/quick-sort-algorithm/
     *
     * @param array to sort
     * @param comparator not null, to compare tuples
     * @param low inclusive
     * @param high inclusive
     */
    public static void quickSort(TupleArray array, Comparator<Tuple> comparator, int low, int high) {
        quickSort(array, array.cursor(), array.cursor(), comparator, low, high);
    }

    private static int partition(TupleArray array, TupleArrayCursor cursor1, TupleArrayCursor cursor2, Comparator<Tuple> comparator, int low, int high) {
        //pick the middle point as pivot, in case of already sorted arrays
        //it speeds up the operation and prevents the quicksort to make a java.lang.StackOverflowError
        array.swap((low+high)/2, high);
        cursor1.moveTo(high);
        final Tuple pivot = cursor1.samples();
        int i = low - 1;
        for (int j = low; j <= high - 1; j++) {
            cursor2.moveTo(j);
            if (comparator.compare(cursor2.samples(),pivot) < 0) {
                i++;
                if (i != j) array.swap(i, j);
            }
        }
        array.swap(i + 1, high);
        return i + 1;
    }

    private static void quickSort(TupleArray array, TupleArrayCursor cursor1, TupleArrayCursor cursor2, Comparator<Tuple> comparator, int low, int high) {

        if (low < high) {
            final int pi = partition(array, cursor1, cursor2, comparator, low, high);
            quickSort(array, cursor1, cursor2, comparator, low, pi - 1);
            quickSort(array, cursor1, cursor2, comparator, pi + 1, high);
        }
    }

    /**
     * Create a ForkJoinTask to sort very large arrays using a custom a ForkJoinPool.
     * The user is responsible for submitting the task to a pool.
     *
     * @param array to sort
     * @param comparator not null, to compare tuples
     * @param low inclusive
     * @param high inclusive
     * @return RecursiveAction to sort the array.
     *          This action returns quickly, but forked tasks continue to be submitted to the pool
     *        Therefor awaiting pool completion is necessary to ensure the operation is finished.
     */
    public static RecursiveAction quickSortAction(TupleArray array, Comparator<Tuple> comparator, int low, int high) {
        return new QuickSortAction(array, array.cursor(), array.cursor(), comparator, low, high);
    }

    private static final class QuickSortAction extends RecursiveAction {

        private final TupleArray array;
        private final TupleArrayCursor cursor1;
        private final TupleArrayCursor cursor2;
        private final Comparator<Tuple> comparator;
        private final int low;
        private final int high;

        public QuickSortAction(TupleArray array, TupleArrayCursor cursor1, TupleArrayCursor cursor2, Comparator<Tuple> comparator, int low, int high) {
            this.array = array;
            this.cursor1 = cursor1;
            this.cursor2 = cursor2;
            this.comparator = comparator;
            this.low = low;
            this.high = high;
        }

        @Override
        protected void compute() {
            if (low < high) {
                final int pi = partition(array, cursor1, cursor2, comparator, low, high);
                QuickSortAction qsa1 = new QuickSortAction(array, cursor1, cursor2, comparator, low, pi - 1);
                qsa1.fork();
                QuickSortAction qsa2 = new QuickSortAction(array, array.cursor(), array.cursor(), comparator, pi + 1, high);
                qsa2.fork();

                //we do not use .join() because is causes stack over flow
            }
        }

    }


    private static DataType bestIntegerDataType(BBox box) {
        double min = box.getMinimum(0);
        double max = box.getMaximum(0);
        for (int i = 1, n = box.getDimension(); i < n; i++) {
            min = Math.min(min, box.getMinimum(i));
            max = Math.max(max, box.getMaximum(i));
        }
        final NumberRange range = NumberRange.create(min, true, max, true);
        return DataType.forRange(range, true);
    }

    private TupleArrays(){}

    private static class Subset extends AbstractTupleArray {

        private final TupleArray base;
        private final int[] index;

        public Subset(TupleArray base, int[] index) {
            this.base = base;
            this.index = index;
        }

        @Override
        public int getLength() {
            return index.length;
        }

        @Override
        public SampleSystem getSampleSystem() {
            return base.getSampleSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            base.setSampleSystem(type);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return base.getCoordinateReferenceSystem();
        }

        @Override
        public int getDimension() {
            return base.getDimension();
        }

        @Override
        public DataType getDataType() {
            return base.getDataType();
        }

        @Override
        public void get(int index, Tuple buffer) {
            base.get(this.index[index], buffer);
        }

        @Override
        public void set(int index, Tuple buffer) {
            base.set(this.index[index], buffer);
        }

        @Override
        public TupleArrayCursor cursor() {
            final TupleArrayCursor bc = base.cursor();
            return new TupleArrayCursor() {
                private int coordinate = -1;
                @Override
                public Tuple<?> samples() {
                    bc.moveTo(index[coordinate]);
                    return bc.samples();
                }

                @Override
                public int coordinate() {
                    return coordinate;
                }

                @Override
                public void moveTo(int coordinate) {
                    this.coordinate = coordinate;
                }

                @Override
                public boolean next() {
                    if (coordinate >= index.length-1) return false;
                    coordinate++;
                    return true;
                }
            };
        }

    }
}
