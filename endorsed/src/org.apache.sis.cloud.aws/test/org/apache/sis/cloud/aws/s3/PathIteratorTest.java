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

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import software.amazon.awssdk.services.s3.model.Bucket;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests {@link PathIterator} and {@link FileService#checkAccess(Path, AccessMode...)} on a bucket root,
 * using a fake S3 client.
 *
 * @author  Quentin Bialota (Geomatys)
 */
@SuppressWarnings("exports")
public final class PathIteratorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PathIteratorTest() {
    }

    /**
     * Creates a self-hosted file system backed by a fake client.
     */
    private static ClientFileSystem createFileSystem(final String... buckets)
            throws URISyntaxException, ReflectiveOperationException
    {
        final ClientFileSystem fs = ClientFileSystemTest.create(true);
        final Field client = ClientFileSystem.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(fs, new S3ClientMock(buckets));
        return fs;
    }

    /**
     * Returns the root of the given bucket as built from an URI, i.e. without bucket metadata.
     */
    static KeyPath bucketRoot(final ClientFileSystem fs, final String bucket) {
        return new KeyPath(fs, bucket, new String[0], true);
    }

    /**
     * Lists the root of a bucket. Used to fail with a {@link NullPointerException}
     * because the root has no key. Also checks that directory names are only names.
     *
     * @throws Exception if an error occurred while creating the file system or listing the bucket.
     */
    @Test
    public void testListBucketRoot() throws Exception {
        final ClientFileSystem fs = createFileSystem("the-bucket");
        final var names = new ArrayList<String>();
        try (DirectoryStream<Path> stream = fs.provider().newDirectoryStream(bucketRoot(fs, "the-bucket"), (p) -> true)) {
            for (final Path child : stream) {
                names.add(child.getFileName().toString());
            }
        }
        assertArrayEquals(new String[] {"common", "data.nc"}, names.toArray());
    }

    /**
     * Tests {@code checkAccess} on the root of a bucket built from an URI (no creation date).
     *
     * @throws Exception if an error occurred while creating the file system.
     */
    @Test
    public void testBucketRootExists() throws Exception {
        final ClientFileSystem fs = createFileSystem("the-bucket");
        final FileService service = assertInstanceOf(FileService.class, fs.provider());
        assertDoesNotThrow(() -> service.checkAccess(bucketRoot(fs, "the-bucket")));
        assertMessageContains(assertThrows(NoSuchFileException.class, () -> service.checkAccess(bucketRoot(fs, "missing-bucket"))), "missing-bucket");

        // A bucket listed by the server has a creation date: no request needed.
        final var listed = new KeyPath(fs, Bucket.builder().name("other").creationDate(java.time.Instant.EPOCH).build());
        assertDoesNotThrow(() -> service.checkAccess(listed));
    }
}
