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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;


/**
 * Extension to Gradle {@link Test} task.
 * Contrarily to other {@code Modular*} classes in this package,
 * this extension replaces completely the Gradle's default task.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
final class ModularTest extends Conventions {
    /**
     * Sentinel value for packages that are exported to all.
     */
    private static final String EXPORTED = "#exported";

    /**
     * Root directories of compiled test class for each module. Keys are module names and values are directories.
     * The name of the directory, as returned by {@link File#getName()}, should be the module name (i.e. the key).
     */
    private final Map<String,File> moduleDirectories;

    /**
     * All packages together with whether the package is visible to all.
     * If the package is exported, then the associated value is {@link #EXPORTED}.
     * Otherwise the associated value is the name of the module that contains the package.
     * This information is needed for building an {@code --add-exports} JVM option.
     */
    private final Map<String,String> packageVisibility;

    /**
     * Packages containing at least one test classes.
     */
    private final List<String> testedPackages;

    /**
     * A buffer for building the package name as we iterate over the directory structure.
     */
    private final StringBuilder packageName;

    /**
     * The main and test directory with leading and trailing platform-specific name separator.
     */
    private final String mainDirectory, testDirectory;

    /**
     * Creates an object for holding temporary data.
     *
     * @param  project  the sub-project being tested.
     */
    private ModularTest(final Project project) {
        super(project);
        moduleDirectories = new LinkedHashMap<>(32);
        packageVisibility = new LinkedHashMap<>(128);
        testedPackages    = new ArrayList<>(64);
        packageName       = new StringBuilder(64);
        mainDirectory     = File.separator + MAIN_DIRECTORY + File.separator;
        testDirectory     = File.separator + TEST_DIRECTORY + File.separator;
    }

    /**
     * Replaces a path from test directory to a path to main directory.
     * This method assumes that the "test" name to replace is the last path element.
     * It should not be used if the path can contain arbitrary directories after "test".
     *
     * @param  file  a path to a file in the test directory.
     * @return path to the same file, but in the main directory.
     */
    private File testToMain(File file) {
        final StringBuilder path = new StringBuilder(file.getPath());
        final int s = path.lastIndexOf(testDirectory);
        if (s >= 0) {
            file = new File(path.replace(s, s + testDirectory.length(), mainDirectory).toString());
        }
        return file;
    }

    /**
     * Given a directory of modules, builds the list of {@code --add-exports} JVM arguments.
     *
     * @param  directory  output directory of test class. Sub-directories shall be module names.
     * @return directories of test classes.
     */
    private void scanAllModules(final File directory) {
        final File[] moduleCandidates = directory.listFiles(File::isDirectory);
        if (moduleCandidates != null) try {
            for (final File moduleRoot : moduleCandidates) {
                final String moduleName = readModuleInfo(moduleRoot);
                if (moduleName != null) {
                    final File existing = moduleDirectories.put(moduleName, moduleRoot);
                    if (existing != null) {
                        throw new SISBuildException("Duplicated module name: " + moduleName +
                                "\n    Found in: " + existing +
                                "\n     Also in: " + moduleRoot);
                    }
                    for (final File headPackage : moduleRoot.listFiles(File::isDirectory)) {
                        collectTestedPackages(moduleName, headPackage);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads the export declarations of the given {@code module-info.class} file.
     * Packages that <em>may</em> need to be specified in an {@code --add-exports}
     * JVM argument are added to the {@link #packageVisibility} map.
     *
     * @param  moduleRoot  the module directory or compiled test class.
     * @return module name, or {@code null} if {@code "module-info.class"} has not been found.
     * @throws IOException if an error occurred while reading the file.
     */
    private String readModuleInfo(final File moduleRoot) throws IOException {
        final File file = testToMain(new File(moduleRoot, "module-info.class"));
        if (!file.isFile()) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            final ModuleDescriptor module = ModuleDescriptor.read(in);
            final String name = module.name();
            module.packages().forEach((p) -> packageVisibility.put(p, name));
            module.exports().forEach((descriptor) -> {
                if (!descriptor.isQualified()) {
                    packageVisibility.put(descriptor.source(), EXPORTED);
                }
            });
            return name;
        }
    }

    /**
     * Collects a list of tested packages, regardless is exported or not.
     * The package names are added to the {@link #testedPackages} list.
     *
     * @param  moduleName  name of the module being scanned.
     * @param  file        directory or file to inspect.
     * @return whether the given file was presumably a test class.
     */
    private boolean collectTestedPackages(final String moduleName, final File file) {
        final File[] content = file.listFiles();
        if (content == null) {
            return file.getName().endsWith(".class");
        }
        final int oldLength = packageName.length();
        packageName.append(file.getName()).append('.');
        boolean hasTests = false;
        for (final File c : content) {
            hasTests |= collectTestedPackages(moduleName, c);
        }
        if (hasTests) {
            final String name = packageName.substring(0, packageName.length() - 1);
            packageVisibility.putIfAbsent(name, moduleName);
            testedPackages.add(name);
        }
        packageName.setLength(oldLength);
        return false;
    }

    /**
     * Returns the {@code --add-module}, {@code --add-reads} and {@code --add-exports} statements
     * to specify as JVM arguments. The export statements may be numerous, so this method tries to
     * add only those that are necessary.
     *
     * @return the list of arguments to add to the JVM.
     */
    private List<String> moduleOptions() {
        final var args = new ArrayList<String>(2 + 4*moduleDirectories.size() + 2*testedPackages.size());
        args.add("--add-modules");
        args.add(String.join(",", moduleDirectories.keySet()));
        /*
         * --patch-module ${module}=${buildDir}/classes/java/test/${it}
         * --add-reads    ${module}=junit,ALL-UNNAMED
         */
        moduleDirectories.entrySet().forEach((entry) -> {
            final String module = entry.getKey();
            args.add("--patch-module"); args.add(module + '=' + entry.getValue());
            args.add("--add-reads");    args.add(module + "=junit,ALL-UNNAMED");
        });
        /*
         * --add-exports ${module}/${package}=junit
         */
        for (final String name : testedPackages) {
            String module = packageVisibility.remove(name);
            if (module != null && !EXPORTED.equals(module)) {
                args.add("--add-exports");
                args.add(module + '/' + name + "=junit");
            }
        }
        return args;
    }

    /**
     * Adds modularization arguments and exports to JUnit all non-exported packages.
     *
     * @param  tasks  the task where to add JVM arguments.
     */
    static void modularizeAndExportToJunit(final Test task) {
        /*
         * Create a module path which excludes the compiled test classes, because those
         * classes will be specified using another JVM option (namely `--patch-module`).
         */
        var mainpath = task.getClasspath().filter((path) -> !path.getName().equals("test"));
        task.jvmArgs(List.of("--module-path", mainpath.getAsPath()));
        /*
         * JPMS options.
         */
        final var t = new ModularTest(task.getProject());
        task.getTestClassesDirs().forEach((dir) -> {
            t.scanAllModules(dir);
        });
        task.jvmArgs(t.moduleOptions());
        task.setTestClassesDirs(task.getProject().files(t.moduleDirectories.values().toArray()));
        /*
         * Custom options specific to Apache SIS.
         * By default, the working directory is the sub-project root.
         * We move the working directory to a sub-directory of `build`.
         * It will be the location of the `derby.log` file for instance.
         */
        task.useJUnitPlatform();
        task.include("**/*Test.class");
        task.setScanForTestClasses(false);

        final File workingDir = t.fileRelativeToBuild("test-output");
        workingDir.mkdirs();
        task.setWorkingDir(workingDir);
        task.getEnvironment().putIfAbsent("SIS_DATA", workingDir.getPath());
    }
}
