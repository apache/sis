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


/**
 * Hard-coded file and directory names used by this package.
 *
 * <p><b>Reminder:</b>
 * If the above constants are modified, please remind to edit the <cite>Distribution file
 * and Pack200 bundle</cite> section in the <code>src/site/apt/index.apt</code> file.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class Filenames {
    /**
     * The target directory. This directory name is hard-coded instead than using a property annotated
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
     * dependencies on platforms that do not support hard links.
     */
    static final String CONTENT_FILE = "content.txt";

    /**
     * The sub-directory inside {@value #TARGET_DIRECTORY} containing pack files.
     * This directory will be automatically created if it does not already exist.
     */
    static final String DISTRIBUTION_DIRECTORY = "distribution";

    /**
     * The path to the directory (relative to the project root) to zip for creating the distribution ZIP file.
     */
    static final String ARTIFACT_PATH = "application/sis-console/src/main/artifact";

    /**
     * The name of the sub-directory inside {@value #ARTIFACT_PATH} where the Pack200 file will be located.
     * Note that we will not write in the real directory, but only in the directory structure which is
     * reproduced in the ZIP file.
     */
    static final String LIB_DIRECTORY = "lib";

    /**
     * The name (without extension) of the big JAR file which will contains everything.
     * This file will be located in the {@value #LIB_DIRECTORY} directory.
     */
    static final String FATJAR_FILE = "sis";

    /**
     * The prefix of the final filename. This is hard coded for now.
     */
    static final String FINALNAME_PREFIX = "apache-sis-";

    /**
     * The extension for Pack200 files.
     */
    static final String PACK_EXTENSION = ".pack.gz";

    /**
     * Do not allow instantiation of this class.
     */
    private Filenames() {
    }

    /**
     * Returns the distribution directory, creating it if needed.
     *
     * @param targetDirectory The {@code target} directory.
     */
    static File distributionDirectory(final File targetDirectory) throws IOException {
        final File outDirectory = new File(targetDirectory, DISTRIBUTION_DIRECTORY);
        if (!outDirectory.isDirectory()) {
            if (!outDirectory.mkdir()) {
                throw new IOException("Can't create the \"" + DISTRIBUTION_DIRECTORY + "\" directory.");
            }
        }
        return outDirectory;
    }
}
