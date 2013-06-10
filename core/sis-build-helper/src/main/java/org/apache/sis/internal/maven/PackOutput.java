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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Enumeration;
import java.util.jar.*;
import java.io.File;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.jar.Pack200.Packer.*;


/**
 * A JAR file to be created for output by {@link Packer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
final class PackOutput implements Closeable {
    /**
     * The extension of class files in a JAR file.
     */
    private static final String CLASS = ".class";

    /**
     * The packer that created this object. Will be used in order to fetch
     * additional informations like the version to declare in the pom.xml file.
     */
    private final Packer packer;

    /**
     * The output file.
     */
    private File file;

    /**
     * The stream where to write the JAR. Will be created
     * only when {@link #open} will be invoked.
     */
    private JarOutputStream out;

    /**
     * The manifest attribute value, or {@code null} if none. We will set this field to the
     * value of the last {@link PackInput} to be used by this {@code PackOutput}. This is on
     * the assumption that the last input is the main one.
     */
    private String mainClass, splashScreen;

    /**
     * The JAR to be used as inputs.
     */
    private final Set<File> inputs;

    /**
     * The entries which were already done by previous invocation of {@link #getInputStream}.
     */
    private final Set<String> entriesDone = new HashSet<>();

    /**
     * Creates an output jar.
     *
     * @param packer    The packer that created this object.
     * @param parent    The parent, or {@code null} if none.
     * @param jars      The JAR filenames.
     */
    PackOutput(final Packer packer, final PackOutput parent, final String[] jars) {
        this.packer = packer;
        if (parent != null) {
            inputs = new LinkedHashSet<>(parent.inputs);
        } else {
            inputs = new LinkedHashSet<>(jars.length * 4/3);
        }
        for (final String jar : jars) {
            final File file = new File(packer.jarDirectory, jar);
            if (!file.isFile()) {
                throw new IllegalArgumentException("Not a file: " + file);
            }
            if (!inputs.add(file)) {
                throw new IllegalArgumentException("Duplicated JAR: " + file);
            }
        }
    }

    /**
     * Returns {@code true} if this pack contains the given JAR file.
     *
     * @param  file The JAR file to check for inclusion.
     * @return {@code true} if this pack contains the given JAR file.
     */
    boolean contains(final File file) {
        return inputs.contains(file);
    }

    /**
     * Copies the entries from the given {@code mapping} to the given {@code actives} map, but
     * only those having a key included in the set of input files used by this {@code PackOutput}.
     *
     * @param mapping The mapping from {@link File} to {@link PackInput}.
     * @param actives Where to store the {@link PackInput} required for
     *        input by this {@code PackOutput}.
     */
    void copyInputs(final Map<File,PackInput> mapping, final Map<File,PackInput> actives) {
        for (final File file : inputs) {
            final PackInput input = mapping.get(file);
            if (input != null) {
                final PackInput old = actives.put(file, input);
                if (old != null && old != input) {
                    throw new AssertionError("Inconsistent mapping.");
                }
            }
        }
    }

    /**
     * Opens the JAR files that were not already opens and store them in the given map.
     *
     * @param  inputs The map where to store the opened JAR files.
     * @throws IOException If a file can not be open.
     */
    void createPackInputs(final Map<File,PackInput> inputs) throws IOException {
        for (final File jar : this.inputs) {
            PackInput in = inputs.get(jar);
            if (in == null) {
                in = new PackInput(jar);
                if (inputs.put(jar, in) != null) {
                    throw new AssertionError(jar);
                }
            }
            if (in.mainClass != null) {
                mainClass = in.mainClass;
            }
            if (in.splashScreen != null) {
                splashScreen = in.splashScreen;
            }
        }
    }

    /**
     * Opens the given JAR file for writing
     *
     * @param  file The file to open.
     * @throws IOException if the file can't be open.
     */
    void open(final File file) throws IOException {
        this.file = file;
        final Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION,       "1.0");
        attributes.put(Attributes.Name.SPECIFICATION_TITLE,    "Apache SIS");
        attributes.put(Attributes.Name.SPECIFICATION_VENDOR,   "Apache SIS");
        attributes.put(Attributes.Name.SPECIFICATION_VERSION,  packer.version);
        attributes.put(Attributes.Name.IMPLEMENTATION_TITLE,   "Apache SIS");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR,  "Apache SIS");
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, packer.version);
        attributes.put(Attributes.Name.IMPLEMENTATION_URL,     "http://sis.apache.org");
        if (mainClass != null) {
            attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        }
        if (splashScreen != null) {
            attributes.put(PackInput.SPLASH_SCREEN, splashScreen);
        }
        /*
         * Add the manifest of every dependencies.
         */
        for (final File input : inputs) {
            if (!input.getName().startsWith("sis-")) {
                String packageName = null;
                try (JarFile jar = new JarFile(input, false)) {
                    final Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        final JarEntry entry = entries.nextElement();
                        final String classname = entry.getName();
                        if (classname.endsWith(CLASS)) {
                            int length = classname.length() - CLASS.length();
                            if (packageName == null) {
                                packageName = classname.substring(0, length);
                            } else {
                                length = Math.min(packageName.length(), length);
                                int i; for (i=0; i<length; i++) {
                                    if (packageName.charAt(i) != classname.charAt(i)) {
                                        break;
                                    }
                                }
                                i = packageName.lastIndexOf('/', i) + 1;
                                packageName = packageName.substring(0, i);
                            }
                        }
                    }
                    if (packageName != null && packageName.length() != 0) {
                        packageName = packageName.substring(0, packageName.length()-1).replace('/', '.');
                        final Attributes src = jar.getManifest().getMainAttributes();
                        attributes = new Attributes();
                        if (copy(src, attributes, Attributes.Name.SPECIFICATION_TITLE)    |
                            copy(src, attributes, Attributes.Name.SPECIFICATION_VENDOR)   |
                            copy(src, attributes, Attributes.Name.SPECIFICATION_VERSION)  |
                            copy(src, attributes, Attributes.Name.IMPLEMENTATION_TITLE)   |
                            copy(src, attributes, Attributes.Name.IMPLEMENTATION_VENDOR)  |
                            copy(src, attributes, Attributes.Name.IMPLEMENTATION_VERSION) |
                            copy(src, attributes, Attributes.Name.IMPLEMENTATION_URL))
                        {
                            manifest.getEntries().put(packageName, attributes);
                        }
                    }
                }
            }
        }
        /*
         * Open the output stream for the big JAR file.
         */
        out = new JarOutputStream(new FileOutputStream(file), manifest);
        out.setLevel(1); // Use a cheap compression level since this JAR file is temporary.
    }

    /**
     * Copies the value of the given attribute from a source {@code Attributes} to a target
     * {@code Attributes} object.
     */
    private static boolean copy(final Attributes src, final Attributes dst, final Attributes.Name name) {
        String value = (String) src.get(name);
        if (value != null && ((value = value.trim()).length()) != 0) {
            dst.put(name, value);
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if entries of the given name are allowed to be concatenated
     * if they appear in more than one input JAR files.
     */
    static boolean mergeAllowed(final String name) {
        return name.startsWith(PackInput.SERVICES);
    }

    /**
     * Begins writing a new JAR entry.
     *
     * @param  entry The new entry to write.
     * @return {@code true} if the entry is ready to write, or {@code false} if it should be skipped.
     * @throws IOException If a failure occurs while creating the entry.
     */
    boolean putNextEntry(final JarEntry entry) throws IOException {
        final String name = entry.getName();
        if (entry.isDirectory() || mergeAllowed(name)) {
            if (!entriesDone.add(name)) {
                return false;
            }
        }
        out.putNextEntry(entry);
        return true;
    }

    /**
     * Writes the given number of bytes.
     *
     * @param  buffer The buffer containing the bytes to write.
     * @param  n The number of bytes to write.
     * @throws IOException if an exception occurred while writing the bytes.
     */
    void write(final byte[] buffer, final int n) throws IOException {
        out.write(buffer, 0, n);
    }

    /**
     * Close the current entry.
     *
     * @throws IOException If an error occurred while closing the entry.
     */
    void closeEntry() throws IOException {
        out.closeEntry();
    }

    /**
     * Closes this output.
     *
     * @throws IOException if an error occurred while closing the file.
     */
    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
        out = null;
    }

    /**
     * Packs the output JAR.
     *
     * @throws IOException if an error occurred while packing the JAR.
     */
    void pack() throws IOException {
        if (out != null) {
            throw new IllegalStateException("JAR output stream not closed.");
        }
        final File inputFile = file;
        String filename = inputFile.getName();
        final int ext = filename.lastIndexOf('.');
        if (ext > 0) {
            filename = filename.substring(0, ext);
        }
        filename += ".pack.gz";
        final File outputFile = new File(inputFile.getParent(), filename);
        if (outputFile.equals(inputFile)) {
            throw new IOException("Input file is already a packed: " + inputFile);
        }
        /*
         * Now process to the compression.
         */
        final Pack200.Packer packer = Pack200.newPacker();
        final Map<String,String> p = packer.properties();
        p.put(EFFORT, String.valueOf(9));  // Maximum compression level.
        p.put(KEEP_FILE_ORDER,    FALSE);  // Reorder files for better compression.
        p.put(MODIFICATION_TIME,  LATEST); // Smear modification times to a single value.
        p.put(DEFLATE_HINT,       TRUE);   // Ignore all JAR deflation requests.
        p.put(UNKNOWN_ATTRIBUTE,  ERROR);  // Throw an error if an attribute is unrecognized
        try (JarFile jarFile = new JarFile(inputFile)) {
            try (OutputStream out = new GZIPOutputStream(new FileOutputStream(outputFile))) {
                packer.pack(jarFile, out);
            }
        }
        if (!inputFile.delete()) {
            throw new IOException("Can't delete temporary file: " + inputFile);
        }
    }
}
