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
package org.apache.sis.buildtools.gradle;

import java.util.Set;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.io.IOException;
import org.gradle.api.Task;
import org.gradle.api.Project;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;


/**
 * Creates a ZIP file containing the Apache SIS binary distribution.
 * The created file contains:
 *
 * <ul>
 *   <li>the content of the <code>optional/src/org.apache.sis.gui/bundle</code> directory;</li>
 *   <li>the content of the <code>endorsed/build/libs</code> and <code>optional/build/libs</code> directories.</li>
 * </ul>
 *
 * Assemblies are created only for the module having a {@code bundle} sub-directory in source code.
 * That module is hard-coded for now (a future version could perform a search if desired).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 */
final class Assembler extends ZipWriter.Apache implements FileFilter {
    /**
     * The module on which to apply this task.
     */
    private static final String MODULE = "org.apache.sis.gui";

    /**
     * The name of the sub-directory inside the ZIP file where the JAR files will be located.
     * Note that we will not write in the real directory, but only in the directory structure
     * which is reproduced in the ZIP file.
     */
    private static final String LIB_DIRECTORY = "lib/";

    /**
     * Subdirectory of {@link #LIB_DIRECTORY} where to put applications.
     * We keep them in a separated directory because we need to put only
     * one of them in the module-path.
     */
    private static final String APP_DIRECTORY = "app/";

    /**
     * Modules to put in the {@link #APP_DIRECTORY} directory.
     */
    private static final Set<String> APPLICATIONS = Set.of(MODULE, "org.apache.sis.console");

    /**
     * Modules to exclude from the {@link #LIB_DIRECTORY}, not counting applications.
     */
    private static final Set<String> EXCLUDES = Set.of(
            "org.apache.sis.profile.japan",             // For avoiding UCAR dependencies.
            "org.apache.sis.cloud.aws");                // For avoiding AWS dependencies.

    /**
     * Suffix of JAR files. This is the part to remove from filenames for getting the module names.
     */
    private static final String JAR_SUFFIX = ".jar";

    /**
     * Whether this task is in the process of copying the application JAR files.
     */
    private boolean isCopyingApplication;

    /**
     * Whether the copy operation shall include dependencies.
     */
    private boolean includeDependencies;

    /**
     * Creates a helper object for creating the assembly.
     *
     * @param  project  the project for which to create an assembly.
     * @param  out      output stream of the ZIP file to create.
     */
    private Assembler(final Project project, final ZipArchiveOutputStream out) {
        super(project, out);
    }

    /**
     * Creates the distribution file.
     *
     * @param  task  the assembler task (in Gradle 8.2.1, it appears to be an opaque decorator that we cannot cast).
     */
    static void create(final Task task) {
        final Project project = task.getProject();
        final File sourceDirectory = getBundleSourceDirectory(project, MODULE);
        if (sourceDirectory.isDirectory()) try {
            File target = fileRelativeToBuild(project, BUNDLE_DIRECTORY);
            mkdir(target);
            String filename = FINALNAME_PREFIX + project.getVersion();
            target = new File(target, filename + ".zip");
            filename += '/';
            final String libDir = filename + LIB_DIRECTORY;
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(target)) {
                final var c = new Assembler(project, out);
                c.writeDirectory(sourceDirectory, null, filename);
                c.isCopyingApplication = false;
                c.copyCompiledJARs(libDir);
                c.isCopyingApplication = true;
                c.copyCompiledJARs(libDir + APP_DIRECTORY);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies all endorsed and optional JAR files compiled by the Apache SIS project.
     *
     * @param  libDir  name of the "lib" directory inside the ZIP file.
     * @throws IOException if an error occurred while copying a file.
     */
    private void copyCompiledJARs(final String libDir) throws IOException {
        includeDependencies = true;
        writeDirectory(fileRelativeToBuild(LIBS_DIRECTORY), this, libDir);
        includeDependencies = false;
        final File endorsed = new File(new File(new File(project.getRootDir(), ENDORSED_SUBPROJECT), BUILD_DIRECTORY), LIBS_DIRECTORY);
        final File[] deps = endorsed.listFiles(this);
        if (deps == null) {
            throw new FileNotFoundException("Missing directory: " + endorsed);
        }
        for (final File dep : deps) {
            writeFile(dep, libDir);
        }
    }

    /**
     * Whether to include the given file in the ZIP file.
     *
     * @param  path  the file or directory to test.
     * @return whether to include the file.
     */
    @Override
    public boolean accept(final File path) {
        String name = path.getName();
        if (name.endsWith(JAR_SUFFIX) && (includeDependencies || name.startsWith("org.apache.sis."))) {
            name = name.substring(0, name.length() - JAR_SUFFIX.length());
            if (!EXCLUDES.contains(name)) {
                return APPLICATIONS.contains(name) == isCopyingApplication;
            }
        }
        return false;
    }
}
