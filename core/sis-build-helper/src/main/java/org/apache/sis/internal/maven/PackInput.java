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
package org.apache.sis.internal.maven;

import java.util.Map;
import java.util.Enumeration;
import java.util.jar.*;
import java.io.File;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;


/**
 * A JAR file to be used for input by {@link Packer}.
 * Those files will be open in read-only mode.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
final class PackInput implements Closeable {
    /**
     * The {@code value} directory.
     */
    private static final String META_INF = "META-INF/";

    /**
     * Files from the {@code META-INF} directory to include.
     * Every files not in this list will be excluded.
     */
    private static final String[] INCLUDES = {"registryFile.jai"};

    /**
     * The {@code value} directory.
     */
    static final String SERVICES = META_INF + "services/";

    /**
     * The prefix of native resources in JAR files. All those resources will be excluded from
     * the PACK200 file and stored in {@link #nativeFiles} instead (unless the map is null).
     */
    private static final String NATIVE = "native/";

    /**
     * The attribute name in {@code MANIFEST.MF} files for splash screen.
     */
    static final Attributes.Name SPLASH_SCREEN = new Attributes.Name("SplashScreen-Image");

    /**
     * The JAR file.
     */
    private JarFile file;

    /**
     * The main class obtained from the manifest, or {@code null} if none.
     */
    final String mainClass;

    /**
     * The splash screen image obtained from the manifest, or {@code null} if none.
     */
    final String splashScreen;

    /**
     * The map where to store native files found during iteration over the JAR entries.
     * Keys are filename without the {@value #NATIVE} prefix. Values are the actual data.
     * If null, then no native files filtering is done.
     */
    private final Map<String,byte[]> nativeFiles;

    /**
     * An enumeration over the entries. We are going to iterate only once.
     */
    private Enumeration<JarEntry> entries;

    /**
     * The current entry under iteration.
     */
    private JarEntry entry;

    /**
     * Opens the given JAR file in read-only mode.
     *
     * @param  file        the file to open.
     * @param  nativeFiles if non-null, where to store native files found during iteration over the JAR entries.
     * @throws IOException if the file can't be open.
     */
    PackInput(final File file, final Map<String,byte[]> nativeFiles) throws IOException {
        this.nativeFiles = nativeFiles;
        this.file = new JarFile(file);
        final Manifest manifest = this.file.getManifest();
        if (manifest != null) {
            final Attributes attributes = manifest.getMainAttributes();
            if (attributes != null) {
                mainClass    = attributes.getValue(Attributes.Name.MAIN_CLASS);
                splashScreen = attributes.getValue(SPLASH_SCREEN);
                return;
            }
        }
        mainClass    = null;
        splashScreen = null;
    }

    /**
     * Returns the entries in the input JAR file.
     *
     * @return the next entry, or {@code null} if the iteration is finished.
     */
    JarEntry nextEntry() throws IOException {
        if (entries == null) {
            entries = file.entries();
        }
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            final String name = entry.getName();
            if (name.startsWith(META_INF) && !name.startsWith(SERVICES)) {
                if (!include(name.substring(META_INF.length()))) {
                    continue;
                }
            }
            if (nativeFiles != null && name.startsWith(NATIVE)) {
                if (!entry.isDirectory()) {
                    if (nativeFiles.put(name.substring(NATIVE.length()), load()) != null) {
                        throw new IOException("Duplicated entry: " + name);
                    }
                }
                continue;
            }
            entry.setMethod(JarEntry.DEFLATED);
            entry.setCompressedSize(-1);                    // Change in method has changed the compression size.
            return entry;
        }
        return entry = null;
    }

    /**
     * Returns {@code true} if the given name is part of the {@link #INCLUDES} list.
     */
    private static boolean include(final String name) {
        for (final String include : INCLUDES) {
            if (name.equals(include)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads in memory the content of current JAR entry.
     * This method should be invoked only for entries of reasonable size.
     */
    private byte[] load() throws IOException {
        final long size = entry.getSize();
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IOException("Unsupported size for \"" + entry.getName() + "\": " + size);
        }
        final byte[] content = new byte[(int) size];
        final int actual;
        try (InputStream in = getInputStream()) {
            actual = in.read(content);
        }
        if (actual != size) {
            throw new IOException("Expected " + size + " bytes in \"" + entry.getName() + "\" but found " + actual);
        }
        return content;
    }

    /**
     * Returns the input stream for the current entry.
     *
     * @param entry The entry for which to get an input stream.
     */
    InputStream getInputStream() throws IOException {
        return file.getInputStream(entry);
    }

    /**
     * Returns the input stream for the entry of the given name. This method must be invoked
     * before the first call to {@link #nextEntry}. Each entry can be requested only once.
     *
     * @param  name  the name of the entry
     * @return the input stream for the requested entry, or {@code null} if none.
     * @throws IOException if the entry can not be read.
     * @throws IllegalStateException Programming error (pre-condition violated).
     */
    InputStream getInputStream(final String name) throws IOException {
        if (entries != null) {
            throw new IllegalStateException("Too late for this method.");
        }
        final JarEntry candidate = file.getJarEntry(name);
        if (candidate == null) {
            return null;
        }
        return file.getInputStream(candidate);
    }

    /**
     * Closes this input.
     *
     * @throws IOException if an error occurred while closing the file.
     */
    @Override
    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
        file    = null;
        entry   = null;
        entries = null;
    }
}
