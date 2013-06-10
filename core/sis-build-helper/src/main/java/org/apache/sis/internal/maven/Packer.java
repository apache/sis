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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.jar.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Creates PAC200 files from the JAR builds by Maven. This tools needs the JAR files to be either provided
 * in the {@code target/binaries} directory, or listed in the {@code target/binaries/content.txt} file.
 *
 * <p><b>Usage:</b> If {@code rootDirectory} is the directory containing the root {@code pom.xml} file,
 * then this class can be used as below (replace {@code "1.0"} by the actual version number and
 * {@code "sis-1.0.jar"} by any filename of your choice):</p>
 *
 * <blockquote><pre> Packer packer = new Packer(new File(rootDirectory, "target"), "1.0");
 * packer.addPack("sis-1.0.jar");
 * packer.createJars();
 * packer.close();
 * packer.pack();</pre></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
final class Packer implements FilenameFilter {
    /**
     * The sub-directory containing pack files. This directory
     * will be automatically created if it doesn't already exist.
     */
    private static final String PACK_DIRECTORY = "bundles";

    /**
     * The Maven target directory. Should contains the {@code "binaries"} sub-directory,
     * which should contains all JAR files collected by {@code sis-build-helper} plugin.
     */
    private final File targetDirectory;

    /**
     * The directory of JAR files. Shall be {@code "target/binaries"}.
     */
    final File jarDirectory;

    /**
     * The JAR files to read, by input filename.
     */
    private final Map<File,PackInput> inputs = new LinkedHashMap<File,PackInput>();

    /**
     * The JAR and PACK files to create, by output name.
     */
    private final Map<String,PackOutput> outputs = new LinkedHashMap<String,PackOutput>();

    /**
     * The version to declare in the manifest file.
     */
    final String version;

