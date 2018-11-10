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

import java.io.*;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * Creates an {@code .oxt} package for <a href="http://www.openoffice.org">OpenOffice.org</a> addins.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
@Mojo(name = "unopkg", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class UnoPkg extends AbstractMojo implements FilenameFilter {
    /**
     * The subdirectory (relative to {@link #baseDirectory}) where the UNO files are expected.
     */
    static final String SOURCE_DIRECTORY = "src/main/unopkg";

    /**
     * Module to decompress. We inflate the {@value} module because Derby is much slower
     * when using an embedded database in a compressed ZIP file compared to flat storage.
     * Since the JAR files are distributed in a ZIP file anyway, inflating that file has
     * little impact on the final ZIP file size.
     */
    private static final String TO_INFLATE = "sis-embedded-data";

    /**
     * The encoding for text files to read and write.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * The string to replace by the final name.
     */
    private static final String FILTERED_NAME = "${project.build.finalName}";

    /**
     * The string to replace by the version number.
     */
    private static final String FILTERED_VERSION = "${project.version}";

    /**
     * Base directory of the module to compile.
     * The UNO files are expected in the {@code "src/main/unopkg"} subdirectory.
     * The plugin will look for the {@code META-INF/manifest.xml} and {@code *.rdb} files in that directory.
     */
    @Parameter(property="basedir", required=true, readonly=true)
    private String baseDirectory;

    /**
     * Directory where the output {@code .oxt} file will be located.
     */
    @Parameter(property="project.build.directory", required=true, readonly=true)
    private String outputDirectory;

    /**
     * In {@code META-INF/manifest.xml}, all occurrences of {@code ${project.build.finalName}}
     * will be replaced by this value.
     */
    @Parameter(property="project.build.finalName", required=true, readonly=true)
    private String finalName;

    /**
     * In {@code description.xml}, all occurrences of {@code ${project.version}}
     * will be replaced by this value.
     */
    @Parameter(property="project.version", required=true, readonly=true)
    private String version;

    /**
     * The name for the {@code .oxt} file to create, without the {@code ".oxt"} filename extension.
     */
    @Parameter(property="project.build.finalName", required=true, readonly=true)
    private String oxtName;

    /**
     * The Maven project running this plugin.
     */
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * Invoked by reflection for creating the MOJO.
     */
    public UnoPkg() {
    }

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param  directory  the directory in which the file was found.
     * @param  name       the name of the file.
     */
    @Override
    public boolean accept(final File directory, final String name) {
        if (name.endsWith("-sources.jar") || name.endsWith("-tests.jar") || name.endsWith("-javadoc.jar")) {
            return false;
        }
        return name.endsWith(".jar") || name.endsWith(".JAR") ||
               name.endsWith(".rdb") || name.endsWith(".RDB") ||
               name.endsWith(".xml") || name.endsWith(".XML") ||
               name.endsWith(".xcu") || name.endsWith(".XCU") ||
               name.endsWith(".txt") || name.endsWith(".TXT") ||
               name.endsWith(".png") || name.endsWith(".PNG");
    }

    /**
     * Generates the {@code .oxt} file from all {@code .jar} files found in the target directory.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final String  manifestName = "META-INF/manifest.xml";
        final File sourceDirectory = new File(baseDirectory, SOURCE_DIRECTORY);
        final File outputDirectory = new File(this.outputDirectory);
        final File         zipFile = new File(outputDirectory, oxtName + ".oxt");
        final File    manifestFile = new File(sourceDirectory, manifestName);
        final File[]          jars = outputDirectory.listFiles(this);
        final File[]          rdbs = sourceDirectory.listFiles(this);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            out.setLevel(9);
            if (manifestFile.isFile()) {
                copyFiltered(manifestFile, out, manifestName);
            }
            /*
             * Copies files from "unopkg" source directory. This include binary files like RDB,
             * but also XML file. We apply filtering on the "description.xml" file in order to
             * set the version number automatically.
             */
            for (final File rdb : rdbs) {
                final String name = rdb.getName();
                if (name.endsWith(".xml")) {
                    copyFiltered(rdb, out, name);
                } else {
                    copy(rdb, out);
                }
            }
            /*
             * Copies the JAR for this module and any additional JARs provided in the output directory.
             */
            for (final File jar : jars) {
                copy(jar, out);
            }
            /*
             * Copies the dependencies.
             */
            for (final Artifact artifact : project.getArtifacts()) {
                final String scope = artifact.getScope();
                if (Artifact.SCOPE_COMPILE.equalsIgnoreCase(scope) ||
                    Artifact.SCOPE_RUNTIME.equalsIgnoreCase(scope))
                {
                    final File file = artifact.getFile();
                    final String name = file.getName();
                    if (name.startsWith(TO_INFLATE)) {
                        copyInflated(file, out);
                    } else {
                        copy(file, out);
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating the oxt file.", e);
        }
    }

    /**
     * Copies the content of the specified binary file to the specified output stream.
     *
     * @param  file    the regular file to copy inside the ZIP file.
     * @param  bundle  the ZIP file where to copy the given regular file.
     */
    private static void copy(final File file, final ZipOutputStream bundle) throws IOException {
        final String name = file.getName();
        final ZipEntry entry = new ZipEntry(name);
        if (name.endsWith(".png")) {
            final long size = file.length();
            entry.setMethod(ZipOutputStream.STORED);
            entry.setSize(size);
            entry.setCompressedSize(size);
            entry.setCrc(getCRC32(file));
        }
        bundle.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            in.transferTo(bundle);
        }
        bundle.closeEntry();
    }

    /**
     * Copies the content of the specified ASCII file to the specified output stream.
     *
     * @param  file    the regular file to copy inside the ZIP file.
     * @param  bundle  the ZIP file where to copy the given regular file.
     * @param  name    the ZIP entry name.
     */
    private void copyFiltered(final File file, final ZipOutputStream bundle, final String name) throws IOException {
        final ZipEntry entry = new ZipEntry(name);
        bundle.putNextEntry(entry);
        final Writer writer = new OutputStreamWriter(bundle, ENCODING);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), ENCODING))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.replace(FILTERED_NAME, finalName);
                line = line.replace(FILTERED_VERSION, version);
                writer.write(line);
                writer.write('\n');
            }
        }
        writer.flush();
        bundle.closeEntry();
    }

    /**
     * Copies a JAR file in the given ZIP file, but without compression for the files
     * in {@code SIS_DATA} directory.
     *
     * @param  file    the JAR file to copy.
     * @param  bundle  destination where to copy the JAR file.
     */
    private static void copyInflated(final File file, final ZipOutputStream bundle) throws IOException {
        final ZipEntry entry = new ZipEntry(file.getName());
        bundle.putNextEntry(entry);
        final ZipOutputStream out = new ZipOutputStream(bundle);
        out.setLevel(9);
        try (ZipFile in = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = in.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry   inEntry  = entries.nextElement();
                final String     name     = inEntry.getName();
                final ZipEntry   outEntry = new ZipEntry(name);
                if (name.startsWith("SIS_DATA")) {
                    final long size = inEntry.getSize();
                    outEntry.setMethod(ZipOutputStream.STORED);
                    outEntry.setSize(size);
                    outEntry.setCompressedSize(size);
                    outEntry.setCrc(inEntry.getCrc());
                }
                try (InputStream inStream = in.getInputStream(inEntry)) {
                    out.putNextEntry(outEntry);
                    inStream.transferTo(out);
                    out.closeEntry();
                }
            }
        }
        out.finish();
        bundle.closeEntry();
    }

    /**
     * Computes CRC32 for the given file.
     */
    private static long getCRC32(final File file) throws IOException {
        final CRC32 crc = new CRC32();
        try (InputStream in = new FileInputStream(file)) {
            final byte[] buffer = new byte[4*1024];
            int length;
            while ((length = in.read(buffer)) >= 0) {
                crc.update(buffer, 0, length);
            }
        }
        return crc.getValue();
    }
}
