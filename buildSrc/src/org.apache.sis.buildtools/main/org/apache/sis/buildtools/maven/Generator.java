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
package org.apache.sis.buildtools.maven;

import java.util.Map;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.lang.module.ModuleDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenArtifactSet;
import org.gradle.api.publish.maven.MavenPublication;
import org.apache.sis.buildtools.gradle.Dependency;
import org.apache.sis.buildtools.gradle.SISBuildException;


/**
 * Generates the {@code pom.xml} file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public final class Generator {
    /**
     * The template to use for generating the {@code pom.xml} file.
     * Properties such as {@code ${sis.foo}} are enumerated in {@link Element}.
     */
    private static final String TEMPLATE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!--
            Licensed to the Apache Software Foundation (ASF) under one
            or more contributor license agreements.  See the NOTICE file
            distributed with this work for additional information
            regarding copyright ownership.  The ASF licenses this file
            to you under the Apache License, Version 2.0 (the
            "License"); you may not use this file except in compliance
            with the License.  You may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

            Unless required by applicable law or agreed to in writing,
            software distributed under the License is distributed on an
            "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
            KIND, either express or implied.  See the License for the
            specific language governing permissions and limitations
            under the License.
        -->

        <project xmlns              = "http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi          = "http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation = "http://maven.apache.org/POM/4.0.0
                                       http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>org.apache.sis</groupId>
                <artifactId>parent</artifactId>
                <version>${sis.version}</version>
            </parent>

            <groupId>${sis.groupId}</groupId>
            <artifactId>${sis.artifactId}</artifactId>
            <name>${sis.name}</name>
            <description>
                ${sis.description}
            </description>

            <dependencies>
            </dependencies>
        </project>
        """;

    /**
     * The {@code pom.xml} file content.
     */
    private final StringBuilder pom;

    /**
     * Information (such as Maven coordinates) of the artifact to publish.
     * Those information are parsed from the original {@code pom.xml} file.
     */
    private final EnumMap<Element,String> info;

    /**
     * The required dependencies as JPML module name.
     * Values are whether the dependency is optional.
     */
    private final Map<String,Boolean> requires;

    /**
     * Creates a new generator which will read and rewrite in the given buffer.
     *
     * @param  pom  the original {@code pom.xml} content to read, and the destination where the rewrite it.
     */
    private Generator(final StringBuilder pom) {
        this.pom = pom;
        info = new EnumMap<>(Element.class);
        requires = new LinkedHashMap<>();
    }

    /**
     * Parses the original {@code pom.xml} content.
     * This content is minimalist, with no copyright header, no parent
     * and no dependencies (in the way that we specified the artifact).
     * The main things that we need to collect are Maven coordinates.
     */
    private void parseOriginalPOM() {
        for (final Element element : Element.values()) {
            int start = pom.indexOf(element.tag);
            if (start >= 0) {
                start += element.tag.length();
                if (pom.indexOf(element.tag, start) >= 0) {
                    throw new SISBuildException("Duplicated tag: " + element.tag);
                }
                final int end = pom.indexOf("<", start);
                if (end < 0) {
                    throw new SISBuildException("Unclosed tag: " + element.tag);
                }
                info.put(element, pom.substring(start, end).trim());
            }
        }
    }

    /**
     * Overwrites the {@code pom.xml} with a new content derived from the template.
     * The Maven coordinates will be written using the information found by {@link #parseOriginalPOM()}.
     * Dependencies are not included, they must be added by {@link #addDependencies(Dependency[])}.
     */
    private void rewritePOM() {
        pom.setLength(0);
        pom.append(TEMPLATE);
        for (final Element element : Element.values()) {
            int start = pom.length();
            while ((start = pom.lastIndexOf(element.property, start)) >= 0) {
                final int end = start + element.property.length();
                final String value = info.get(element);
                if (value == null) {
                    throw new SISBuildException("Tag not found: " + element.tag);
                }
                pom.replace(start, end, value);
            }
        }
        final int start = pom.indexOf("${");
        if (start >= 0) {
            int end = pom.indexOf("}", start) + 1;
            if (end <= 0) end = pom.length();
            throw new SISBuildException("Unknown property: " + pom.substring(start, end));
        }
    }

    /**
     * Adds the given dependencies at the end of the {@code pom.xml}.
     * An initial POM must have been written by {@link #rewritePOM()}.
     * Dependencies must have been collected by {@link #findUsedDependencies(MavenArtifactSet)} first.
     *
     * @param  dependencies  the project dependencies to add.
     */
    private void addDependencies(final Map<String,Dependency> dependencies) {
        final int start = pom.lastIndexOf("\n", pom.lastIndexOf("</dependencies>")) + 1;
        if (start <= 0) {
            throw new SISBuildException("Bad pom.xml template.");
        }
        final String suffix = pom.substring(start);
        pom.setLength(start);
        for (final Map.Entry<String,Boolean> entry : requires.entrySet()) {
            final Dependency dep = dependencies.get(entry.getKey());
            if (dep != null) {
                dep.writeAsMaven(pom, 2, entry.getValue());
            }
        }
        pom.append(suffix);
    }

    /**
     * Collects the dependencies declared in {@code module-info.class} for the given artifacts.
     * Artifact without {@code module-info.class} are ignored. Duplicated values are ignored.
     * Dependencies are added in the {@link #requires} map.
     *
     * @param  artifacts  the artifacts for which to collect dependencies.
     * @throws IOException if an error occurred while reading {@code module-info.class}.
     */
    private void findUsedDependencies(final MavenArtifactSet artifacts) throws IOException {
        for (final MavenArtifact artifact : artifacts) {
            try (JarFile zip = new JarFile(artifact.getFile(), false, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
                final ZipEntry entry = zip.getEntry("module-info.class");
                if (entry != null) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        final ModuleDescriptor module = ModuleDescriptor.read(in);
                        for (final ModuleDescriptor.Requires r : module.requires()) {
                            requires.put(r.name(), r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC));
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads and rewrites the {@code pom.xml} file content.
     *
     * @param  project      the project to be published.
     * @param  subProjects  the sub-projects, which also need to be included as dependencies across JPMS modules.
     * @param  pub          configuration of the artifact to publish in Maven format.
     * @param  xml          provider of the original content, and where to write the result.
     */
    public static void rewrite(final Project project, final Collection<Dependency> subProjects, final MavenPublication pub, final XmlProvider xml) {
        final var g = new Generator(xml.asString());
        g.parseOriginalPOM();
        g.rewritePOM();
        try {
            g.findUsedDependencies(pub.getArtifacts());
            g.addDependencies(Dependency.jpms(project, subProjects));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
