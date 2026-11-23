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

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;


/**
 * Stack of the bands of several coded coverages sharing the same geometry.
 * The bands of the first coverage come first, then the bands of the second coverage, <i>etc.</i>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ReorderCodedCoverage
 */
public final class CompoundCodedCoverage extends AbstractCodedCoverage{

    private final CodedCoverage[] coverages;

    /**
     * For each band of this coverage, the index of the coverage providing it.
     */
    private final int[] bandToCoverage;

    /**
     * For each band of this coverage, the band index in the coverage providing it.
     */
    private final int[] bandToCoverageBand;

    /**
     * Creates a coverage with all the bands of the given coverages, in order.
     * All coverages shall have the same geometry.
     * The sample dimensions are those of the coverages.
     */
    public CompoundCodedCoverage(GenericName name, CodedCoverage[] coverages) throws FactoryException {
        super(name, coverages[0].getGeometry(),
              Stream.of(coverages).map(CodedCoverage::getSampleDimensions).flatMap(List::stream).toList());
        this.coverages = coverages;
        this.bandToCoverage = coverageOfBands(coverages);
        this.bandToCoverageBand = bandsInCoverages(coverages);
    }

    /**
     * Returns, for each band of all coverages in order, the index of the coverage providing it.
     */
    private static int[] coverageOfBands(final CodedCoverage[] coverages) {
        return IntStream.range(0, coverages.length)
                .flatMap((i) -> IntStream.generate(() -> i).limit(coverages[i].getSampleDimensions().size()))
                .toArray();
    }

    /**
     * Returns, for each band of all coverages in order, the band index in the coverage providing it.
     */
    private static int[] bandsInCoverages(final CodedCoverage[] coverages) {
        return Stream.of(coverages)
                .flatMapToInt((c) -> IntStream.range(0, c.getSampleDimensions().size()))
                .toArray();
    }

    @Override
    public CodeIterator createIterator() {
        return new BandedIterator();
    }

    @Override
    public WritableCodeIterator createWritableIterator() {
        throw new UnsupportedOperationException("Not supported.");
    }

    private final class BandedIterator implements CodeIterator {

        private final CodeIterator[] iterators;

        BandedIterator() {
            iterators = new CodeIterator[coverages.length];
            for (int i = 0; i < iterators.length; i++) {
                iterators[i] = coverages[i].createIterator();
            }
        }

        @Override
        public int getNumBands() {
            return bandToCoverage.length;
        }

        @Override
        public double getSampleDouble(int band) {
            final CodeIterator ci = iterators[bandToCoverage[band]];
            return ci.getSampleDouble(bandToCoverageBand[band]);
        }

        @Override
        public int[] getPosition() {
            return iterators[0].getPosition();
        }

        @Override
        public void moveTo(int[] zid) {
            for (int i = 0; i < iterators.length; i++) {
                iterators[i].moveTo(zid);
            }
        }

        /**
         * Moves the first iterator to its next cell, and the other iterators to the same position.
         * The iteration order of the coverages may differ.
         */
        @Override
        public boolean next() {
            if (!iterators[0].next()) {
                return false;
            }
            final int[] position = iterators[0].getPosition();
            for (int i = 1; i < iterators.length; i++) {
                iterators[i].moveTo(position);
            }
            return true;
        }

        @Override
        public void rewind() {
            iterators[0].rewind();
        }

    }

}
