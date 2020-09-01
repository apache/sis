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
package org.apache.sis.internal.storage;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.datum.PixelInCell;

/**
 * A GridCoverage resource in memory. The GridCoverage is specified at construction time.
 * Metadata can be specified by overriding {@link #createMetadata(MetadataBuilder)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class MemoryGridResource extends AbstractGridResource {

    private final GridCoverage2D coverage;

    /**
     * Creates a new coverage stored in memory.
     *
     * @param parent     listeners of the parent resource, or {@code null} if none.
     * @param coverage   stored coverage, coverage will not be copied, can not be null.
     */
    public MemoryGridResource(final StoreListeners parent, final GridCoverage2D coverage) {
        super(parent);
        ArgumentChecks.ensureNonNull("coverage", coverage);
        this.coverage = coverage;
    }

    /**
     * {@inheritDoc }
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return coverage.getGridGeometry();
    }

    /**
     * {@inheritDoc }
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return coverage.getSampleDimensions();
    }

    /**
     * {@inheritDoc }
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {

        //quick return of the original coverage
        if (domain == null && (range == null || range.length == 0)) {
            return coverage;
        }

        /*
        Subsampling is ignored because it is an expensive operation.
        Clipping and range selection are light and current implementation
        do not copy any data.
        */
        GridGeometry areaOfInterest = coverage.getGridGeometry();
        GridExtent intersection = areaOfInterest.getExtent();
        if (domain != null) {
            final GridDerivation derivation = getGridGeometry().derive()
                    .rounding(GridRoundingMode.ENCLOSING)
                    .subgrid(domain);
            intersection = derivation.getIntersection();
            /*
            Extent is left as null, the grid coverage builder will extract the size from the image.
            Image size and offset may not be exactly what was requested by the intersection.
            */
            areaOfInterest = new GridGeometry(null,
                    PixelInCell.CELL_CENTER,
                    areaOfInterest.getGridToCRS(PixelInCell.CELL_CENTER),
                    areaOfInterest.getCoordinateReferenceSystem());
        }

        RenderedImage render = coverage.render(intersection);

        /*
        Select range.
        */
        List<SampleDimension> sampleDimensions = coverage.getSampleDimensions();
        if (range != null && range.length > 0) {
            render = new ImageProcessor().selectBands(render, range);
            final List<SampleDimension> sds = new ArrayList<>();
            for (int i : range) {
                sds.add(sampleDimensions.get(i));
            }
            sampleDimensions = sds;
        }

        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setValues(render);
        gcb.setRanges(sampleDimensions);
        gcb.setDomain(areaOfInterest);
        return gcb.build();
    }

}
