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

import java.util.Arrays;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.referencing.rs.CodeOperation;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.WritableCodeIterator;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ResampledCodedCoverage extends AbstractCodedCoverage {

    private final CodedCoverage base;

    private final CodeTransform sourceGridTrs;
    private final CodeTransform targetGridTrs;
    private final CodeOperation operation;


    public ResampledCodedCoverage(GenericName name, CodedCoverage base, CodedGeometry target) throws FactoryException {
        super(name, target, base.getSampleDimensions());
        this.base = base;

        final CodedGeometry baseGeometry = base.getGeometry();
        sourceGridTrs = baseGeometry.getGridToRS();
        targetGridTrs = target.getGridToRS();
        operation = ReferenceSystems.findOperation(target.getReferenceSystem(), baseGeometry.getReferenceSystem(), null);
    }

    @Override
    public CodeIterator createIterator() {
        return new BandedIterator();
    }

    @Override
    public WritableCodeIterator createWritableIterator() {
        throw new UnsupportedOperationException("Not supported.");
    }

    private class BandedIterator implements CodeIterator {

        private long linearPosition = -1;
        private final CodeIterator sourceIterator;
        private final long nbCell;

        private boolean sourceBanded;
        private boolean sourceMoved = false;
        private boolean sourceExist = false;

        BandedIterator() {
            sourceIterator = base.createIterator();
            nbCell = extent.getLatticePointCount();
            sourceBanded = sourceIterator instanceof CodeIterator;
        }

        @Override
        public int getNumBands() {
            return base.getSampleDimensions().size();
        }

        private long toLinearPosition(int[] pos) {
            long p = 0;
            for (int i = 0; i < dimension; i++) {
                p += pos[i] * dimStep[i];
            }
            return p;
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
            sourceMoved = false;
        }

        @Override
        public boolean next() {
            sourceMoved = false;
            if (linearPosition < nbCell-1) {
                linearPosition ++;
                return true;
            } else {
                return false;
            }
        }

        private void moveSource() {
            if (sourceMoved) return;
            sourceMoved = true;
            try {
                final int[] targetPos = getPosition();
                final Code targetCode = targetGridTrs.toCode(targetPos);
                final Code sourceCode = operation.transform(targetCode, null);
                int[] sourcePos = sourceGridTrs.toGrid(sourceCode);
                sourceIterator.moveTo(sourcePos);
                sourceExist = true;
            } catch (TransformException | IllegalArgumentException e) {
                sourceExist = false;
            }
        }

        @Override
        public void rewind() {
            linearPosition = -1;
            sourceMoved = false;
        }

        @Override
        public double getSampleDouble(int band) {
            moveSource();
            if (sourceExist) {
                return sourceIterator.getSampleDouble(band);
            }
            return Double.NaN;
        }

    }

}
