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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.gradle.api.Task;
import org.gradle.api.Project;


/**
 * Creates an {@code .oxt} package for <a href="http://www.openoffice.org">OpenOffice.org</a> addins.
 * Those files are basically ZIP files with {@code META-INF/manifest.xml} and {@code .rdb} entries.
 * The principle is similar to Java JAR files.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Quentin Bialota (Geomatys)
 */
final class UnoPkg extends ZipWriter.JDK {
    /**
     * The module on which to apply this task.
     */
    static final String MODULE = "org.apache.sis.openoffice";

    /**
     * Module to decompress. We inflate the {@value} module because Derby is much slower
     * when using an embedded database in a compressed ZIP file compared to flat storage.
     * Since the JAR files are distributed in a ZIP file anyway, inflating that file has
     * little impact on the final ZIP file size.
     */
    private static final String TO_INFLATE = "org.apache.sis.referencing.database";

    /**
     * Hard-coded set of libraries to include.
     *
     * @see #accept(File, String)
     */
    private static final Set<String> LIBRARIES = Set.of(
            "java.measure",
            "org.opengis.geoapi",
            "org.opengis.geoapi.pending",
            "org.apache.sis.util",
            "org.apache.sis.metadata",
            "org.apache.sis.referencing",
            "org.apache.sis.feature",
            "org.apache.sis.storage",
            "org.apache.sis.openoffice",
            "jakarta.activation",
            "jakarta.xml.bind",
            TO_INFLATE);

    /**
     * The string to replace by the final name.
     */
    private static final String FILTERED_NAME = "${project.build.finalName}";

    /**
     * The string to replace by the version number.
     */
    private static final String FILTERED_VERSION = "${project.version}";

    /**
     * Creates a helper object for creating the assembly.
     *
     * @param  project  the project for which to create an assembly.
     * @param  out      output stream of the ZIP file to create.
     */
    private UnoPkg(final Project project, final ZipOutputStream out) {
        super(project, out);
    }

    /**
     * Generates the {@code .oxt} file from a selected amount if {@code .jar} files.
     *
     * @param  task  the assembler task (in Gradle 8.2.1, it appears to be an opaque decorator that we cannot cast).
     */
    static void create(final Task task) {
        final Project project = task.getProject();
        final File sourceDirectory = getBundleSourceDirectory(project, MODULE);
        final File libsDirectory = fileRelativeToBuild(project, LIBS_DIRECTORY);
        if (sourceDirectory.isDirectory() && libsDirectory.isDirectory()) try {
            File target = fileRelativeToBuild(project, BUNDLE_DIRECTORY);
            mkdir(target);
            target = new File(target, FINALNAME_PREFIX + project.getVersion() + ".oxt");
            try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
                final var c = new UnoPkg(project, out);
                c.writeDirectory(sourceDirectory, UnoPkg::filterSource, "");
                c.writeDirectory(libsDirectory, UnoPkg::filterDependency, "");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Tests if a specified file from the source directory should be included in the bundle.
     *
     * @param  file  the file to test.
     * @return whether the file should be included.
     */
    private static boolean filterSource(final File file) {
        return !file.getName().equals("org");
    }

    /**
     * Tests if a specified file from the libraries directory should be included in the bundle.
     *
     * @param  file  the file to test.
     * @return whether the file should be included.
     */
    private static boolean filterDependency(final File file) {
        final String name = file.getName();
        return name.endsWith(".jar") && LIBRARIES.contains(name.substring(0, name.length() - 4));
    }

    /**
     * Writes the content of the specified file to the output stream of a ZIP entry.
     */
    @Override
    protected void writeEntryContent(final File source, final OutputStream out) throws IOException {
        final String filename = source.getName();
        if (filename.endsWith(".xml")) {
            copyFiltered(source, out);
        } else if (filename.equals(TO_INFLATE + ".jar")) {
            copyInflated(source, out);
        } else {
            super.writeEntryContent(source, out);
        }
    }

    /**
     * Copies the content of the specified ASCII file to the output stream.
     * Occurrences of {@value #FILTERED_NAME} and {@value #FILTERED_VERSION}
     * are replaced by actual values.
     *
     * @param  file    the regular file to copy inside the ZIP file.
     * @param  name    the ZIP entry name.
     */
    private void copyFiltered(final File source, final OutputStream out) throws IOException {
        final String version   = project.getVersion().toString();
        final String finalName = FINALNAME_PREFIX + version;
        final var    writer    = new OutputStreamWriter(out, ENCODING);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(source), ENCODING))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.replace(FILTERED_NAME, finalName);
                line = line.replace(FILTERED_VERSION, version);
                writer.write(line);
                writer.write('\n');
            }
        }
        writer.flush();
    }

    /**
     * Copies a JAR file in the ZIP file, but without compression.
     * This is recommended for files in the {@code SIS_DATA} directory.
     *
     * @param  file  the JAR file to copy.
     */
    private void copyInflated(final File file, final OutputStream out) throws IOException {
        final ZipOutputStream jar = new ZipOutputStream(out);
        try (ZipFile in = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = in.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry   entry    = entries.nextElement();
                final String     name     = entry.getName();
                final ZipEntry   outEntry = new ZipEntry(name);
                if (name.startsWith("SIS_DATA")) {
                    final long size = entry.getSize();
                    outEntry.setMethod(ZipOutputStream.STORED);
                    outEntry.setSize(size);
                    outEntry.setCompressedSize(size);
                    outEntry.setCrc(entry.getCrc());
                }
                try (InputStream inStream = in.getInputStream(entry)) {
                    jar.putNextEntry(outEntry);
                    inStream.transferTo(jar);
                    jar.closeEntry();
                }
            }
        }
        jar.finish();
    }
}
