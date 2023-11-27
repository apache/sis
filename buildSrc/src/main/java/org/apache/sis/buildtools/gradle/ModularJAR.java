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
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Jar;


/**
 * Extension to Gradle {@link Jar} task. This extension generates one JAR file per module.
 * If the {@code build.gradle} file specifies a {@code Main-Class} attribute, the latter
 * can apply to only one module, which is identified in this class as the "main module".
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ModularJAR extends ZipWriter.JDK {
    /**
     * Version of the GeoAPI dependency.
     * This value depends on the Apache SIS branch being compiled.
     *
     * @todo should be specified in a property.
     */
    private static final String GEOAPI_VERSION = "3.1-SNAPSHOT";

    /**
     * Attributes where values are class names. Those attributes can be assigned
     * to only one module, because the same class cannot appear in two modules.
     */
    private static final String[] CLASSNAME_ATTRIBUTES = {
        Attributes.Name.MAIN_CLASS.toString(),
        "RegistrationClassName"
    };

    /**
     * Creates a helper instance.
     *
     * @param  project  the sub-project being compiled.
     * @param  out      output stream of the JAR file to create.
     */
    private ModularJAR(final Project project, final JarOutputStream out) {
        super(project, out);
    }

    /**
     * Lists all compiled modules in the current Gradle sub-project.
     *
     * @return all modules in current Gradle sub-project.
     */
    private static File[] listModules(final Project project) {
        return fileRelativeToBuild(project, MAIN_CLASSES_DIRECTORY).listFiles(File::isDirectory);
    }

    /**
     * Invoked when the {@code jar} task is executed.
     *
     * @param  context  the extension which is invoking this task.
     * @param  task     the {@link Jar} task to configure.
     */
    static void execute(final BuildHelper context, final Jar task) {
        final Project       project    = task.getProject();
        final Map<String,?> attributes = task.getManifest().getAttributes();
        final var   filteredAttributes = new HashMap<String,String>();
        final var  classnameAttributes = new HashMap<String,String>();
        for (final String classname : CLASSNAME_ATTRIBUTES) {
            final Object value = attributes.get(classname);
            if (value instanceof String) {
                final String path = ((String) value).replace('.', File.separatorChar) + ".class";
                classnameAttributes.put(classname, path);
            }
        }
        final File target = fileRelativeToBuild(project, LIBS_DIRECTORY);
        for (final File module : listModules(project)) {
            for (final Iterator<Map.Entry<String,String>> it = classnameAttributes.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<String,String> entry = it.next();
                if (new File(module, entry.getValue()).isFile()) {
                    final String key = entry.getKey();
                    filteredAttributes.put(key, (String) attributes.get(key));
                    it.remove();
                }
            }
            try {
                final String name = module.getName();
                write(project, module, new File(target, name + ".jar"), filteredAttributes);
                ModularSources.write(task, name, false);
                ModularSources.write(task, name, true);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            filteredAttributes.clear();
        }
        for (final Map.Entry<String,String> entry : classnameAttributes.entrySet()) {
            task.getLogger().warn(entry.getKey() + " not found: " + entry.getValue());
        }
        try {
            linkDependencies(project, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (UnsupportedOperationException e) {
            project.getLogger().warn("Links to dependencies not provided because hard-links are not supported on this platform.", e);
        }
    }

    /**
     * Writes a JAR file for a single module.
     *
     * @param  project   the project for which to write a JAR file.
     * @param  source    root directory of compiled class files for the module to package.
     * @param  target    the file to write.
     * @param  specific  attributes specific to this module.
     * @throws IOException if an error occurred while reading the source or writing the JAR file.
     */
    private static void write(final Project project, final File source, final File target,
                              final Map<String,String> specific) throws IOException
    {
        final var mf = new Manifest();
        final Attributes attributes = mf.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION,       "1.0");
        attributes.put(Attributes.Name.SPECIFICATION_TITLE,    "GeoAPI");
        attributes.put(Attributes.Name.SPECIFICATION_VENDOR,   "Open Geospatial Consortium");
        attributes.put(Attributes.Name.SPECIFICATION_VERSION,   GEOAPI_VERSION);
        attributes.put(Attributes.Name.IMPLEMENTATION_TITLE,   "Apache Spatial Information System (SIS)");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR,  "The Apache Software Foundation");
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION,  project.getVersion().toString());
        for (final Map.Entry<String,String> entry : specific.entrySet()) {
            attributes.put(new Attributes.Name(entry.getKey()), entry.getValue());
        }
        try (JarOutputStream out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(target)), mf)) {
            new ModularJAR(project, out).writeDirectory(source, null, "");
        }
    }

    /**
     * Adds hard-links to all dependencies.
     *
     * @param  project  the project for which to add dependencies.
     * @param  target   where to write dependencies.
     * @throws IOException if an I/O error occurred while reading the module description.
     * @throws UnsupportedOperationException if links are not supported on this platform.
     */
    private static void linkDependencies(final Project project, final File target) throws IOException {
        for (final Dependency dep : Dependency.find(project)) {
            if (dep.module != null) {
                final Path f = new File(target, dep.module + ".jar").toPath();
                if (Files.notExists(f)) {
                    Files.createLink(f, dep.file.toPath());
                }
            }
        }
    }
}
