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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Queue;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.compile.CompileOptions;
import org.apache.sis.buildtools.resources.IndexedResourceCompiler;


/**
 * Extension to Gradle {@link JavaCompile} task.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ModularCompilation extends Conventions {
    /**
     * Resources to copy after the compilation is finished.
     * We copy resources after compilation because otherwise, they seem to be deleted randomly.
     */
    private final Queue<Map<File,Object>> pendingResources;

    /**
     * Creates a new helper instance for Apache SIS compilation.
     *
     * @param  project  the sub-project being compiled.
     */
    ModularCompilation(final Project project) {
        super(project);
        pendingResources = new ConcurrentLinkedQueue<>();
    }

    /**
     * Returns {@code true} if the specified compilation task is for compiling the tests.
     *
     * @param  task  the task to check.
     * @return whether the given task is compiling tests.
     */
    private static boolean isCompileTest(final Task task) {
        return task.getName().contains("Test");
    }

    /**
     * Returns the value of the {@code --module-source-path} option.
     * That option is critical for Module Source Hierarchy.
     * The directory structure that we expect for Apache SIS project is:
     *
     * <pre>&lt;gradle sub-project&gt;
     *     ├─ build
     *     └─ src
     *         ├─ org.apache.sis.metadata
     *         │    ├─ main
     *         │    │   ├─ module-info.java
     *         │    │   └─ org/apache/sis/metadata/…
     *         │    └─ test
     *         │        └─ org/apache/sis/metadata/…
     *         ├─ org.apache.sis.referencing
     *         │    ├─ main
     *         │    │   ├─ module-info.java
     *         │    │   └─ org/apache/sis/referencing/…
     *         │    └─ test
     *         │        └─ org/apache/sis/referencing/…
     *         └─ etc.</pre>
     *
     * @param  project        the Gradle project being compiled.
     * @param  isCompileTest  whether to specify the main sources or the test sources.
     * @return value of the {@code --module-source-path} option.
     */
    static String getModuleSourcePath(final Project project, final boolean isCompileTest) {
        File path;
        path = project.file(SOURCE_DIRECTORY);
        path = new File(path, "*");        // Handled in a special way by `--module-source-path`.
        path = new File(path, isCompileTest ? TEST_DIRECTORY : MAIN_DIRECTORY);
        return path.getPath();
    }

    /**
     * Invoked when the {@code compileJava} task is executed.
     *
     * @param  context  the extension which is invoking this task.
     * @param  task     the {@link JavaCompile} task to configure.
     */
    static void modularize(final BuildHelper context, final JavaCompile task) {
        final boolean  isCompileTest = isCompileTest(task);
        final Project        project = task.getProject();
        final CompileOptions options = task.getOptions();
        options.setEncoding(ENCODING);      // The character encoding to be used when reading source files.
        options.setDeprecation(true);       // Whether to log details of usage of deprecated members or classes.

        final List<String> args = options.getCompilerArgs();
        args.add("--module-source-path");
        args.add(getModuleSourcePath(project, isCompileTest));
        /*
         * WORKAROUND FOR GRADLE 8.1 PROBLEM:
         * The class-path and the module-path should be mutually exclusive set of path elements.
         * In theory Gradle should decide itself which dependencies to put on the module path,
         * since this property is true by default:
         *
         *     modularity.inferModulePath
         *
         * However it doesn't seem to work with Gradle 8.1.1 because Gradle does not recognize
         * the Module Source Hierarchy. Instead of separating ourselves the path elements, an
         * easier workaround is to put everything on the module path. Javac seems to manage.
         *
         * This code replace the following code in `build.gradle.kts` file:
         *
         *     options.compilerArgs.add("--module-path=${classpath.asPath}")
         *     setClasspath(files())       // For avoiding duplication with the --module-path option.
         */
        args.add("--module-path");
        args.add(task.getClasspath().getAsPath());
        task.setClasspath(project.files());          // For avoiding duplication with the --module-path option.
        /*
         * Declare explicitly all modules to take in account.
         * It avoid problems when recompiling only a few classes.
         */
        args.add("--add-modules");
        args.add(String.join(",", context.getModuleNames()));
        /*
         * If compiling tests, declare that we are patching modules.
         */
        if (isCompileTest) {
            context.getPatchedDirectories().forEach((entry) -> {
                args.add("--patch-module");
                args.add(entry.getKey() + '=' + entry.getValue());
            });
        }
    }

    /*
     * ┌───────────────────────────────────────────────────────────────────┐
     * │  Code below this point are for handling international resources.  │
     * │  This is a non-standard feature specific to Apache SIS project.   │
     * │  For modularisation of arbitrary projects, ignore the remaining.  │
     * └───────────────────────────────────────────────────────────────────┘
     */

    /**
     * File extensions of resources to not copy.
     * In addition, everything in {@code doc-files} sub-directories will be excluded.
     *
     * @todo Reduce this list to IDL and Markdown by converting HTML and text files.
     */
    private static final Set<String> EXCLUDE_RESOURCES = Set.of(
            "tmp", "bak", "log",    // Same extensions as in `.gitignore`.
            "idl",                  // Source code for OpenOffice add-in.
            "md",                   // Notes in Markdown format.
            "html");                // Richer alternative to Markdown.

    /**
     * Compiles the international resources that are found in all modules.
     * If the {@code *.properties} file is not a resource to compile,
     * then the file will be linked or copied instead of being compiled.
     *
     * @param  context  the extension which is invoking this task.
     * @param  task     the compilation task which will be executed after this method.
     */
    final void compileOrcopyResources(final BuildHelper context, final Task task) {
        final boolean isCompileTest = isCompileTest(task);
        pendingResources.clear();
        /*
         * There is probably a cleaner way to get the source directories than above hack.
         * For now we use hard-coded paths for main and test classes.
         */
        int errors = 0;
        @SuppressWarnings("LocalVariableHidesMemberVariable")       // Should be the same value.
        final Project project = task.getProject();
        final File outputDir = fileRelativeToBuild(project,
                isCompileTest ? TEST_CLASSES_DIRECTORY
                              : MAIN_CLASSES_DIRECTORY);
        final var results = new HashMap<File,Object>();
        for (final File sourceDirectory : context.getSourceDirectories(isCompileTest)) {
            final var buildDirectory = new File(outputDir, sourceDirectory.getParentFile().getName());
            final var compiler = new IndexedResourceCompiler(sourceDirectory, buildDirectory, results) {
                @Override protected void info   (String message) {project.getLogger().info(message);}
                @Override protected void warning(String message) {project.getLogger().warn(message);}
                @Override protected void otherResource(File source, File target) throws IOException {
                    if (include(source)) {
                        results.put(target, source);
                    }
                }
            };
            try {
                errors += compiler.onJavaSourceDirectory();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (errors != 0) {
            throw new SISBuildException(String.valueOf(errors) + " errors in resources bundles.");
        }
        pendingResources.add(results);
    }

    /**
     * Writes now the resources which were found by {@code #compileOrcopyResources(…)}.
     * This operation should be done last, after compilation, because otherwise Gradle
     * may delete the resources as part of a clean operation.
     */
    final void flushResources() {
        Map<File,Object> resources;
        while ((resources = pendingResources.poll()) != null) {
            for (final Map.Entry<File,Object> entry : resources.entrySet()) try {
                final File target = entry.getKey();
                final Object data = entry.getValue();
                if (data instanceof byte[]) {
                    Files.write(target.toPath(), (byte[]) data);
                } else {
                    final File source = (File) data;
                    linkOrCopy(source.toPath(), target.toPath());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Whether to copy the specified resource.
     *
     * @param  resource  the resource file to potentially copy.
     * @return {@code true} for copying or {@code false} for ignoring.
     */
    private static boolean include(File resource) {
        final File parent = resource.getParentFile();
        if (parent != null && parent.getName().equals("doc-files")) {
            return false;
        }
        String ext = resource.getName();
        ext = ext.substring(ext.lastIndexOf('.') + 1);
        return !EXCLUDE_RESOURCES.contains(ext);
    }
}
