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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * Compiles the international resources that are found in the module from which this mojo is invoked.
 * See the <code><a href="{@website}/sis-build-helper/index.html">sis-build-helper</a></code> module
 * for more information.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 *
 * @goal compile-resources
 * @phase generate-resources
 */
public class ResourceCompilerMojo extends AbstractMojo implements FilenameFilter {
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
     * @parameter property="project.build.outputDirectory"
     * @required
     */
    private String outputDirectory;

    /**
     * The <code>compileSourceRoots</code> named "java" as a <code>File</code>.
     */
    private File javaDirectoryFile;

    /**
     * The <code>outputDirectory</code> as a <code>File</code>.
     */
    private File outputDirectoryFile;

    /**
     * Executes the mojo.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    public void execute() throws MojoExecutionException {
        int errors = 0;
        outputDirectoryFile = new File(outputDirectory);
        for (final String sourceDirectory : compileSourceRoots) {
            final File directory = new File(sourceDirectory);
            if (directory.getName().equals("java")) {
                javaDirectoryFile = directory;
                errors += processAllResourceDirectories(directory);
            }
        }
        if (errors != 0) {
            throw new ResourceCompilerException(String.valueOf(errors) + " errors in resources bundles.");
        }
    }

    /**
     * Recursively scans the directories for a sub-package named "resources",
     * then invokes the resource compiler for that directory.
     */
    private int processAllResourceDirectories(final File directory) throws ResourceCompilerException {
        int errors = 0;
        for (final File subdir : directory.listFiles()) {
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
            super(javaDirectoryFile, outputDirectoryFile, resourcesToProcess);
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
