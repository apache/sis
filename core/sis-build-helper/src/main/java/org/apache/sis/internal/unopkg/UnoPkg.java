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
package org.apache.sis.internal.unopkg;

import java.io.*;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.jar.JarFile;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static java.util.jar.Pack200.Packer;


/**
 * Creates an {@code .oxt} package for <a href="http://www.openoffice.org">OpenOffice.org</a> addins.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@Mojo(name = "unopkg", defaultPhase = LifecyclePhase.PACKAGE)
public final class UnoPkg extends AbstractMojo implements FilenameFilter {
    /**
     * The subdirectory (relative to {@link #baseDirectory}) where the UNO files are expected.
     */
    static final String SOURCE_DIRECTORY = "src/main/unopkg";

    /**
     * The encoding for text files to read and write.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * The string to replace by the final name.
     */
    private static final String SUBSTITUTE = "${project.build.finalName}";

    /**
     * Base directory of the module to compile.
     * The UNO files are expect in the {@code "src/main/unopkg"} subdirectory.
     * The plugin will look for the {@code META-INF/manifest.xml} and {@code *.rdb} files in that directory.
     */
    @Parameter(property="basedir", required=true, readonly=true)
    private String baseDirectory;

    /**
     * Directory where the output {@code .oxt} file will be located.
     */
    @Parameter(property="project.build.directory", required=true, readonly=true)
    private String outputDirectory;

    /**
     * In {@code META-INF/manifest.xml}, all occurrences of {@code ${project.build.finalName}}
     * will be replaced by this value.
     */
    @Parameter(property="project.build.finalName", required=true, readonly=true)
    private String finalName;

    /**
     * The name for the {@code .oxt} file to create, without the {@code ".oxt"} filename extension.
     */
    @Parameter(property="project.build.finalName", required=true, readonly=true)
    private String oxtName;

    /**
     * {@code true} for using Pack200 compression. If {@code true}, then the add-in registration
     * class needs to unpack the files itself before use. The default value is {@code false}.
     */
    @Parameter(defaultValue="false")
    private String pack200;

    /**
     * The Maven project running this plugin.
     */
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * The prefix to be added before JAR file names.
     * To be determined by heuristic rule.
     */
    private transient String prefix;

    /**
     * Apply prefix only for dependencies of this group.
     */
    private transient String prefixGroup;

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param   directory the directory in which the file was found.
     * @param   name      the name of the file.
     */
    @Override
    public boolean accept(final File directory, final String name) {
        if (name.endsWith("-sources.jar") || name.endsWith("-tests.jar") || name.endsWith("-javadoc.jar")) {
            return false;
        }
        return name.endsWith(".jar") || name.endsWith(".JAR") ||
               name.endsWith(".rdb") || name.endsWith(".RDB") ||
               name.endsWith(".xml") || name.endsWith(".XML") ||
               name.endsWith(".xcu") || name.endsWith(".XCU") ||
               name.endsWith(".png") || name.endsWith(".PNG");
    }

    /**
     * Generates the {@code .oxt} file from all {@code .jar} files found in the target directory.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final int i = finalName.indexOf(project.getArtifactId());
        prefix = (i >= 0) ? finalName.substring(0, i) : "";
        prefixGroup = project.getGroupId();
        try {
            createPackage();
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating the oxt file.", e);
        }
    }

    /**
     * Creates the {@code .oxt} file.
     */
    private void createPackage() throws IOException {
        final String  manifestName = "META-INF/manifest.xml";
        final File sourceDirectory = new File(baseDirectory, SOURCE_DIRECTORY);
        final File outputDirectory = new File(this.outputDirectory);
        final File         zipFile = new File(outputDirectory, oxtName + ".oxt");
        final File    manifestFile = new File(sourceDirectory, manifestName);
        final File[]          jars = outputDirectory.listFiles(this);
        final File[]          rdbs = sourceDirectory.listFiles(this);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            out.setLevel(9);
            if (manifestFile.isFile()) {
                copyFiltered(manifestFile, out, manifestName);
            }
            /*
             * Copies the RDB files.
             */
            for (int i=0; i<rdbs.length; i++) {
                copy(rdbs[i], out, null);
            }
            /*
             * Copies the JAR (and any additional JARs provided in the output directory).
             * Do not pack this JAR, because it contains the code needed to inflate the
             * PACK200 file.
             */
            for (int i=0; i<jars.length; i++) {
                copy(jars[i], out, null);
            }
            /*
             * Copies the dependencies, optionally in a single PACK200 entry.
             */
            Pack200.Packer packer = null;
            if (Boolean.parseBoolean(pack200)) {
                packer = Pack200.newPacker();
                final Map<String,String> p = packer.properties();
                p.put(Packer.EFFORT,            "9");               // take more time choosing codings for better compression.
                p.put(Packer.SEGMENT_LIMIT,     "-1");              // use largest-possible archive segments (>10% better compression).
                p.put(Packer.KEEP_FILE_ORDER,   Packer.FALSE);      // reorder files for better compression.
                p.put(Packer.MODIFICATION_TIME, Packer.LATEST);     // smear modification times to a single value.
                p.put(Packer.CODE_ATTRIBUTE_PFX+"LineNumberTable",    Packer.STRIP);        // discard debug attributes.
                p.put(Packer.CODE_ATTRIBUTE_PFX+"LocalVariableTable", Packer.STRIP);        // discard debug attributes.
                p.put(Packer.CLASS_ATTRIBUTE_PFX+"SourceFile",        Packer.STRIP);        // discard debug attributes.
                p.put(Packer.DEFLATE_HINT,      Packer.TRUE);       // transmitting a single request to use "compress" mode.
                p.put(Packer.UNKNOWN_ATTRIBUTE, Packer.ERROR);      // throw an error if an attribute is unrecognized.
            }
            for (final Artifact artifact : project.getDependencyArtifacts()) {
                final String scope = artifact.getScope();
                if (scope != null &&  // Maven 2.0.6 bug?
                   (scope.equalsIgnoreCase(Artifact.SCOPE_COMPILE) ||
                    scope.equalsIgnoreCase(Artifact.SCOPE_RUNTIME)))
                {
                    final File file = artifact.getFile();
                    String name = file.getName();
                    if (artifact.getGroupId().startsWith(prefixGroup) && !name.startsWith(prefix)) {
                        name = prefix + name;
                    }
                    if (packer != null && name.endsWith(".jar")) {
                        name = name.substring(0, name.length()-3) + "pack";
                        try (JarFile jar = new FilteredJarFile(file)) {
                            out.putNextEntry(new ZipEntry(name));
                            packer.pack(jar, out);
                            out.closeEntry();
                        }
                    } else {
                        copy(file, out, name);
                    }
                }
            }
        }
    }

    /**
     * Copies the content of the specified binary file to the specified output stream.
     *
     * @param name The ZIP entry name, or {@code null} for using the name of the given file.
     */
    private static void copy(final File file, final ZipOutputStream out, String name) throws IOException {
        if (name == null) {
            name = file.getName();
        }
        final ZipEntry entry = new ZipEntry(name);
        if (name.endsWith(".png")) {
            final long size = file.length();
            entry.setMethod(ZipOutputStream.STORED);
            entry.setSize(size);
            entry.setCompressedSize(size);
            entry.setCrc(getCRC32(file));
        }
        out.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            final byte[] buffer = new byte[4*1024];
            int length;
            while ((length = in.read(buffer)) >= 0) {
                out.write(buffer, 0, length);
            }
        }
        out.closeEntry();
    }

    /**
     * Copies the content of the specified ASCII file to the specified output stream.
     */
    private void copyFiltered(final File file, final ZipOutputStream out, String name) throws IOException {
        if (name == null) {
            name = file.getName();
        }
        final ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        final Writer writer = new OutputStreamWriter(out, ENCODING);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), ENCODING))) {
            String line; while ((line=in.readLine()) != null) {
                int r=-1; while ((r=line.indexOf(SUBSTITUTE, r+1)) >= 0) {
                    line = line.substring(0, r) + finalName + line.substring(r + SUBSTITUTE.length());
                }
                writer.write(line);
                writer.write('\n');
            }
        }
        writer.flush();
        out.closeEntry();
    }

    /**
     * Computes CRC32 for the given file.
     */
    private static long getCRC32(final File file) throws IOException {
        final CRC32 crc = new CRC32();
        try (InputStream in = new FileInputStream(file)) {
            final byte[] buffer = new byte[4*1024];
            int length;
            while ((length = in.read(buffer)) >= 0) {
                crc.update(buffer, 0, length);
            }
        }
        return crc.getValue();
    }
}
