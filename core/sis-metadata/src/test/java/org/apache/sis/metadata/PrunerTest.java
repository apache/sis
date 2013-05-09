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
package org.apache.sis.metadata;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.ValueExistencePolicy.isNullOrEmpty;


/**
 * Tests the {@link AbstractMetadata#isEmpty()} and {@link ModifiableMetadata#prune()} methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
@DependsOn(ValueMapTest.class)
public final strictfp class PrunerTest extends TestCase {
    /**
     * The root metadata object being tested.
     */
    private final DefaultMetadata metadata;

    /**
     * A child of the metadata object being tested.
     */
    private final DefaultDataIdentification identification;

    /**
     * A child of the metadata object being tested.
     */
    private final DefaultExtent extent;

    /**
     * A child of the metadata object being tested.
     */
    private final DefaultGeographicBoundingBox bbox;

    /**
     * Creates the metadata objects to be used for the test.
     */
    public PrunerTest() {
        metadata       = new DefaultMetadata();
        identification = new DefaultDataIdentification();
        extent         = new DefaultExtent();
        bbox           = new DefaultGeographicBoundingBox();
        extent.getGeographicElements().add(bbox);
        identification.getExtents().add(extent);
        metadata.getIdentificationInfo().add(identification);
    }

    /**
     * Tests the {@link AbstractMetadata#isEmpty()} method.
     */
    @Test
    public void testIsEmpty() {
        /*
         * Initially empty tree, or tree with only empty element.
         */
        assertTrue("GeographicBoundingBox", bbox.isEmpty());
        assertTrue("Extent",                extent.isEmpty());
        assertTrue("DataIdentification",    identification.isEmpty());
        assertTrue("Metadata",              metadata.isEmpty());
        /*
         * Set a non-empty identification info.
         */
        identification.setCitation(new DefaultCitation("A citation title"));
        assertTrue ("GeographicBoundingBox", bbox.isEmpty());
        assertTrue ("Extent",                extent.isEmpty());
        assertFalse("DataIdentification",    identification.isEmpty());
        assertFalse("Metadata",              metadata.isEmpty());
        /*
         * Set a non-empty metadata info.
         */
        metadata.setFileIdentifier("A file identifiers");
        assertTrue ("GeographicBoundingBox", bbox.isEmpty());
        assertTrue ("Extent",                extent.isEmpty());
        assertFalse("DataIdentification",    identification.isEmpty());
        assertFalse("Metadata",              metadata.isEmpty());
        /*
         * Set an empty string in an element.
         */
        identification.setCitation(new DefaultCitation("  "));
        assertTrue ("GeographicBoundingBox", bbox.isEmpty());
        assertTrue ("Extent",                extent.isEmpty());
        assertTrue ("DataIdentification",    identification.isEmpty());
        assertFalse("Metadata",              metadata.isEmpty());
        /*
         * Set an empty string in an element.
         */
        metadata.setFileIdentifier("   ");
        assertTrue("Metadata", metadata.isEmpty());
    }

    /**
     * Tests the {@link ModifiableMetadata#prune()} method.
     */
    @Test
    public void testPrune() {
        metadata.setFileIdentifier("A file identifiers");
        identification.setCitation(new DefaultCitation("A citation title"));
        assertFalse(isNullOrEmpty(metadata.getFileIdentifier()));
        assertFalse(isNullOrEmpty(identification.getCitation()));
        assertEquals(1, metadata.getIdentificationInfo().size());
        assertEquals(1, identification.getExtents().size());
        assertEquals(1, extent.getGeographicElements().size());
        assertFalse(metadata.isEmpty());

        metadata.prune();
        assertFalse(isNullOrEmpty(metadata.getFileIdentifier()));
        assertFalse(isNullOrEmpty(identification.getCitation()));
        assertEquals(1, metadata.getIdentificationInfo().size());
        assertEquals(0, identification.getExtents().size());
        assertEquals(0, extent.getGeographicElements().size());
        assertFalse(metadata.isEmpty());

        metadata.setFileIdentifier(" ");
        identification.setCitation(new DefaultCitation(" "));
        assertNotNull(metadata.getFileIdentifier());
        metadata.prune();

        assertNull(metadata.getFileIdentifier());
        assertNull(identification.getCitation());
        assertTrue(metadata.getIdentificationInfo().isEmpty());
        assertTrue(identification.getExtents().isEmpty());
        assertTrue(extent.getGeographicElements().isEmpty());
        assertTrue(metadata.isEmpty());
    }
}
