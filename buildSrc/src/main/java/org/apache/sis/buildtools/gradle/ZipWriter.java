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
package org.apache.sis.buildtools.gradle;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.gradle.api.Project;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;


/**
 * Base class of tasks producing a ZIP or JAR file.
 * One of {@link JDK} or {@link Apache} inner classed should be used.
 * The method to invoke is {@link #writeDirectory(File, FileFilter, String)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ZipWriter extends Conventions {
    /**
     * Name of the entry in process of being written in the output stream.
     * Path separator is standardized to {@code '/'} (not platform-specific).
     */
    private final StringBuilder path;

    /**
     * Buffer for transferring file contents to ZIP or JAR entries.
     * Allocated only once and reused for all entries.
     */
    private final byte[] buffer;

    /**
     * Creates a helper instance.
     *
     * @param  project  the sub-project being compiled.
     * @param  out      output stream of the ZIP or JAR file to create.
     */
    private ZipWriter(final Project project) {
        super(project);
        this.path   = new StringBuilder();
        this.buffer = new byte[8192];
    }

    /**
     * Returns the ZIP output stream. This is either the JDK {@link ZipArchiveEntry}
     * or the Apache Common Compress {@link ZipArchiveOutputStream} implementation.
     */
    abstract OutputStream out();

    /**
     * Begins writing a new ZIP file entry.
     *
     * @param  source  file which will be copied in the entry.
     * @param  name    name of the entry to add.
     * @throws IOException if an error occurred.
     */
    abstract void putNextEntry(File source, String name) throws IOException;

    /**
     * Closes the current entry.
     *
     * @throws IOException if an error occurred.
     */
    abstract void closeEntry() throws IOException;

    /**
     * An output stream backed by the JDK implementation.
     */
    static class JDK extends ZipWriter {
        /**
         * Output stream of the ZIP or JAR file to create.
         */
        protected final ZipOutputStream out;

        /**
         * Creates a new ZIP output stream.
         *
         * @param  project  the sub-project being compiled.
         * @param  out      the stream where to write ZIP entries.
         */
        JDK(final Project project, final ZipOutputStream out) {
            super(project);
            this.out = out;
            out.setLevel(Deflater.BEST_COMPRESSION);
        }

        /** Returns the ZIP output stream. */
        @Override final OutputStream out() {
            return out;
        }

        /** Begins writing a new ZIP file entry. */
        @Override final void putNextEntry(final File source, final String name) throws IOException {
            final var entry = new ZipEntry(name);
            completeEntry(source, entry);
            out.putNextEntry(entry);
        }

        /** Closes the current entry. */
        @Override final void closeEntry() throws IOException {
            out.closeEntry();
        }
    }

    /**
     * An output stream backed by the Apache implementation.
     * This implementation is used when we need the capability to set Unix execution flag on some files.
     */
    static class Apache extends ZipWriter {
        /**
         * Output stream of the ZIP or JAR file to create.
         */
        protected final ZipArchiveOutputStream out;

        /**
         * Creates a new ZIP output stream.
         *
         * @param  project  the sub-project being compiled.
         * @param  out      the stream where to write ZIP entries.
         */
        Apache(final Project project, final ZipArchiveOutputStream out) {
            super(project);
            this.out = out;
            out.setLevel(Deflater.BEST_COMPRESSION);
        }

        /** Returns the ZIP output stream. */
        @Override final OutputStream out() {
            return out;
        }

        /** Begins writing a new ZIP file entry. */
        @Override final void putNextEntry(final File source, final String name) throws IOException {
            final var entry = new ZipArchiveEntry(name);
            completeEntry(source, entry);
            if (source.canExecute()) {
                entry.setUnixMode(0744);
            }
            out.putArchiveEntry(entry);
        }

        /** Closes the current entry. */
        @Override final void closeEntry() throws IOException {
            out.closeArchiveEntry();
        }
    }

    /**
     * Configures the entry for the file to be copied.
     *
     * @param  source  the file which will be written in the entry.
     * @param  entry   the entry to configure.
     */
    private static void completeEntry(final File source, final ZipEntry entry) {
        if (entry.isDirectory()) {
            entry.setMethod(ZipEntry.STORED);
            entry.setCompressedSize(0);
            entry.setSize(0);
            entry.setCrc(0);
        } else {
            entry.setMethod(ZipEntry.DEFLATED);
            entry.setSize(source.length());
        }
        entry.setTime(source.lastModified());
    }

    /**
     * List all files in the given directory excluding hidden files.
     * This method sorts the elements with regular files before directories,
     * then case-sensitive alphabetical order with upper-cases before lower cases.
     *
     * @param  directory  the directory from which to get the files.
     * @return files in the directory, or {@code null} if the argument is not a directory.
     */
    private static File[] listIgnoreHidden(final File directory) {
        final File[] files = directory.listFiles((f) -> !f.isHidden());
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                int c = f1.isDirectory() ? 1 : 0;
                if (f2.isDirectory()) c--;
                if (c == 0) {
                    c = f1.getName().compareTo(f2.getName());
                }
                return c;
            });
        }
        return files;
    }

    /**
     * Copies to the ZIP file the content of the given directory.
     *
     * @param  source  the directory to zip.
     * @param  filter  filter to apply on the files of the root directory, or {@code null} if none.
     * @param  target  where to write the files in the ZIP file. Empty for writing in the root.
     * @throws IOException if an error occurred while reading the source or writing the ZIP file.
     */
    protected final void writeDirectory(final File source, final FileFilter filter, final String target) throws IOException {
        final File[] files = listIgnoreHidden(source);
        if (files == null) {
            throw new FileNotFoundException("Directory does not exist: " + source);
        }
        path.setLength(0);
        path.append(target);
        for (final File file : files) {
            if (filter == null || filter.accept(file)) {
                appendRecursively(file);
            }
        }
    }

    /**
     * Writes a single file or directory.
     *
     * @param  source  the file or directory to add.
     * @param  target  name of the entry in the ZIP file.
     * @throws IOException if an error occurred while reading the source or writing the ZIP file.
     */
    protected final void writeFile(final File source, final String target) throws IOException {
        path.setLength(0);
        path.append(target);
        appendRecursively(source);
    }

    /**
     * Appends the content of the given file. If the file is a directory,
     * then the content of that directory will be appended recursively.
     *
     * @param  source  the file to append.
     * @throws IOException if an error occurred while reading the source or writing the ZIP file.
     */
    private void appendRecursively(final File source) throws IOException {
        final File[] files = listIgnoreHidden(source);
        final int parentNameLength = path.length();
        path.append(source.getName());
        if (files != null) path.append('/');                    // Fixed by ZIP standard, not platform-dependent.
        putNextEntry(source, path.toString());
        if (files == null) {
            writeEntryContent(source, out());
        }
        closeEntry();
        if (files != null) {
            for (final File sub : files) {
                appendRecursively(sub);
            }
        }
        path.setLength(parentNameLength);
    }

    /**
     * Writes the content of the specified file to the output stream of a ZIP entry.
     * Subclasses can override for filtering the content.
     *
     * @param  source  the file to open for providing an entry content.
     * @param  out     where to write the (potentially filtered) file content.
     * @throws IOException if an error occurred while reading the source or writing the ZIP file.
     */
    protected void writeEntryContent(final File source, final OutputStream out) throws IOException {
        try (final InputStream in = new FileInputStream(source)) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        }
    }
}
