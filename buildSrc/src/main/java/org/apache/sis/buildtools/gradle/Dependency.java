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
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.publish.maven.MavenPublication;


/**
 * Information about a dependency.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Dependency {
    /**
     * Workaround for what seems to be a Gradle bug. {@link MavenPublication#getArtifactId()}
     * does not contain the value specified in the {@code build.gradle.kts} file.
     */
    private static final Map<String,String> MODULE_NAMES = Map.ofEntries(
        Map.entry("util",                     "core:sis-utility"),
        Map.entry("metadata",                 "core:sis-metadata"),
        Map.entry("referencing",              "core:sis-referencing"),
        Map.entry("referencing.gazetteer",    "core:sis-referencing-by-identifiers"),
        Map.entry("feature",                  "core:sis-feature"),
        Map.entry("portrayal",                "core:sis-portrayal"),
        Map.entry("storage",                  "storage:sis-storage"),
        Map.entry("storage.sql",              "storage:sis-sqlstore"),
        Map.entry("storage.xml",              "storage:sis-xmlstore"),
        Map.entry("storage.netcdf",           "storage:sis-netcdf"),
        Map.entry("storage.geotiff",          "storage:sis-geotiff"),
        Map.entry("storage.earthobservation", "storage:sis-earth-observation"),
        Map.entry("cloud.aws",                "cloud:sis-cloud-aws"),
        Map.entry("profile.france",           "profiles:sis-french-profile"),
        Map.entry("profile.japan",            "profiles:sis-japan-profile"),
        Map.entry("console",                  "application:sis-console"),
        Map.entry("openoffice",               "application:sis-openoffice"),
        Map.entry("epsg",                     "non-free:sis-epsg"),                 // Optional.
        Map.entry("database",                 "non-free:sis-embedded-data"),        // Optional.
        Map.entry("gui",                      "application:sis-javafx"),            // Optional.
        Map.entry("cql",                      "core:sis-cql"),                      // Incubator.
        Map.entry("storage.shapefile",        "storage:sis-shapefile"),
        Map.entry("storage.geoheif",          "storage:sis-geoheif"),
        Map.entry("storage.gsf",              "storage:sis-gsf"),
        Map.entry("storage.gdal",             "storage:sis-gdal"),
        Map.entry("storage.geopackage",       "storage:sis-geopackage"),
        Map.entry("storage.coveragejson",     "storage:sis-coveragejson"),
        Map.entry("portrayal.map",            "core:sis-portrayal-map"),
        Map.entry("webapp",                   "application:sis-webapp")
    );

    /**
     * The attribute for automatic module name in {@code META-INF/MANIFEST.MF} files.
     */
    private static final Attributes.Name AUTO_MODULE_NAME = new Attributes.Name("Automatic-Module-Name");

    /**
     * Path to the dependency JAR file.
     */
    public final File file;

    /**
     * Maven coordinates of the dependency.
     *
     * @see #writeAsMaven(StringBuilder, String)
     */
    private final String group, name, version;

    /**
     * Name of the JPMS module of the dependency, or {@code null} if unspecified.
     */
    public final String module;

    /**
     * Creates information about a publication to be handled as a dependency for other publication.
     *
     * @param  pub  the publication.
     */
    Dependency(final MavenPublication pub) {
        final String patch = MODULE_NAMES.get(pub.getName());
        if (patch == null) {
            throw new SISBuildException("Unknown publication: " + pub.getName());
        }
        final int s = patch.indexOf(':');
        group   = "org.apache.sis." + patch.substring(0, s);
        name    = patch.substring(s+1);
        version = pub.getVersion();
        module  = "org.apache.sis." + pub.getName();
        file    = null;
    }

    /**
     * Creates a new dependency.
     *
     * @param  artifact  Gradle artifact to be converted to Maven one.
     * @throws IOException if an I/O error occurred while reading the module description.
     */
    private Dependency(final ResolvedArtifact artifact) throws IOException {
        final ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
        group   = id.getGroup();
        name    = id.getName();
        version = id.getVersion();
        file    = artifact.getFile();
        module  = getModuleName();
    }

    /**
     * Returns the name of the JPMS module of the dependency.
     *
     * @return JPMS module name, or {@code null} if unspecified.
     * @throws IOException if an I/O error occurred while reading the module description.
     */
    private String getModuleName() throws IOException {
        try (JarFile zip = new JarFile(file, false, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
            final ZipEntry entry = zip.getEntry("module-info.class");
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    final ModuleDescriptor desc = ModuleDescriptor.read(in);
                    return desc.name();
                }
            }
            final Manifest mf = zip.getManifest();
            if (mf != null) {
                final Object value = mf.getMainAttributes().get(AUTO_MODULE_NAME);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        }
        return name.replace('-', '.');
    }

    /**
     * Returns the dependencies of the given project.
     *
     * @param  project  the project for which to find dependencies.
     * @return dependencies of the specified project.
     * @throws IOException if an I/O error occurred while reading the module description.
     */
    public static List<Dependency> find(final Project project) throws IOException {
        final var dependencies = new ArrayList<Dependency>();
        final ConfigurationContainer configurations = project.getConfigurations();
        for (final String type : new String[] {"implementation","compileOnly","runtimeOnly"}) {
            final Configuration config = configurations.getByName(type).copyRecursive();
            config.setCanBeResolved(true);
            for (final ResolvedArtifact artifact : config.getResolvedConfiguration().getResolvedArtifacts()) {
                dependencies.add(new Dependency(artifact));
            }
        }
        return dependencies;
    }

    /**
     * Returns the JPMS dependencies of the given project.
     * Keys are JPMS module names are values are dependency descriptions.
     *
     * @param  project      the project for which to find dependencies.
     * @param  subProjects  the sub-projects, which also need to be included as dependencies across JPMS modules.
     * @return dependencies of the specified project.
     * @throws IOException if an I/O error occurred while reading the module description.
     */
    public static Map<String,Dependency> jpms(final Project project, final Collection<Dependency> subProjects) throws IOException {
        final var dependencies = new HashMap<String,Dependency>();
        final var warnings     = new HashSet<String>();
        final var sources      = find(project);
        sources.addAll(subProjects);
        sources.forEach((dep) -> {
            if (dep.module != null) {
                final Dependency old = dependencies.putIfAbsent(dep.module, dep);
                if (old != null && !old.equals(dep)) {
                    final StringBuilder message = new StringBuilder(600).append("Inconsistent dependencies:\n");
                    old.writeAsMaven(message, 1, false);
                    dep.writeAsMaven(message, 1, false);
                    final String s = message.toString();
                    if (warnings.add(s)) {
                        project.getLogger().warn(s);
                    }
                }
            }
        });
        return dependencies;
    }

    /**
     * Writes the Maven {@code <dependency>} element in the given buffer.
     *
     * @param  pom       where to write.
     * @param  level     the initial indentation level.
     * @param  optional  whether the dependency is optional.
     */
    public final void writeAsMaven(final StringBuilder pom, final int level, final boolean optional) {
        final String indent = "    ";
        final String margin = indent.repeat(level);
        pom.append(margin).append("<dependency>\n");
        if (module != null) {
            pom.append(margin).append(indent).append("<!-- module ").append(module).append(" -->\n");
        }
        pom.append(margin).append(indent).append("<groupId>")   .append(group)  .append("</groupId>\n");
        pom.append(margin).append(indent).append("<artifactId>").append(name)   .append("</artifactId>\n");
        pom.append(margin).append(indent).append("<version>")   .append(version).append("</version>\n");
        if (optional) {
            pom.append(margin).append(indent).append("<optional>true</optional>\n");
        }
        pom.append(margin).append("</dependency>\n");
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return string representation of the {@code <dependency>} element.
     */
    @Override
    public String toString() {
        final var pom = new StringBuilder(100);
        writeAsMaven(pom, 0, false);
        return pom.toString();
    }

    /**
     * Compares this dependency with the given object for equality.
     *
     * @param  obj  the object to compare with this dependency.
     * @return whether the two object are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Dependency)) {
            return false;
        }
        final var other = (Dependency) obj;
        return Objects.equals(file,    other.file)    &&
               Objects.equals(group,   other.group)   &&
               Objects.equals(name,    other.name)    &&
               Objects.equals(version, other.version) &&
               Objects.equals(module,  other.module);
    }

    /**
     * {@return a hash code for this dependency}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(file, group, name, version, module);
    }
}
