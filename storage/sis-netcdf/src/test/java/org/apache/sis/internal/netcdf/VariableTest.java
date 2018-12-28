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
import org.apache.sis.util.Workaround;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link Variable} implementation. The default implementation tests
 * {@code org.apache.sis.internal.netcdf.ucar.VariableWrapper} since the UCAR
 * library is our reference implementation. However subclasses can override the
 * {@link #createDecoder(TestData)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
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
     * Whether the {@code "runtime"} variable in {@link TestData#NETCDF_4D_PROJECTED} is considered an axis or not.
     * The UCAR library considers it as an axis because it has an {@code "_CoordinateAxisType"} attribute.
     * Apache SIS does not consider it as an axis because that variable does not match any dimension and is not used
     * in any other variable.
     */
    protected boolean isRuntimeAnAxis;

    /**
     * Creates a new test.
     */
    public VariableTest() {
        isRuntimeAnAxis = true;
    }

    /**
     * Gets the variable from the given decoder, reordering them if the decoder is a wrapper for UCAR library.
     * We perform this reordering because UCAR library does not always return the variables in the order they
     * are declared. In the case of the {@link TestData#NETCDF_4D_PROJECTED} file, the CIP variable is expected
     * last but UCAR library put it second.
     */
    @Workaround(library = "UCAR", version = "4.6.11")
    private Variable[] getVariablesCIP(final Decoder decoder) {
        Variable[] variables = decoder.getVariables();
        if (decoder instanceof DecoderWrapper) {
            variables = variables.clone();
            final Variable cip = variables[1];
            final int last = variables.length - 1;
            System.arraycopy(variables, 2, variables, 1, last - 1);
            variables[last] = cip;
        }
        return variables;
    }

    /**
     * Tests the basic properties of all variables found in the {@link TestData#NETCDF_4D_PROJECTED} file.
     * The tested methods are:
     *
     * <ul>
     *   <li>{@link Variable#getName()}</li>
     *   <li>{@link Variable#getDescription()}</li>
     *   <li>{@link Variable#getDataType()}</li>
     *   <li>{@link Variable#getShape()} length</li>
     *   <li>{@link Variable#isCoordinateSystemAxis()}</li>
     *   <li>{@link Variable#isCoverage()}</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testBasicProperties() throws IOException, DataStoreException {
        assertBasicPropertiesEqual(new Object[] {
        // __name______________description_____________________datatype_______dim__axis?__raster?
            "grid_mapping_0", null,                            DataType.INT,    0, false, false,
            "x0",             "projection_x_coordinate",       DataType.FLOAT,  1, true,  false,
            "y0",             "projection_y_coordinate",       DataType.FLOAT,  1, true,  false,
            "z0",             "Flight levels in 100s of feet", DataType.FLOAT,  1, true,  false,
            "time",           "Data time",                     DataType.DOUBLE, 1, true,  false,
            "runtime",        "Data generation time",          DataType.DOUBLE, 1, isRuntimeAnAxis, false,
            "CIP",            "Current Icing Product",         DataType.FLOAT,  4, false, true
        }, getVariablesCIP(selectDataset(TestData.NETCDF_4D_PROJECTED)));
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
            assertEquals(name, expected[propertyIndex++], variable.getShape().length);
            assertEquals(name, expected[propertyIndex++], variable.isCoordinateSystemAxis());
            assertEquals(name, expected[propertyIndex++], variable.isCoverage());
            assertEquals(0, propertyIndex % NUM_BASIC_PROPERTY_COLUMNS);            // Sanity check for VariableTest itself.
        }
        assertEquals("Expected more variables.",
                expected.length / NUM_BASIC_PROPERTY_COLUMNS,
                propertyIndex   / NUM_BASIC_PROPERTY_COLUMNS);
    }

    /**
     * Tests {@link Variable#getGridDimensionNames()} and {@link Variable#getShape()}
     * on a simple two-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGridRange2D() throws IOException, DataStoreException {
        final Variable variable = selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getVariables()[0];
        assertEquals("SST", variable.getName());

        assertArrayEquals("getGridDimensionNames()", new String[] {
            "lat", "lon"
        }, variable.getGridDimensionNames());

        assertArrayEquals("getGridEnvelope()", new int[] {
            73, 73
        }, variable.getShape());
    }

    /**
     * Tests {@link Variable#getGridDimensionNames()} and {@link Variable#getShape()}
     * on a compound four-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGridRange4D() throws IOException, DataStoreException {
        final Variable variable = getVariablesCIP(selectDataset(TestData.NETCDF_4D_PROJECTED))[6];
        assertEquals("CIP", variable.getName());

        assertArrayEquals("getGridDimensionNames()", new String[] {
            "time", "z0", "y0", "x0"
        }, variable.getGridDimensionNames());

        assertArrayEquals("getGridEnvelope()", new int[] {
            1, 4, 19, 38
        }, variable.getShape());
    }

    /**
     * Tests {@link Variable#getAttributeValues(String, boolean)}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGetAttributes() throws IOException, DataStoreException {
        final Variable[] variables = getVariablesCIP(selectDataset(TestData.NETCDF_4D_PROJECTED));
        Variable variable = variables[6];
        assertEquals("CIP", variable.getName());
        assertArrayEquals("CIP:_FillValue", new Number[] { -1f  }, variable.getAttributeValues("_FillValue", true));
        assertArrayEquals("CIP:_FillValue", new String[] {"-1.0"}, variable.getAttributeValues("_FillValue", false));
        assertArrayEquals("CIP:units",      new String[] {   "%"}, variable.getAttributeValues("units",      false));
        assertArrayEquals("CIP:units",      new Number[] {      }, variable.getAttributeValues("units",      true));

        variable = variables[0];
        assertEquals("grid_mapping_0", variable.getName());
        assertArrayEquals("standard_parallel", new Number[] { 25.f,   25.05f}, variable.getAttributeValues("standard_parallel", true));
        assertArrayEquals("standard_parallel", new String[] {"25.0", "25.05"}, variable.getAttributeValues("standard_parallel", false));
    }

    /**
     * Tests {@link Variable#read()} on a one-dimensional variable.
     *
     * @throws IOException if an error occurred while reading the netCDF file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testRead1D() throws IOException, DataStoreException {
        final Variable variable = selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getVariables()[2];
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
