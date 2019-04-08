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
package org.apache.sis.internal.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;

/**
 * Tests the {@link BufferedGridCoverage} implementation.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class BufferedGridCoverageTest extends TestCase {

    @Test
    public void testCoverage2D() {

        //create coverage
        final GridExtent extent = new GridExtent(null, new long[]{0,0}, new long[]{1,1}, true);
        final MathTransform gridToCrs = new AffineTransform2D(1, 0, 0, 1, 0, 0);
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final GridGeometry gridgeom = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);

        final MathTransform1D toUnits = (MathTransform1D) MathTransforms.linear(0.5, 100);
        final SampleDimension sd = new SampleDimension.Builder().setName("t").addQuantitative("data", NumberRange.create(-10, true, 10, true), toUnits, Units.CELSIUS).build();

        final BufferedGridCoverage coverage = new BufferedGridCoverage(gridgeom, Arrays.asList(sd), DataBuffer.TYPE_SHORT);

        BufferedImage img = (BufferedImage) coverage.render(null);
        img.getRaster().setSample(0, 0, 0, 0);
        img.getRaster().setSample(1, 0, 0, 5);
        img.getRaster().setSample(0, 1, 0, -5);
        img.getRaster().setSample(1, 1, 0, -10);

        //test not converted values
        RenderedImage notConverted = (BufferedImage) coverage.render(null);
        testSamples(notConverted, new double[][]{{0,5},{-5,-10}});

        //test converted values
        org.apache.sis.coverage.grid.GridCoverage convertedCoverage = coverage.forConvertedValues(true);
        BufferedImage converted = (BufferedImage) convertedCoverage.render(null);
        testSamples(converted, new double[][]{{100,102.5},{97.5,95}});

        //test writing in geophysic
        converted.getRaster().setSample(0, 0, 0, 70); // 70 = x * 0.5 + 100 // (70-100)/0.5 = x // x = -60
        converted.getRaster().setSample(1, 0, 0, 2.5);
        converted.getRaster().setSample(0, 1, 0, -8);
        converted.getRaster().setSample(1, 1, 0, -90);
        testSamples(notConverted, new double[][]{{-60,-195},{-216,-380}});

    }

    private void testSamples(RenderedImage image, double[][] values) {
        final Raster raster = image.getData();
        for (int y=0;y<values.length;y++) {
            for (int x=0;x<values[0].length;x++) {
                double value = raster.getSampleDouble(x, y, 0);
                Assert.assertEquals(values[y][x], value, 0.0);
            }
        }
    }

}
