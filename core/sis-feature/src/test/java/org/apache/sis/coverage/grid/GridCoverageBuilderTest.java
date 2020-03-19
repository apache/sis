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
package org.apache.sis.coverage.grid;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.NullArgumentException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * Tests the {@link GridCoverageBuilder} helper class.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public class GridCoverageBuilderTest extends TestCase {

    /**
     * Tests {@link GridCoverageBuilder#setValues(Image)}.
     */
    @Test
    public void createFromImageTest() {

        final RenderedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);

        final GridCoverageBuilder builder = new GridCoverageBuilder();
        builder.setValues(image);

        { //sample dimensions and an undefined grid geometry should be created
            GridCoverage coverage = builder.build();
            Assert.assertEquals(4, coverage.getSampleDimensions().size());
            Assert.assertFalse(coverage.getGridGeometry().isDefined(GridGeometry.CRS));
            Assert.assertFalse(coverage.getGridGeometry().isDefined(GridGeometry.ENVELOPE));
            Assert.assertFalse(coverage.getGridGeometry().isDefined(GridGeometry.GRID_TO_CRS));
        }

        { //should cause an exceptin number of sample dimensions do not match
            builder.setRanges(new SampleDimension.Builder().setName(0).build());
            try {
                GridCoverage coverage = builder.build();
                Assert.fail("Wrong number of sample dimensions, build should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }

        { //number of sample matches, should build correctly
            final SampleDimension r0 = new SampleDimension.Builder().setName(0).build();
            final SampleDimension r1 = new SampleDimension.Builder().setName(1).build();
            final SampleDimension r2 = new SampleDimension.Builder().setName(2).build();
            final SampleDimension r3 = new SampleDimension.Builder().setName(3).build();
            builder.setRanges(r0, r1, r2, r3);
            GridCoverage coverage = builder.build();
        }

        { //should cause an exceptin extent size do not match
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 10);
            env.setRange(1, 0, 10);
            final GridGeometry grid = new GridGeometry(new GridExtent(8, 6), env);
            builder.setDomain(grid);
            try {
                GridCoverage coverage = builder.build();
                Assert.fail("Wrong extent size, build should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }

        { //extent size matches, should build correctly
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 10);
            env.setRange(1, 0, 10);
            final GridGeometry grid = new GridGeometry(new GridExtent(10, 10), env);
            builder.setDomain(grid);
            GridCoverage coverage = builder.build();
        }
    }

    /**
     * Tests {@link GridCoverageBuilder#setValues(Raster)}.
     */
    @Test
    public void createFromRasterTest() {

        final WritableRaster raster = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR).getRaster();

        final GridCoverageBuilder builder = new GridCoverageBuilder();
        builder.setValues(raster);

        { //sample dimensions and an undefined grid geometry should be created
            GridCoverage coverage = builder.build();
            Assert.assertEquals(3, coverage.getSampleDimensions().size());
            Assert.assertFalse(coverage.getGridGeometry().isDefined(GridGeometry.CRS));
            Assert.assertFalse(coverage.getGridGeometry().isDefined(GridGeometry.ENVELOPE));
            Assert.assertFalse(coverage.getGridGeometry().isDefined(GridGeometry.GRID_TO_CRS));
        }

        { //should cause an exceptin number of sample dimensions do not match
            builder.setRanges(new SampleDimension.Builder().setName(0).build());
            try {
                GridCoverage coverage = builder.build();
                Assert.fail("Wrong number of sample dimensions, build should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }

        { //number of sample matches, should build correctly
            final SampleDimension r0 = new SampleDimension.Builder().setName(0).build();
            final SampleDimension r1 = new SampleDimension.Builder().setName(1).build();
            final SampleDimension r2 = new SampleDimension.Builder().setName(2).build();
            builder.setRanges(r0, r1, r2);
            GridCoverage coverage = builder.build();
        }

        { //should cause an exceptin extent size do not match
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 10);
            env.setRange(1, 0, 10);
            final GridGeometry grid = new GridGeometry(new GridExtent(8, 6), env);
            builder.setDomain(grid);
            try {
                GridCoverage coverage = builder.build();
                Assert.fail("Wrong extent size, build should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }

        { //extent size matches, should build correctly
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 10);
            env.setRange(1, 0, 10);
            final GridGeometry grid = new GridGeometry(new GridExtent(10, 10), env);
            builder.setDomain(grid);
            GridCoverage coverage = builder.build();
        }
    }

    /**
     * Tests {@link GridCoverageBuilder#setValues(DataBuffer)}.
     */
    @Test
    public void createFromBufferTest() {

        final DataBuffer buffer = new DataBufferInt(new int[]{1,2,3,4,5,6},6);

        final GridCoverageBuilder builder = new GridCoverageBuilder();
        builder.setValues(buffer);
        final SampleDimension r0 = new SampleDimension.Builder().setName(0).build();
        builder.setRanges(r0);

        { // size is undefined, build should fail
            try {
                GridCoverage coverage = builder.build();
                Assert.fail("Extent is undefined, build should fail");
            } catch (NullArgumentException ex) {
                //ok
            }
        }

        { //should cause an exceptin extent size do not match
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 10);
            env.setRange(1, 0, 10);
            final GridGeometry grid = new GridGeometry(new GridExtent(8, 6), env);
            builder.setDomain(grid);
            try {
                GridCoverage coverage = builder.build();
                Assert.fail("Wrong extent size, build should fail");
            } catch (IllegalGridGeometryException ex) {
                //ok
            }
        }

        { //extent size matches, should build correctly
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 10);
            env.setRange(1, 0, 10);
            final GridGeometry grid = new GridGeometry(new GridExtent(3, 2), env);
            builder.setDomain(grid);
            GridCoverage coverage = builder.build();
        }

    }

}
