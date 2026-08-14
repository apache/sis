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
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CompoundCodedCoverage extends AbstractCodedCoverage{

    private final CodedCoverage[] coverages;
    private final int[] bandToCoverage;
    private final int[] bandToCoverageBand;

    public CompoundCodedCoverage(GenericName name, CodedCoverage[] coverages, List<SampleDimension> sampleDimensions) throws FactoryException {
        super(name, coverages[0].getGeometry(), sampleDimensions == null ? List.of(coverages).stream().map(CodedCoverage::getSampleDimensions).flatMap(List::stream).toList() : sampleDimensions);
        this.coverages = coverages;
        this.bandToCoverage = new int[this.sampleDimensions.size()];
        this.bandToCoverageBand = new int[bandToCoverage.length];

        int idx = 0;
        for (int i = 0; i < coverages.length; i++) {
            final List<SampleDimension> sds = coverages[i].getSampleDimensions();
            int si = 0;
            for (SampleDimension sd : sds) {
                bandToCoverage[idx] = i;
                bandToCoverageBand[idx] = si;
                idx++;
                si++;
            }
        }
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

        @Override
        public boolean next() {
            boolean hasNext = false;
            for (int i = 0; i < iterators.length; i++) {
                hasNext = iterators[i].next();
            }
            return hasNext;
        }

        @Override
        public void rewind() {
            for (int i = 0; i < iterators.length; i++) {
                iterators[i].rewind();
            }
        }

    }

}
