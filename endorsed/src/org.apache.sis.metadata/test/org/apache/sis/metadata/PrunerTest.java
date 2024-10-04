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

import java.util.Set;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction;
import org.apache.sis.metadata.simple.SimpleIdentifier;
import static org.apache.sis.metadata.ValueExistencePolicy.isNullOrEmpty;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link AbstractMetadata#isEmpty()} and {@link ModifiableMetadata#prune()} methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PrunerTest extends TestCase {
    /**
     * The root metadata object being tested.
     */
    private final DefaultMetadata metadata;

    /**
     * A child of the metadata object being tested.
     */
    private final DefaultDataIdentification identification;

    /**
     * A child of another child metadata object being tested.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-194">SIS-194</a>
     */
    private final DefaultRepresentativeFraction scale;

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
        scale          = new DefaultRepresentativeFraction();
        extent         = new DefaultExtent();
        bbox           = new DefaultGeographicBoundingBox();
        extent.setGeographicElements(Set.of(bbox));
        identification.setExtents(Set.of(extent));
        identification.setSpatialResolutions(Set.of(new DefaultResolution(scale)));
        metadata.setIdentificationInfo(Set.of(identification));
    }

    /**
     * Tests the {@link AbstractMetadata#isEmpty()} method.
     */
    @Test
    public void testIsEmpty() {
        /*
         * Initially empty tree, or tree with only empty element.
         */
        assertTrue(bbox.isEmpty());
        assertTrue(extent.isEmpty());
        assertTrue(scale.isEmpty());
        assertTrue(identification.isEmpty());
        assertTrue(metadata.isEmpty());
        /*
         * Set a non-empty identification info.
         */
        identification.setCitation(new DefaultCitation("A citation title"));
        assertTrue (bbox.isEmpty());
        assertTrue (extent.isEmpty());
        assertTrue (scale.isEmpty());
        assertFalse(identification.isEmpty());
        assertFalse(metadata.isEmpty());
        /*
         * Set a non-empty metadata info.
         */
        metadata.setMetadataIdentifier(new SimpleIdentifier(null, "A file identifiers", false));
        assertTrue (bbox.isEmpty());
        assertTrue (extent.isEmpty());
        assertTrue (scale.isEmpty());
        assertFalse(identification.isEmpty());
        assertFalse(metadata.isEmpty());
        /*
         * Set an empty string in an element.
         */
        identification.setCitation(new DefaultCitation("  "));
        assertTrue (bbox.isEmpty());
        assertTrue (extent.isEmpty());
        assertTrue (scale.isEmpty());
        assertTrue (identification.isEmpty());
        assertFalse(metadata.isEmpty());
        /*
         * Set a representative fraction.
         */
        scale.setDenominator(1000);
        assertTrue (bbox.isEmpty());
        assertTrue (extent.isEmpty());
        assertFalse(scale.isEmpty());
        assertFalse(identification.isEmpty());
        assertFalse(metadata.isEmpty());
        /*
         * Set an empty string in an element.
         */
        scale.setScale(Double.NaN);
        metadata.setMetadataIdentifier(new SimpleIdentifier(null, "   ", false));
        assertTrue(scale.isEmpty());
        assertTrue(identification.isEmpty());
        assertTrue(metadata.isEmpty());
    }

    /**
     * Adds to the {@link #metadata} an object having a cyclic association.
     * The cycle is between {@code platform.instrument} and {@code instrument.isMountedOn}.
     */
    private void createCyclicMetadata() {
        metadata.setAcquisitionInformation(Set.of(MetadataStandardTest.createCyclicMetadata()));
    }

    /**
     * Tests the {@link AbstractMetadata#isEmpty()} method on a metadata object having a cycle association.
     * In absence of safety guard against infinite recursion, this test would produce {@link StackOverflowError}.
     */
    @Test
    public void testIsEmptyOnCyclicMetadata() {
        assertTrue(metadata.isEmpty());
        createCyclicMetadata();
        assertFalse(metadata.isEmpty());
    }

    /**
     * Tests the {@link ModifiableMetadata#prune()} method.
     */
    @Test
    public void testPrune() {
        metadata.setMetadataIdentifier(new SimpleIdentifier(null, "A file identifiers", false));
        identification.setCitation(new DefaultCitation("A citation title"));
        assertFalse(isNullOrEmpty(metadata.getMetadataIdentifier()));
        assertFalse(isNullOrEmpty(identification.getCitation()));
        assertEquals(1, metadata.getIdentificationInfo().size());
        assertEquals(1, identification.getExtents().size());
        assertEquals(1, extent.getGeographicElements().size());
        assertFalse(metadata.isEmpty());

        metadata.prune();
        assertFalse(isNullOrEmpty(metadata.getMetadataIdentifier()));
        assertFalse(isNullOrEmpty(identification.getCitation()));
        assertEquals(1, metadata.getIdentificationInfo().size());
        assertEquals(0, identification.getExtents().size());
        assertEquals(0, extent.getGeographicElements().size());
        assertFalse(metadata.isEmpty());

        metadata.setMetadataIdentifier(new SimpleIdentifier(null, " ", false));
        identification.setCitation(new DefaultCitation(" "));
        assertNotNull(metadata.getMetadataIdentifier());
        metadata.prune();

        assertNull(metadata.getMetadataIdentifier());
        assertNull(identification.getCitation());
        assertTrue(metadata.getIdentificationInfo().isEmpty());
        assertTrue(identification.getExtents().isEmpty());
        assertTrue(extent.getGeographicElements().isEmpty());
        assertTrue(metadata.isEmpty());
    }

    /**
     * Tests the {@link AbstractMetadata#prune()} method on a metadata object having a cycle association.
     * In absence of safety guard against infinite recursion, this test would produce {@link StackOverflowError}.
     */
    @Test
    public void testPruneOnCyclicMetadata() {
        createCyclicMetadata();
        assertEquals(1, metadata.getIdentificationInfo()    .size());
        assertEquals(1, metadata.getAcquisitionInformation().size());
        metadata.prune();
        assertEquals(0, metadata.getIdentificationInfo()    .size());
        assertEquals(1, metadata.getAcquisitionInformation().size());
    }
}
