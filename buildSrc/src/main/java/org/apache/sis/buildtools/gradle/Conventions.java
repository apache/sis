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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.gradle.api.Project;


/**
 * An helper class working on hard-coded file and directory names.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class Conventions {
    /**
     * The directory where source code is located, relative to sub-project directory.
     * This is a Gradle convention (inherited from Maven).
     */
    static final String SOURCE_DIRECTORY = "src";

    /**
     * The directory where compilation result are saved, relative to sub-project directory.
     * This is a Gradle convention.
     */
    static final String BUILD_DIRECTORY = "build";

    /**
     * The root directory of the build tools (custom Gradle plugin).
     * This is a Gradle convention.
     */
    static final String BUILD_TOOLS_DIRECTORY = "buildSrc";

    /**
     * The sub-directory inside "{@value #SOURCE_DIRECTORY}" for main Java source code.
     * This is a Gradle convention (inherited from Maven), except that the module name
     * is inserted between "{@value #SOURCE_DIRECTORY}" and "{@value}".
     */
    static final String MAIN_DIRECTORY = "main";

    /**
     * The sub-directory inside "{@value #SOURCE_DIRECTORY}" for test Java source code.
     * This is a Gradle convention (inherited from Maven), except that the module name
     * is inserted between "{@value #SOURCE_DIRECTORY}" and "{@value}".
     */
    static final String TEST_DIRECTORY = "test";

    /**
     * The sub-directory inside "{@value #BUILD_DIRECTORY}" for compiled main class files.
     * This is a Gradle convention.
     */
    static final String MAIN_CLASSES_DIRECTORY = "classes/java/" + MAIN_DIRECTORY;

    /**
     * The sub-directory inside "{@value #BUILD_DIRECTORY}" for compiled test class files.
     * This is a Gradle convention.
     */
    static final String TEST_CLASSES_DIRECTORY = "classes/java/" + TEST_DIRECTORY;

    /**
     * The sub-directory inside "{@value #BUILD_DIRECTORY}" for the JAR files.
     * This is a Gradle convention.
     */
    static final String LIBS_DIRECTORY = "libs";

    /**
     * The sub-directory inside "{@value #BUILD_DIRECTORY}" for the Javadoc and sources ZIP files.
     */
    static final String DOCS_DIRECTORY = "docs";

    /**
     * The sub-directory inside "{@value #BUILD_DIRECTORY}" for the ZIP or OXT files (assemblies).
     * Also the sibling directory of "{@value #MAIN_DIRECTORY}" and "{@value #TEST_DIRECTORY}"
     * for the sources to copy in the ZIP file. This is an Apache SIS convention.
     */
    static final String BUNDLE_DIRECTORY = "bundle";

    /**
     * Name of the sub-project containing the JAR files that are included in Apache SIS releases.
     */
    static final String ENDORSED_SUBPROJECT = "endorsed";

    /**
     * The prefix of the final filename of bundles created by assembly tasks.
     */
    static final String FINALNAME_PREFIX = "apache-sis-";

    /**
     * The encoding to use everywhere (source code, generated HTML pages, …).
     */
    static final String ENCODING = "UTF-8";

    /**
     * The sub-project being compiled.
     */
    protected final Project project;

    /**
     * Creates the helper class.
     *
     * @param  project  the sub-project being compiled.
     */
    Conventions(final Project project) {
        this.project = project;
    }

    /**
     * Returns the directory of the source files for the bundle in the given module.
     *
     * @param  project  the sub-project being compiled.
     * @param  module   the module for which to get the bundle source directory.
     * @return the bundle source directory of the specified module.
     */
    static File getBundleSourceDirectory(final Project project, final String module) {
        return new File(new File(new File(project.getProjectDir(), SOURCE_DIRECTORY), module), BUNDLE_DIRECTORY);
    }

    /**
     * Returns the path of a file in the build directory.
     *
     * @param  project   the sub-project being compiled.
     * @param  filename  name of the file in the build directory.
     * @return path to the file in the build directory.
     */
    static File fileRelativeToBuild(final Project project, final String filename) {
        return project.getLayout().getBuildDirectory().file(filename).get().getAsFile();
    }

    /**
     * Returns a sub-directory of the directory where the compilation result is saved.
     *
     * @param  path  name of sub-directories, in order.
     * @return path of the given name relative to build directory.
     */
    final File fileRelativeToBuild(final String... path) {
        File file = fileRelativeToBuild(project, path[0]);
        for (int i=1; i<path.length; i++) {
            file = new File(file, path[i]);
        }
        return file;
    }

    /**
     * Returns the absolute path of a file relative to the root of the whole project.
     *
     * @param  filename  name of the filename to return.
     * @return path of the given name relative to project root.
     */
    final File fileRelativeToRoot(final String filename) {
        return new File(project.getRootDir(), filename);
    }

    /**
     * Creates a subdirectory if it does not already exists.
     * The parent directory must exist, this method intentionally does not create it.
     *
     * @param  dir  the subdirectory to create.
     * @throws IOException if the subdirectory cannot be created.
     */
    static void mkdir(final File dir) throws IOException {
        if (!(dir.exists() || dir.mkdir())) {
            throw new IOException("Failed to create \"" + dir.getName() + "\" directory.");
        }
    }

    /**
     * Potentially creates a pseudo-copy (using hard-link) of the given file.
     * If the file system does not support hard links, then a real copy is done.
     * If the target file already exists and is not older than the source, then this method does nothing.
     *
     * @param  source  the file to copy.
     * @param  target  the destination file.
     * @throws IOException if an error occurred while creating the hard-link or copying the file.
     */
    static void linkOrCopy(final Path source, final Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            if (Files.getLastModifiedTime(source).compareTo(Files.getLastModifiedTime(target)) <= 0) {
                return;     // File is assumed up-to-date.
            }
            Files.delete(target);
        }
        Files.createDirectories(target.getParent());
        try {
            // `toRealPath()` is necessary, otherwise symbolic links are reproduced verbatim and become broken.
            Files.createLink(target, source.toRealPath());
        } catch (UnsupportedOperationException e) {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
