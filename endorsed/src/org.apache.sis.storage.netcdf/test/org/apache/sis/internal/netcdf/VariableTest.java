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
import java.util.Arrays;
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

import static org.junit.Assert.*;


/**
 * Tests the {@link Variable} implementation. The default implementation tests
 * {@code org.apache.sis.internal.netcdf.ucar.VariableWrapper} since the UCAR
 * library is our reference implementation. However, subclasses can override the
 * {@link #createDecoder(TestData)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.3
 */
@DependsOn(DecoderTest.class)
public class VariableTest extends TestCase {
    /**
     * Expected number of columns per variables for the {@code expected} argument
     * given to the {@link #assertBasicPropertiesEqual(Object[], Variable[])} method.
     */
    private static final int NUM_BASIC_PROPERTY_COLUMNS = 5;

    /**
     * Creates a new test.
     */
    public VariableTest() {
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
     *   <li>{@link Variable#getRole()}</li>
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
            "runtime",        "Data generation time",          DataType.DOUBLE, 1, VariableRole.AXIS,
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
        int propertyIndex = 0;
        for (final Variable variable : variables) {
            final String name = variable.getName();
            final DataType dataType = variable.getDataType();
            assertFalse("Too many variables.", propertyIndex == expected.length);
            assertEquals(name, expected[propertyIndex++], name);
            assertEquals(name, expected[propertyIndex++], variable.getDescription());
            assertEquals(name, expected[propertyIndex++], dataType);
            assertEquals(name, expected[propertyIndex++], variable.getGridDimensions().size());
            assertEquals(name, expected[propertyIndex++], variable.getRole());
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
        /*
         * From CF-Conventions:
         * The recommended unit of latitude is degrees_north. Also acceptable are degree_north, degree_N, degrees_N, degreeN, and degreesN.
         * The recommended unit of longitude is degrees_east. Also acceptable are degree_east, degree_E, degrees_E, degreeE, and degreesE.
         */
        assertSame(Units.DEGREE, variable.parseUnit("degrees_north"));
        assertSame(Units.DEGREE, variable.parseUnit("degrees_east"));
        assertSame(Units.DEGREE, variable.parseUnit("degree_north"));
        assertSame(Units.DEGREE, variable.parseUnit("degree_east"));
        assertSame(Units.DEGREE, variable.parseUnit("degrees_N"));
        assertSame(Units.DEGREE, variable.parseUnit("degrees_E"));
        assertSame(Units.DEGREE, variable.parseUnit("degree_N"));
        assertSame(Units.DEGREE, variable.parseUnit("degree_E"));
        assertSame(Units.DEGREE, variable.parseUnit("degreesN"));
        assertSame(Units.DEGREE, variable.parseUnit("degreesE"));
        assertSame(Units.DEGREE, variable.parseUnit("degreeN"));
        assertSame(Units.DEGREE, variable.parseUnit("degreeE"));

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
     * Tests {@link Variable#getAttributeValue(String)} and related methods.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGetAttributes() throws IOException, DataStoreException {
        final Variable[] variables = getVariablesCIP(selectDataset(TestData.NETCDF_4D_PROJECTED));
        Variable variable = variables[6];
        assertEquals("CIP", variable.getName());
        assertSingletonEquals(variable, "_FillValue", -1f);
        assertSingletonEquals(variable, "units",      "%");

        variable = variables[0];
        assertEquals("grid_mapping_0", variable.getName());
        assertVectorEquals(variable, "standard_parallel", 25.f, 25.05f);
    }

    /**
     * Asserts that the attribute of given name contains a value equals to the expected value.
     * This method is used for attributes that are expected to contain singleton.
     */
    private static void assertSingletonEquals(final Node variable, final String name, final Object expected) {
        final String t = expected.toString();
        assertEquals     ("getAttributeValue",     expected,         variable.getAttributeValue    (name));
        assertEquals     ("getAttributeAsString",  t,                variable.getAttributeAsString (name));
        assertArrayEquals("getAttributeAsStrings", new String[] {t}, variable.getAttributeAsStrings(name, ' '));
        if (expected instanceof Number) {
            final double en = ((Number) expected).doubleValue();
            assertEquals("getAttributeAsDouble", en, variable.getAttributeAsDouble(name), STRICT);
            final Vector vector = variable.getAttributeAsVector(name);
            assertNotNull("getAttributeAsVector", vector);
            assertEquals(1, vector.size());
            assertEquals(en, vector.get(0).doubleValue(), STRICT);
        } else {
            assertNull("getAttributeAsVector", variable.getAttributeAsVector(name));
        }
    }

    /**
     * Asserts that the attribute of given name contains a value equals to the expected value.
     * This method is used for attributes that are expected to contain vector.
     */
    private static void assertVectorEquals(final Node variable, final String name, final Number... expected) {
        final Vector values = variable.getAttributeAsVector(name);
        assertNotNull(name, values);
        assertEquals ("size", expected.length, values.size());
        assertTrue   ("getAttributeAsDouble", Double.isNaN(variable.getAttributeAsDouble(name)));
        assertEquals ("getAttributeValue", values, variable.getAttributeValue(name));
        final Object[] texts = Arrays.stream(expected).map(Object::toString).toArray();
        assertArrayEquals("getAttributeAsStrings", texts, variable.getAttributeAsStrings(name, ' '));
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
        assertSame(data, variable.readAnyType());
        assertEquals("lon", Float.class, data.getElementType());
        final int length = data.size();
        assertEquals("length", 73, length);
        for (int i=0; i<length; i++) {
            assertEquals("Longitude value", -180 + 5*i, data.floatValue(i), 0f);
        }
    }

    /**
     * Tests {@link Variable#readAnyType()} on strings.
     *
     * @throws IOException if an error occurred while reading the netCDF file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testReadStrings() throws IOException, DataStoreException {
        final Variable variable = selectDataset(TestData.MOVING_FEATURES).findVariable("features");
        assertEquals("features", variable.getName());
        final List<?> identifiers = variable.readAnyType();
        assertArrayEquals(new String[] {"a4078a16", "1e146c16", "f50ff004", "", ""}, identifiers.toArray());
    }
}
