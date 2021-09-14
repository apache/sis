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
import org.apache.maven.plugin.MojoExecutionException;


/**
 * Hard-coded file and directory names used by this package.
 *
 * <p><b>Reminder:</b>
 * if the above constants are modified, please remind to edit the <cite>Distribution file</cite>
 * section in the <code>site/content/build.mdtext</code> file.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
final class Filenames {
    /**
     * The target directory. This directory name is hard-coded instead of using a property annotated
     * by {@code @Parameter(defaultValue="${project.build.directory}")} - or alternatively by invoking
     * {@code MavenProject.getModel().getBuild().getDirectory()}, because we need the target directory
     * of the project root rather than the directory of the module being built.
     */
    static final String TARGET_DIRECTORY = "target";

    /**
     * The sub-directory inside {@value #TARGET_DIRECTORY} for binaries.
     */
    static final String BINARIES_DIRECTORY = "binaries";

    /**
     * The name of the file inside {@value #BINARIES_DIRECTORY} where to list SIS JAR files and their
     * dependencies on platforms that do not support hard links. Also the file to ignore when copying
     * entries in a ZIP file.
     */
    static final String OTHER_DEPENDENCIES_FILE = "other_dependencies.txt";

    /**
     * The file to ignore when copying entries in a ZIP file.
     * Those files appear in the {@value #ARTIFACT_PATH} directory.
     */
    static final String CONTENT_FILE = "content.txt";

    /**
     * The sub-directory inside {@value #TARGET_DIRECTORY} containing pack files.
     * This directory will be automatically created if it does not already exist.
     */
    private static final String DISTRIBUTION_DIRECTORY = "distribution";

    /**
     * The path to the directory (relative to the project directory) to zip for creating the distribution ZIP file.
     */
    static final String ARTIFACT_PATH = "src/main/artifact";

    /**
     * The name of the sub-directory inside {@value #ARTIFACT_PATH} where the JAR files will be located.
     * Note that we will not write in the real directory, but only in the directory structure which is
     * reproduced in the ZIP file.
     */
    static final String LIB_DIRECTORY = "lib";

    /**
     * The prefix of native resources in JAR files. All those resources will be excluded from
     * the JAR copied in the zip file and stored in a {@code nativeFiles} map instead.
     */
    static final String NATIVE_DIRECTORY = "native/";

    /**
     * The prefix of the final filename. This is hard coded for now.
     */
    static final String FINALNAME_PREFIX = "apache-sis-";

    /**
     * Do not allow instantiation of this class.
     */
    private Filenames() {
    }

    /**
     * Returns the distribution file, creating its directory if needed.
     *
     * @param  rootDirectory  the project root directory.
     * @param  filename       name of the file to create.
     */
    static File distributionFile(final String rootDirectory, final String filename) throws MojoExecutionException {
        final File outDirectory = new File(new File(rootDirectory, TARGET_DIRECTORY), DISTRIBUTION_DIRECTORY);
        if (!outDirectory.isDirectory()) {
            if (!outDirectory.mkdir()) {
                throw new MojoExecutionException("Can't create the \"" + DISTRIBUTION_DIRECTORY + "\" directory.");
            }
        }
        return new File(outDirectory, filename);
    }
}
