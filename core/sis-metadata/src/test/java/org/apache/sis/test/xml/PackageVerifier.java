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
package org.apache.sis.test.xml;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.apache.sis.internal.jaxb.LegacyNamespaces;


/**
 * Verify JAXB annotations in a single package. A new instance of this class is created by
 * {@link SchemaCompliance#verify(java.nio.file.Path)} for each Java package to be verified.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final strictfp class PackageVerifier {
    /**
     * Classes or properties having a JAXB annotation in this namespace should be deprecated.
     */
    private static final Set<String> LEGACY_NAMESPACES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(LegacyNamespaces.GMD, LegacyNamespaces.GMI, LegacyNamespaces.SRV)));

    /**
     * The schemas to compare with the JAXB annotations.
     * Additional schemas will be loaded as needed.
     */
    private final SchemaCompliance schemas;

    /**
     * The package name, for reporting error.
     */
    private final String packageName;

    /**
     * The default namespace to use if a class does not define explicitely a namespace.
     */
    private final String defaultNS;

    /**
     * Whether a namespace is actually used of not.
     * We use this map for identifying unnecessary prefix declarations.
     */
    private final Map<String,Boolean> namespaceIsUsed;

    /**
     * Creates a new verifier for the given package.
     */
    PackageVerifier(final SchemaCompliance schemas, final Package pkg)
            throws IOException, ParserConfigurationException, SAXException, SchemaException
    {
        this.schemas = schemas;
        namespaceIsUsed = new HashMap<>();
        String name = "?", namespace = "";
        if (pkg != null) {
            name = pkg.getName();
            final XmlSchema schema = pkg.getAnnotation(XmlSchema.class);
            if (schema != null) {
                namespace = schema.namespace();
                String location = schema.location();
                if (!XmlSchema.NO_LOCATION.equals(location)) {
                    if (!location.startsWith(schema.namespace())) {
                        throw new SchemaException("XML schema location inconsistent with namespace in package " + name);
                    }
                    schemas.loadSchema(location);
                }
                for (final XmlNs xmlns : schema.xmlns()) {
                    final String pr = xmlns.prefix();
                    final String ns = xmlns.namespaceURI();
                    final String cr = schemas.allXmlNS.put(pr, ns);
                    if (cr != null && !cr.equals(ns)) {
                        throw new SchemaException("Prefix \"" + pr + "\" associated to two different namespaces:\n" + cr + '\n' + ns);
                    }
                    if (namespaceIsUsed.put(ns, Boolean.FALSE) != null) {
                        throw new SchemaException("Duplicated namespace in package " + name + ":\n" + ns);
                    }
                }
            }
        }
        packageName = name;
        defaultNS = namespace;
    }

    /**
     * Verifies {@code @XmlType} and {@code @XmlRootElement} on the class. This method verifies naming convention
     * (type name should be same as root element name with {@value SchemaCompliance#TYPE_SUFFIX} suffix appended),
     * ensures that the name exists in the schema, and checks the namespace.
     *
     * @param  type  the class on which to verify annotations.
     */
    final void verify(final Class<?> type)
            throws IOException, ParserConfigurationException, SAXException, SchemaException
    {
        final XmlType        xmlType = type.getDeclaredAnnotation(XmlType.class);
        final XmlRootElement xmlRoot = type.getDeclaredAnnotation(XmlRootElement.class);
        if (xmlRoot == null && xmlType == null) {
            return;
        }
        /*
         * Get the type name and namespace from the @XmlType or @XmlRootElement annotations.
         * If both of them are present, verify that they are consistent (same namespace and
         * same name with "_Type" suffix in @XmlType). If the type name is not declared, we
         * assume that it is the same than the class name (this is what Apache SIS 0.8 does
         * in its org.apache.sis.internal.jaxb.code package for CodeList adapters).
         */
        String namespace;
        final String className;     // ISO class name (not the same than Java class name).
        if (xmlRoot != null) {
            namespace = xmlRoot.namespace();
            className = xmlRoot.name();
            if (xmlType != null) {
                if (!namespace.equals(xmlType.namespace())) {
                    throw new SchemaException("Mismatched namespace in @XmlType and @XmlRootElement of " + type);
                }
                SchemaCompliance.verifyNamingConvention(type.getName(), className, xmlType.name(), SchemaCompliance.TYPE_SUFFIX);
            }
        } else {
            namespace = xmlType.namespace();
            final String name = xmlType.name();
            className = name.equals(AnnotationConsistencyCheck.DEFAULT)
                        ? type.getSimpleName() : SchemaCompliance.trim(name, SchemaCompliance.TYPE_SUFFIX);
        }
        /*
         * Verify that the namespace declared on the class is not redundant with the namespace
         * declared in the package. Actually redundant namespaces are not wrong, but we try to
         * reduce code size.
         */
        if (namespace.equals(AnnotationConsistencyCheck.DEFAULT)) {
            namespace = defaultNS;
        } else if (namespace.equals(defaultNS)) {
            throw new SchemaException("Redundant namespace declaration in " + type);
        }
        /*
         * Verify that the namespace has a prefix associated to it in the package-info file.
         */
        if (namespaceIsUsed.put(namespace, Boolean.TRUE) == null) {
            throw new SchemaException("Namespace of " + type + " has no prefix in package-info.");
        }
        /*
         * Properties in the legacy GMD or GMI namespaces may be deprecated, depending if a replacement
         * is already available or not. However properties in other namespaces should not be deprecated.
         * Validation of deprecated properties is skipped because we didn't loaded their schema.
         */
        if (LEGACY_NAMESPACES.contains(namespace)) {
            return;
        }
        if (type.isAnnotationPresent(Deprecated.class)) {
            throw new SchemaException("Unexpected deprecation status of " + type);
        }
        /*
         * Verify that class name exists, then verify its namespace (associated to the null key by convention).
         */
        final Map<String, SchemaCompliance.Info> properties = schemas.typeDefinition(className);
        if (properties == null) {
            throw new SchemaException("Unknown name declared in @XmlRootElement of " + type);
        }
        final String expectedNS = properties.get(null).namespace;
        if (!namespace.equals(expectedNS)) {
            throw new SchemaException(className + " shall be associated to namespace " + expectedNS);
        }
        for (final Method method : type.getDeclaredMethods()) {
            final XmlElement element = method.getDeclaredAnnotation(XmlElement.class);
            if (element == null) {
                continue;                               // No @XmlElement annotation - skip this property.
            }
            final String name = element.name();
            String ns = element.namespace();
            if (ns.equals(AnnotationConsistencyCheck.DEFAULT)) {
                ns = namespace;
            }
            /*
             * We do not verify fully the properties in legacy namespaces because we didn't loaded their schemas.
             * However we verify at least that those properties are not declared as required.
             */
            if (LEGACY_NAMESPACES.contains(ns)) {
                if (element.required()) {
                    throw new SchemaException("Legacy property " + className + '.' + name + " should not be required.");
                }
                continue;                               // Property in a legacy namespace - skip it.
            }
            /*
             * Property in non-legacy namespaces should not be deprecated. Verify also their namespace.
             */
            if (method.isAnnotationPresent(Deprecated.class)) {
                throw new SchemaException("Unexpected deprecation status of " + className + '.' + name);
            }
            final SchemaCompliance.Info info = properties.get(name);
            if (info == null) {
                throw new SchemaException("Unexpected XML element " + className + '.' + name);
            }
            if (info.namespace != null && !ns.equals(info.namespace)) {
                throw new SchemaException(className + '.' + name + " is associated to namespace " + ns
                        + " while " + info.namespace + " was expected.");
            }
            if (element.required() != info.isRequired) {
                throw new SchemaException("Wrong requirement flag for " + className + '.' + name);
            }
        }
    }

    /**
     * Verifies if there is any unused namespace or adapters in package-info file.
     */
    final void reportUnused() throws SchemaException {
        for (final Map.Entry<String,Boolean> entry : namespaceIsUsed.entrySet()) {
            if (!entry.getValue()) {
//              TODO: to be enabled after we processed properties.
//              throw new SchemaException("Unused namespace in package " + packageName + ":\n" + entry.getKey());
            }
        }
    }
}