    /**
     * Creates a packer.
     *
     * @param targetDirectory The Maven target directory.
     * @param version The version to declare in the manifest file.
     */
    Packer(final File targetDirectory, final String version) throws FileNotFoundException {
        this.version = version;
        this.targetDirectory = targetDirectory;
        this.jarDirectory = new File(targetDirectory, JarCollector.SUB_DIRECTORY);
        if (!jarDirectory.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + jarDirectory);
        }
    }

    /**
     * Adds a pack which will contain every JAR files in the target directory.
     * The given {@code pack} name is the name of the JAR file to create before to be packed.
     * This filename shall ends with the "{@code .jar}" suffix. That suffix will be replaced
     * by {@code ".pack.gz"} at Pack200 creation time.
     *
     * @param  pack The name of the JAR file to create before the Pack200 creation.
     * @throws IOException If an error occurred while collecting the target directory content.
     */
    public void addPack(final String pack) throws IOException {
        final Set<String> list = JarCollector.loadDependencyList(new File(jarDirectory, JarCollector.CONTENT_FILE));
        list.addAll(Arrays.asList(jarDirectory.list(this)));
        addPack(null, pack, list.toArray(new String[list.size()]));
    }

    /**
     * Adds the given JAR files for the given pack. This method can be invoked when we want to
     * create a Pack200 file containing only a subset of all available JAR files, or when some
     * JAR files to include are specified by a previously created Pack200 file.
     *
     * <p>The filenames in the given {@code jars} array can contains the {@code '*'} wildcards.
     * However at most one entry can match, otherwise an exception will be thrown. This limited
     * wildcards support is mostly a convenience for avoiding to specify the version number of
     * JAR files.</p>
     *
     * @param  parent The pack from which to inherit the JAR files, or {@code null} if none.
     * @param  pack   The name of the JAR file to create before the Pack200 creation.
     * @param  jars   The list of JAR files in this pack file. Filenames can contain the {@code '*'} wildcards.
     * @throws IOException If an error occurred while collecting the target directory content.
     */
    public void addPack(final String parent, final String pack, final String[] jars) throws IOException {
        PackOutput p = null;
        if (parent != null) {
            p = outputs.get(parent);
            if (p == null) {
                throw new IllegalArgumentException("Non-existant pack: " + parent);
            }
        }
        /*
         * If there is wildcard, replace the wildcard by the full name.
         * We allows only one name (the wildcard should be used for the
         * version number only, and we don't allow many versions of the
         * same file).
         */
        for (int i=0; i<jars.length; i++) {
            final String jarFile = jars[i];
            final int w = jarFile.lastIndexOf('*');
            if (w >= 0) {
                final String prefix = jarFile.substring(0, w);
                final String suffix = jarFile.substring(w+1);
                final String[] f = jarDirectory.list(new FilenameFilter() {
                    @Override public boolean accept(final File directory, final String name) {
                        return name.startsWith(prefix) && name.endsWith(suffix);
                    }
                });
                if (f == null) {
                    throw new FileNotFoundException("Directory not found: " + jarDirectory);
                }
                switch (f.length) {
                    case 1:  jars[i] = f[0]; break;
                    case 0:  throw new IllegalArgumentException("No file found for pattern: " + jarFile);
                    default: throw new IllegalArgumentException("Duplicated files: " + f[0] + " and " + f[1]);
                }
            }
        }
        p = new PackOutput(this, p, jars);
        if (outputs.put(pack, p) != null) {
            throw new IllegalArgumentException("Duplicated pack: " + pack);
        }
    }

    /**
     * Creates the JAR files from the packages declared with {@link #addPack}.
     *
     * @throws IOException if an error occurred while reading existing JAR files
     *         or writing to the packed files.
     */
    public void createJars() throws IOException {
        /*
         * Creates the output directory. We do that first in order to avoid the
         * costly opening of all JAR files if we can't create this directory.
         */
        final File outDirectory = new File(targetDirectory, PACK_DIRECTORY);
        if (!outDirectory.isDirectory()) {
            if (!outDirectory.mkdir()) {
                throw new IOException("Can't create the \"" + PACK_DIRECTORY + "\" directory.");
            }
        }
        /*
         * Opens all input JAR files in read-only mode, and create the initially empty output JAR
         * file. We need to open all input files in order to check for duplicate entries before we
         * start the writing process. Files in the META-INF/services directory need to be merged.
         */
        for (final Map.Entry<String,PackOutput> entry : outputs.entrySet()) {
            final String name = entry.getKey();
            final PackOutput pack = entry.getValue();
            pack.createPackInputs(inputs);
            pack.open(new File(outDirectory, name));
        }
        /*
         * Iterates through the individual jars and merge them in single, bigger JAR file.
         * During each iteration we get the array of output streams where a particular file
         * need to be copied - all those "active" output streams will be filled in parallel.
         */
        final byte[] buffer = new byte[64*1024];
        final Map<File,PackInput> activeInputs = new LinkedHashMap<File,PackInput>(inputs.size() * 4/3);
        final PackOutput[] activesForFile      = new PackOutput[outputs.size()];
        final PackOutput[] activesForEntry     = new PackOutput[activesForFile.length];
        final PackOutput[] activesForFollow    = new PackOutput[activesForFile.length];
        for (final Iterator<Map.Entry<File,PackInput>> it = inputs.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<File,PackInput> fileInputPair = it.next();
            final File  inputFile = fileInputPair.getKey();
            final PackInput input = fileInputPair.getValue();
            try {
                it.remove(); // Needs to be before next usage of "inputs" below.
                int countForFile = 0;
                for (final PackOutput candidate : outputs.values()) {
                    if (candidate.contains(inputFile)) {
                        activesForFile[countForFile++] = candidate;
                        candidate.copyInputs(inputs, activeInputs);
                    }
                }
                /*
                 * "activesForFile" now contains the list of PackOutput we need to care about
                 * for the current PackInput (i.e. a whole input JAR). Copies every entries
                 * found in that JAR.
                 */
                for (JarEntry entry; (entry = input.nextEntry()) != null;) {
                    int countForEntry = 0;
                    for (int i=0; i<countForFile; i++) {
                        final PackOutput candidate = activesForFile[i];
                        if (candidate.putNextEntry(entry)) {
                            activesForEntry[countForEntry++] = candidate;
                        }
                    }
                    copy(input.getInputStream(), activesForEntry, countForEntry, buffer);
                    /*
                     * From that points, the entry has been copied to all target JARs. Now looks in
                     * following input JARs to see if there is some META-INF/services files to merge.
                     */
                    final String name = entry.getName();
                    if (PackOutput.mergeAllowed(name)) {
                        for (final Map.Entry<File,PackInput> continuing : activeInputs.entrySet()) {
                            final InputStream in = continuing.getValue().getInputStream(name);
                            if (in != null) {
                                final File file = continuing.getKey();
                                int countForFollow = 0;
                                for (int i=0; i<countForEntry; i++) {
                                    final PackOutput candidate = activesForEntry[i];
                                    if (candidate.contains(file)) {
                                        activesForFollow[countForFollow++] = candidate;
                                    }
                                }
                                copy(in, activesForFollow, countForFollow, buffer);
                                Arrays.fill(activesForFollow, null);
                            }
                        }
                    }
                    for (int i=0; i<countForEntry; i++) {
                        activesForEntry[i].closeEntry();
                    }
                    Arrays.fill(activesForEntry, null);
                }
                Arrays.fill(activesForFile, null);
                activeInputs.clear();
            } finally {
                input.close();
            }
        }
        close();
    }

    /**
     * Copies fully the given input stream to the given destination.
     * The given input stream is closed after the copy.
     *
     * @param  in     The input stream from which to get the the content to copy.
     * @param  out    Where to copy the input stream content.
     * @param  count  Number of valid entries in the {@code out} array.
     * @param  buffer Temporary buffer to reuse at each method call.
     * @throws IOException If an error occurred during the copy.
     */
    private static void copy(final InputStream in, final PackOutput[] out, final int count,
                             final byte[] buffer) throws IOException
    {
        int n;
        while ((n = in.read(buffer)) >= 0) {
            for (int i=0; i<count; i++) {
                out[i].write(buffer, n);
            }
        }
        in.close();
    }

    /**
     * Closes all streams.
     *
     * @throws IOException If an error occurred while closing the stream.
     */
    public void close() throws IOException {
        for (final PackOutput jar : outputs.values()) {
            jar.close();
        }
        for (final PackInput jar : inputs.values()) {
            jar.close();
        }
    }

    /**
     * Launch Pack200 after output JAR files have been created.
     *
     * @throws IOException If an error occurred while creating the PACK200 file.
     */
    public void pack() throws IOException {
        for (final PackOutput jar : outputs.values()) {
            jar.pack();
        }
    }

    /**
     * Filter the JAR files.
     *
     * @param  directory The directory (ignored).
     * @param  name The filename.
     * @return {@code true} if the given filename ends with {@code ".jar"}.
     */
    @Override
    public boolean accept(final File directory, final String name) {
        return name.endsWith(".jar");
    }
}
