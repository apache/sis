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
import java.io.FilterOutputStream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import static org.apache.sis.internal.maven.Filenames.*;


/**
 * Creates a ZIP files containing the content of the <code>application/sis-console/src/main/artifact</code>
 * directory together with the Pack200 file created by <code>BundleCreator</code>.
 * This mojo can be invoked from the command line as below:
 *
 * <blockquote><code>mvn org.apache.sis.core:sis-build-helper:dist --non-recursive</code></blockquote>
 *
 * Do not forget the <code>--non-recursive</code> option, otherwise the Mojo will be executed many time.
 *
 * <p><b>Current limitation:</b>
 * The current implementation uses some hard-coded paths and filenames.
 * See the <cite>Distribution file and Pack200 bundle</cite> section in the <code>src/site/apt/index.apt</code>
 * file for more information.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@Mojo(name = "dist", defaultPhase = LifecyclePhase.INSTALL)
public class Assembler extends AbstractMojo implements FilenameFilter {
    /**
     * Project information (name, version, URL).
     */
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * The root directory (without the "<code>target/binaries</code>" sub-directory) where JARs
     * are to be copied. It should be the directory of the root <code>pom.xml</code>.
     */
    @Parameter(property="session.executionRootDirectory", required=true)
    private String rootDirectory;

    /**
     * Creates the distribution file.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final File sourceDirectory = new File(rootDirectory, ARTIFACT_PATH);
        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Directory not found: " + sourceDirectory);
        }
        final File targetDirectory = new File(rootDirectory, TARGET_DIRECTORY);
        final String version = project.getVersion();
        final String artifactBase = FINALNAME_PREFIX + version;
        try {
            final File targetFile = new File(distributionDirectory(targetDirectory), artifactBase + ".zip");
            final ZipArchiveOutputStream zip = new ZipArchiveOutputStream(targetFile);
            try {
                zip.setLevel(9);
                appendRecursively(sourceDirectory, artifactBase, zip, new byte[8192]);
                /*
                 * At this point, all the "application/sis-console/src/main/artifact" and sub-directories
                 * have been zipped.  Now generate the Pack200 file and zip it directly (without creating
                 * a temporary "sis.pack.gz" file).
                 */
                final Packer packer = new Packer(project.getName(), project.getUrl(), version, targetDirectory);
                final ZipArchiveEntry entry = new ZipArchiveEntry(
                        artifactBase + '/' + LIB_DIRECTORY + '/' + FATJAR_FILE + PACK_EXTENSION);
                entry.setMethod(ZipArchiveEntry.STORED);
                zip.putArchiveEntry(entry);
                packer.preparePack200(FATJAR_FILE + ".jar").pack(new FilterOutputStream(zip) {
                    /*
                     * Closes the archive entry, not the ZIP file.
                     */
                    @Override
                    public void close() throws IOException {
                        zip.closeArchiveEntry();
                    }
                });
            } finally {
                zip.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Adds the given file in the ZIP file. If the given file is a directory, then this method
     * recursively adds all files contained in this directory. This method is invoked for zipping
     * the "application/sis-console/src/main/artifact" directory and sub-directories before to zip
     * the Pack200 file.
     */
    private void appendRecursively(final File file, String relativeFile, final ZipArchiveOutputStream out,
            final byte[] buffer) throws IOException
    {
        if (file.isDirectory()) {
            relativeFile += '/';
        }
        final ZipArchiveEntry entry = new ZipArchiveEntry(file, relativeFile);
        if (file.canExecute()) {
            entry.setUnixMode(0744);
        }
        out.putArchiveEntry(entry);
        if (!entry.isDirectory()) {
            final FileInputStream in = new FileInputStream(file);
            try {
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                }
            } finally {
                in.close();
            }
        }
        out.closeArchiveEntry();
        if (entry.isDirectory()) {
            for (final String filename : file.list(this)) {
                appendRecursively(new File(file, filename), relativeFile.concat(filename), out, buffer);
            }
        }
    }

    /**
     * The filter to use for selecting the files to be included in the ZIP file.
     *
     * @param  directory The directory.
     * @param  filename  The filename.
     * @return {@code true} if the given file should be included in the ZIP file.
     */
    @Override
    public boolean accept(final File directory, final String filename) {
        return !filename.isEmpty() && filename.charAt(0) != '.';
    }
}
