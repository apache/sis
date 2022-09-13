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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.referencing.operation.MathTransform1D;

/**
 * Tests {@link ConvertedCoverageResource}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class ConvertedCoverageResourceTest extends TestCase {


    /**
     * Tests {@link ConvertedCoverageResource}.
     */
    @Test
    public void testConvert() throws DataStoreException {

        //source coverage
        final BufferedImage data = new BufferedImage(360, 180, BufferedImage.TYPE_BYTE_GRAY);
        final GridGeometry grid = new GridGeometry(new GridExtent(360,180), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()), GridOrientation.HOMOTHETY);
        final GridCoverage2D coverage = new GridCoverage2D(grid, null, data);
        final GridCoverageResource source = new MemoryGridResource(null, coverage);

        //converted coverage
        final MathTransform1D converter = (MathTransform1D) MathTransforms.linear(2, 10);
        final ConvertedCoverageResource converted = new ConvertedCoverageResource(source, new MathTransform1D[]{converter}, null);

        //ensure structure is preserved
        assertEquals(source.getGridGeometry(), converted.getGridGeometry());
        assertEquals(source.getSampleDimensions(), converted.getSampleDimensions());
        assertEquals(source.getIdentifier(), converted.getIdentifier());

        //ensure values are modified
        final RenderedImage convertedImage = converted.read(grid, 0).render(grid.getExtent());
        final PixelIterator ite = PixelIterator.create(convertedImage);
        ite.moveTo(0, 0);
        assertEquals(10, ite.getSample(0));
    }
}
