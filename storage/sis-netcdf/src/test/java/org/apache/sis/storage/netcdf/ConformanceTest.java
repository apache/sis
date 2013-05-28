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
package org.apache.sis.storage.netcdf;

import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.io.IOException;
import ucar.nc2.NetcdfFile;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.wrapper.netcdf.NetcdfMetadataTest;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link MetadataReader} by inheriting the tests defined in the {@code geoapi-netcdf} module.
 * The tests are overridden in order to add some additional assertions for attributes not parsed by
 * the GeoAPI demo code.
 *
 * <p>This tests uses the UCAR implementation for reading NetCDF attributes.
 * For a test using the SIS embedded implementation, see {@link MetadataReaderTest}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
@DependsOn(MetadataReaderTest.class)
public final strictfp class ConformanceTest extends NetcdfMetadataTest {
    /**
     * Reads a metadata object from the given NetCDF file.
     * This method is invoked by the tests inherited from the {@code geoapi-test} module.
     *
     * {@note The method name is "<code>wrap</code>" because the GeoAPI implementation maps the
     *        metadata methods to <code>NetcdfFile.findAttribute(String)</code> method calls.
     *        However in SIS implementation, the metadata object is fully created right at this
     *        method invocation time.}
     *
     * @param  file The NetCDF file to wrap.
     * @return A metadata implementation created from the attributes found in the given file.
     * @throws IOException If an error occurred while reading the given NetCDF file.
     */
    @Override
    protected Metadata wrap(final NetcdfFile file) throws IOException {
        final Decoder decoder = new DecoderWrapper(null, file);
        final MetadataReader ncISO = new MetadataReader(null, decoder);
        return ncISO.read();
        // Do not close the file, as this will be done by the parent test class.
    }

    /**
     * Asserts that the given map is empty.
     */
    private static void assertEmpty(final Map<?,?> map) {
        if (!map.isEmpty()) {
            fail(map.toString());
        }
    }

    /**
     * Adds a set of common property values expected by every tests in this class.
     *
     * @param expected   The map where to add additional attributes expected by the test.
     * @param hasContact {@code true} for adding contact information.
     */
    private static void addCommonProperties(final Map<String,Object> expected, final boolean hasContact) {
        assertNull(expected.put("metadataStandardName", "ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data"));
        assertNull(expected.put("metadataStandardVersion", "ISO 19115-2:2009(E)"));
        if (hasContact) {
            assertNull(expected.put("identificationInfo.pointOfContact.role", Role.POINT_OF_CONTACT));
            assertNull(expected.put("contact.role", Role.POINT_OF_CONTACT));
        }
    }

    /**
     * Tests a file that contains THREDDS metadata. This method inherits the tests defined in GeoAPI,
     * and adds some additional tests for attributes parsed by SIS but not by GeoAPI.
     *
     * @throws IOException If the test file can not be read.
     */
    @Test
    @Override
    public void testTHREDDS() throws IOException {
        final Map<String,Object> expected = expectedProperties;
        addCommonProperties(expected, true);
        assertNull(expected.put("identificationInfo.citation.title",           "crm_v1.grd"));
        assertNull(expected.put("identificationInfo.citation.identifier.code", "crm_v1"));
        assertNull(expected.put("contentInfo.dimension.sequenceIdentifier",    "z"));
        super.testTHREDDS();
        assertEquals("hierarchyLevel", new HashSet<ScopeCode>(Arrays.asList(ScopeCode.DATASET, ScopeCode.SERVICE)),
                metadata.getHierarchyLevels());
        /*
         * In the SIS case, the Metadata/Contact and Metadata/Identification/PointOfContact
         * proprties are not just equals - they are expected to be the exact same instance.
         */
        assertSame("identificationInfo.pointOfContact", getSingleton(metadata.getContacts()),
                getSingleton(getSingleton(metadata.getIdentificationInfo()).getPointOfContacts()));
        /*
         * Properties have been removed from the map as they were processed.
         * Since expect every properties to have been processed, the maps should be empty by now.
         */
        assertEmpty(expectedProperties);
        assertEmpty(actualProperties);
    }

    /**
     * Tests a NetCDF binary file. This method inherits the tests defined in GeoAPI,
     * and adds some additional tests for attributes parsed by SIS but not GeoAPI.
     *
     * @throws IOException If the test file can not be read.
     */
    @Test
    @Override
    public void testNCEP() throws IOException {
        addCommonProperties(expectedProperties, true);
        super.testNCEP();
        assertSame("hierarchyLevel", ScopeCode.DATASET, getSingleton(metadata.getHierarchyLevels()));
        /*
         * In the SIS case, the Metadata/Contact and Metadata/Identification/PointOfContact
         * proprties are not just equals - they are expected to be the exact same instance.
         */
        assertSame("identificationInfo.pointOfContact", getSingleton(metadata.getContacts()),
                getSingleton(getSingleton(metadata.getIdentificationInfo()).getPointOfContacts()));
        /*
         * Metadata / Data Identification / Temporal Extent.
         */
        final DataIdentification identification = (DataIdentification) getSingleton(metadata.getIdentificationInfo());
        final TemporalExtent text = getSingleton(getSingleton(identification.getExtents()).getTemporalElements());
        // Can not test at this time, since it requires the sis-temporal module (TODO).

// TODO assertEmpty(expectedProperties);
        assertEmpty(actualProperties);
    }

    /**
     * Tests the Landsat file (binary format).
     *
     * @throws IOException If the test file can not be read.
     */
    @Test
    @Override
    public void testLandsat() throws IOException {
        addCommonProperties(expectedProperties, false);
        super.testLandsat();
        assertSame("hierarchyLevel", ScopeCode.DATASET, getSingleton(metadata.getHierarchyLevels()));

        assertEmpty(expectedProperties);
        assertEmpty(actualProperties);
    }

    /**
     * Tests the "Current Icing Product" file (binary format).
     *
     * @throws IOException If the test file can not be read.
     */
    @Test
    @Override
    public void testCIP() throws IOException {
        final Map<String,Object> expected = expectedProperties;
        addCommonProperties(expected, true);
        super.testCIP();
        assertSame("hierarchyLevel", ScopeCode.DATASET, getSingleton(metadata.getHierarchyLevels()));
        /*
         * In the SIS case, the Metadata/Contact and Metadata/Identification/PointOfContact
         * proprties are not just equals - they are expected to be the exact same instance.
         */
        assertSame("identificationInfo.pointOfContact", getSingleton(metadata.getContacts()),
                getSingleton(getSingleton(metadata.getIdentificationInfo()).getPointOfContacts()));

// TODO assertEmpty(expectedProperties);
        assertEmpty(actualProperties);
    }
}
