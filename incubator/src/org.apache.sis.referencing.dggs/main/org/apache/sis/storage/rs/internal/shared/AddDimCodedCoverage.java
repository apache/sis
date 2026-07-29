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
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author jsorel
 */
public final class AddDimCodedCoverage extends CodedCoverage {

    private final CodedCoverage source;
    private final GridGeometry slice;
    private final CodedGeometry compound;

    public AddDimCodedCoverage(CodedCoverage source, GridGeometry slice) {
        this.source = source;
        this.slice = slice;
        compound = CodedGeometry.compound(source.getGeometry(), new CodedGeometry(slice));
    }

    public CodedCoverage getSource() {
        return source;
    }

    @Override
    public CodedGeometry getGeometry() {
        return compound;
    }

    @Override
    public CodeIterator createIterator() {
        return new Ite();
    }

    @Override
    public WritableCodeIterator createWritableIterator() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public double[] getResolution(boolean allowEstimate) throws DataStoreException {
        double[] resolution = source.getResolution(allowEstimate);
        resolution = Arrays.copyOf(resolution, resolution.length+1);

        return resolution;
    }

    @Override
    public GridCoverage sample(GridGeometry fullArea, GridGeometry tileArea) throws CannotEvaluateException {
        return source.sample(fullArea, tileArea);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        try {
            return ReferenceSystems.getLeaningCRS(compound.getReferenceSystem());
        } catch (FactoryException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        Optional<Envelope> envelope = source.getEnvelope();
        if (envelope.isEmpty()) return envelope;
        try {
            return Optional.of(Envelopes.compound(envelope.get(), slice.getEnvelope()));
        } catch (FactoryException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<SampleDimension> getSampleDimensions() {
        return source.getSampleDimensions();
    }

    @Override
    public Evaluator evaluator() {
        return new Eval();
    }

    private final class Ite implements CodeIterator {

        private final CodeIterator base;

        public Ite() {
            this.base = source.createIterator();
        }

        @Override
        public int[] getPosition() {
            int[] position = base.getPosition();
            return Arrays.copyOf(position, position.length+1);
        }

        @Override
        public void moveTo(int[] zid) {
            if (zid[zid.length] != 0) {
                throw new IllegalArgumentException("Zone " + zid[0] +" is not part of this coverage");
            }
            base.moveTo(zid);
        }

        @Override
        public boolean next() {
            return base.next();
        }

        @Override
        public int getNumBands() {
            return base.getNumBands();
        }

        @Override
        public double getSampleDouble(int band) {
            return base.getSampleDouble(band);
        }

        @Override
        public void rewind() {
            base.rewind();
        }

    }

    private final class Eval implements Evaluator {

        private final Evaluator base;

        private Eval() {
            base = source.evaluator();
        }

        @Override
        public BandedCoverage getCoverage() {
            return AddDimCodedCoverage.this;
        }

        @Override
        public boolean isNullIfOutside() {
            return base.isNullIfOutside();
        }

        @Override
        public void setNullIfOutside(boolean flag) {
            base.setNullIfOutside(flag);
        }

        @Override
        public boolean isWraparoundEnabled() {
            return base.isWraparoundEnabled();
        }

        @Override
        public void setWraparoundEnabled(boolean allow) {
            base.setWraparoundEnabled(allow);
        }

        @Override
        public double[] apply(DirectPosition point) throws CannotEvaluateException {
            //TODO check additional dimension
            return base.apply(point);
        }

    }
}
