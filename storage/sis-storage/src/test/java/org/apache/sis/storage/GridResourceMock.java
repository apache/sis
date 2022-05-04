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

import java.util.List;
import java.util.Collections;
import java.awt.image.BufferedImage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;

import static org.junit.Assert.*;


/**
 * A resource for one-banded images generated on-the-fly when {@link #read(GridGeometry, int...)} is invoked.
 * This class has some similarities with {@link org.apache.sis.internal.storage.MemoryGridResource} except that
 * the {@link GridCoverage} returned by {@link #read read(…)} is guaranteed to wrap an image having exactly the
 * requested size (i.e. the size specified by {@link GridGeometry#getExtent()}).
 * By contrast {@code org.apache.sis.internal.storage.MemoryGridResource} may return images larger than requested,
 * which make testing more difficult.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
final strictfp class GridResourceMock extends AbstractGridCoverageResource {
    /**
     * Grid geometry of this resource, specified at construction time.
     */
    private final GridGeometry gridGeometry;

    /**
     * A dummy sample dimension.
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * Creates a resource mock with the given grid geometry.
     */
    GridResourceMock(final GridGeometry gridGeometry) {
        super(null, false);
        assertNotNull(gridGeometry);
        this.gridGeometry     = gridGeometry;
        this.sampleDimensions = Collections.singletonList(new SampleDimension.Builder().setName(0).build());
    }

    /**
     * Returns the grid geometry specified at construction time.
     */
    @Override
    public GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns a dummy sample dimension. This resource always provide exactly one band.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // The list is unmodifiable.
    public List<SampleDimension> getSampleDimensions() {
        return sampleDimensions;
    }

    /**
     * Returns a grid geometry wrapping a dummy image having exactly the requested size.
     * The image will always be a {@link BufferedImage} with pixel coordinates starting at (0,0).
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for the whole domain.
     * @param  range   must be null, empty or a singleton containing only value 0.
     * @return the grid coverage for the specified domain.
     */
    @Override
    public GridCoverage read(GridGeometry domain, final int... range) {
        assertTrue(range == null || range.length == 0 || (range.length == 1 && range[0] == 0));
        if (domain == null) {
            domain = gridGeometry;
        } else {
            domain = gridGeometry.derive().subgrid(domain).build();
        }
        final GridExtent extent = domain.getExtent();
        final BufferedImage img = new BufferedImage(
                StrictMath.toIntExact(extent.getSize(0)),
                StrictMath.toIntExact(extent.getSize(1)),
                BufferedImage.TYPE_BYTE_BINARY);

        return new GridCoverage2D(domain, sampleDimensions, img);
    }
}
