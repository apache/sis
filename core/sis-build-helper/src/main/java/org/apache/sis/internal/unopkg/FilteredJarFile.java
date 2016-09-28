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
package org.apache.sis.internal.unopkg;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;


/**
 * A JAR file which can exclude some file entries and some attributes from the {@code MANIFEST.MF} file.
 * The main purpose of this class is to exclude the signature from JAR files before to compress them
 * using the {@code pack200} tools, because {@code pack200} modifies the binary stream, thus making
 * the signature invalid. If we don't remove the signature, attempts to use the JAR file may result
 * in {@literal "SHA1 digest error for <JAR file>"} error messages.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class FilteredJarFile extends JarFile {
    /**
     * The manifest encoding in JAR files.
     */
    private static final String MANIFEST_ENCODING = "UTF-8";

    /**
     * Open the file specified by the given name.
     */
    FilteredJarFile(final File filename) throws IOException {
        super(filename);
    }

    /**
     * Returns the list of entries, excluding Maven files (which are of no interest for the add-in)
     * and the signature.
     */
    @Override
    public Enumeration<JarEntry> entries() {
        final List<JarEntry> entries = Collections.list(super.entries());
        for (final Iterator<JarEntry> it=entries.iterator(); it.hasNext();) {
            final String name = it.next().getName();
            if (name.startsWith("META-INF/")) {
                if (name.startsWith("META-INF/maven/") || name.endsWith(".SF") || name.endsWith(".RSA")) {
                    it.remove();
                }
            }
        }
        return Collections.enumeration(entries);
    }

    /**
     * Returns the input stream for the given entry. If the given entry is the manifest,
     * then this method will filter the manifest content in order to exclude the signature.
     */
    @Override
    public InputStream getInputStream(final ZipEntry ze) throws IOException {
        final InputStream in = super.getInputStream(ze);
        if (!ze.getName().equals(JarFile.MANIFEST_NAME)) {
            return in;
        }
        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, MANIFEST_ENCODING))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("SHA1-Digest:")) {
                    final int n = lines.size();
                    if (n == 0 || !lines.get(n-1).trim().startsWith("Name:")) {
                        throw new IOException("Can not process the following line from " +
                                JarFile.MANIFEST_NAME + ":\n" + line);
                    }
                    lines.remove(n-1);
                    continue;
                }
                lines.add(line);
            }
        }
        /*
         * 'in' has been closed at this point (indirectly, by closing the reader).
         * Now remove trailing empty lines, and returns the new MANIFEST.MF content.
         */
        for (int i=lines.size(); --i>=0;) {
            if (!lines.get(i).trim().isEmpty()) {
                break;
            }
            lines.remove(i);
        }
        final StringBuilder buffer = new StringBuilder(lines.size() * 60);
        for (final String line : lines) {
            buffer.append(line).append('\n');
        }
        return new ByteArrayInputStream(buffer.toString().getBytes(MANIFEST_ENCODING));
    }
}
