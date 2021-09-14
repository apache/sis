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
package org.apache.sis.test;

import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.sis.internal.jdk9.JDK9;

import static org.junit.Assert.*;


/**
 * Provides methods that depend on assumption on the project directory layout.
 * We currently use Maven conventions, but this class provide a central place
 * to revisit if we want to change convention in the future.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ProjectDirectories {
    /**
     * The directory where are stored the compiled Java classes for the package of the given class.
     * The constructor used the given class as a sample member for getting this package directory.
     */
    public final Path classesPackageDirectory;

    /**
     * The root directory where are stored the compiled Java classes.
     * The constructor used the given class as a sample member for getting this directory.
     *
     * <p>If we are running the tests from another environment than Maven (e.g. from NetBeans project),
     * then this directory may contain all modules instead of only the module of the class given to
     * the constructor.</p>
     */
    public final Path classesRootDirectory;

    /**
     * Name of the package of the class given at construction time.
     */
    private final String packageName;

    /**
     * Find classes and sources directories using the given class as a member.
     * In the case of Maven multi-modules project, the directories will apply
     * only to the module containing the given class.
     *
     * @param  c  a sample member of the classes for which to get the directories.
     */
    public ProjectDirectories(final Class<?> c) {
        final URL resource = c.getResource(c.getSimpleName() + ".class");
        assertNotNull("Class not found.", resource);
        Path dir;
        try {
            dir = Paths.get(resource.toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        classesPackageDirectory = dir;
        packageName = JDK9.getPackageName(c);
        String pkg = packageName;
        int s = pkg.length();
        do {
            pkg = pkg.substring(0, s);
            s = pkg.lastIndexOf('.');
            assertEquals ("Unexpected directory structure.", pkg.substring(s+1), dir.getFileName().toString());
            assertNotNull("Unexpected directory structure.", dir = dir.getParent());
        } while (s >= 0);
        classesRootDirectory = dir;
    }

    /**
     * Returns the root directory of source Java code.
     *
     * @param  module  module name, e.g. {@code "core/sis-referencing"}.
     *                 Used only if the project is not a Maven project.
     * @return root directory of source Java files.
     */
    public Path getSourcesRootDirectory(final String module) {
        Path dir = getMavenModule();
        if (dir == null) {
            /*
             * This block is executed only if the compiled class file was not found in the location
             * that we would have expected for a Maven project. Maybe the class is in the NetBeans
             * build directory instead.
             */
            dir = classesRootDirectory;
            do {
                if (dir == null) {
                    throw new AssertionError("No more parent directory.");
                }
                dir = dir.getParent();
            } while (!Files.exists(dir.resolve("pom.xml")));
            dir = dir.resolve(module.replace('/', File.separatorChar));
        }
        /*
         * At this point `dir` is the root of the Maven module
         * which contains the class given to the constructor.
         */
        dir = dir.resolve("src").resolve("main").resolve("java");
        if (!Files.isDirectory(dir)) {
            throw new AssertionError("Not a directory: " + dir);
        }
        return dir;
    }

    /**
     * Returns the directory of source code for the package of the class given at construction time.
     *
     * @param  module  module name, e.g. {@code "core/sis-referencing"}.
     *                 Used only if the project is not a Maven project.
     * @return package directory of source Java files.
     */
    public Path getSourcesPackageDirectory(final String module) {
        return getSourcesRootDirectory(module).resolve(packageName.replace('.', File.separatorChar));
    }

    /**
     * Returns whether the {@link #classesRootDirectory} is the sub-directory of a tree following Maven conventions.
     * This method verifies that the parent directories are {@code "target/*classes"} and that the parent directory
     * contains a {@code pom.xml} file.
     *
     * @return whether we are in a Maven module.
     */
    public boolean isMavenModule() {
        return getMavenModule() != null;
    }

    /**
     * Returns the path to Maven module, or {@code null} if none.
     */
    private Path getMavenModule() {
        Path dir = classesRootDirectory;
        if (dir.getFileName().toString().endsWith("classes")) {
            dir = dir.getParent();
            if (dir != null && dir.getFileName().toString().equals("target")) {
                dir = dir.getParent();
                if (dir != null && Files.isRegularFile(dir.resolve("pom.xml"))) {
                    return dir;
                }
            }
        }
        return null;
    }
}
