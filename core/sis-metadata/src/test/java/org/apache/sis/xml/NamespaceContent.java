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
package org.apache.sis.xml;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import java.lang.reflect.Method;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.sis.test.xml.SchemaException;
import org.apache.sis.internal.jaxb.LegacyNamespaces;


/**
 * Creates the {@value TransformingReader#FILENAME} file. This class needs to be executed only when the content
 * has changed, or for verifying the current file. Output format contains namespaces first, then classes,
 * then properties. Example:
 *
 * {@preformat
 * http://standards.iso.org/iso/19115/-3/cit/1.0
 *   CI_Address
 *     administrativeArea
 *     city
 *   CI_Citation
 *     citedResponsibleParty
 * }
 */
public final class NamespaceContent {
    /**
     * Properties in those namespaces do not have older namespaces to map from.
     */
    private static final Set<String> LEGACY_NAMESPACES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(LegacyNamespaces.GMD, LegacyNamespaces.GMI, LegacyNamespaces.SRV)));

    /**
     * The {@value} string used in JAXB annotations for default names or namespaces.
     */
    private static final String DEFAULT = "##default";

    /**
     * Root directory from which to search for classes.
     */
    private final Path classRootDirectory;

    /**
     * The content to write. Keys in the first (outer) map are namespaces. Keys in the enclosed maps
     * are class names. Keys in the enclosed set are property names.
     */
    private final Map<String, Map<String, Set<String>>> content;

    /**
     * Creates a new {@value TransformingReader#FILENAME} generator for classes under the given directory.
     * The given directory shall be the root of {@code "*.class"} files.
     *
     * @param  classRootDirectory   the root of compiled class files.
     */
    public NamespaceContent(final Path classRootDirectory) {
        this.classRootDirectory = classRootDirectory;
        content = new TreeMap<>();
    }

    /**
     * Gets the namespaces, types and properties for all class files in the given directory and sub-directories.
     * Those information are memorized for future listing with {@link #print(Appendable)}.
     *
     * @param  directory  the directory to scan for classes, relative to class root directory.
     * @throws IOException if an error occurred while reading files or schemas.
     * @throws ClassNotFoundException if an error occurred while loading a {@code "*.class"} file.
     * @throws SchemaException if two properties have the same name in the same class and namespace.
     */
    public void add(final Path directory) throws IOException, ClassNotFoundException, SchemaException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(classRootDirectory.resolve(directory))) {
            for (Path path : stream) {
                final String filename = path.getFileName().toString();
                if (!filename.startsWith(".")) {
                    if (Files.isDirectory(path)) {
                        add(path);
                    } else if (filename.endsWith(".class")) {
                        path = classRootDirectory.relativize(path);
                        String classname = path.toString();
                        classname = classname.substring(0, classname.length() - 6).replace('/', '.');
                        add(Class.forName(classname));
                    }
                }
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
    }

    /**
     * Gets the namespaces, types and properties for the given class.
     * Properties defined in super-classes will be copied as if they were declared in-line.
     * Those information are memorized for future listing with {@link #print(Appendable)}.
     *
     * @throws SchemaException if two properties have the same name in the same class and namespace.
     */
    private void add(Class<?> classe) throws SchemaException {
        XmlRootElement root = classe.getDeclaredAnnotation(XmlRootElement.class);
        if (root != null) {
            /*
             * Add the following entry:
             *
             *     http://a.namespace
             *      PX_AClass
             *       <type>
             *
             * Then list all properties below "PX_AClass". Note that the namespace may change because properties
             * may be declared in different namespaces, but the class name stay the same. If the same properties
             * are inherited by many classes, they will be repeated in each subclass.
             */
            final String topLevelTypeName = root.name();
            String classNS = namespace(classe, root.namespace());
            add(classNS, topLevelTypeName, TransformingReader.TYPE_KEY);
            for (;; classNS = namespace(classe, root.namespace())) {
                for (final Method method : classe.getDeclaredMethods()) {
                    if (!method.isBridge()) {
                        final XmlElement xe = method.getDeclaredAnnotation(XmlElement.class);
                        if (xe != null) {
                            String namespace = xe.namespace();
                            if (namespace.equals(DEFAULT)) {
                                namespace = classNS;
                            }
                            add(namespace, topLevelTypeName, xe.name());
                        }
                    }
                }
                classe = classe.getSuperclass();
                root = classe.getDeclaredAnnotation(XmlRootElement.class);
                if (root == null) break;
            }
        } else {
            /*
             * In Apache SIS implementation, classes without JAXB annotation except on a single method are
             * code lists or enumerations. Those classes have exactly one method annotated with @XmlElement,
             * and that method actually gives a type, not a property (because of the way OGC/ISO wrap every
             * properties in a type).
             */
            XmlElement singleton = null;
            for (final Method method : classe.getDeclaredMethods()) {
                final XmlElement xe = method.getDeclaredAnnotation(XmlElement.class);
                if (xe != null) {
                    if (singleton != null) return;
                    singleton = xe;
                }
            }
            if (singleton != null) {
                add(namespace(classe, singleton.namespace()), singleton.name(), TransformingReader.TYPE_KEY);
            }
        }
    }

    private static String namespace(final Class<?> classe, String classNS) {
        if (classNS.equals(DEFAULT)) {
            classNS = classe.getPackage().getDeclaredAnnotation(XmlSchema.class).namespace();
        }
        return classNS;
    }

    /**
     * Adds a property in the given class in the given namespace.
     */
    private void add(final String namespace, final String typeName, final String property) throws SchemaException {
        if (!LEGACY_NAMESPACES.contains(namespace)) {
            if (!content.computeIfAbsent(namespace, (k) -> new TreeMap<>())
                        .computeIfAbsent(typeName,  (k) -> new TreeSet<>())
                        .add(property))
            {
                if (typeName.equals("Integer")) return;     // Exception because of GO_Integer and GO_Integer64.
                throw new SchemaException(String.format("Duplicated property %s.%s in:%n%s", typeName, property, namespace));
            }
        }
    }

    /**
     * Prints the {@value TransformingReader#FILENAME} file.
     *
     * @param  out  where to print the content.
     * @throws IOException if an error occurred while printing the content.
     */
    public void print(final Appendable out) throws IOException {
        for (final Map.Entry<String, Map<String, Set<String>>> e : content.entrySet()) {
            out.append(e.getKey()).append('\n');                                            // Namespace
            for (final Map.Entry<String, Set<String>> c : e.getValue().entrySet()) {
                out.append(' ').append(c.getKey()).append('\n');                            // Class
                for (final String p : c.getValue()) {
                    out.append("  ").append(p).append('\n');                                // Property
                }
            }
        }
    }
}
