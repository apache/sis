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
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.LinkedHashSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;

import static org.apache.sis.internal.maven.Filenames.*;


/**
 * Collects <code>.jar</code> files in a single "{@code target/binaries}" directory.
 * Dependencies are collected as well, except if already presents. This mojo uses hard links
 * on platforms that support them. If hard links are not supported, then this mojo will instead
 * creates a "{@code target/binaries/content.txt}" file listing the dependencies.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@Mojo(name = "collect-jars",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresDependencyResolution = ResolutionScope.RUNTIME)
public final class JarCollector extends AbstractMojo implements FileFilter {
    /**
     * The Maven project running this plugin.
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
     * Copies the {@code *.jar} files to the collect directory.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (rootDirectory == null || rootDirectory.startsWith("${")) {
            getLog().warn("Unresolved directory: " + rootDirectory);
            return;
        }
        /*
         * Now collects the JARs.
         */
        try {
            collect();
        } catch (IOException e) {
            throw new MojoExecutionException("Error collecting the JAR files.", e);
        }
    }

    /**
     * Implementation of the {@link #execute()} method.
     */
    private void collect() throws MojoExecutionException, IOException {
        /*
         * Make sure that we are collecting the JAR file from a module which produced
         * such file. Some modules use pom packaging, which do not produce any JAR file.
         */
        final File jarFile = getProjectFile();
        if (jarFile == null) {
            return;
        }
        /*
         * Get the "target" directory of the parent pom.xml and make sure it exists.
         */
        File collect = new File(rootDirectory, TARGET_DIRECTORY);
        if (!collect.exists()) {
            if (!collect.mkdir()) {
                throw new MojoExecutionException("Failed to create \"" + TARGET_DIRECTORY + "\" directory.");
            }
        }
        if (collect.getCanonicalFile().equals(jarFile.getParentFile().getCanonicalFile())) {
            /*
             * The parent's directory is the same one than this module's directory.
             * In other words, this plugin is not executed from the parent POM. Do
             * not copy anything, since this is not the place where we want to
             * collect the JAR files.
             */
            return;
        }
        /*
         * Creates a "binaries" subdirectory inside the "target" directory, then copy the
         * JAR file compiled by Maven. If an JAR file already existed, it will be deleted.
         */
        collect = new File(collect, BINARIES_DIRECTORY);
        if (!collect.exists()) {
            if (!collect.mkdir()) {
                throw new MojoExecutionException("Failed to create \"" + BINARIES_DIRECTORY + "\" directory.");
            }
        }
        File copy = new File(collect, jarFile.getName());
        copy.delete();
        linkFileToDirectory(jarFile, copy);
        /*
         * Copies the dependencies.
         */
        final Set<Artifact> dependencies = project.getArtifacts();
        if (dependencies != null) {
            for (final Artifact artifact : dependencies) {
                final String scope = artifact.getScope();
                if (scope != null &&  // Maven 2.0.6 bug?
                   (scope.equalsIgnoreCase(Artifact.SCOPE_COMPILE) ||
                    scope.equalsIgnoreCase(Artifact.SCOPE_RUNTIME)))
                {
                    final File file = artifact.getFile();
                    if (file != null) { // I'm not sure why the file is sometime null...
                        copy = new File(collect, getFinalName(file, artifact));
                        if (!copy.exists()) {
                            /*
                             * Copies the dependency only if it was not already copied. Note that
                             * the module's JAR was copied unconditionally above (because it may
                             * be the result of a new compilation).
                             */
                            linkFileToDirectory(file, copy);
                        }
                    }
                }
            }
        }
    }

    /**
     * Filters the content of the "target" directory in order to keep only the project
     * build result. We scan the directory because the final name may be different than
     * the actual file name, because a classifier may have been added to the name.
     *
     * <p>The {@code .jar} extension is not quite appropriate for source and Javadoc files;
     * a better extension would be {@code .zip}. Unfortunately the {@code .jar} extension
     * for those content is a very common practice, so we have to filter them.</p>
     */
    @Override
    public boolean accept(final File pathname) {
        final String name = pathname.getName();
        return name.endsWith(".jar")         &&
              !name.endsWith("-tests.jar")   &&
              !name.endsWith("-sources.jar") &&
              !name.endsWith("-javadoc.jar") &&
               name.startsWith(project.getBuild().getFinalName()) &&
               pathname.isFile();
    }

    /**
     * Returns the JAR file, or {@code null} if none.
     * In case of doubt, conservatively returns {@code null}.
     */
    private File getProjectFile() {
        final File[] files = new File(project.getBuild().getDirectory()).listFiles(this);
        if (files != null && files.length == 1) {
            return files[0];
        }
        return null;
    }

    /**
     * Returns the name of the given file. If the given file is a snapshot, then the
     * {@code "SNAPSHOT"} will be replaced by the timestamp if possible.
     *
     * @param  file     The file from which to get the filename.
     * @param  artifact The artifact that produced the given file.
     * @return The filename to use.
     */
    private static String getFinalName(final File file, final Artifact artifact) {
        String filename = file.getName();
        final String baseVersion = artifact.getBaseVersion();
        if (baseVersion != null) {
            final int pos = filename.lastIndexOf(baseVersion);
            if (pos >= 0) {
                final String version = artifact.getVersion();
                if (version != null && !baseVersion.equals(version)) {
                    filename = filename.substring(0, pos) + version + filename.substring(pos + baseVersion.length());
                }
            }
        }
        return filename;
    }

    /**
     * Creates a link from the given source file to the given target file.
     * On JDK6 or on platform that do not support links, this method rather
     * updates the <code>content.txt</code> file.
     *
     * @param file The source file to read.
     * @param copy The destination file to create.
     */
    private static void linkFileToDirectory(final File file, final File copy) throws IOException {
        /*
         * If we can not use hard links, creates or updates a "target/content.txt" file instead.
         * This file will contains the list of all dependencies, without duplicated values.
         */
        final File dependenciesFile = new File(copy.getParentFile(), CONTENT_FILE);
        final Set<String> dependencies = loadDependencyList(dependenciesFile);
        if (dependencies.add(file.getPath())) {
            // Save the dependencies list only if it has been modified.
            final BufferedWriter out = new BufferedWriter(new FileWriter(dependenciesFile));
            try {
                for (final String dependency : dependencies) {
                    out.write(dependency);
                    out.newLine();
                }
            } finally {
                out.close();
            }
        }
    }

    /**
     * Loads the {@value #CONTENT_FILE} from the given directory, if it exists.
     * Otherwise returns an empty but modifiable set. This method is invoked only on
     * platforms that do not support hard links.
     */
    static Set<String> loadDependencyList(final File dependenciesFile) throws IOException {
        final Set<String> dependencies = new LinkedHashSet<String>();
        if (dependenciesFile.exists()) {
            final BufferedReader in = new BufferedReader(new FileReader(dependenciesFile));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        dependencies.add(line);
                    }
                }
            } finally {
                in.close();
            }
        }
        return dependencies;
    }
}
