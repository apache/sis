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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.apache.sis.internal.maven.Filenames.*;


/**
 * Creates a PACK200 files from the JAR in the {@code target/binaries} directory.
 * This tools needs the JAR files to be either copied or linked in the {@code target/binaries} directory,
 * or listed in the {@code target/binaries/content.txt} file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
final class Packer implements FilenameFilter {
    /**
     * The project name, URL and version to declare in the manifest file, or {@code null} if none.
     */
    private final String projectName, projectURL, version;

    /**
     * The Maven target directory. Shall contain the {@code "binaries"} sub-directory,
     * which shall contain all JAR files collected by {@code sis-build-helper} plugin.
     */
    private final File targetDirectory;

    /**
     * The directory of input JAR files. Shall be {@code "target/binaries"}.
     */
    private final File binariesDirectory;

    /**
     * Creates a packer.
     *
     * @param  projectName     The project name to declare in the manifest file, or {@code null} if none.
     * @param  projectURL      The project URL to declare in the manifest file, or {@code null} if none.
     * @param  version         The project version to declare in the manifest file, or {@code null} if none.
     * @param  targetDirectory The Maven target directory.
     * @throws FileNotFoundException if the {@code target/binaries} directory is not found.
     */
    Packer(final String projectName, final String projectURL, final String version,
           final File targetDirectory) throws FileNotFoundException
    {
        this.projectName = projectName;
        this.projectURL  = projectURL;
        this.version     = version;
        this.targetDirectory = targetDirectory;
        this.binariesDirectory = new File(targetDirectory, BINARIES_DIRECTORY);
        if (!binariesDirectory.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + binariesDirectory);
        }
    }

    /**
     * Filter the input JAR files. This is for internal usage by {@link #createOutputJAR(String)} only.
     *
     * @param  directory The directory (ignored).
     * @param  name The filename.
     * @return {@code true} if the given filename ends with {@code ".jar"}.
     */
    @Override
    public boolean accept(final File directory, final String name) {
        return name.endsWith(".jar");
    }

    /**
     * Returns the list of input JAR files, together with a helper class for copying the data in the Pack200 file.
     * All input JAR files are opened by this method. They will need to be closed by {@link PackInput#close()}.
     */
    private Map<File,PackInput> getInputJARs() throws IOException {
        final Set<String> filenames = JarCollector.loadDependencyList(new File(binariesDirectory, CONTENT_FILE));
        filenames.addAll(Arrays.asList(binariesDirectory.list(this)));
        final Map<File,PackInput> inputJARs = new LinkedHashMap<File,PackInput>(filenames.size() * 4/3);
        for (final String filename : filenames) {
            File file = new File(filename);
            if (!file.isAbsolute()) {
                file = new File(binariesDirectory, filename);
            }
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
     * @param  outputJAR The name of the JAR file to create before the Pack200 creation.
     * @throws IOException If an error occurred while collecting the target directory content.
     */
    PackOutput preparePack200(final String outputJAR) throws IOException {
        /*
         * Creates the output directory. We do that first in order to avoid the
         * costly opening of all JAR files if we can't create this directory.
         */
        final File outDirectory = distributionDirectory(targetDirectory);
        final PackOutput output = new PackOutput(getInputJARs(), new File(outDirectory, outputJAR));
        try {
            output.open(projectName, projectURL, version);
            output.writeContent();
        } finally {
            output.close();
        }
        return output;
    }
}
