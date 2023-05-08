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
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.opengis.test.dataset.TestData;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link Grid} implementation. The default implementation tests
 * {@code org.apache.sis.internal.netcdf.ucar.GridGeometryWrapper} since the UCAR
 * library is our reference implementation. However, subclasses can override the
 * {@link #createDecoder(TestData)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.3
 */
@DependsOn(VariableTest.class)
public class GridTest extends TestCase {
    /**
     * Whether the {@code "runtime"} variable in {@link TestData#NETCDF_4D_PROJECTED} is used as a target dimension
     * for the {@code gridToCRS} transform. The UCAR library and Apache SIS implementation have different behavior
     * regarding this dimension.
     */
    protected boolean includeRuntimeDimension;

    /**
     * Optionally filters out some grid geometries that shall be ignored by the tests.
     * The default implementation returns the given array unmodified. This method is overridden by
     * {@code GridGeometryInfoTest} in order to ignore one-dimensional coordinate systems created
     * by {@code GridGeometry} but not by the UCAR library.
     *
     * @param  geometries  the grid geometries created by {@link Decoder}.
     * @return the grid geometries to test.
     */
    protected Grid[] filter(final Grid[] geometries) {
        return geometries;
    }

    /**
     * Tests {@link Grid#getSourceDimensions()} and {@code Grid.getTargetDimensions()}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testDimensions() throws IOException, DataStoreException {
        Grid geometry = getSingleton(filter(selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getGridCandidates()));
        assertEquals("getSourceDimensions()", 2, geometry.getSourceDimensions());
        assertEquals("getTargetDimensions()", 2, geometry.getAxes(decoder()).length);

        final int n = includeRuntimeDimension ? 5 : 4;
        geometry = getSingleton(filter(selectDataset(TestData.NETCDF_4D_PROJECTED).getGridCandidates()));
        assertEquals("getSourceDimensions()", 4, geometry.getSourceDimensions());
        assertEquals("getTargetDimensions()", n, geometry.getAxes(decoder()).length);
    }

    /**
     * Tests {@link Grid#getAxes(Decoder)} on a two-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    @DependsOnMethod("testDimensions")
    public void testAxes2D() throws IOException, DataStoreException {
        final Axis[] axes = getSingleton(filter(selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getGridCandidates())).getAxes(decoder());
        assertEquals(2, axes.length);
        final Axis x = axes[0];
        final Axis y = axes[1];

        assertEquals('λ', x.abbreviation);
        assertEquals('φ', y.abbreviation);

        assertArrayEquals(new int[] {1}, x.gridDimensionIndices);
        assertArrayEquals(new int[] {0}, y.gridDimensionIndices);

        assertEquals(73, x.getMainSize().getAsLong());
        assertEquals(73, y.getMainSize().getAsLong());
    }

    /**
     * Tests {@link Grid#getAxes(Decoder)} on a four-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    @DependsOnMethod("testDimensions")
    public void testAxes4D() throws IOException, DataStoreException {
        final Axis[] axes = getSingleton(filter(selectDataset(TestData.NETCDF_4D_PROJECTED).getGridCandidates())).getAxes(decoder());
        assertEquals(includeRuntimeDimension ? 5 : 4, axes.length);
        final Axis x = axes[0];
        final Axis y = axes[1];
        final Axis z = axes[2];
        final Axis t = axes[3];

        assertEquals('E', x.abbreviation);
        assertEquals('N', y.abbreviation);
        assertEquals('H', z.abbreviation);
        assertEquals('t', t.abbreviation);

        assertArrayEquals(new int[] {3}, x.gridDimensionIndices);
        assertArrayEquals(new int[] {2}, y.gridDimensionIndices);
        assertArrayEquals(new int[] {1}, z.gridDimensionIndices);
        assertArrayEquals(new int[] {0}, t.gridDimensionIndices);

        assertEquals(38, x.getMainSize().getAsLong());
        assertEquals(19, y.getMainSize().getAsLong());
        assertEquals( 4, z.getMainSize().getAsLong());
        assertEquals( 1, t.getMainSize().getAsLong());

        if (includeRuntimeDimension) {
            final Axis r = axes[4];
            assertEquals('t', r.abbreviation);
            assertArrayEquals(new int[] {0}, r.gridDimensionIndices);
            assertEquals(1, r.getMainSize().getAsLong());
        }
    }

    /**
     * Tests {@link GridMapping#forVariable(Variable)} with a projected CRS.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGridMapping() throws IOException, DataStoreException {
        final Node data = selectDataset(TestData.NETCDF_4D_PROJECTED).findNode("CIP");
        final GridMapping mapping = GridMapping.forVariable((Variable) data);
        assertNotNull("mapping", mapping);
        assertInstanceOf("crs", ProjectedCRS.class, mapping.crs);
        final ParameterValueGroup pg = ((ProjectedCRS) mapping.crs).getConversionFromBase().getParameterValues();
        assertEquals("Latitude of false origin",           25,    pg.parameter("Latitude of false origin")         .doubleValue(), STRICT);
        assertEquals("Longitude of false origin",         -95,    pg.parameter("Longitude of false origin")        .doubleValue(), STRICT);
        assertEquals("Latitude of 1st standard parallel",  25,    pg.parameter("Latitude of 1st standard parallel").doubleValue(), STRICT);
        assertEquals("Latitude of 2nd standard parallel",  25.05, pg.parameter("Latitude of 2nd standard parallel").doubleValue(), STRICT);
    }
}
