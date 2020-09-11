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
package org.apache.sis.internal.storage.query;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.internal.storage.AbstractGridResource;
import org.apache.sis.storage.DataStoreException;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class MockGridResource extends AbstractGridResource{

    private final GridGeometry gridGeometry;
    private final List<SampleDimension> sampleDimensions;

    MockGridResource(GridGeometry gridGeometry) {
        super(null);
        this.gridGeometry = gridGeometry;
        this.sampleDimensions = Arrays.asList(new SampleDimension.Builder().setName(0).build());
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return gridGeometry;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return sampleDimensions;
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        if (domain == null) {
            domain = gridGeometry;
        }

        GridGeometry newGrid = gridGeometry.derive()
                .rounding(GridRoundingMode.ENCLOSING)
                .subgrid(domain)
                .build();

        List<SampleDimension> bands = getSampleDimensions();
        if (range != null && range.length > 0) {
            final List<SampleDimension> valids = new ArrayList<>(range.length);
            for (int i : range) {
                valids.add(bands.get(i));
            }
            bands = valids;
        }

        BufferedImage img = new BufferedImage(
                (int) newGrid.getExtent().getSize(0),
                (int) newGrid.getExtent().getSize(1),
                BufferedImage.TYPE_BYTE_GRAY);

        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setValues(img);
        gcb.setDomain(newGrid);
        gcb.setRanges(bands.get(0));
        return gcb.build();
    }

}
