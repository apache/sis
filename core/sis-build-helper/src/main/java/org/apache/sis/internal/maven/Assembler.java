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

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import static org.apache.sis.internal.maven.Filenames.*;


/**
 * Creates a ZIP file containing the Apache SIS binary distribution.
 * The created file contains:
 *
 * <ul>
 *   <li>the content of the <code>application/sis-console/src/main/artifact</code> directory;</li>
 *   <li>the JAR files of all modules and their dependencies, without their native resources;</li>
 *   <li>the native resources in a separated {@code lib/} directory.</li>
 * </ul>
 *
 * This MOJO can be invoked from the command line in the {@code sis-console} module as below:
 *
 * <blockquote><code>mvn package org.apache.sis.core:sis-build-helper:dist</code></blockquote>
 *
 * <h2>Limitation</h2>
 * The current implementation uses some hard-coded paths and filenames.
 * See the <cite>Distribution file</cite> section in
 * <a href="http://sis.apache.org/build.html">Build from source</a> page
 * for more information.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
@Mojo(name = "dist", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class Assembler extends AbstractMojo implements FilenameFilter {
    /**
     * Project information (name, version, URL).
     */
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * Base directory of the module to compile.
     * Artifact content is expected in the {@code "src/main/artifact"} subdirectory.
     */
    @Parameter(property="basedir", required=true, readonly=true)
    private String baseDirectory;

    /**
     * The root directory (without the "<code>target/binaries</code>" sub-directory) where JARs
     * are to be copied. It should be the directory of the root <code>pom.xml</code>.
     */
    @Parameter(property="session.executionRootDirectory", required=true)
    private String rootDirectory;

    /**
     * Invoked by reflection for creating the MOJO.
     */
    public Assembler() {
    }

    /**
     * Creates the distribution file.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final File sourceDirectory = new File(baseDirectory, ARTIFACT_PATH);
        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Directory not found: " + sourceDirectory);
        }
        final String artifactBase = FINALNAME_PREFIX + project.getVersion();
        final File targetFile = distributionFile(rootDirectory, artifactBase + ".zip");
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(targetFile)) {
            zip.setLevel(9);
            appendRecursively(sourceDirectory, artifactBase, zip);
            /*
             * At this point, all the "application/sis-console/src/main/artifact" and sub-directories
             * have been zipped. Now append the JAR files for each module and their dependencies.
             */
            final Map<String,byte[]> nativeFiles = new LinkedHashMap<>();
            for (final File file : files(project)) {
                ZipArchiveEntry entry = new ZipArchiveEntry(artifactBase + '/' + LIB_DIRECTORY + '/' + file.getName());
                zip.putArchiveEntry(entry);
                appendJAR(file, zip, nativeFiles);
                zip.closeArchiveEntry();
            }
            /*
             * At this point we finished creating all entries in the ZIP file, except native resources.
             * Copy them now.
             */
            for (final Map.Entry<String,byte[]> nf : nativeFiles.entrySet()) {
                ZipArchiveEntry entry = new ZipArchiveEntry(artifactBase + '/' + LIB_DIRECTORY + '/' + nf.getKey());
                entry.setUnixMode(0555);        // Readable and executable for all, but not writable.
                zip.putArchiveEntry(entry);
                zip.write(nf.getValue());
                zip.closeArchiveEntry();
                nf.setValue(null);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns all files to include for the given Maven project.
     */
    private static Set<File> files(final MavenProject project) throws MojoExecutionException {
        final Set<File> files = new LinkedHashSet<>();
        files.add(project.getArtifact().getFile());
        for (final Artifact dep : project.getArtifacts()) {
            final String scope = dep.getScope();
            if (Artifact.SCOPE_COMPILE.equalsIgnoreCase(scope) ||
                Artifact.SCOPE_RUNTIME.equalsIgnoreCase(scope))
            {
                files.add(dep.getFile());
            }
        }
        if (files.remove(null)) {
            throw new MojoExecutionException("Invocation of this MOJO shall be done together with a \"package\" Maven phase.");
        }
        return files;
    }

    /**
     * Adds the given file in the ZIP file. If the given file is a directory, then this method
     * recursively adds all files contained in this directory. This method is invoked for zipping
     * the "application/sis-console/src/main/artifact" directory and sub-directories before to zip.
     */
    private void appendRecursively(final File file, String relativeFile, final ZipArchiveOutputStream out) throws IOException {
        if (file.isDirectory()) {
            relativeFile += '/';
        }
        final ZipArchiveEntry entry = new ZipArchiveEntry(file, relativeFile);
        if (file.canExecute()) {
            entry.setUnixMode(0744);
        }
        out.putArchiveEntry(entry);
        if (!entry.isDirectory()) {
            try (FileInputStream in = new FileInputStream(file)) {
                in.transferTo(out);
            }
        }
        out.closeArchiveEntry();
        if (entry.isDirectory()) {
            for (final String filename : file.list(this)) {
                appendRecursively(new File(file, filename), relativeFile.concat(filename), out);
            }
        }
    }

    /**
     * The filter to use for selecting the files to be included in the ZIP file.
     *
     * @param  directory  the directory.
     * @param  filename   the filename.
     * @return {@code true} if the given file should be included in the ZIP file.
     */
    @Override
    public boolean accept(final File directory, final String filename) {
        return !filename.isEmpty() && filename.charAt(0) != '.' && !filename.equals(CONTENT_FILE);
    }

    /**
     * Returns {@code true} if the given JAR file contains at least one resource in the
     * {@value Filenames#NATIVE_DIRECTORY} directory. Those files will need to be rewritten
     * in order to exclude those resources.
     */
    private static boolean hasNativeResources(final ZipFile in) {
        final Enumeration<? extends ZipEntry> entries = in.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(NATIVE_DIRECTORY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies a JAR file in the given ZIP file, except the native resources which are stored in the given map.
     *
     * @param  file         the JAR file to copy.
     * @param  bundle       destination where to copy the JAR file.
     * @param  nativeFiles  where to store the native resources.
     */
    private void appendJAR(final File file, final ZipArchiveOutputStream bundle, final Map<String,byte[]> nativeFiles)
            throws IOException
    {
        try (ZipFile in = new ZipFile(file)) {
            if (hasNativeResources(in)) {
                final ZipOutputStream out = new ZipOutputStream(bundle);
                out.setLevel(9);
                final Enumeration<? extends ZipEntry> entries = in.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    final String    name = entry.getName();
                    try (InputStream eis = in.getInputStream(entry)) {
                        if (!name.startsWith(NATIVE_DIRECTORY)) {
                            out.putNextEntry(new ZipEntry(name));
                            eis.transferTo(out);                            // Copy the entry verbatim.
                            out.closeEntry();
                        } else if (!entry.isDirectory()) {
                            final long size = entry.getSize();              // For memorizing the entry without copying it now.
                            if (size <= 0 || size > Integer.MAX_VALUE) {
                                throw new IOException(String.format("Errors while copying %s:%n"
                                        + "Unsupported size for \"%s\" entry: %d", file, name, size));
                            }
                            final byte[] content = new byte[(int) size];
                            final int actual = eis.read(content);
                            if (actual != size) {
                                throw new IOException(String.format("Errors while copying %s:%n"
                                        + "Expected %d bytes in \"%s\" but found %d", file, size, name, actual));
                            }
                            if (nativeFiles.put(name.substring(NATIVE_DIRECTORY.length()), content) != null) {
                                throw new IOException(String.format("Errors while copying %s:%n"
                                        + "Duplicated entry: %s", file, name));
                            }
                        }
                    }
                }
                out.finish();
                return;
            }
        }
        /*
         * If the JAR file has no native resources to exclude, we can copy the JAR file verbatim.
         * This is faster than inflating and deflating again the JAR file.
         */
        try (FileInputStream in = new FileInputStream(file)) {
            in.transferTo(bundle);
        }
    }
}
