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
import java.io.IOException;
import ucar.nc2.NetcdfFile;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.wrapper.netcdf.NetcdfMetadataTest;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.internal.netcdf.TestCase;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
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
 * @version 0.8
 * @since   0.3
 * @module
 */
@DependsOn(MetadataReaderTest.class)
public final strictfp class ConformanceTest extends NetcdfMetadataTest {
    /**
     * Reads a metadata object from the given NetCDF file.
     * This method is invoked by the tests inherited from the {@code geoapi-test} module.
     *
     * <div class="note"><b>Note:</b>
     * The method name is "{@code wrap}" because the GeoAPI implementation maps the metadata methods to
     * {@code NetcdfFile.findAttribute(String)} method calls. However in SIS implementation, the metadata
     * object is fully created right at this method invocation time.</div>
     *
     * @param  file the NetCDF file to wrap.
     * @return a metadata implementation created from the attributes found in the given file.
     * @throws IOException if an error occurred while reading the given NetCDF file.
     */
    @Override
    protected Metadata wrap(final NetcdfFile file) throws IOException {
        final Decoder decoder = new DecoderWrapper(TestCase.LISTENERS, file);
        final MetadataReader ncISO = new MetadataReader(decoder);
        try {
            return ncISO.read();
            // Do not close the file, as this will be done by the parent test class.
        } catch (DataStoreException e) {
            throw new AssertionError(e);
        }
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
     * @param expected   the map where to add additional attributes expected by the test.
     * @param hasContact {@code true} for adding contact information.
     */
    private static void addCommonProperties(final Map<String,Object> expected, final boolean hasContact) {
        assertNull(expected.put("metadataStandardName", "Geographic Information — Metadata Part 1: Fundamentals"));
        assertNull(expected.put("metadataStandardVersion", "ISO 19115-1:2014(E)"));
        if (hasContact) {
            assertNull(expected.put("identificationInfo.pointOfContact.role", Role.POINT_OF_CONTACT));
            assertNull(expected.put("contact.role", Role.POINT_OF_CONTACT));
        }
    }

    /**
     * Tests a file that contains THREDDS metadata. This method inherits the tests defined in GeoAPI,
     * and adds some additional tests for attributes parsed by SIS but not by GeoAPI.
     *
     * @throws IOException if the test file can not be read.
     */
    @Test
    @Override
    public void testTHREDDS() throws IOException {
        final Map<String,Object> expected = expectedProperties;
        addCommonProperties(expected, true);
        assertNull(expected.put("identificationInfo.citation.title",           "crm_v1.grd"));
        assertNull(expected.put("identificationInfo.citation.identifier.code", "crm_v1"));
        assertNull(expected.put("contentInfo.dimension.sequenceIdentifier",    "z"));
        assertNull(expected.put("identificationInfo.citation.date.date", TestUtilities.date("2011-04-19 00:00:00")));
        assertNull(expected.put("identificationInfo.citation.date.dateType", DateType.CREATION));
        super.testTHREDDS();
        assertArrayEquals("metadataScopes", new DefaultMetadataScope[] {
                new DefaultMetadataScope(ScopeCode.DATASET, null),
                new DefaultMetadataScope(ScopeCode.SERVICE, "http://localhost:8080//thredds/wms/crm/crm_vol9.nc"),
                new DefaultMetadataScope(ScopeCode.SERVICE, "http://localhost:8080//thredds/wcs/crm/crm_vol9.nc")},
                metadata.getMetadataScopes().toArray());
        /*
         * In the SIS case, the Metadata/Contact and Metadata/Identification/PointOfContact
         * proprties are not just equals - they are expected to be the exact same instance.
         */
        assertSame("identificationInfo.pointOfContact", getSingleton(metadata.getContacts()),
                getSingleton(getSingleton(metadata.getIdentificationInfo()).getPointOfContacts()));
        /*
         * Properties have been removed from the map as they were processed.
         * Since we expect every properties to have been processed, the maps should be empty by now.
         */
        assertEmpty(expectedProperties);
        assertEmpty(actualProperties);
    }

    /**
     * Tests a NetCDF binary file. This method inherits the tests defined in GeoAPI,
     * and adds some additional tests for attributes parsed by SIS but not GeoAPI.
     *
     * @throws IOException if the test file can not be read.
     */
    @Test
    @Override
    public void testNCEP() throws IOException {
        addCommonProperties(expectedProperties, true);
        super.testNCEP();
        assertSame("metadataScope", ScopeCode.DATASET, getSingleton(metadata.getMetadataScopes()).getResourceScope());
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
     * @throws IOException if the test file can not be read.
     */
    @Test
    @Override
    public void testLandsat() throws IOException {
        final Map<String,Object> expected = expectedProperties;
        addCommonProperties(expected, false);
        assertNull(expected.put("identificationInfo.citation.title", "Landsat-GDAL"));
        assertNull(expected.put("metadataIdentifier.code", "Landsat-GDAL"));
        super.testLandsat();
        assertSame("metadataScope", ScopeCode.DATASET, getSingleton(metadata.getMetadataScopes()).getResourceScope());

        assertEmpty(expectedProperties);
        assertEmpty(actualProperties);
    }

    /**
     * Tests the "Current Icing Product" file (binary format).
     *
     * @throws IOException if the test file can not be read.
     */
    @Test
    @Override
    public void testCIP() throws IOException {
        final Map<String,Object> expected = expectedProperties;
        addCommonProperties(expected, true);
        assertNull(expected.put("identificationInfo.citation.title", "CIP"));
        assertNull(expected.put("metadataIdentifier.code", "CIP"));
        super.testCIP();
        assertSame("metadataScope", ScopeCode.DATASET, getSingleton(metadata.getMetadataScopes()).getResourceScope());
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
