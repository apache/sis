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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class NDArrays {

    public static Array of(List<? extends Tuple> vectors, int dimension, DataType dataType) {
        return of(vectors, SampleSystem.ofSize(dimension), dataType);
    }

    public static Array of(List<? extends Tuple> vectors, SampleSystem type, DataType dataType) {
        final int dimension = type.getSize();
        final Array array;
        switch (dataType) {
            case BYTE : array = new ArrayMemory.Byte(type, new byte[vectors.size() * dimension]); break;
            case UBYTE : array = new ArrayMemory.UByte(type, new byte[vectors.size() * dimension]); break;
            case SHORT : array = new ArrayMemory.Short(type, new short[vectors.size() * dimension]); break;
            case USHORT : array = new ArrayMemory.UShort(type, new short[vectors.size() * dimension]); break;
            case INT : array = new ArrayMemory.Int(type, new int[vectors.size() * dimension]); break;
            case UINT : array = new ArrayMemory.UInt(type, new int[vectors.size() * dimension]); break;
            case LONG : array = new ArrayMemory.Long(type, new long[vectors.size() * dimension]); break;
            case FLOAT : array = new ArrayMemory.Float(type, new float[vectors.size() * dimension]); break;
            case DOUBLE : array = new ArrayMemory.Double(type, new double[vectors.size() * dimension]); break;
            default : throw new IllegalArgumentException("Unexpected data type " + dataType);
        }

        for (int i = 0, n = vectors.size(); i < n; i++) {
            array.set(i, vectors.get(i));
        }

        return array;
    }

    public static Array of(SampleSystem type, DataType dataType, long nbTuple) {
        final int dimension = type.getSize();
        final long size = nbTuple * dimension;
        final int isize = Math.toIntExact(size);
        final Array array;
        switch (dataType) {
            case BYTE : array = new ArrayMemory.Byte(type, new byte[isize]); break;
            case UBYTE : array = new ArrayMemory.UByte(type, new byte[isize]); break;
            case SHORT : array = new ArrayMemory.Short(type, new short[isize]); break;
            case USHORT : array = new ArrayMemory.UShort(type, new short[isize]); break;
            case INT : array = new ArrayMemory.Int(type, new int[isize]); break;
            case UINT : array = new ArrayMemory.UInt(type, new int[isize]); break;
            case LONG : array = new ArrayMemory.Long(type, new long[isize]); break;
            case FLOAT : array = new ArrayMemory.Float(type, new float[isize]); break;
            case DOUBLE : array = new ArrayMemory.Double(type, new double[isize]); break;
            default : throw new IllegalArgumentException("Unexpected data type " + dataType);
        }
        return array;
    }

    public static Array of(int dimension, byte ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static Array of(int dimension, short ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static Array of(int dimension, int ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static Array of(int dimension, long ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static Array of(int dimension, float ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static Array of(int dimension, double ... values) {
        return of(SampleSystem.ofSize(dimension), values);
    }

    public static Array ofUnsigned(int dimension, byte ... values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static Array ofUnsigned(int dimension, short ... values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static Array ofUnsigned(int dimension, int ... values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static Array ofUnsigned(int dimension, List<Integer> values) {
        return ofUnsigned(SampleSystem.ofSize(dimension), values);
    }

    public static Array of(CoordinateReferenceSystem crs, byte ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static Array of(SampleSystem type, byte ... values) {
        return new ArrayMemory.Byte(type, values);
    }

    public static Array of(CoordinateReferenceSystem crs, int ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static Array of(SampleSystem type, int ... values) {
        return new ArrayMemory.Int(type, values);
    }

    public static Array of(CoordinateReferenceSystem crs, short ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static Array of(SampleSystem type, short ... values) {
        return new ArrayMemory.Short(type, values);
    }

    public static Array of(CoordinateReferenceSystem crs, long ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static Array of(SampleSystem type, long ... values) {
        return new ArrayMemory.Long(type, values);
    }

    public static Array of(CoordinateReferenceSystem crs, float ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static Array of(SampleSystem type, float ... values) {
        return new ArrayMemory.Float(type, values);
    }

    public static Array of(CoordinateReferenceSystem crs, double ... values) {
        return of(SampleSystem.of(crs), values);
    }

    public static Array of(SampleSystem type, double ... values) {
        return new ArrayMemory.Double(type, values);
    }

    public static Array ofUnsigned(SampleSystem type, byte ... values) {
        return new ArrayMemory.UByte(type, values);
    }

    public static Array ofUnsigned(SampleSystem type, short ... values) {
        return new ArrayMemory.UShort(type, values);
    }

    public static Array ofUnsigned(SampleSystem type, int ... values) {
        return new ArrayMemory.UInt(type, values);
    }

    public static Array ofUnsigned(SampleSystem type, List<Integer> values) {
        return new ArrayMemory.UInt(type, values);
    }

    /**
     * Compute the range of the given array.
     *
     * @param array to compute bbox from
     * @return vectors with the same crs as the array, lower and upper bounds.
     */
    public static BBox computeRange(Array array) {

        final int dim = array.getDimension();
        final double[] min = new double[dim];
        final double[] max = new double[dim];
        final double[] buffer = new double[dim];
        java.util.Arrays.fill(min, Double.NaN);
        java.util.Arrays.fill(max, Double.NaN);

        final Cursor cursor = array.cursor();
        if (cursor.next()) {
            cursor.samples().toArrayDouble(buffer, 0);
            System.arraycopy(buffer, 0, min, 0, dim);
            System.arraycopy(buffer, 0, max, 0, dim);
        }

        int i;
        while (cursor.next()) {
            cursor.samples().toArrayDouble(buffer, 0);
            for (i = 0; i < dim; i++) {
                if (Double.isNaN(buffer[i])) continue;
                if (Double.isNaN(min[i]) || (buffer[i] < min[i])) min[i] = buffer[i];
                if (Double.isNaN(max[i]) || (buffer[i] > max[i])) max[i] = buffer[i];
            }
        }

        final CoordinateReferenceSystem crs = array.getCoordinateReferenceSystem();
        final BBox bbox = crs == null ? new BBox(array.getDimension()) : new BBox(crs);
        for (i = 0; i < dim; i++) {
            bbox.setRange(i, min[i], max[i]);
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
    public static Array packIntegerDataType(Array array) {
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
                Array cp = of(array.getSampleSystem(), dt, array.getLength());
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
    public static Array unmodifiable(Array array) {
        return new ArrayUnmodifiable(array);
    }

    /**
     * Create an unmodifiable view of the given cursor.
     *
     * @param cursor not null
     * @return unmodifiable view of the array cursor.
     */
    public static Cursor unmodifiable(Cursor cursor) {
        return new CursorUnmodifiable(cursor);
    }

    /**
     * Create an concatenated view of the given arrays.
     *
     * @param arrays not null
     * @return unmodifiable view of the array cursor.
     */
    public static Array concatenate(Array ... arrays) {
        return new ArrayConcatenated(arrays);
    }

    /**
     * Group each tuple in given arrays.
     * Returns an array of same length and sum of all dimensions.
     *
     * @param arrays not null or empty
     * @return never null, if arrays size is one, then a copy is returned
     */
    public static Array group(Array ... arrays) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("arrays", arrays);
        final long length = arrays[0].getLength();
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

        final Array result;
        if (undefined) {
            SampleSystem sampleSystem = new SampleSystem(dataType, allDims.toArray(new SampleDimension[0]));
            result = of(sampleSystem, dataType, length);
        } else {
            result = of(SampleSystem.of(CRS.compound(crs)), dataType, length);
        }

        if (length == 0) return result;

        final Cursor resultCursor = result.cursor();
        int offset = 0;
        for (int a = 0; a < arrays.length; a++) {
            final int dim = arrays[a].getDimension();
            Cursor cursor = arrays[a].cursor();
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
    public static Array subset(Array array, int ... selection) {
        return subset(array, ArraysExt.copyAsLongs(selection));
    }

    /**
     * Create a view of only the given selected index tuples in the array.
     */
    public static Array subset(Array array, long ... selection) {
        return new Subset(array, selection);
    }

    /**
     * View TupleArray as a list.
     */
    public static List<Tuple> asList(Array array) {
        final int size = Math.toIntExact(array.getLength());
        return new AbstractList<Tuple>() {
            @Override
            public Tuple get(int index) {
                return array.get(index);
            }

            @Override
            public int size() {
                return size;
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
    public static void shuffle(Array array) {
        final Random random = new Random();
        for (long i = array.getLength() - 1; i >= 1; i--) {
            array.swap(i, random.nextLong(i));
        }
    }

    /**
     * Sort tuple array, in place, using quick sort algorithm.
     * Inspired by : https://www.geeksforgeeks.org/quick-sort-algorithm/
     */
    public static void quickSort(Array array, Comparator<Tuple> comparator) {
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
    public static void quickSort(Array array, Comparator<Tuple> comparator, long low, long high) {
        quickSort(array, array.cursor(), array.cursor(), comparator, low, high);
    }

    private static long partition(Array array, Cursor cursor1, Cursor cursor2, Comparator<Tuple> comparator, long low, long high) {
        //pick the middle point as pivot, in case of already sorted arrays
        //it speeds up the operation and prevents the quicksort to make a java.lang.StackOverflowError
        array.swap((low+high)/2, high);
        cursor1.moveTo(high);
        final Tuple pivot = cursor1.samples();
        long i = low - 1;
        for (long j = low; j <= high - 1; j++) {
            cursor2.moveTo(j);
            if (comparator.compare(cursor2.samples(),pivot) < 0) {
                i++;
                if (i != j) array.swap(i, j);
            }
        }
        array.swap(i + 1, high);
        return i + 1;
    }

    private static void quickSort(Array array, Cursor cursor1, Cursor cursor2, Comparator<Tuple> comparator, long low, long high) {

        if (low < high) {
            final long pi = partition(array, cursor1, cursor2, comparator, low, high);
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
    public static RecursiveAction quickSortAction(Array array, Comparator<Tuple> comparator, long low, long high) {
        return new QuickSortAction(array, array.cursor(), array.cursor(), comparator, low, high);
    }

    private static final class QuickSortAction extends RecursiveAction {

        private final Array array;
        private final Cursor cursor1;
        private final Cursor cursor2;
        private final Comparator<Tuple> comparator;
        private final long low;
        private final long high;

        public QuickSortAction(Array array, Cursor cursor1, Cursor cursor2, Comparator<Tuple> comparator, long low, long high) {
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
                final long pi = partition(array, cursor1, cursor2, comparator, low, high);
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

    private NDArrays(){}

    private static class Subset extends AbstractArray {

        private final Array base;
        private final long[] index;

        public Subset(Array base, long[] index) {
            this.base = base;
            this.index = index;
        }

        @Override
        public long getLength() {
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
        public void get(long index, Tuple buffer) {
            base.get(this.index[Math.toIntExact(index)], buffer);
        }

        @Override
        public void set(long index, Tuple buffer) {
            base.set(this.index[Math.toIntExact(index)], buffer);
        }

        @Override
        public Cursor cursor() {
            final Cursor bc = base.cursor();
            return new Cursor() {
                private long coordinate = -1;
                @Override
                public Tuple<?> samples() {
                    bc.moveTo(index[Math.toIntExact(coordinate)]);
                    return bc.samples();
                }

                @Override
                public long coordinate() {
                    return coordinate;
                }

                @Override
                public void moveTo(long coordinate) {
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
