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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipOutputStream;
import org.gradle.api.Project;
import org.gradle.api.Task;


/**
 * Generates the source codes on a per-module basis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ModularSources extends ZipWriter.JDK {
    /**
     * The Java system property to set to {@code true} for enabling Javadoc generation.
     */
    private static final String RELEASE_VERSION_PROPERTY = "org.apache.sis.releaseVersion";

    /**
     * Creates a helper instance.
     *
     * @param  project  the sub-project being compiled.
     * @param  out      output stream of the JAR file to create.
     */
    private ModularSources(final Project project, final ZipOutputStream out) {
        super(project, out);
    }

    /**
     * Deletes the given directory recursively.
     *
     * @param  f  file or directory to delete.
     * @throws IOException if the file or directory cannot be deleted.
     */
    private static void delete(final File f) throws IOException {
        final File[] files = f.listFiles();
        if (files != null) {
            for (File c : files)
                delete(c);
        }
        if (!f.delete()) {
            throw new IOException("Cannot delete " + f);
        }
    }

    /**
     * Returns the path to the sources of the given module.
     *
     * @param  project  the project.
     * @param  module   name of the module.
     * @return path to the sources of the specified module.
     */
    private static File sourcesDir(final Project project, final String module) {
        return new File(new File(project.file(Conventions.SOURCE_DIRECTORY), module), Conventions.MAIN_DIRECTORY);
    }

    /**
     * Creates the JAR files for source codes or javadoc.
     *
     * @param  task     the JAR task being executed.
     * @param  module   name of the module for which to create sources JAR.
     * @param  javadoc  whether to create Javadoc.
     * @throws IOException if an error occurred while writing the source ZIP files.
     * @throws InterruptedException if the process has been interrupted while generating javadoc.
     */
    static void write(final Task task, final String module, final boolean javadoc) throws IOException, InterruptedException {
        final Project project = task.getProject();
        final File sources;
        if (javadoc) {
            sources = new File(task.getTemporaryDir(), "javadoc");
            if (!sources.mkdir()) {
                delete(sources);
                if (!sources.mkdir()) {
                    throw new IOException("Cannot create " + sources);
                }
            }
            /*
             * For performance reason, we actually generate the Javadoc only of the
             * "org.apache.sis.releaseVersion" system property is set to a non-null value.
             * Otherwise the Javadoc files will be empty.
             */
            final String version = System.getProperty(RELEASE_VERSION_PROPERTY);
            if (version != null) {
                final var pb = new ProcessBuilder("javadoc",
                        "-locale",              "en",
                        "-doctitle",            "Apache SIS " + version + " API",
                        "-tag",                 "category:X:Category:",
                        "-tag",                 "todo:a:TODO:",
                        "-nonavbar", "-noindex", "-nodeprecatedlist", "-notree", "-nohelp",
                        "--module-path",        Conventions.fileRelativeToBuild(project, LIBS_DIRECTORY).getPath(),
                        "--module-source-path", sourcesDir(project, "*").getPath(),
                        "--add-modules",        "org.apache.sis.storage",       // For allowing some forward references.
                        "-d",                   sources.getPath(),
                        "--module",             module);

                final File errors = new File(sources, "errors.log");
                pb.redirectError(errors);
                final Process p = pb.start();
                p.waitFor();
                if (p.exitValue() == 0) {
                    errors.delete();
                }
            } else {
                Files.writeString(new File(sources, "README").toPath(),
                        "For performance reason, Javadoc generation is disabled by default.\n" +
                        "For generating Javadoc, set the following system property:\n" +
                        "\n" +
                        "    " + RELEASE_VERSION_PROPERTY + "=<version>\n" +
                        "\n");
            }
        } else {
            sources = sourcesDir(project, module);
        }
        final var target = Conventions.fileRelativeToBuild(project, Conventions.DOCS_DIRECTORY);
        target.mkdir();
        final File file = new File(target, module + '-' + (javadoc ? "javadoc" : "sources") + ".jar");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            final var writer = new ModularSources(project, out);
            writer.writeDirectory(sources, null, "");
        }
    }
}
