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
package org.apache.sis.storage.landsat;

import java.util.regex.Matcher;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link MetadataReader}.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 */
public final class MetadataReaderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MetadataReaderTest() {
    }

    /**
     * Tests the regular expression used for detecting the
     * “Image courtesy of the U.S. Geological Survey” credit.
     */
    @Test
    public void testCreditPattern() {
        final Matcher m = MetadataReader.CREDIT.matcher("Image courtesy of the U.S. Geological Survey");
        assertTrue("matches", m.find());
        assertEquals("end", 22, m.end());
    }
}
