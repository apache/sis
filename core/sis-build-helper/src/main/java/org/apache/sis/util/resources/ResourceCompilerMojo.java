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
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;


/**
 * Compiles the international resources that are found in the module from which this mojo is invoked.
 * See the <code><a href="{@website}/sis-build-helper/index.html">sis-build-helper</a></code> module
 * for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Olivier Nouguier (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.4
 * @module
 *
 * @goal compile-resources
 * @phase generate-resources
 */
public class ResourceCompilerMojo extends AbstractMojo implements FilenameFilter {
    /**
     * Project information (name, version, URL).
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * A handler for the Eclipse workspace, used for declaring new resources.
     * When Maven is run from the command line, this object does nothing.
     *
     * @see <a href="http://wiki.eclipse.org/M2E_compatible_maven_plugins">M2E compatible maven plugins</a>
     *
     * @component
     */
    private BuildContext buildContext;

    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter property="project.compileSourceRoots"
     * @required
     * @readonly
     */
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
     *
     * @parameter default-value="${project.build.directory}/generated-resources"
     * @required
     */
    private File outputDirectory;

    /**
     * The <code>compileSourceRoots</code> named "java" as a <code>File</code>.
     */
    private File javaDirectoryFile;

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
                if (!isIncremental) {
                    Scanner scanner = buildContext.newScanner(directory);
                    scanner.setIncludes(new String[] {"*.properties"});
                    scanner.scan();
                    if (scanner.getIncludedFiles() == null) {
                        continue;
                    }
                }
                javaDirectoryFile = directory;
                errors += processAllResourceDirectories(directory);
                buildContext.refresh(directory);
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
     * Recursively scans the directories for a sub-package named "resources",
     * then invokes the resource compiler for that directory.
     */
    private int processAllResourceDirectories(final File directory) throws ResourceCompilerException {
        int errors = 0;
        final File[] subdirs = directory.listFiles();
        if (subdirs != null) { // Appears to be sometime null with auto-generated sub-directories.
            for (final File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    if (subdir.getName().equals("resources")) {
                        final File[] resourcesToProcess = subdir.listFiles(this);
                        if (resourcesToProcess != null && resourcesToProcess.length != 0) {
                            errors += new Compiler(resourcesToProcess).run();
                        }
                    } else {
                        errors += processAllResourceDirectories(subdir);
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Returns {@code true} if the given file is the source code for a resources bundle.
     * This method returns {@code true} if the given file is a Java source file and if a
     * properties file of the same name exists.
     *
     * @param directory The directory.
     * @param name The file name.
     * @return {@code true} if the given file is a property file.
     */
    @Override
    public final boolean accept(final File directory, String name) {
        if (!name.endsWith(IndexedResourceCompiler.JAVA_EXT)) {
            return false;
        }
        name = name.substring(0, name.length() - IndexedResourceCompiler.JAVA_EXT.length());
        name += IndexedResourceCompiler.PROPERTIES_EXT;
        return new File(directory, name).isFile();
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
