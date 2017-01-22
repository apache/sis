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
import org.apache.sis.math.Vector;
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
 * @version 0.8
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
     * </ul>
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testBasicProperties() throws IOException, DataStoreException {
        assertBasicPropertiesEqual(new Object[] {
        // __name______________description_________________________________datatype_______dim__axis?__raster?
            "reftime",        "reference time",                            DataType.DOUBLE, 1, false, false,
            "datetime",       "reference date and time",                   DataType.CHAR,   2, false, false,
            "forecasttime",   "forecast date and time",                    DataType.CHAR,   2, false, false,
            "model_id",       "generating process ID number",              DataType.INT,    1, false, false,
            "nav_model",      "navigation model name",                     DataType.CHAR,   2, false, false,
            "grid_type_code", "GRIB-1 GDS data representation type",       DataType.INT,    1, false, false,
            "grid_type",      "GRIB-1 grid type",                          DataType.CHAR,   2, false, false,
            "grid_name",      "grid name",                                 DataType.CHAR,   2, false, false,
            "grid_center",    "GRIB-1 originating center ID",              DataType.INT,    1, false, false,
            "grid_number",    "GRIB-1 catalogued grid numbers",            DataType.INT,    2, false, false,
            "i_dim",          "longitude dimension name",                  DataType.CHAR,   2, false, false,
            "j_dim",          "latitude dimension name",                   DataType.CHAR,   2, false, false,
            "Ni",             "number of points along a latitude circle",  DataType.INT,    1, false, false,
            "Nj",             "number of points along a longitude circle", DataType.INT,    1, false, false,
            "La1",            "latitude of first grid point",              DataType.FLOAT,  1, false, false,
            "Lo1",            "longitude of first grid point",             DataType.FLOAT,  1, false, false,
            "La2",            "latitude of last grid point",               DataType.FLOAT,  1, false, false,
            "Lo2",            "longitude of last grid point",              DataType.FLOAT,  1, false, false,
            "Di",             "longitudinal direction increment",          DataType.FLOAT,  1, false, false,
            "Dj",             "latitudinal direction increment",           DataType.FLOAT,  1, false, false,
            "ResCompFlag",    "resolution and component flags",            DataType.BYTE,   1, false, false,
            "SST",            "Sea temperature",                           DataType.FLOAT,  3, false, true,
            "valtime",        "valid time",                                DataType.DOUBLE, 1, true,  false,
            "valtime_offset", "hours from reference time",                 DataType.DOUBLE, 1, true,  false,
            "lat",            "latitude",                                  DataType.FLOAT,  1, true,  false,
            "lon",            "longitude",                                 DataType.FLOAT,  1, true,  false
        }, selectDataset(NCEP).getVariables());
    }

    /**
     * Compares the basic properties of the given variables.
     *
     * @param  expected   the expected property values.
     * @param  variables  the variable for which to test properties.
     */
    private static void assertBasicPropertiesEqual(final Object[] expected, final Variable[] variables) {
        int propertyIndex = 0;
        for (final Variable variable : variables) {
            final String name = variable.getName();
            final DataType dataType = variable.getDataType();
            assertFalse("Too many variables.", propertyIndex == expected.length);
            assertEquals(name, expected[propertyIndex++], name);
            assertEquals(name, expected[propertyIndex++], variable.getDescription());
            assertEquals(name, expected[propertyIndex++], dataType);
            assertEquals(name, expected[propertyIndex++], variable.getGridEnvelope().length);
            assertEquals(name, expected[propertyIndex++], variable.isCoordinateSystemAxis());
            assertEquals(name, expected[propertyIndex++], variable.isCoverage(2));
            assertEquals(0, propertyIndex % NUM_BASIC_PROPERTY_COLUMNS);            // Sanity check for VariableTest itself.
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
     * @throws IOException if an error occurred while reading the NetCDF file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testRead1D() throws IOException, DataStoreException {
        final Variable variable = selectDataset(NCEP).getVariables()[25];
        assertEquals("lon", variable.getName());
        final Vector data = variable.read();
        assertEquals("lon", Float.class, data.getElementType());
        final int length = data.size();
        assertEquals("length", 73, length);
        for (int i=0; i<length; i++) {
            assertEquals("Longitude value", -180 + 5*i, data.floatValue(i), 0f);
        }
    }
}
