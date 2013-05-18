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
import static org.opengis.wrapper.netcdf.IOTestCase.NCEP;
import org.junit.Test;

import static org.junit.Assert.*;


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
public strictfp class VariableTest extends TestCase {
    /**
     * Expected number of columns per variables for the {@code expected} argument
     * given to the {@link #assertBasicPropertiesEqual(Object[], Variable[])} method.
     */
    private static final int NUM_BASIC_PROPERTY_COLUMNS = 5;

    /**
     * Tests the basic properties of all variables found in the {@link #NCEP} file.
     * The tested methods are:
     *
     * <ul>
     *   <li>{@link Variable#getName()}</li>
     *   <li>{@link Variable#getDescription()}</li>
     *   <li>{@link Variable#getDataType()}</li>
     *   <li>{@link Variable#getDimensionLengths()}</li>
     *   <li>{@link Variable#isCoverage(int)}</li>
     * </ul>
     *
     * @throws IOException If an error occurred while reading the NetCDF file.
     */
    @Test
    public void testGetName() throws IOException {
        assertBasicPropertiesEqual(new Object[] {
        // __name______________description_________________________________data type____dim_raster?
            "reftime",        "reference time",                            double.class, 1, false,
            "datetime",       "reference date and time",                   char  .class, 2, false,
            "forecasttime",   "forecast date and time",                    char  .class, 2, false,
            "model_id",       "generating process ID number",              int   .class, 1, false,
            "nav_model",      "navigation model name",                     char  .class, 2, false,
            "grid_type_code", "GRIB-1 GDS data representation type",       int   .class, 1, false,
            "grid_type",      "GRIB-1 grid type",                          char  .class, 2, false,
            "grid_name",      "grid name",                                 char  .class, 2, false,
            "grid_center",    "GRIB-1 originating center ID",              int   .class, 1, false,
            "grid_number",    "GRIB-1 catalogued grid numbers",            int   .class, 2, false,
            "i_dim",          "longitude dimension name",                  char  .class, 2, false,
            "j_dim",          "latitude dimension name",                   char  .class, 2, false,
            "Ni",             "number of points along a latitude circle",  int   .class, 1, false,
            "Nj",             "number of points along a longitude circle", int   .class, 1, false,
            "La1",            "latitude of first grid point",              float .class, 1, false,
            "Lo1",            "longitude of first grid point",             float .class, 1, false,
            "La2",            "latitude of last grid point",               float .class, 1, false,
            "Lo2",            "longitude of last grid point",              float .class, 1, false,
            "Di",             "longitudinal direction increment",          float .class, 1, false,
            "Dj",             "latitudinal direction increment",           float .class, 1, false,
            "ResCompFlag",    "resolution and component flags",            byte  .class, 1, false,
            "SST",            "Sea temperature",                           float .class, 3, true,
            "valtime",        "valid time",                                double.class, 1, false,
            "valtime_offset", "hours from reference time",                 double.class, 1, false,
            "lat",            "latitude",                                  float .class, 1, false,
            "lon",            "longitude",                                 float .class, 1, false
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
            assertEquals("getName()",             expected[propertyIndex++], variable.getName());
            assertEquals("getDescription()",      expected[propertyIndex++], variable.getDescription());
            assertEquals("getDataType()",         expected[propertyIndex++], variable.getDataType());
            assertEquals("getDimensionLengths()", expected[propertyIndex++], variable.getDimensionLengths().length);
            assertEquals("isCoverage(2)",         expected[propertyIndex++], variable.isCoverage(2));
            assertEquals(0, propertyIndex % NUM_BASIC_PROPERTY_COLUMNS); // Sanity check for VariableTest itself.
        }
        assertEquals("Expected more variables.",
                expected.length / NUM_BASIC_PROPERTY_COLUMNS,
                propertyIndex   / NUM_BASIC_PROPERTY_COLUMNS);
    }
}
