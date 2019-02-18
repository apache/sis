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

import java.util.List;
import java.io.IOException;
import java.time.Instant;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Workaround;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.measure.Units;
import org.apache.sis.test.DependsOn;
import org.opengis.test.dataset.TestData;
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
    private static final int NUM_BASIC_PROPERTY_COLUMNS = 5;

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
     * last but UCAR library puts it second.
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
     *   <li>{@link Variable#getGridDimensions()} length</li>
     *   <li>{@link Convention#roleOf(Variable)}</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testBasicProperties() throws IOException, DataStoreException {
        assertBasicPropertiesEqual(new Object[] {
        // __name______________description_____________________datatype_______dim__role
            "grid_mapping_0", null,                            DataType.INT,    0, VariableRole.OTHER,
            "x0",             "projection_x_coordinate",       DataType.FLOAT,  1, VariableRole.AXIS,
            "y0",             "projection_y_coordinate",       DataType.FLOAT,  1, VariableRole.AXIS,
            "z0",             "Flight levels in 100s of feet", DataType.FLOAT,  1, VariableRole.AXIS,
            "time",           "Data time",                     DataType.DOUBLE, 1, VariableRole.AXIS,
            "runtime",        "Data generation time",          DataType.DOUBLE, 1, isRuntimeAnAxis ? VariableRole.AXIS : VariableRole.OTHER,
            "CIP",            "Current Icing Product",         DataType.FLOAT,  4, VariableRole.COVERAGE
        }, getVariablesCIP(selectDataset(TestData.NETCDF_4D_PROJECTED)));
    }

    /**
     * Compares the basic properties of the given variables.
     *
     * @param  expected   the expected property values.
     * @param  variables  the variable for which to test properties.
     */
    private static void assertBasicPropertiesEqual(final Object[] expected, final Variable[] variables) {
        final Convention convention = new Convention();
        int propertyIndex = 0;
        for (final Variable variable : variables) {
            final String name = variable.getName();
            final DataType dataType = variable.getDataType();
            assertFalse("Too many variables.", propertyIndex == expected.length);
            assertEquals(name, expected[propertyIndex++], name);
            assertEquals(name, expected[propertyIndex++], variable.getDescription());
            assertEquals(name, expected[propertyIndex++], dataType);
            assertEquals(name, expected[propertyIndex++], variable.getGridDimensions().size());
            assertEquals(name, expected[propertyIndex++], convention.roleOf(variable));
            assertEquals(0, propertyIndex % NUM_BASIC_PROPERTY_COLUMNS);            // Sanity check for VariableTest itself.
        }
        assertEquals("Expected more variables.",
                expected.length / NUM_BASIC_PROPERTY_COLUMNS,
                propertyIndex   / NUM_BASIC_PROPERTY_COLUMNS);
    }

    /**
     * Tests {@link Variable#parseUnit(String)} method.
     *
     * @throws Exception if an I/O or logical error occurred while opening the file.
     */
    @Test
    public void testParseUnit() throws Exception {
        final Variable variable = selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getVariables()[0];
        assertSame(Units.SECOND, variable.parseUnit("s"));
        assertSame(Units.SECOND, variable.parseUnit("second"));
        assertSame(Units.SECOND, variable.parseUnit("seconds"));
        assertSame(Units.MINUTE, variable.parseUnit("min"));
        assertSame(Units.MINUTE, variable.parseUnit("minute"));
        assertSame(Units.MINUTE, variable.parseUnit("minutes"));
        assertSame(Units.HOUR,   variable.parseUnit("h"));
        assertSame(Units.HOUR,   variable.parseUnit("hr"));
        assertSame(Units.HOUR,   variable.parseUnit("hour"));
        assertSame(Units.HOUR,   variable.parseUnit("hours"));
        assertSame(Units.DAY,    variable.parseUnit("d"));
        assertSame(Units.DAY,    variable.parseUnit("day"));
        assertSame(Units.DAY,    variable.parseUnit("days"));
        /*
         * Parsing date set the epoch as a side effect.
         */
        final Instant save = variable.epoch;
        try {
            assertSame(Units.DAY, variable.parseUnit("days since 1992-10-8 15:15:42.5 -06:00"));
            assertEquals("epoch", variable.epoch, Instant.parse("1992-10-08T21:15:42.500Z"));
        } finally {
            variable.epoch = save;
        }
    }

    /**
     * Returns the dimension names.
     */
    private static String[] names(final List<Dimension> dimensions) {
        return dimensions.stream().map(Dimension::getName).toArray(String[]::new);
    }

    /**
     * Returns the dimension lengths.
     */
    private static long[] lengths(final List<Dimension> dimensions) {
        return dimensions.stream().mapToLong(Dimension::length).toArray();
    }

    /**
     * Tests {@link Variable#getGridDimensions()} on a simple two-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGridRange2D() throws IOException, DataStoreException {
        final Variable variable = selectDataset(TestData.NETCDF_2D_GEOGRAPHIC).getVariables()[0];
        assertEquals("SST", variable.getName());

        final List<Dimension> dimensions = variable.getGridDimensions();
        assertArrayEquals("getGridDimensionNames()", new String[] {
            "lat", "lon"
        }, names(dimensions));

        assertArrayEquals("getGridEnvelope()", new long[] {
            73, 73
        }, lengths(dimensions));
    }

    /**
     * Tests {@link Variable#getGridDimensions()} on a compound four-dimensional dataset.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGridRange4D() throws IOException, DataStoreException {
        final Variable variable = getVariablesCIP(selectDataset(TestData.NETCDF_4D_PROJECTED))[6];
        assertEquals("CIP", variable.getName());

        final List<Dimension> dimensions = variable.getGridDimensions();
        assertArrayEquals("getGridDimensionNames()", new String[] {
            "time", "z0", "y0", "x0"
        }, names(dimensions));

        assertArrayEquals("getGridEnvelope()", new long[] {
            1, 4, 19, 38
        }, lengths(dimensions));
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
