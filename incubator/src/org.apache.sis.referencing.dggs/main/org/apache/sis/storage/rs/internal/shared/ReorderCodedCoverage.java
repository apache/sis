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
import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;


/**
 * View of a coded coverage with a selection of its bands, in any order.
 *
 * <ul>
 *   <li>A band of the source may be selected more than once.</li>
 *   <li>The selected bands may be renamed.</li>
 *   <li>The view is writable if the source is writable; writes go to the source.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ReorderCodedCoverage extends AbstractCodedCoverage {

    /**
     * The coverage providing the values.
     */
    private final CodedCoverage source;

    /**
     * For each band of this coverage, the index of the band in the source coverage.
     */
    private final int[] bands;

    /**
     * Creates a view of the given bands of the given coverage.
     *
     * @param  name    name of this coverage.
     * @param  source  the coverage providing the values.
     * @param  bands   for each band of this coverage, the index of the band in the source coverage.
     * @throws FactoryException if the coverage reference system cannot be built.
     * @throws IndexOutOfBoundsException if a band index is out of bounds.
     */
    public ReorderCodedCoverage(final GenericName name, final CodedCoverage source, final int... bands) throws FactoryException {
        this(name, source, bands, null);
    }

    /**
     * Creates a view of the given bands of the given coverage, with new band names.
     * The categories, units and background values of the bands are kept.
     *
     * @param  name       name of this coverage.
     * @param  source     the coverage providing the values.
     * @param  bands      for each band of this coverage, the index of the band in the source coverage.
     * @param  bandNames  for each band of this coverage, its new name, or {@code null} for the names of the source.
     * @throws FactoryException if the coverage reference system cannot be built.
     * @throws IndexOutOfBoundsException if a band index is out of bounds.
     * @throws IllegalArgumentException if the number of names is not the number of bands.
     */
    public ReorderCodedCoverage(final GenericName name, final CodedCoverage source, final int[] bands, final GenericName[] bandNames)
            throws FactoryException
    {
        super(name, source.getGeometry(), select(source.getSampleDimensions(), bands, bandNames));
        this.source = source;
        this.bands  = bands.clone();
    }

    /**
     * Returns the sample dimensions at the given indices, renamed if names are given.
     */
    private static List<SampleDimension> select(final List<SampleDimension> all, final int[] bands, final GenericName[] bandNames) {
        if (bandNames != null && bandNames.length != bands.length) {
            throw new IllegalArgumentException("Expected " + bands.length + " band names but got " + bandNames.length + '.');
        }
        final List<SampleDimension> selected = new ArrayList<>(bands.length);
        for (int i = 0; i < bands.length; i++) {
            final SampleDimension band = all.get(bands[i]);
            selected.add(bandNames == null ? band : rename(band, bandNames[i]));
        }
        return selected;
    }

    /**
     * Returns the given sample dimension with another name.
     * The categories, units and background value are preserved.
     */
    static SampleDimension rename(final SampleDimension band, final GenericName name) {
        return new SampleDimension(name, band.getBackground().orElse(null), band.getCategories());
    }

    @Override
    public CodeIterator createIterator() {
        return new BandIterator(source.createIterator());
    }

    /**
     * Returns an iterator writing in the source coverage.
     *
     * @throws UnsupportedOperationException if the source coverage is not writable.
     */
    @Override
    public WritableCodeIterator createWritableIterator() {
        return new WritableBandIterator(source.createWritableIterator());
    }

    /**
     * Iterator reading the selected bands of the source iterator.
     */
    private class BandIterator implements CodeIterator {

        private final CodeIterator base;

        BandIterator(final CodeIterator base) {
            this.base = base;
        }

        @Override
        public int[] getPosition() {
            return base.getPosition();
        }

        @Override
        public void moveTo(final int[] position) {
            base.moveTo(position);
        }

        @Override
        public boolean next() {
            return base.next();
        }

        @Override
        public int getNumBands() {
            return bands.length;
        }

        @Override
        public double getSampleDouble(final int band) {
            return base.getSampleDouble(bands[band]);
        }

        @Override
        public void rewind() {
            base.rewind();
        }
    }

    /**
     * Iterator reading and writing the selected bands of the source iterator.
     */
    private final class WritableBandIterator extends BandIterator implements WritableCodeIterator {

        private final WritableCodeIterator base;

        WritableBandIterator(final WritableCodeIterator base) {
            super(base);
            this.base = base;
        }

        @Override
        public void setSample(final int band, final double value) {
            base.setSample(bands[band], value);
        }

        @Override
        public void close() throws DataStoreException {
            base.close();
        }
    }
}
