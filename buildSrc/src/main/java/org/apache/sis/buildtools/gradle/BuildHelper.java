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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;


/**
 * Registration of Gradle tasks for building Apache SIS.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BuildHelper implements Plugin<Project> {
    /**
     * Roots of package hierarchy inside each Java module. Keys are module names and values are directories.
     * Populated by {@link #setSourceModulePaths(Project)} and should not be modified after that method call.
     */
    private final Map<String,File> mainDirs, testDirs;

    /**
     * Creates the task.
     */
    public BuildHelper() {
        mainDirs = new LinkedHashMap<>(32);
        testDirs = new LinkedHashMap<>(32);
    }

    /**
     * Sets the collection of sources as one directory per module.
     * The expected hierarchy is:
     *
     * <pre>endorsed
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
     * @param  project  the project for which to set the sources.
     */
    private void setSourceModulePaths(final Project project) {
        final File[] modules = project.file(Conventions.SOURCE_DIRECTORY).listFiles(File::isDirectory);
        if (modules == null) {
            throw new IllegalArgumentException("Not a directory: " + Conventions.SOURCE_DIRECTORY);
        }
        for (final File module : modules) {
            File p;
            final String name = module.getName();
            if ((p = new File(module, Conventions.MAIN_DIRECTORY)).isDirectory()) mainDirs.put(name, p);
            if ((p = new File(module, Conventions.TEST_DIRECTORY)).isDirectory()) testDirs.put(name, p);
        }
        final SourceSetContainer sources = project.getExtensions().getByType(SourceSetContainer.class);
        sources.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava().setSrcDirs(mainDirs.values());
        sources.getByName(SourceSet.TEST_SOURCE_SET_NAME).getJava().setSrcDirs(testDirs.values());
    }

    /**
     * Returns the name of all main modules.
     *
     * @return module names. This collection is not cloned, <strong>do not modify</strong>.
     */
    final Set<String> getModuleNames() {
        return mainDirs.keySet();
    }

    /**
     * Returns the roots of package hierarchy inside each Java module.
     * There is one directory per module.
     *
     * @param  tests  {@code true} for returning test directories, or {@code false} for main directories.
     * @return the directories. This collection is not cloned, <strong>do not modify</strong>.
     */
    final Collection<File> getSourceDirectories(final boolean tests) {
        return (tests ? testDirs : mainDirs).values();
    }

    /**
     * Returns the main modules to patch with test classes.
     */
    final Stream<Map.Entry<String,File>> getPatchedDirectories() {
        return testDirs.entrySet().stream().filter((e) -> mainDirs.containsKey(e.getKey()));
    }

    /**
     * Registers tasks potentially used for Apache SIS build.
     * This method is invoked during the configuration phase of Gradle.
     *
     * @param  project  the project where to register the task.
     */
    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);         // This plugin depends on the Java plugin.
        setSourceModulePaths(project);
        /*
         * Configurations to be applied before a task is executed.
         * Tasks are compilation, tests and javadoc.
         */
        final TaskContainer tasks = project.getTasks();
        tasks.withType(JavaCompile.class).forEach((task) -> {       // We usually have only one instance.
            final var ext = new ModularCompilation(task.getProject());
            task.doFirst((t) -> {
                ModularCompilation.modularize(BuildHelper.this, (JavaCompile) t);
                ext.compileOrcopyResources(BuildHelper.this, t);
            });
            task.doLast((t) -> {
                ext.flushResources();
                JavaMaker.execute(project);
            });
        });
        tasks.withType(Test.class).forEach((task) -> {
            task.doFirst((t) -> {
                ModularTest.modularizeAndExportToJunit((Test) t);
            });
        });
        tasks.withType(Javadoc.class).forEach((task) -> {
            task.doFirst((t) -> {
                new ModularJavadoc((Javadoc) t).modularize(BuildHelper.this);
            });
        });
        tasks.withType(Jar.class).forEach((task) -> {
            task.getInputs().dir(Conventions.SOURCE_DIRECTORY);
            task.getOutputs().dir(Conventions.fileRelativeToBuild(project, Conventions.LIBS_DIRECTORY));
            task.getOutputs().dir(Conventions.fileRelativeToBuild(project, Conventions.DOCS_DIRECTORY));
            task.setActions(List.of((t) -> {            // Replace the default action by our own.
                ModularJAR.execute(BuildHelper.this, (Jar) t);
            }));
        });
        final Task assemble = tasks.getByPath("assemble");
        assemble.dependsOn("jar");
        assemble.doLast((t) -> {     // tasks.withType(Assembler.class) does not work.
            Assembler.create(t);
            UnoPkg.create(t);
        });
        /*
         * Configuration of the publication to Maven repository.
         */
        new ModularPublishing(project).configure();
    }
}
