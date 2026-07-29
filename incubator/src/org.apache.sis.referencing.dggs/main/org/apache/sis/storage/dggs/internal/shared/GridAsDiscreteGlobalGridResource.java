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

import java.util.List;
import java.util.Optional;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.dggs.DiscreteGlobalGridCoverageProcessor;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.storage.dggs.DiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

/**
 * View a grid coverage resource as a dggrs coverage resource.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GridAsDiscreteGlobalGridResource extends AbstractResource implements DiscreteGlobalGridResource {

    private final GridCoverageResource source;
    private final DiscreteGlobalGridGeometry gridGeometry;
    private final int maxLevel;

    public GridAsDiscreteGlobalGridResource(DiscreteGlobalGridReferenceSystem dggrs, GridCoverageResource resource)
            throws DataStoreException, IncommensurableException, TransformException {
        super(null);
        if (resource.getGridGeometry().getDimension() != 2) {
            throw new IllegalArgumentException("Only 2D coverage resources are supported. Use GridAsReferencedGridResource for N dimensions.");
        }
        this.source = resource;
        this.gridGeometry = DiscreteGlobalGridGeometry.unstructured(dggrs, null, null);

        Quantity<?> res = computeAverageResolution(resource.getGridGeometry());
        DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        coder.setPrecision(res, null);
        maxLevel = coder.getPrecisionLevel();
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return source.getIdentifier();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return source.getSampleDimensions();
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return source.getEnvelope();
    }

    @Override
    public DiscreteGlobalGridGeometry getGridGeometry() {
        return gridGeometry;
    }

    @Override
    public NumberRange<Integer> getAvailableDepths() {
        return NumberRange.create(0, true, maxLevel, true);
    }

    @Override
    public int getDefaultDepth() {
        return 0;
    }

    @Override
    public int getMaxRelativeDepth() {
        return 9;
    }

    @Override
    public CodedCoverage read(CodedGeometry grid, int... range) throws DataStoreException {
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridResource.toDiscreteGlobalGridGeometry(grid);
        final DiscreteGlobalGridCoverageProcessor processor = new DiscreteGlobalGridCoverageProcessor();
        try {
            return processor.resample(source, geometry, range);
        } catch (TransformException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * compute average resolution of a grid geometry
     */
    public static Quantity<?> computeAverageResolution(GridGeometry domain) throws TransformException {
        final SingleCRS horizontalCrs = CRS.getHorizontalComponent(domain.getCoordinateReferenceSystem());
        //todo we should slice the domain to horizontal crs to be sure we have the resolution at index 0
        //we use 1/100 of the resolution as distance to avoid problems with pole to pole coverages with very low resolution
        final double resScale = 100;
        final double resolution = domain.getResolution(true)[0] / resScale;
        final Envelope envelope = domain.getEnvelope(horizontalCrs);
        final DirectPosition start = GeneralEnvelope.castOrCopy(envelope).getMedian();
        final DirectPosition end = new DirectPosition2D(start.getCoordinateReferenceSystem());
        end.setCoordinate(0, start.getCoordinate(0) + resolution);
        end.setCoordinate(1, start.getCoordinate(1));
        final GeodeticCalculator calculator = GeodeticCalculator.create(horizontalCrs);
        calculator.setStartPoint(start);
        calculator.setEndPoint(end);
        final double distance = calculator.getGeodesicDistance();
        return Quantities.create(distance * resScale, Units.METRE);
    }

}
