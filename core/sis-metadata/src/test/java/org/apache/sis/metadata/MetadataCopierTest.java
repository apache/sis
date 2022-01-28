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

import java.util.Map;
import java.util.Locale;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.GeographicExtent;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link MetadataCopier} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see org.apache.sis.internal.metadata.MergerTest
 *
 * @since 0.8
 * @module
 */
@DependsOn(MetadataStandardTest.class)
public final strictfp class MetadataCopierTest extends TestCase {
    /**
     * Tests {@link MetadataCopier#copy(Object)}.
     */
    @Test
    public void testCopy() {
        final MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
        final DefaultCitation original = HardCodedCitations.EPSG;
        final DefaultCitation copy = (DefaultCitation) copier.copy(original);
        assertNotSame(original, copy);
        assertNotSame(getSingleton(original.getCitedResponsibleParties()),
                      getSingleton(copy.getCitedResponsibleParties()));
        assertEquals(original, copy);
    }

    /**
     * Tests {@link MetadataCopier#copy(Class, Object)}.
     */
    @Test
    public void testCopyWithType() {
        final MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
        final DefaultCitation original = HardCodedCitations.EPSG;
        final Citation copy = copier.copy(Citation.class, original);
        assertNotSame(original, copy);
        assertNotSame(getSingleton(original.getCitedResponsibleParties()),
                      getSingleton(copy.getCitedResponsibleParties()));
        assertEquals(original, copy);
    }

    /**
     * Tests {@link MetadataCopier#copy(Class, Object)} when the given type is a parent
     * of the interface implemented by the given argument.
     */
    @Test
    public void testCopyWithSuperType() {
        final MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
        final DefaultGeographicDescription original = new DefaultGeographicDescription("Some area.");
        final GeographicExtent copy = copier.copy(GeographicExtent.class, original);
        assertNotSame(original, copy);
        assertEquals (original, copy);
    }

    /**
     * Tests {@link MetadataCopier#copy(Class, Object)} with an implementation class specified instead of an interface.
     */
    @Test
    public void testWrongArgument() {
        final MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
        final DefaultCitation original = HardCodedCitations.EPSG;
        try {
            copier.copy(DefaultCitation.class, original);
            fail("Should not have accepted implementation class argument.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("DefaultCitation"));
        }
    }

    /**
     * Tests with a metadata containing a {@link DefaultMetadata#getLocalesAndCharsets()} property.
     * This property is defined by a {@link Map}.
     */
    @Test
    public void testLocaleAndCharsets() {
        final MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
        final DefaultMetadata original = new DefaultMetadata();
        original.getLocalesAndCharsets().put(Locale.FRENCH,   StandardCharsets.UTF_8);
        original.getLocalesAndCharsets().put(Locale.JAPANESE, StandardCharsets.UTF_16);
        final Metadata copy = copier.copy(Metadata.class, original);
        final Map<Locale,Charset> lc = copy.getLocalesAndCharsets();
        assertEquals(StandardCharsets.UTF_8,  lc.get(Locale.FRENCH));
        assertEquals(StandardCharsets.UTF_16, lc.get(Locale.JAPANESE));
        assertEquals (original, copy);
        assertNotSame(original, copy);
        assertNotSame(original.getLocalesAndCharsets(), lc);
    }
}
