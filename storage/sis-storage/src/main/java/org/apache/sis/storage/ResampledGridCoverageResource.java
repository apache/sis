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
package org.apache.sis.storage;

import java.util.Optional;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.storage.DerivedGridCoverageResource;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

/**
 * A decoration over a resource to resample it on a specified {@link #outputGeometry grid geometry} when a {@link #read(GridGeometry, int...)} is triggered.
 *
 * @see ResourceProcessor#resample(GridCoverageResource, CoordinateReferenceSystem, GenericName)
 * @see ResourceProcessor#resample(GridCoverageResource, GridGeometry, GenericName)
 *
 * @author Alexis Manin (Geomatys)
 */
final class ResampledGridCoverageResource extends DerivedGridCoverageResource {

    private final GridCoverageProcessor processor;
    private final GridGeometry outputGeometry;

    ResampledGridCoverageResource(GridCoverageResource source, GridGeometry outputGeometry, GenericName name, GridCoverageProcessor processor) {
        super(name, source);
        this.processor = processor;
        this.outputGeometry = outputGeometry;
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        return outputGeometry.isDefined(GridGeometry.ENVELOPE)
                ? Optional.of(outputGeometry.getEnvelope())
                : Optional.empty();
    }

    @Override
    public GridGeometry getGridGeometry() { return outputGeometry; }

    @Override
    public GridCoverageResource subset(Query query) throws DataStoreException {
        if (query instanceof CoverageQuery) {
            CoverageQuery cq = (CoverageQuery) query;
            GridGeometry selection = cq.getSelection();
            if (selection != null) {
                selection = outputGeometry.derive().subgrid(selection).build();
                final GridCoverageResource updatedResample = new ResampledGridCoverageResource(source, selection, null, processor);
                cq = cq.clone();
                cq.setSelection((GridGeometry) null);
                return updatedResample.subset(cq);
            }
        }

        return super.subset(query);
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        domain = domain == null
                ? outputGeometry
                : outputGeometry.derive().subgrid(domain).build();

        GridCoverage rawRead = source.read(domain, ranges);
        try {
            return processor.resample(rawRead, domain);
        } catch (TransformException e) {
            throw new DataStoreException("Cannot adapt source to resampling domain", e);
        }
    }
}
