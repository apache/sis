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

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.IOException;

import static org.apache.sis.internal.maven.Filenames.*;


/**
 * Creates a PACK200 files from the JAR in the {@code target/binaries} directory.
 * This tools needs the JAR files to be either copied or linked in the {@code target/binaries} directory,
 * or listed in the {@code target/binaries/content.txt} file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.3
 * @module
 */
final class Packer {
    /**
     * The project name and version to declare in the manifest file, or {@code null} if none.
     */
    private final String projectName, version;

    /**
     * JAR files of the main project together with its dependencies.
     */
    private final Set<File> files;

    /**
     * The Maven target directory. Shall contain the {@code "binaries"} sub-directory,
     * which shall contain all JAR files collected by {@code sis-build-helper} plugin.
     */
    private final File targetDirectory;

    /**
     * Creates a packer.
     *
     * @param  projectName      the project name to declare in the manifest file, or {@code null} if none.
     * @param  version          the project version to declare in the manifest file, or {@code null} if none.
     * @param  files            the JAR files of the main project together with its dependencies.
     * @param  targetDirectory  the Maven target directory.
     */
    Packer(final String projectName, final String version, final Set<File> files, final File targetDirectory) {
        this.projectName     = projectName;
        this.version         = version;
        this.files           = files;
        this.targetDirectory = targetDirectory;
    }

    /**
     * Returns the list of input JAR files, together with a helper class for copying the data in the Pack200 file.
     * All input JAR files are opened by this method. They will need to be closed by {@link PackInput#close()}.
     */
    private Map<File,PackInput> getInputJARs() throws IOException {
        final Map<File,PackInput> inputJARs = new LinkedHashMap<>(files.size() * 4/3);
        for (final File file : files) {
            if (!file.isFile() || !file.canRead()) {
                throw new IllegalArgumentException("Not a file or can not read: " + file);
            }
            if (inputJARs.put(file, new PackInput(file)) != null) {
                throw new IllegalArgumentException("Duplicated JAR: " + file);
            }
        }
        return inputJARs;
    }

    /**
     * Prepares the Pack 200 file which will contain every JAR files in the {@code target/binaries} directory.
     * The given {@code outputJAR} name is the name of the JAR file to create before to be packed.
     * This filename shall end with the "{@code .jar}" suffix.
     *
     * <p>Callers needs to invoke one of the {@code PackOutput.pack(…)} methods on the returned object.</p>
     *
     * @param  outputJAR  the name of the JAR file to create before the Pack200 creation.
     * @throws IOException if an error occurred while collecting the target directory content.
     */
    PackOutput preparePack200(final String outputJAR) throws IOException {
        /*
         * Creates the output directory. We do that first in order to avoid the
         * costly opening of all JAR files if we can't create this directory.
         */
        final File outDirectory = distributionDirectory(targetDirectory);
        final PackOutput output = new PackOutput(getInputJARs(), new File(outDirectory, outputJAR));
        try {
            output.open(projectName, version);
            output.writeContent();
        } finally {
            output.close();
        }
        return output;
    }
}
