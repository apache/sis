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
package org.apache.sis.util.resources;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.apache.sis.util.resources.IndexedResourceCompiler.JAVA_EXT;
import static org.apache.sis.util.resources.IndexedResourceCompiler.PROPERTIES_EXT;


/**
 * Compiles the international resources that are found in the module from which this mojo is invoked.
 * See the <code>sis-build-helper</code> module for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Olivier Nouguier (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@Mojo(name = "compile-resources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ResourceCompilerMojo extends AbstractMojo implements FilenameFilter {
    /**
     * Pattern to filter properties files that were modified.
     */
    private static final String[] PROPERTIES_PATTERN = new String[] {"**/*.properties"};

    /**
     * Project information (name, version, URL).
     */
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * A handler for the Eclipse workspace, used for declaring new resources.
     * When Maven is run from the command line, this object does nothing.
     *
     * @see <a href="http://wiki.eclipse.org/M2E_compatible_maven_plugins">M2E compatible maven plugins</a>
     */
    @Component
    private BuildContext buildContext;

    /**
     * The source directories containing the sources to be compiled.
     */
    @Parameter(property="project.compileSourceRoots", required=true, readonly=true)
    private List<String> compileSourceRoots;

    /**
     * Directory containing the generated class files.
     *
     * <p><b>Note:</b> at the time of writing, we found no well-established convention for generated resources.
     * The conventions that we found were rather for generated sources. In the later case, the conventions use
     * a different directory for each Maven plugin, e.g. <code>"generated-sources/xxx"</code>. But in our case
     * (for resources), such separation seems of limited use since the resources are copied verbatim in the JAR
     * file, so preventing clash in the <code>generated-resources</code> directory would not prevent clash in
     * the JAR file anyway.</p>
     */
    @Parameter(defaultValue="${project.build.directory}/generated-resources", required=true)
    private File outputDirectory;

    /**
     * The <code>compileSourceRoots</code> named "java" as a <code>File</code>.
     */
    private File javaDirectoryFile;

    /**
     * Constructs a new resource compiler MOJO.
     */
    public ResourceCompilerMojo() {
    }

    /**
     * Executes the mojo.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final boolean isIncremental = buildContext.isIncremental();
        declareOutputDirectory();

        int errors = 0;
        for (final String sourceDirectory : compileSourceRoots) {
            final File directory = new File(sourceDirectory);
            if (directory.getName().equals("java")) {
                /*
                 * Check if we can skip the resources compilation (Eclipse environment only).
                 *
                 * Scanner.getIncludedFiles() returns an array of modified files. For now we ignore the array
                 * content and unconditionally re-compile all resource files as soon as at least one file has
                 * been modified. This is okay for now since changes in resource files are rare and compiling
                 * them is very fast.
                 */
                if (isIncremental) {
                    Scanner scanner = buildContext.newScanner(directory);
                    scanner.setIncludes(PROPERTIES_PATTERN);
                    scanner.scan();
                    final String[] includedFiles = scanner.getIncludedFiles();
                    if (includedFiles == null || includedFiles.length == 0) {
                        continue;
                    }
                }
                javaDirectoryFile = directory;
                errors += processAllResourceDirectories(directory);
                buildContext.refresh(outputDirectory);
            }
        }
        if (errors != 0) {
            throw new ResourceCompilerException(String.valueOf(errors) + " errors in resources bundles.");
        }
    }

    /**
     * Declares {@link #outputDirectory} as resource, for inclusion by Maven in the JAR file.
     */
    private void declareOutputDirectory() {
        final Resource resource = new Resource();
        resource.setDirectory(outputDirectory.getPath());
        project.addResource(resource);
    }

    /**
     * Recursively scans the directories and find all Java classes having a property files of the same name.
     * Then invokes the resource compiler for those files.
     */
    private int processAllResourceDirectories(final File directory) throws ResourceCompilerException {
        int errors = 0;
        final File[] subdirs = directory.listFiles();
        if (subdirs != null) { // Appears to be sometime null with auto-generated sub-directories.
            for (final File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    File[] resourcesToProcess = subdir.listFiles(this);
                    int count = filterLanguages(resourcesToProcess);
                    if (count != 0) {
                        count = toJavaSourceFiles(resourcesToProcess, count);
                        if (count != 0) {
                            resourcesToProcess = Arrays.copyOf(resourcesToProcess, count);
                            errors += new Compiler(resourcesToProcess).run();
                        }
                    }
                    errors += processAllResourceDirectories(subdir);
                }
            }
        }
        return errors;
    }

    /**
     * Accepts all {@code "*.properties"} files.
     *
     * @param directory The directory.
     * @param name The file name.
     * @return {@code true} if the given file is a property file.
     */
    @Override
    public final boolean accept(final File directory, final String name) {
        return name.endsWith(PROPERTIES_EXT);
    }

    /**
     * Retains only the properties files which seems to be about internationalized resources.
     * For example if the given array contains the following files:
     * <ul>
     *   <li>{@code "Errors.properties"}</li>
     *   <li>{@code "Errors_en.properties"}</li>
     *   <li>{@code "Errors_fr.properties"}</li>
     *   <li>{@code "Messages.properties"}</li>
     *   <li>{@code "Messages_en.properties"}</li>
     *   <li>{@code "Messages_fr.properties"}</li>
     *   <li>{@code "NotAnInternationalResource.properties"}</li>
     * </ul>
     *
     * Then this method will retain the following files:
     * <ul>
     *   <li>{@code "Errors.properties"}</li>
     *   <li>{@code "Messages.properties"}</li>
     * </ul>
     *
     * @param  resourcesToProcess The files to filter. This array will be overwritten in-place.
     * @return Number of valid elements in the {@code resourcesToProcess} after this method completion.
     */
    static int filterLanguages(final File[] resourcesToProcess) {
        int count = 0;
        if (resourcesToProcess != null) {
            Arrays.sort(resourcesToProcess);
            for (int i=0; i<resourcesToProcess.length;) {
                final File file = resourcesToProcess[i];
                String name = file.getName();
                name = name.substring(0, name.length() - PROPERTIES_EXT.length()) + '_';
                final int fileIndex = i;
                while (++i < resourcesToProcess.length) {
                    if (!resourcesToProcess[i].getName().startsWith(name)) {
                        break;
                    }
                }
                // Accepts the property file only if we found at least one language.
                // Example: "Messages.properties" and "Messages_en.properties".
                if (i - fileIndex >= 2) {
                    resourcesToProcess[count++] = file;
                }
            }
        }
        return count;
    }

    /**
     * Converts the given property files into Java source file, provided that the later exists.
     * The given array is overwritten in place.
     *
     * @param  resourcesToProcess The filtered resource files, as returned by {@link #filterLanguages(File[])}.
     * @param  count Number of valid elements in {@code resourcesToProcess}.
     * @return Number of valid elements after this method completion.
     */
    private static int toJavaSourceFiles(final File[] resourcesToProcess, final int count) {
        int n = 0;
        for (int i=0; i<count; i++) {
            File file = resourcesToProcess[i];
            String name = file.getName();
            name = name.substring(0, name.length() - PROPERTIES_EXT.length());
            name += JAVA_EXT;
            file = new File(file.getParentFile(), name);
            if (file.isFile()) {
                resourcesToProcess[n++] = file;
            }
        }
        return n;
    }

    /**
     * A resource compiler that delegates the messages to the Mojo logger.
     */
    private final class Compiler extends IndexedResourceCompiler {
        public Compiler(File[] resourcesToProcess) {
            super(javaDirectoryFile, outputDirectory, resourcesToProcess);
        }

        /**
         * Logs the given message at the {@code INFO} level.
         */
        @Override
        protected void info(final String message) {
            getLog().info(message);
        }

        /**
         * Logs the given message at the {@code WARNING} level.
         */
        @Override
        protected void warning(final String message) {
            getLog().warn(message);
        }
    }
}
