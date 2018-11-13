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
package org.apache.sis.internal.netcdf;

import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.opengis.test.dataset.TestData;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link GridGeometry} implementation. The default implementation tests
 * {@code org.apache.sis.internal.netcdf.ucar.GridGeometryWrapper} since the UCAR
 * library is our reference implementation. However subclasses can override the
 * {@link #createDecoder(TestData)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@DependsOn(VariableTest.class)
public strictfp class GridGeometryTest extends TestCase {
    /**
     * Optionally filters out some grid geometries that shall be ignored by the tests.
     * The default implementation returns the given array unmodified. This method is overridden by
     * {@code GridGeometryInfoTest} in order to ignore one-dimensional coordinate systems created
     * by {@code GridGeometry} but not by the UCAR library.
     *
     * @param  geometries  the grid geometries created by {@link Decoder}.
     * @return the grid geometries to test.
     */
    protected GridGeometry[] filter(final GridGeometry[] geometries) {
        return geometries;
    }

    /**
     * Tests {@link GridGeometry#getSourceDimensions()} and {@link GridGeometry#getTargetDimensions()}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testDimensions() throws IOException, DataStoreException {
        GridGeometry geometry = getSingleton(filter(selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getGridGeometries()));
        assertEquals("getSourceDimensions()", 2, geometry.getSourceDimensions());
        assertEquals("getTargetDimensions()", 2, geometry.getTargetDimensions());

        geometry = getSingleton(filter(selectDataset(TestData.NETCDF_4D_PROJECTED).getGridGeometries()));
        assertEquals("getSourceDimensions()", 4, geometry.getSourceDimensions());
        assertEquals("getTargetDimensions()", 4, geometry.getTargetDimensions());
    }

    /**
     * Tests {@link GridGeometry#getAxes()} on a two-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    @DependsOnMethod("testDimensions")
    public void testAxes2D() throws IOException, DataStoreException {
        final Axis[] axes = getSingleton(filter(selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getGridGeometries())).getAxes();
        assertEquals(2, axes.length);
        final Axis x = axes[1];
        final Axis y = axes[0];

        assertEquals('λ', x.abbreviation);
        assertEquals('φ', y.abbreviation);

        assertArrayEquals(new int[] {1}, x.sourceDimensions);
        assertArrayEquals(new int[] {0}, y.sourceDimensions);

        assertArrayEquals(new int[] {73}, x.sourceSizes);
        assertArrayEquals(new int[] {73}, y.sourceSizes);
    }

    /**
     * Tests {@link GridGeometry#getAxes()} on a four-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    @DependsOnMethod("testDimensions")
    public void testAxes4D() throws IOException, DataStoreException {
        final Axis[] axes = getSingleton(filter(selectDataset(TestData.NETCDF_4D_PROJECTED).getGridGeometries())).getAxes();
        assertEquals(4, axes.length);
        final Axis x = axes[3];
        final Axis y = axes[2];
        final Axis z = axes[1];
        final Axis t = axes[0];

        assertEquals('x', x.abbreviation);
        assertEquals('y', y.abbreviation);
        assertEquals('H', z.abbreviation);
        assertEquals('t', t.abbreviation);

        assertArrayEquals(new int[] {3}, x.sourceDimensions);
        assertArrayEquals(new int[] {2}, y.sourceDimensions);
        assertArrayEquals(new int[] {1}, z.sourceDimensions);
        assertArrayEquals(new int[] {0}, t.sourceDimensions);

        assertArrayEquals(new int[] {38}, x.sourceSizes);
        assertArrayEquals(new int[] {19}, y.sourceSizes);
        assertArrayEquals(new int[] { 4}, z.sourceSizes);
        assertArrayEquals(new int[] { 1}, t.sourceSizes);
    }
}
