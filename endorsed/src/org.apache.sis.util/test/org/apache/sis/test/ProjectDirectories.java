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

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * Provides methods that depend on assumption on the project directory layout.
 * This class provideS a central place to revisit when the layout changes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ProjectDirectories {
    /**
     * The directory where are stored the compiled Java classes for the package of the given class.
     * The constructor uses the given class as a sample member for getting this package directory.
     */
    public final Path classesPackageDirectory;

    /**
     * The root directory where are stored the compiled Java classes in the module.
     * The constructor uses the given class as a sample member for getting this directory.
     * This is the directory of the package hierarchy. For example, if the class is in the
     * {@code org.apache.sis.referencing} module, then the filename (tip) of this directory
     * is that module name.
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
        assertNotNull(resource, "Class not found.");
        Path dir;
        try {
            dir = Path.of(resource.toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        classesPackageDirectory = dir;
        packageName = c.getPackageName();
        String pkg = packageName;
        int s = pkg.length();
        do {
            pkg = pkg.substring(0, s);
            s = pkg.lastIndexOf('.');
            assertEquals (pkg.substring(s+1), dir.getFileName().toString(), "Unexpected directory structure.");
            assertNotNull(dir = dir.getParent(), "Unexpected directory structure.");
        } while (s >= 0);
        classesRootDirectory = dir;
    }

    /**
     * Returns the root directory of source Java code.
     *
     * @return root directory of source Java files.
     */
    public Path getSourcesRootDirectory() {
        Path dir = getProjectDirectory();
        dir = dir.resolve("src").resolve(classesRootDirectory.getFileName()).resolve("main");
        if (!Files.isDirectory(dir)) {
            throw new RuntimeException("Not a directory: " + dir);
        }
        return dir;
    }

    /**
     * Returns the directory of source code for the package of the class given at construction time.
     *
     * @return package directory of source Java files.
     */
    public Path getSourcesPackageDirectory() {
        return getSourcesRootDirectory().resolve(packageName.replace('.', File.separatorChar));
    }

    /**
     * Returns the path to the sub-project.
     */
    private Path getProjectDirectory() {
        Path dir = classesRootDirectory;
        while ((dir = dir.getParent()) != null) {
            Path p = dir.resolve("endorsed");
            if (Files.exists(p)) {
                return p;
            }
        }
        throw new RuntimeException("Directory not found.");
    }
}
