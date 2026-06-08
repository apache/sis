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

import java.net.URISyntaxException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ClientFileSystem}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 */
@SuppressWarnings("exports")
public final class ClientFileSystemTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ClientFileSystemTest() {
    }

    /**
     * Returns the file system to use for testing purpose.
     *
     * @param  selfHosted  whether the service should be self hosted.
     * @throws URISyntaxException if an error occurred while creating the <abbr>URI</abbr> for the self-hosted S3.
     */
    static ClientFileSystem create(final boolean selfHosted) throws URISyntaxException {
        final var fs = new FileService();
        if (selfHosted) {
            return new ClientFileSystem(fs, null, new Server(null, "testhost", 8581, true, null), null, null);
        } else {
            return new ClientFileSystem(fs, new Server(null));
        }
    }

    /**
     * Tests {@link ClientFileSystem#getSeparator()}.
     *
     * @throws URISyntaxException if an error occurred while creating the <abbr>URI</abbr> for the self-hosted S3.
     */
    @Test
    public void testGetSeparator() throws URISyntaxException {
        assertEquals("/", create(false).getSeparator());
        assertEquals("/", create(true).getSeparator());
    }
}
