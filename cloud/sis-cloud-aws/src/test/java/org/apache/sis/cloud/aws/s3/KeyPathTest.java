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

import java.net.URI;
import java.nio.file.Path;
import java.util.Iterator;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import software.amazon.awssdk.services.s3.model.Bucket;

import static org.junit.Assert.*;


/**
 * Tests {@link KeyPath}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class KeyPathTest extends TestCase {
    /**
     * A URI to use for testing purpose.
     */
    static final URI DUMMY_URI = URI.create("S3://the-bucket/first/second/third/the-file");

    /**
     * The file system used in for the test paths.
     */
    private final ClientFileSystem fs;

    /**
     * The path to test.
     */
    private final KeyPath absolute, relative;

    /**
     * Creates a new test case.
     */
    public KeyPathTest() {
        fs = ClientFileSystemTest.create();
        final KeyPath root = new KeyPath(fs, Bucket.builder().name("the-bucket").build());
        absolute = new KeyPath(root, "first/second/third/the-file", false);
        relative = new KeyPath(fs, "second/third/the-file", false);
    }

    /**
     * Tests the parsing done in the constructor.
     */
    @Test
    public void testConstructor() {
        assertEquals(relative, new KeyPath(fs, "second/third/the-file", new String[0], false));
        assertEquals(relative, new KeyPath(fs, "second/", new String[] {"/third/", "the-file"}, false));
        assertEquals(absolute, new KeyPath(fs, "S3://the-bucket/first/second/third/the-file", new String[0], false));
        assertEquals(absolute, new KeyPath(fs, "/the-bucket/", new String[] {"first", "second/", "/third/", "the-file"}, false));
        assertEquals(absolute, new KeyPath(fs, "the-bucket", new String[] {"/first/second/third/the-file"}, true));
    }

    /**
     * Tests {@link KeyPath#isAbsolute()}.
     */
    @Test
    public void testIsAbsolute() {
        assertTrue (absolute.isAbsolute());
        assertFalse(relative.isAbsolute());
    }

    /**
     * Tests {@link KeyPath#getRoot()}.
     */
    @Test
    public void testGetRoot() {
        assertNull(relative.getRoot());
        final KeyPath p = (KeyPath) absolute.getRoot();
        assertSame(p, p.getRoot());
        assertTrue(p.isDirectory);
        assertNotNull(p.bucket);
        assertNull(p.key);
    }

    /**
     * Tests {@link KeyPath#getFileName()}.
     */
    @Test
    public void testGetFileName() {
        KeyPath p = (KeyPath) absolute.getFileName();
        assertSame(p, p.getFileName());
        assertEquals("the-file", p.key);
        assertFalse(p.isDirectory);
        assertNull(p.bucket);
        assertEquals(p, relative.getFileName());

        p = (KeyPath) absolute.getRoot().getFileName();
        assertSame(p, p.getFileName());
        assertTrue(p.isDirectory);
        assertNull(p.key);
    }

    /**
     * Tests {@link KeyPath#getParent()}.
     */
    @Test
    public void testGetParent() {
        KeyPath p = (KeyPath) absolute.getParent();
        assertEquals("first/second/third", p.key);
        assertTrue(p.isDirectory);
        assertNotNull(p.bucket);

        p = (KeyPath) relative.getParent();
        assertEquals("second/third", p.key);
        assertTrue(p.isDirectory);
        assertNull(p.bucket);
    }

    /**
     * Tests {@link KeyPath#getNameCount()}.
     */
    @Test
    public void testGetNameCount() {
        assertEquals(5, absolute.getNameCount());
        assertEquals(3, relative.getNameCount());
        assertEquals(1, absolute.getRoot().getNameCount());
    }

    /**
     * Tests {@link KeyPath#getName(int)}.
     */
    @Test
    public void testGetName() {
        assertEquals("S3://the-bucket", absolute.getName(0).toString());
        assertEquals("first",           absolute.getName(1).toString());
        assertEquals("second",          absolute.getName(2).toString());
        assertEquals("third",           absolute.getName(3).toString());
        assertEquals("the-file",        absolute.getName(4).toString());
        assertEquals("second",          relative.getName(0).toString());
        assertEquals("third",           relative.getName(1).toString());
        assertEquals("the-file",        relative.getName(2).toString());
        try {
            absolute.getName(5);
            fail("Expected an exception.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests {@link KeyPath#subpath(int, int)}.
     */
    @Test
    public void testSubpath() {
        assertEquals("S3://the-bucket/first/second", absolute.subpath(0, 3).toString());
        assertEquals("second/third/the-file",        absolute.subpath(2, 5).toString());
        assertEquals("second/third",                 absolute.subpath(2, 4).toString());
        assertEquals("third/the-file",               relative.subpath(1, 3).toString());
        assertSame(absolute, absolute.subpath(0, 5));
        assertSame(relative, relative.subpath(0, 3));
    }

    /**
     * Tests {@link KeyPath#startsWith(Path)}.
     */
    @Test
    public void testStartsWith() {
        assertFalse(absolute.startsWith(new KeyPath(fs,       "first/second", true)));
        assertTrue (absolute.startsWith(new KeyPath(absolute, "first/second", true)));
        assertFalse(absolute.startsWith(new KeyPath(absolute, "first/secon",  true)));
        assertFalse(absolute.startsWith(new KeyPath(absolute,  "irst/second", true)));
        assertFalse(absolute.startsWith(relative));
        assertFalse(relative.startsWith(absolute));
        assertTrue (relative.startsWith(relative));
        assertTrue (absolute.startsWith(absolute));
    }

    /**
     * Tests {@link KeyPath#endsWith(Path)}.
     */
    @Test
    public void testEndsWith() {
        assertTrue (relative.endsWith(relative));
        assertTrue (absolute.endsWith(absolute));
        assertTrue (absolute.endsWith(relative));
        assertFalse(relative.endsWith(absolute));
        assertFalse(absolute.endsWith(new KeyPath(absolute, "the-file", false)));
        assertTrue (absolute.endsWith(new KeyPath(fs,       "the-file", false)));
        assertFalse(absolute.endsWith(new KeyPath(fs,       "he-file", false)));
    }

    /**
     * Tests {@link KeyPath#resolve(Path)}.
     */
    @Test
    public void testResolve() {
        assertSame(absolute, relative.resolve(absolute));
        final KeyPath tip = new KeyPath(fs, "tip", false);
        assertEquals("second/third/the-file/tip", relative.resolve(tip).toString());
        assertEquals("S3://the-bucket/first/second/third/the-file/tip", absolute.resolve(tip).toString());
        assertEquals(absolute, new KeyPath(absolute, "first", true).resolve(relative));
    }

    /**
     * Tests {@link KeyPath#relativize(Path)}.
     */
    @Test
    public void testRelativize() {
        final KeyPath base = new KeyPath(absolute, "first", true);
        assertEquals(relative, base.relativize(absolute));
    }

    /**
     * Tests {@link KeyPath#iterator()}.
     */
    @Test
    public void testIterator() {
        verifyIterator(absolute.iterator(), true);
        verifyIterator(relative.iterator(), false);
    }

    /**
     * Verifies each iteration step on {@link #absolute} or {@link #relative} path.
     */
    private void verifyIterator(final Iterator<Path> it, final boolean isAbsolute) {
        if (isAbsolute) {
            assertTrue(it.hasNext()); assertEquals(new KeyPath(absolute, null, true), it.next());
            assertTrue(it.hasNext()); assertEquals(new KeyPath(fs, "first", true), it.next());
        }
        assertTrue(it.hasNext()); assertEquals(new KeyPath(fs, "second",   true),  it.next());
        assertTrue(it.hasNext()); assertEquals(new KeyPath(fs, "third",    true),  it.next());
        assertTrue(it.hasNext()); assertEquals(new KeyPath(fs, "the-file", false), it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Tests {@link KeyPath#compareTo(Path)}.
     */
    @Test
    public void testCompareTo() {
        assertEquals( 0, absolute.compareTo(absolute));
        assertEquals( 0, relative.compareTo(relative));
        assertEquals(-1, absolute.compareTo(relative));
        assertEquals(+1, relative.compareTo(absolute));
        assertTrue(absolute.compareTo(new KeyPath(absolute, "first", true)) > 0);
    }
}
