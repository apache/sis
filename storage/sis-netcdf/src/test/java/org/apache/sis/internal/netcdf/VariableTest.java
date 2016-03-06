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
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link Variable} implementation. The default implementation tests
 * {@link org.apache.sis.internal.netcdf.ucar.VariableWrapper} since the UCAR
 * library is our reference implementation. However subclasses can override the
 * {@link #createDecoder(String)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(DecoderTest.class)
public strictfp class VariableTest extends TestCase {
    /**
     * Expected number of columns per variables for the {@code expected} argument
     * given to the {@link #assertBasicPropertiesEqual(Object[], Variable[])} method.
     */
    private static final int NUM_BASIC_PROPERTY_COLUMNS = 6;

    /**
     * Tests the basic properties of all variables found in the {@link #NCEP} file.
     * The tested methods are:
     *
     * <ul>
     *   <li>{@link Variable#getName()}</li>
     *   <li>{@link Variable#getDescription()}</li>
     *   <li>{@link Variable#getDataType()}</li>
     *   <li>{@link Variable#getGridEnvelope()} length</li>
     *   <li>{@link Variable#isCoordinateSystemAxis()}</li>
     *   <li>{@link Variable#isCoverage(int)}</li>
     *   <li>{@link Variable#isUnsigned()}</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testBasicProperties() throws IOException, DataStoreException {
        assertBasicPropertiesEqual(new Object[] {
        // __name______________description_________________________________datatype____dim__axis?__raster?
            "reftime",        "reference time",                            double.class, 1, false, false,
            "datetime",       "reference date and time",                   char  .class, 2, false, false,
            "forecasttime",   "forecast date and time",                    char  .class, 2, false, false,
            "model_id",       "generating process ID number",              int   .class, 1, false, false,
            "nav_model",      "navigation model name",                     char  .class, 2, false, false,
            "grid_type_code", "GRIB-1 GDS data representation type",       int   .class, 1, false, false,
            "grid_type",      "GRIB-1 grid type",                          char  .class, 2, false, false,
            "grid_name",      "grid name",                                 char  .class, 2, false, false,
            "grid_center",    "GRIB-1 originating center ID",              int   .class, 1, false, false,
            "grid_number",    "GRIB-1 catalogued grid numbers",            int   .class, 2, false, false,
            "i_dim",          "longitude dimension name",                  char  .class, 2, false, false,
            "j_dim",          "latitude dimension name",                   char  .class, 2, false, false,
            "Ni",             "number of points along a latitude circle",  int   .class, 1, false, false,
            "Nj",             "number of points along a longitude circle", int   .class, 1, false, false,
            "La1",            "latitude of first grid point",              float .class, 1, false, false,
            "Lo1",            "longitude of first grid point",             float .class, 1, false, false,
            "La2",            "latitude of last grid point",               float .class, 1, false, false,
            "Lo2",            "longitude of last grid point",              float .class, 1, false, false,
            "Di",             "longitudinal direction increment",          float .class, 1, false, false,
            "Dj",             "latitudinal direction increment",           float .class, 1, false, false,
            "ResCompFlag",    "resolution and component flags",            byte  .class, 1, false, false,
            "SST",            "Sea temperature",                           float .class, 3, false, true,
            "valtime",        "valid time",                                double.class, 1, true,  false,
            "valtime_offset", "hours from reference time",                 double.class, 1, true,  false,
            "lat",            "latitude",                                  float .class, 1, true,  false,
            "lon",            "longitude",                                 float .class, 1, true,  false
        }, selectDataset(NCEP).getVariables());
    }

    /**
     * Compares the basic properties of the given variables.
     *
     * @param expected  The expected property values.
     * @param variables The variable for which to test properties.
     */
    private static void assertBasicPropertiesEqual(final Object[] expected, final Variable[] variables) {
        int propertyIndex = 0;
        for (final Variable variable : variables) {
            assertFalse("Too many variables.", propertyIndex == expected.length);
            assertEquals("getName()",                expected[propertyIndex++], variable.getName());
            assertEquals("getDescription()",         expected[propertyIndex++], variable.getDescription());
            assertEquals("getDataType()",            expected[propertyIndex++], variable.getDataType());
            assertEquals("getDimensionLengths()",    expected[propertyIndex++], variable.getGridEnvelope().length);
            assertEquals("isCoordinateSystemAxis()", expected[propertyIndex++], variable.isCoordinateSystemAxis());
            assertEquals("isCoverage(2)",            expected[propertyIndex++], variable.isCoverage(2));
            assertEquals(0, propertyIndex % NUM_BASIC_PROPERTY_COLUMNS); // Sanity check for VariableTest itself.

            // All variable in our current test dataset are unsigned.
            assertFalse("isUnsigned()", variable.isUnsigned());
        }
        assertEquals("Expected more variables.",
                expected.length / NUM_BASIC_PROPERTY_COLUMNS,
                propertyIndex   / NUM_BASIC_PROPERTY_COLUMNS);
    }

    /**
     * Tests {@link Variable#getGridDimensionNames()} and {@link Variable#getGridEnvelope()}.
     * Current implementation tests on the {@code "SST"} variable.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGridDimensions() throws IOException, DataStoreException {
        final Variable variable = selectDataset(NCEP).getVariables()[21];
        assertEquals("SST", variable.getName());

        assertArrayEquals("getGridDimensionNames()", new String[] {
            "record", "lat", "lon"
        }, variable.getGridDimensionNames());

        assertArrayEquals("getGridEnvelope()", new int[] {
            1, 73, 73
        }, variable.getGridEnvelope());
    }

    /**
     * Tests {@link Variable#getAttributeValues(String, boolean)}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGetAttributes() throws IOException, DataStoreException {
        final Variable[] variables = selectDataset(NCEP).getVariables();
        Variable variable = variables[9];
        assertEquals("grid_number", variable.getName());
        assertArrayEquals("grid_number:_FillValue", new Number[] { -9999 }, variable.getAttributeValues("_FillValue", true));
        assertArrayEquals("grid_number:_FillValue", new String[] {"-9999"}, variable.getAttributeValues("_FillValue", false));

        variable = variables[21];
        assertEquals("SST", variable.getName());
        assertArrayEquals("SST:_FillValue", new Number[] { -9999f  }, variable.getAttributeValues("_FillValue", true));
        assertArrayEquals("SST:_FillValue", new String[] {"-9999.0"}, variable.getAttributeValues("_FillValue", false));
        assertArrayEquals("SST:units",      new String[] {"degK"},    variable.getAttributeValues("units",      false));
        assertArrayEquals("SST:units",      new Number[] {      },    variable.getAttributeValues("units",      true));
    }

    /**
     * Tests {@link Variable#read()} on a one-dimensional variable.
     *
     * @throws IOException If an error occurred while reading the NetCDF file.
     * @throws DataStoreException Should never happen.
     */
    @Test
    public void testRead1D() throws IOException, DataStoreException {
        final Variable variable = selectDataset(NCEP).getVariables()[25];
        assertEquals("lon", variable.getName());
        final Object data = variable.read();
        assertInstanceOf("lon", float[].class, data);
        final float[] array = (float[]) data;
        assertEquals(73, array.length);
        for (int i=0; i<array.length; i++) {
            assertEquals("Longitude value", -180 + 5*i, array[i], 0f);
        }
    }
}
