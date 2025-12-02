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
package org.apache.sis.storage.isobmff.gimi;

import java.util.UUID;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Verifies the <abbr>UUID</abbr> declared in extensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ExtensionTest {
    /**
     * Creates a new test case.
     */
    public ExtensionTest() {
    }

    /**
     * Verifies the <abbr>UUID</abbr> declared in extensions.
     * Some identifiers were defined in previous <abbr>GIMI</abbr> versions
     * and are supported by <abbr>SIS</abbr> for compatibility reasons.
     */
    @Test
    public void verifyUUIDs() {
        assertEquals(UUID.fromString("4a66efa7-e541-526c-9427-9e77617feb7d"), UnknownProperty.EXTENDED_TYPE);
        assertEquals(UUID.fromString("137a1742-75ac-4747-82bc-659576e8675b"), ModelCRS.EXTENDED_TYPE);
        assertEquals(UUID.fromString("c683364f-d6a4-48b8-a76b-17a30af40c10"), ModelTiePoint.EXTENDED_TYPE);
        assertEquals(UUID.fromString("763cf838-b630-440b-84f8-be44bf9910af"), ModelTransformation.EXTENDED_TYPE);
    }
}
