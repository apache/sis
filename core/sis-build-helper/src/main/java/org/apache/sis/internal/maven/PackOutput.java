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
import java.util.Iterator;
import java.util.Enumeration;
import java.util.jar.*;
import java.io.File;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.jar.Pack200.Packer.*;
import static org.apache.sis.internal.maven.Filenames.*;


/**
 * A JAR file to be created for output by {@link Packer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
final class PackOutput implements Closeable {
    /**
     * The extension of class files in a JAR file.
     */
    private static final String CLASS = ".class";

    /**
     * The output file path.
     */
    private final File outputJAR;

    /**
     * The stream where to write the JAR.
     * Created only when {@link #open(File)} is invoked.
     */
    private JarOutputStream outputStream;

    /**
     * The manifest attribute value, or {@code null} if none. We will set this field to the
     * value of the last {@link PackInput} to be used by this {@code PackOutput}. This is on
     * the assumption that the last input is the main one.
     */
    private String mainClass, splashScreen;

    /**
     * The JAR to be used as inputs. The elements in this map will be removed by the
     * {@link #write()} method as we are done copying the content of JAR files.
     */
    private final Map<File,PackInput> inputJARs;

    /**
     * The entries which were already written in the output JAR file.
     * There is two kind of entries which need this check:
     *
     * <ul>
     *   <li>Directories, which may be duplicated in different JAR files.</li>
     *   <li>{@code META-INF/services} files which were merged in a single file.</li>
     * </ul>
     *
     * @see #isMergeAllowed(String)
     */
    private final Set<String> entriesDone = new HashSet<String>();

    /**
     * Returns {@code true} if entries of the given name are allowed to be concatenated
     * when they appear in more than one input JAR files.
     *
     * @see #entriesDone
     */
    private static boolean isMergeAllowed(final String name) {
        return name.startsWith(PackInput.SERVICES);
    }

    /**
     * Creates an output jar.
     *
     * @param inputJARs The input JAR filenames together with their {@code PackInput} helpers.
     * @param outputJAR The output JAR filename.
     */
    PackOutput(final Map<File,PackInput> inputJARs, final File outputJAR) {
        this.inputJARs = inputJARs;
        this.outputJAR = outputJAR;
        for (final PackInput in : inputJARs.values()) {
            if (in.mainClass != null) {
                mainClass = in.mainClass;
            }
            if (in.splashScreen != null) {
                splashScreen = in.splashScreen;
            }
        }
    }

    /**
     * Opens the given JAR file for writing and creates its manifest.
     *
     * @param  projectName The project name, or {@code null} if none.
     * @param  projectURL  The project URL, or {@code null} if none.
     * @param  version     The project version, or {@code null} if none.
     * @throws IOException if the file can't be open.
     */
    void open(final String projectName, final String projectURL, final String version) throws IOException {
        final Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (projectName != null) {
            attributes.put(Attributes.Name.SPECIFICATION_TITLE,    projectName);
            attributes.put(Attributes.Name.SPECIFICATION_VENDOR,   projectName);
            attributes.put(Attributes.Name.IMPLEMENTATION_TITLE,   projectName);
            attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR,  projectName);
        }
        if (projectURL != null) {
            attributes.put(Attributes.Name.IMPLEMENTATION_URL, projectURL);
        }
        if (version != null) {
            attributes.put(Attributes.Name.SPECIFICATION_VERSION,  version);
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        }
        if (mainClass != null) {
            attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        }
        if (splashScreen != null) {
            attributes.put(PackInput.SPLASH_SCREEN, splashScreen);
        }
        /*
         * Add the package-level manifest of every dependencies.
         */
        for (final File input : inputJARs.keySet()) {
            if (!input.getName().startsWith("sis-")) {
                String packageName = null;
                final JarFile jar = new JarFile(input, false);
                try {
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
                } finally {
                    jar.close();
                }
            }
        }
        /*
         * Open the output stream for the big JAR file.
         */
        outputStream = new JarOutputStream(new FileOutputStream(outputJAR), manifest);
        outputStream.setLevel(1); // Use a cheap compression level since this JAR file is temporary.
    }

    /**
     * Copies the value of the given attribute from a source {@code Attributes} to a target
     * {@code Attributes} object. This is used for copying the package-level attributes.
     *
     * @return {@code true} if the attribute has been copied.
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
     * Iterates through the individual jars and merge them in single, bigger JAR file.
     * This method closes the input JAR files as they are done.
     */
    final void writeContent() throws IOException {
        final byte[] buffer = new byte[64 * 1024];
        for (final Iterator<Map.Entry<File,PackInput>> it = inputJARs.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<File,PackInput> inputJAR = it.next();
            it.remove(); // Needs to be removed before the inner loop below.
            final PackInput input = inputJAR.getValue();
            try {
                for (JarEntry entry; (entry = input.nextEntry()) != null;) {
                    final String name = entry.getName();
                    boolean isMergeAllowed = false;
                    if (entry.isDirectory() || (isMergeAllowed = isMergeAllowed(name))) {
                        if (!entriesDone.add(name)) {
                            continue;
                        }
                    }
                    outputStream.putNextEntry(entry);
                    copy(input.getInputStream(), buffer);
                    /*
                     * From that points, the entry has been copied to the target JAR. Now look in
                     * following input JARs to see if there is some META-INF/services files to merge.
                     */
                    if (isMergeAllowed) {
                        for (final Map.Entry<File,PackInput> continuing : inputJARs.entrySet()) {
                            final InputStream in = continuing.getValue().getInputStream(name);
                            if (in != null) {
                                copy(in, buffer);
                            }
                        }
                    }
                    outputStream.closeEntry();
                }
            } finally {
                input.close();
            }
        }
    }

    /**
     * Copies fully the given input stream to the given destination.
     * The given input stream is closed after the copy.
     *
     * @param  in     The input stream from which to get the the content to copy.
     * @param  buffer Temporary buffer to reuse at each method call.
     * @throws IOException If an error occurred during the copy.
     */
    void copy(final InputStream in, final byte[] buffer) throws IOException {
        int n;
        while ((n = in.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, n);
        }
        in.close();
    }

    /**
     * Closes this output.
     *
     * @throws IOException if an error occurred while closing the file.
     */
    @Override
    public void close() throws IOException {
        for (final PackInput input : inputJARs.values()) {
            /*
             * The code in this loop is never executed in normal execution, since the map shall be empty after
             * successful completion of 'writeContent()'. However the map may be non-empty if the above method
             * threw an exception, in which case this 'close()' method will be invoked in a 'finally' block.
             */
            input.close();
        }
        inputJARs.clear();
        if (outputStream != null) {
            outputStream.close();
        }
        outputStream = null;
    }

    /**
     * Creates a Pack200 file from the output JAR, then delete the JAR.
     *
     * @throws IOException if an error occurred while packing the JAR.
     */
    void pack() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("JAR output stream not closed.");
        }
        final File inputFile = outputJAR;
        String filename = inputFile.getName();
        final int ext = filename.lastIndexOf('.');
        if (ext > 0) {
            filename = filename.substring(0, ext);
        }
        filename += PACK_EXTENSION;
        final File outputFile = new File(inputFile.getParent(), filename);
        if (outputFile.equals(inputFile)) {
            throw new IOException("Input file is already packed: " + inputFile);
        }
        pack(new FileOutputStream(outputFile));
    }

    /**
     * Creates a Pack200 file from the output JAR, then delete the JAR.
     *
     * @param  out Where to write the Pack200. This stream will be closed by this method.
     * @throws IOException if an error occurred while packing the JAR.
     */
    void pack(final OutputStream out) throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("JAR output stream not closed.");
        }
        final File inputFile = outputJAR;
        final Pack200.Packer packer = Pack200.newPacker();
        final Map<String,String> p = packer.properties();
        p.put(EFFORT, String.valueOf(9));  // Maximum compression level.
        p.put(KEEP_FILE_ORDER,    FALSE);  // Reorder files for better compression.
        p.put(MODIFICATION_TIME,  LATEST); // Smear modification times to a single value.
        p.put(DEFLATE_HINT,       TRUE);   // Ignore all JAR deflation requests.
        p.put(UNKNOWN_ATTRIBUTE,  ERROR);  // Throw an error if an attribute is unrecognized
        final JarFile jarFile = new JarFile(inputFile);
        try {
            final OutputStream deflater = new GZIPOutputStream(out);
            try {
                packer.pack(jarFile, deflater);
            } finally {
                deflater.close();
            }
        } finally {
            jarFile.close();
        }
        if (!inputFile.delete()) {
            throw new IOException("Can't delete temporary file: " + inputFile);
        }
    }
}
