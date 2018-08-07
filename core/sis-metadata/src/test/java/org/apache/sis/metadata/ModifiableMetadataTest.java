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

import java.util.Arrays;
import java.util.Collections;
import org.opengis.metadata.distribution.MediumFormat;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.distribution.DefaultMedium;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link ModifiableMetadata}, in particular the state transitions.
 * This class uses {@link DefaultMedium} as an arbitrary metadata implementation for running the tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(AbstractMetadataTest.class)
public final strictfp class ModifiableMetadataTest extends TestCase {
    /**
     * An arbitrary metadata on which to perform the tests.
     */
    private final DefaultMedium md;

    /**
     * Creates an arbitrary metadata for testing purpose.
     */
    public ModifiableMetadataTest() {
        md = new DefaultMedium();
        md.setMediumNote(new SimpleInternationalString("The original note."));
        md.setIdentifier(new DefaultIdentifier("A medium identifier"));
        assertInstanceOf("mediumFormat", CodeListSet.class, md.getMediumFormats());    // Force assignation of a Set in private field.
    }

    /**
     * Returns the state of the identifier of the metadata.
     */
    private ModifiableMetadata.State identifierState() {
        return ((DefaultIdentifier) md.getIdentifier()).state();
    }

    /**
     * Verifies the metadata properties values.
     */
    private void assertPropertiesEqual(final Integer volumes, final String mediumNote, final MediumFormat... formats) {
        assertEquals("mediumNote", mediumNote, String.valueOf(md.getMediumNote()));
        assertEquals("volumes", volumes, md.getVolumes());
        assertSetEquals(Arrays.asList(formats), md.getMediumFormats());
    }

    /**
     * Verifies the exception for an unmodifiable metadata.
     */
    private static void verifyUnmodifiableException(final UnmodifiableMetadataException e) {
        assertNotNull("Expected an error message.", e.getMessage());
    }

    /**
     * Tests the behavior when state is {@link ModifiableMetadata.State#EDITABLE}.
     * Setting new values and overwriting existing values are allowed.
     */
    @Test
    public void testStateEditable() {
        assertFalse("transition", md.transition(ModifiableMetadata.State.EDITABLE));        // Shall be a no-op.
        assertEquals("state", ModifiableMetadata.State.EDITABLE, md.state());
        assertEquals("identifier.state", ModifiableMetadata.State.EDITABLE, identifierState());
        /*
         * Verify conditions given in Javadoc: allow new values and overwriting.
         */
        md.setVolumes(4);                                                                   // New value.
        md.setMediumNote(new SimpleInternationalString("A new note."));                     // Overwriting.
        md.getMediumFormats().add(MediumFormat.TAR);
        md.setMediumFormats(Collections.singleton(MediumFormat.CPIO));                      // Discard TAR.
        md.getMediumFormats().add(MediumFormat.ISO_9660);
        assertPropertiesEqual(4, "A new note.", MediumFormat.CPIO, MediumFormat.ISO_9660);
    }

    /**
     * Tests the behavior when state is {@link ModifiableMetadata.State#COMPLETABLE}.
     * Setting new values is allowed but overwriting existing values is not allowed.
     */
    @Test
    public void testStateCompletable() {
        assertTrue("transition", md.transition(ModifiableMetadata.State.COMPLETABLE));
        assertEquals("state", ModifiableMetadata.State.COMPLETABLE, md.state());
        assertEquals("identifier.state", ModifiableMetadata.State.COMPLETABLE, identifierState());
        try {
            md.transition(ModifiableMetadata.State.EDITABLE);
            fail("Shall not be allowed to transition back to editable state.");
        } catch (UnmodifiableMetadataException e) {
            verifyUnmodifiableException(e);
        }
        /*
         * Verify conditions given in Javadoc: allow new values but not overwriting.
         */
        md.setVolumes(4);
        try {
            md.setMediumNote(new SimpleInternationalString("A new note."));
            fail("Overwriting an existing value shall not be allowed.");
        } catch (UnmodifiableMetadataException e) {
            verifyUnmodifiableException(e);
            assertTrue(e.getMessage().contains("The original note."));
        }
        try {
            md.getMediumFormats().add(MediumFormat.TAR);
            fail("Adding new value shall not be allowed.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
        md.setMediumFormats(Collections.singleton(MediumFormat.CPIO));
        try {
            md.getMediumFormats().add(MediumFormat.ISO_9660);
            fail("Adding new value shall not be allowed.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
        assertPropertiesEqual(4, "The original note.", MediumFormat.CPIO);
    }

    /**
     * Tests the behavior when state is {@link ModifiableMetadata.State#FINAL}.
     * Setting new values and overwriting existing values are <strong>not</strong> allowed.
     */
    @Test
    public void testStateFinal() {
        assertTrue("transition", md.transition(ModifiableMetadata.State.FINAL));
        assertEquals("state", ModifiableMetadata.State.FINAL, md.state());
        assertEquals("identifier.state", ModifiableMetadata.State.FINAL, identifierState());
        try {
            md.transition(ModifiableMetadata.State.EDITABLE);
            fail("Shall not be allowed to transition back to editable state.");
        } catch (UnmodifiableMetadataException e) {
            verifyUnmodifiableException(e);
        }
        /*
         * Verify conditions given in Javadoc: new values and overwriting not allowed.
         */
        try {
            md.setVolumes(4);
            fail("Setting new value shall not be allowed.");
        } catch (UnmodifiableMetadataException e) {
            verifyUnmodifiableException(e);
        }
        try {
            md.setMediumNote(new SimpleInternationalString("A new note."));
            fail("Overwriting an existing value shall not be allowed.");
        } catch (UnmodifiableMetadataException e) {
            verifyUnmodifiableException(e);
        }
        try {
            md.getMediumFormats().add(MediumFormat.TAR);
            fail("Adding new value shall not be allowed.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
        try {
            md.setMediumFormats(Collections.singleton(MediumFormat.CPIO));
            fail("Setting new value shall not be allowed.");
        } catch (UnmodifiableMetadataException e) {
            verifyUnmodifiableException(e);
        }
        assertPropertiesEqual(null, "The original note.");
    }
}
