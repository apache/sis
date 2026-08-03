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
package org.apache.sis.storage.dggs.internal.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridCoverageProcessor;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FeatureSetAsDiscreteGlobalGridResource extends AbstractResource implements DiscreteGlobalGridResource {

    private final List<CodedGeometry> gridGeometries = new ArrayList<>();
    private final DiscreteGlobalGridCoverageProcessor processor;
    private final FeatureSet featureSet;
    private final int defaultDepth;
    private final int maxRelativeDepth;
    private final NumberRange<Integer> availableDepths;

    //caches
    private List<SampleDimension> sampleDimensions;

    public FeatureSetAsDiscreteGlobalGridResource(DiscreteGlobalGridReferenceSystem dggrs, FeatureSet featureSet, DiscreteGlobalGridCoverageProcessor processor) {
        this(featureSet, processor, dggrs);
    }

    public FeatureSetAsDiscreteGlobalGridResource(FeatureSet featureSet, DiscreteGlobalGridCoverageProcessor processor, DiscreteGlobalGridReferenceSystem ... dggrs) {
        super(null);
        this.processor = processor;
        this.featureSet = featureSet;
        this.defaultDepth = 3;
        this.maxRelativeDepth = 10;
        for (DiscreteGlobalGridReferenceSystem dggs : dggrs) {
            gridGeometries.add(DiscreteGlobalGridGeometry.unstructured(dggs, null, null));
        }
        this.availableDepths = NumberRange.create(0, true, dggrs[0].getGridSystem().getHierarchy().getGrids().size()-1, true);
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return featureSet.getIdentifier();
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return featureSet.getEnvelope();
    }

    @Override
    protected Metadata createMetadata() throws DataStoreException {
        return featureSet.getMetadata();
    }

    public FeatureSet getOrigin() {
        return featureSet;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        init();
        return Collections.unmodifiableList(sampleDimensions);
    }

    @Override
    public DiscreteGlobalGridGeometry getGridGeometry() {
        return (DiscreteGlobalGridGeometry) gridGeometries.get(0);
    }

    @Override
    public List<CodedGeometry> getAlternateGridGeometry() throws DataStoreException {
        return Collections.unmodifiableList(gridGeometries);
    }

    @Override
    public int getDefaultDepth() {
        return defaultDepth;
    }

    @Override
    public int getMaxRelativeDepth() {
        return maxRelativeDepth;
    }

    @Override
    public NumberRange<Integer> getAvailableDepths() {
        return availableDepths;
    }

    private synchronized void init() throws DataStoreException {
        if (sampleDimensions != null) return;

        final FeatureType type = featureSet.getType();

        //create a sample type with only attribute types
        sampleDimensions = new ArrayList<>();
        for (PropertyType pt : type.getProperties(true)) {
            if (AttributeConvention.contains(pt.getName())) continue;
            if (!(pt instanceof AttributeType at)) continue;
            final Class valueClass = at.getValueClass();
            if (Geometry.class.isAssignableFrom(valueClass)) continue;

            //build matching sample dimensions
            final SampleDimension.Builder sdb = new SampleDimension.Builder();
            sdb.setName(at.getName());
            final SampleDimension sd = sdb.build();
            sampleDimensions.add(sd);
        }
    }

    @Override
    public CodedCoverage read(CodedGeometry grid, int... range) throws DataStoreException {
        init();
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridResource.toDiscreteGlobalGridGeometry(grid);

        final Optional<Envelope> envelope = featureSet.getEnvelope();
        if (!envelope.isEmpty()) {
            Envelope e = envelope.get();
            final CoordinateReferenceSystem crs2d = CRS.getHorizontalComponent(e.getCoordinateReferenceSystem());
            try {
                e = Envelopes.transform(e, crs2d);
                Envelope gridEnv = geometry.getEnvelope(crs2d);

                if (!GeneralEnvelope.castOrCopy(gridEnv).intersects(e)) {
                    //no data
                    throw new NoSuchDataException();
                }
            } catch (TransformException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        }

        try {
            return processor.resample(featureSet, geometry, sampleDimensions);
        } catch (TransformException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

}
