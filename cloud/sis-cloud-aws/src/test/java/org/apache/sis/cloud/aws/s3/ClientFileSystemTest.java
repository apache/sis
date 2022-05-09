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
package org.apache.sis.cloud.aws.s3;

import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests {@link ClientFileSystem}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class ClientFileSystemTest extends TestCase {
    /**
     * The instance to use for testing purposes.
     */
    private final ClientFileSystem fs;

    /**
     * Returns the file system to use for testing purpose.
     */
    static ClientFileSystem create() {
        return new ClientFileSystem(new FileService(), null);
    }

    /**
     * Creates a new test case.
     */
    public ClientFileSystemTest() {
        fs = create();
    }

    /**
     * Tests {@link ClientFileSystem#getSeparator()}.
     */
    @Test
    public void testGetSeparator() {
        assertEquals("/", fs.getSeparator());
    }
}
