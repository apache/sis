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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.opengis.annotation.UML;
import org.apache.sis.util.Classes;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.xml.Namespaces;


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
     * Sentinel value used in {@link #LEGACY_NAMESPACES} for meaning "all properties in that namespace".
     */
    @SuppressWarnings("unchecked")
    private static final Set<String> ALL = InfiniteSet.INSTANCE;

    /**
     * Classes or properties having a JAXB annotation in this namespace should be deprecated.
     * Deprecated namespaces are enumerated as keys. If the associated value is {@link #ALL},
     * the whole namespace is deprecated. If the value is not ALL, then only the enumerated
     * properties are deprecated.
     *
     * <p>Non-ALL values are rare. They happen in a few cases where a property is legacy despite its namespace.
     * Those "properties" are errors in the legacy ISO 19139:2007 schema; they were types without their property
     * wrappers. For example in {@code SV_CoupledResource}, {@code <gco:ScopedName>} was marshalled without its
     * {@code <srv:scopedName>} wrapper — note the upper and lower-case "s". Because {@code ScopedName} is a type,
     * we had to keep the namespace declared in {@link org.apache.sis.util.iso.DefaultScopedName}
     * (the replacement is performed by {@code org.apache.sis.xml.FilteredWriter}).
     * </p>
     */
    private static final Map<String, Set<String>> LEGACY_NAMESPACES;
    static {
        final Map<String, Set<String>> m = new HashMap<>(8);
        m.put(LegacyNamespaces.GMD, ALL);
        m.put(LegacyNamespaces.GMI, ALL);
        m.put(LegacyNamespaces.GMX, ALL);
        m.put(LegacyNamespaces.SRV, ALL);
        m.put(Namespaces.GCO, Collections.singleton("ScopedName"));     // Not to be confused with standard <srv:scopedName>
        LEGACY_NAMESPACES = Collections.unmodifiableMap(m);
    }

    /**
     * Types declared in JAXB annotations to be considered as equivalent to types in XML schemas.
     */
    private static final Map<String,String> TYPE_EQUIVALENCES;
    static {
        final Map<String,String> m = new HashMap<>();
        m.put("PT_FreeText",             "CharacterString");
        m.put("Abstract_Citation",       "CI_Citation");
        m.put("AbstractCI_Party",        "CI_Party");
        m.put("Abstract_Responsibility", "CI_Responsibility");
        m.put("Abstract_Extent",         "EX_Extent");
        TYPE_EQUIVALENCES = Collections.unmodifiableMap(m);
    }

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
    private final String packageNS;

    /**
     * The namespace of the class under examination.
     * This field must be updated for every class found in a package.
     */
    private String classNS;

    /**
     * The class under examination, used in error messages.
     * This field must be updated for every class found in a package.
     */
    private Class<?> currentClass;

    /**
     * Whether the class under examination is defined in a legacy namespace.
     * In such case, some checks may be skipped because we didn't loaded schemas for legacy properties.
     */
    private boolean isDeprecatedClass;

    /**
     * The schema definition for the class under examination.
     *
     * @see SchemaCompliance#typeDefinition(String)
     */
    private Map<String, SchemaCompliance.Info> properties;

    /**
     * Whether a namespace is actually used of not.
     * We use this map for identifying unnecessary prefix declarations.
     */
    private final Map<String,Boolean> namespaceIsUsed;

    /**
     * Whether adapters declared in {@code package-info.java} are used or not.
     */
    private final Map<Class<?>,Boolean> adapterIsUsed;

    /**
     * Creates a new verifier for the given package.
     */
    PackageVerifier(final SchemaCompliance schemas, final Package pkg)
            throws IOException, ParserConfigurationException, SAXException, SchemaException
    {
        this.schemas = schemas;
        namespaceIsUsed = new HashMap<>();
        adapterIsUsed = new HashMap<>();
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
                        throw new SchemaException(String.format("Prefix \"%s\" associated to two different namespaces:%n%s%n%s", pr, cr, ns));
                    }
                    if (namespaceIsUsed.put(ns, Boolean.FALSE) != null) {
                        throw new SchemaException(String.format("Duplicated namespace in package %s:%n%s", name, ns));
                    }
                }
            }
            /*
             * Lists the type of all values for which an adapter is declared in package-info.
             * If the type is not explicitely declared, then it is inferred from class signature.
             */
            final XmlJavaTypeAdapters adapters = pkg.getAnnotation(XmlJavaTypeAdapters.class);
            if (adapters != null) {
                for (final XmlJavaTypeAdapter adapter : adapters.value()) {
                    Class<?> propertyType = adapter.type();
                    if (propertyType == XmlJavaTypeAdapter.DEFAULT.class) {
                        for (Class<?> c = adapter.value(); ; c = c.getSuperclass()) {
                            final Type type = c.getGenericSuperclass();
                            if (type == null) {
                                throw new SchemaException(String.format(
                                        "Can not infer type for %s adapter.", adapter.value().getName()));
                            }
                            if (type instanceof ParameterizedType) {
                                final Type[] p = ((ParameterizedType) type).getActualTypeArguments();
                                if (p.length == 2) {
                                    Type pt = p[1];
                                    if (pt instanceof ParameterizedType) {
                                        pt = ((ParameterizedType) pt).getRawType();
                                    }
                                    if (pt instanceof Class<?>) {
                                        propertyType = (Class<?>) pt;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (adapterIsUsed.put((Class<?>) propertyType, Boolean.FALSE) != null) {
                        throw new SchemaException(String.format(
                                "More than one adapter for %s in package %s", propertyType, name));
                    }
                }
            }
        }
        packageName = name;
        packageNS = namespace;
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
        /*
         * Reinitialize fields to be updated for each class.
         */
        classNS           = null;
        currentClass      = type;
        isDeprecatedClass = false;
        properties        = Collections.emptyMap();

        final XmlType        xmlType = type.getDeclaredAnnotation(XmlType.class);
        final XmlRootElement xmlRoot = type.getDeclaredAnnotation(XmlRootElement.class);
        XmlElement codeList = null;
        /*
         * Get the type name and namespace from the @XmlType or @XmlRootElement annotations.
         * If both of them are present, verify that they are consistent (same namespace and
         * same name with "_Type" suffix in @XmlType). If the type name is not declared, we
         * assume that it is the same than the class name (this is what Apache SIS 0.8 does
         * in its org.apache.sis.internal.jaxb.code package for CodeList adapters).
         */
        final String isoName;       // ISO class name (not the same than Java class name).
        if (xmlRoot != null) {
            classNS = xmlRoot.namespace();
            isoName = xmlRoot.name();
            if (xmlType != null) {
                if (!classNS.equals(xmlType.namespace())) {
                    throw new SchemaException(errorInClassMember(null)
                            .append("Mismatched namespace in @XmlType and @XmlRootElement."));
                }
                SchemaCompliance.verifyNamingConvention(type.getName(), isoName, xmlType.name(), SchemaCompliance.TYPE_SUFFIX);
            }
        } else if (xmlType != null) {
            classNS = xmlType.namespace();
            final String name = xmlType.name();
            isoName = SchemaCompliance.trim(name, SchemaCompliance.TYPE_SUFFIX);
        } else {
            /*
             * If there is neither @XmlRootElement or @XmlType annotation, it may be a code list as implemented
             * in the org.apache.sis.internal.jaxb.code package. Those adapters have a single @XmlElement which
             * is to be interpreted as if it was the actual type.
             */
            for (final Method method : type.getDeclaredMethods()) {
                final XmlElement e = method.getDeclaredAnnotation(XmlElement.class);
                if (e != null) {
                    if (codeList != null) return;
                    codeList = e;
                }
            }
            if (codeList == null) return;
            classNS = codeList.namespace();
            isoName = codeList.name();
        }
        /*
         * Verify that the namespace declared on the class is not redundant with the namespace
         * declared in the package. Actually redundant namespaces are not wrong, but we try to
         * reduce code size.
         */
        if (classNS.equals(AnnotationConsistencyCheck.DEFAULT)) {
            classNS = packageNS;
        } else if (classNS.equals(packageNS)) {
            throw new SchemaException(errorInClassMember(null)
                    .append("Redundant namespace declaration: ").append(classNS));
        }
        /*
         * Verify that the namespace has a prefix associated to it in the package-info file.
         */
        if (namespaceIsUsed.put(classNS, Boolean.TRUE) == null) {
            throw new SchemaException(errorInClassMember(null)
                    .append("No prefix in package-info for ").append(classNS));
        }
        /*
         * Properties in the legacy GMD or GMI namespaces may be deprecated, depending if a replacement
         * is already available or not. However properties in other namespaces should not be deprecated.
         * Some validations of deprecated properties are skipped because we didn't loaded their schema.
         */
        isDeprecatedClass = (LEGACY_NAMESPACES.get(classNS) == ALL);
        if (!isDeprecatedClass) {
            if (type.isAnnotationPresent(Deprecated.class)) {
                throw new SchemaException(errorInClassMember(null)
                        .append("Unexpected @Deprecated annotation."));
            }
            /*
             * Verify that class name exists, then verify its namespace (associated to the null key by convention).
             */
            properties = schemas.typeDefinition(isoName);
            if (properties == null) {
                throw new SchemaException(errorInClassMember(null)
                        .append("Unknown name declared in @XmlRootElement: ").append(isoName));
            }
            final String expectedNS = properties.get(null).namespace;
            if (!classNS.equals(expectedNS)) {
                throw new SchemaException(errorInClassMember(null)
                        .append(isoName).append(" shall be associated to namespace ").append(expectedNS));
            }
            if (codeList != null) return;                   // If the class was a code list, we are done.
        }
        /*
         * At this point the classNS, className, isDeprecatedClass and properties field have been set.
         * We can now loop over the XML elements, which may be on fields or on methods (public or private).
         */
        for (final Field field : type.getDeclaredFields()) {
            Class<?> valueType = field.getType();
            final boolean isCollection = Collection.class.isAssignableFrom(valueType);
            if (isCollection) {
                valueType = Classes.boundOfParameterizedProperty(field);
            }
            verify(field, field.getName(), valueType, isCollection);
        }
        for (final Method method : type.getDeclaredMethods()) {
            Class<?> valueType = method.getReturnType();
            final boolean isCollection = Collection.class.isAssignableFrom(valueType);
            if (isCollection) {
                valueType = Classes.boundOfParameterizedProperty(method);
            }
            verify(method, method.getName(), valueType, isCollection);
        }
    }

    /**
     * Validate a field or a method against the expected schema.
     *
     * @param  property      the field or method to validate.
     * @param  javaName      the field name or method name in Java code.
     * @param  valueType     the field type or the method return type, or element type in case of collection.
     * @param  isCollection  whether the given value type is the element type of a collection.
     */
    private void verify(final AnnotatedElement property, final String javaName,
            final Class<?> valueType, final boolean isCollection) throws SchemaException
    {
        final XmlElement element = property.getDeclaredAnnotation(XmlElement.class);
        if (element == null) {
            return;                               // No @XmlElement annotation - skip this property.
        }
        String name = element.name();
        if (name.equals(AnnotationConsistencyCheck.DEFAULT)) {
            name = javaName;
        }
        String ns = element.namespace();
        if (ns.equals(AnnotationConsistencyCheck.DEFAULT)) {
            ns = classNS;
        }
        if (namespaceIsUsed.put(ns, Boolean.TRUE) == null) {
            throw new SchemaException(errorInClassMember(javaName)
                    .append("Missing @XmlNs for namespace ").append(ns));
        }
        /*
         * Remember that we need an adapter for this property, unless the method or field defines its own adapter.
         * In theory we do not need to report missing adapter since JAXB performs its own check, but we do anyway
         * because JAXB has default adapters for String, Double, Boolean, Date, etc. which do not match the way
         * OGC/ISO marshal those elements.
         */
        if (!property.isAnnotationPresent(XmlJavaTypeAdapter.class) && (valueType != null)
                && !valueType.getName().startsWith(Modules.CLASSNAME_PREFIX))
        {
            Class<?> c = valueType;
            while (adapterIsUsed.replace(c, Boolean.TRUE) == null) {
                final Class<?> parent = c.getSuperclass();
                if (parent != null) {
                    c = parent;
                } else {
                    final Class<?>[] p = c.getInterfaces();
                    if (p.length == 0) {
                        throw new SchemaException(errorInClassMember(javaName)
                                .append("Missing @XmlJavaTypeAdapter for ").append(valueType));
                    }
                    c = p[0];   // Take only the first interface, which should be the "main" parent.
                }
            }
        }
        /*
         * We do not verify fully the properties in legacy namespaces because we didn't loaded their schemas.
         * However we verify at least that those properties are not declared as required.
         */
        if (LEGACY_NAMESPACES.getOrDefault(ns, Collections.emptySet()).contains(name)) {
            if (!isDeprecatedClass && element.required()) {
                throw new SchemaException(errorInClassMember(javaName)
                        .append("Legacy property should not be required."));
            }
        } else {
            /*
             * Property in non-legacy namespaces should not be deprecated. Verify also their namespace
             * and whether the property is required or optional, and whether it should be a collection.
             */
            if (property.isAnnotationPresent(Deprecated.class)) {
                throw new SchemaException(errorInClassMember(javaName)
                        .append("Unexpected deprecation status."));
            }
            final SchemaCompliance.Info info = properties.get(name);
            if (info == null) {
                throw new SchemaException(errorInClassMember(javaName)
                        .append("Unexpected XML element: ").append(name));
            }
            if (info.namespace != null && !ns.equals(info.namespace)) {
                throw new SchemaException(errorInClassMember(javaName)
                        .append("Declared namespace: ").append(ns).append(System.lineSeparator())
                        .append("Expected namespace: ").append(info.namespace));
            }
            if (element.required() != info.isRequired) {
                throw new SchemaException(errorInClassMember(javaName)
                        .append("Expected @XmlElement(required = ").append(info.isRequired).append(')'));
            }
            /*
             * Following is a continuation of our check for cardinality, but also the beginning of the check
             * for return value type. The return type should be an interface with a UML annotation; we check
             * that this annotation contains the name of the expected type.
             */
            if (isCollection) {
                if (!info.isCollection) {
                    if (false)  // Temporarily disabled because require GeoAPI modifications.
                    throw new SchemaException(errorInClassMember(javaName).append("Value should be a singleton."));
                }
            } else if (info.isCollection) {
                if (false)  // Temporarily disabled because require GeoAPI modifications.
                throw new SchemaException(errorInClassMember(javaName).append("Value should be a collection."));
            }
            if (valueType != null) {
                final UML valueUML = valueType.getAnnotation(UML.class);
                if (valueUML != null) {
                    String expected = info.typeName;
                    String actual   = valueUML.identifier();
                    expected = TYPE_EQUIVALENCES.getOrDefault(expected, expected);
                    actual   = TYPE_EQUIVALENCES.getOrDefault(actual,   actual);
                    if (!expected.equals(actual)) {
                        if (false)  // Temporarily disabled because require GeoAPI modifications.
                        throw new SchemaException(errorInClassMember(javaName)
                                .append("Declared value type: ").append(actual).append(System.lineSeparator())
                                .append("Expected value type: ").append(expected));
                    }
                }
            }
            /*
             * Verify if we have a @XmlNs for the type of the value. This is probably not required, but we
             * do that as a safety. A common namespace added by this check is Metadata Common Classes (MCC).
             */
            final Map<String, SchemaCompliance.Info> valueInfo = schemas.typeDefinition(info.typeName);
            if (valueInfo != null) {
                final String valueNS = valueInfo.get(null).namespace;
                if (namespaceIsUsed.put(valueNS, Boolean.TRUE) == null) {
                    throw new SchemaException(errorInClassMember(javaName)
                            .append("Missing @XmlNs for property value namespace: ").append(valueNS));
                }
            }
        }
    }

    /**
     * Returns a message beginning with "Error in …", to be completed by the caller.
     * This is an helper method for exception messages.
     *
     * @param  name  the property name, or {@code null} if none.
     */
    private StringBuilder errorInClassMember(final String name) {
        final StringBuilder builder = new StringBuilder(80).append("Error in ");
        if (isDeprecatedClass) {
            builder.append("legacy ");
        }
        builder.append(currentClass.getCanonicalName());
        if (name != null) {
            builder.append('.').append(name);
        }
        return builder.append(':').append(System.lineSeparator());
    }

    /**
     * Verifies if there is any unused namespace or adapter in package-info file.
     */
    final void reportUnused() throws SchemaException {
        for (final Map.Entry<String,Boolean> entry : namespaceIsUsed.entrySet()) {
            if (!entry.getValue()) {
                throw new SchemaException(String.format("Unused namespace in package %s:%n%s", packageName, entry.getKey()));
            }
        }
        for (final Map.Entry<Class<?>,Boolean> entry : adapterIsUsed.entrySet()) {
            if (!entry.getValue()) {
                throw new SchemaException(String.format("Unused adapter in package %s for %s.", packageName, entry.getKey()));
            }
        }
    }
}
