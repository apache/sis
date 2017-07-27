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

import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
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
 * @version 0.8
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
}
