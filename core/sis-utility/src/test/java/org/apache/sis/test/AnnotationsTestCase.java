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
package org.apache.sis.test;

import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.CodeList;
import org.opengis.annotation.UML;
import org.opengis.annotation.Obligation;
import org.opengis.annotation.Specification;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.xml.Namespaces;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Base class for validations of {@link UML}, {@link XmlElement} and other annotations.
 * Some tests performed by this class are:
 *
 * <ul>
 *   <li>All implementation classes have {@link XmlRootElement} and {@link XmlType} annotations.</li>
 *   <li>The name declared in the {@code XmlType} annotations matches the
 *       {@link #getExpectedXmlTypeForElement expected value}.</li>
 *   <li>The name declared in the {@code XmlRootElement} (classes) or {@link XmlElement} (methods)
 *       annotations matches the identifier declared in the {@link UML} annotation of the GeoAPI interfaces.
 *       The UML - XML name mapping can be changed by overriding {@link #getExpectedXmlElementName(UML)} and
 *       {@link #getExpectedXmlRootElementName(UML)}.</li>
 *   <li>The {@code XmlElement.required()} boolean is consistent with the UML {@linkplain Obligation obligation}.</li>
 *   <li>The namespace declared in the {@code XmlRootElement} or {@code XmlElement} annotations
 *       is not redundant with the {@link XmlSchema} annotation in the package.</li>
 *   <li>The prefixes declared in the {@link XmlNs} annotations match the
 *       {@linkplain Namespaces#getPreferredPrefix expected prefixes}.</li>
 *   <li>The {@linkplain #getWrapperFor wrapper}, if any, is consistent.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public abstract strictfp class AnnotationsTestCase extends TestCase {
    /**
     * The {@value} string used in JAXB annotations for default names or namespaces.
     */
    private static final String DEFAULT = "##default";

    /**
     * The GeoAPI interfaces, {@link CodeList} or {@link Enum} types to test.
     */
    protected final Class<?>[] types;

    /**
     * The type being tested, or {@code null} if none. In case of test failure, this information
     * will be used by {@link #printFailureLocation()} for formatting a message giving the name
     * of class and method where the failure occurred.
     */
    protected String testingClass;

    /**
     * The method being tested, or {@code null} if none. In case of test failure, this information
     * will be used by {@link #printFailureLocation()} for formatting a message giving the name of
     * class and method where the failure occurred.
     */
    protected String testingMethod;

    /**
     * Creates a new test suite for the given types.
     *
     * @param types The GeoAPI interfaces, {@link CodeList} or {@link Enum} types to test.
     */
    protected AnnotationsTestCase(final Class<?>... types) {
        this.types = types;
    }

    /**
     * Returns the SIS implementation class for the given GeoAPI interface.
     * For example the implementation of the {@link org.opengis.metadata.citation.Citation}
     * interface is the {@link org.apache.sis.metadata.iso.citation.DefaultCitation} class.
     *
     * @param  <T>  The type represented by the {@code type} argument.
     * @param  type The GeoAPI interface (never a {@link CodeList} or {@link Enum} type).
     * @return The SIS implementation for the given interface.
     */
    protected abstract <T> Class<? extends T> getImplementation(Class<T> type);

    /**
     * If the given GeoAPI type, when marshalled to XML, is wrapped into an other XML element,
     * returns the class of the wrapper for that XML element. Otherwise returns {@code null}.
     * Such wrappers are unusual in XML (except for lists), but the ISO 19139 standard do that
     * systematically for every elements.
     *
     * <p><b>Example:</b> when a {@link org.apache.sis.metadata.iso.citation.DefaultContact}
     * is marshalled to XML inside a {@code ResponsibleParty}, the element is not marshalled
     * directly inside its parent as we usually do in XML. Instead, we have a {@code <CI_Contact>}.
     * inside the {@code <contactInfo>} element as below:</p>
     *
     * {@preformat xml
     *   <CI_ResponsibleParty>
     *     <contactInfo>
     *       <CI_Contact>
     *         ...
     *       </CI_Contact>
     *     </contactInfo>
     *   </CI_ResponsibleParty>
     * }
     *
     * To reflect that fact, this method shall return the internal {@code CI_Contact}
     * wrapper class for the {@link org.apache.sis.metadata.iso.citation.DefaultCitation} argument.
     * If no wrapper is expected for the given class, then this method shall return {@code null}.
     *
     * <p>If a wrapper is expected for the given class but was not found, then this method shall throw
     * {@link ClassNotFoundException}. Note that no wrapper may be defined explicitely for the given type,
     * while a wrapper is defined for a parent of the given type. This method does not need to care about
     * such situation, since the caller will automatically searches for a parent class if
     * {@code ClassNotFoundException} has been thrown.</p>
     *
     * <p>In SIS implementation, most wrappers are also {@link javax.xml.bind.annotation.adapters.XmlAdapter}.
     * But this is not a requirement.</p>
     *
     * @param  type The GeoAPI interface, {@link CodeList} or {@link Enum} type.
     * @return The wrapper for the given type, or {@code null} if none.
     * @throws ClassNotFoundException If a wrapper was expected but not found.
     */
    protected abstract Class<?> getWrapperFor(Class<?> type) throws ClassNotFoundException;

    /**
     * The value returned by {@link AnnotationsTestCase#getWrapperFor(Class)}, together with
     * a boolean telling whether the wrapper has been found in the tested class or in one
     * of its parent classes.
     */
    private static final class WrapperClass {
        final Class<?> type;
        boolean isInherited;

        WrapperClass(final Class<?> type) {
            this.type = type;
        }
    }

    /**
     * Returns the value of {@link #getWrapperFor(Class)} for the given class, or for a parent
     * of the given class if {@code getWrapperFor(Class)} threw {@code ClassNotFoundException}.
     *
     * @param  type The GeoAPI interface, {@link CodeList} or {@link Enum} type.
     * @return The wrapper for the given type. {@link WrapperClass#type} is {@code null} if
     *         no wrapper has been found.
     * @throws ClassNotFoundException If a wrapper was expected but not found in the
     *         given type neither in any of the parent classes.
     */
    private WrapperClass getWrapperInHierarchy(final Class<?> type) throws ClassNotFoundException {
        try {
            return new WrapperClass(getWrapperFor(type));
        } catch (ClassNotFoundException e) {
            for (final Class<?> parent : type.getInterfaces()) {
                if (ArraysExt.containsIdentity(types, parent)) try {
                    final WrapperClass wrapper = getWrapperInHierarchy(parent);
                    wrapper.isInherited = true;
                    return wrapper;
                } catch (ClassNotFoundException e2) {
                    // JDK7 branch does: e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    /**
     * Returns the XML type for an element of the given type. For example in ISO 19139,
     * the XML type of {@code CI_Citation} is {@code CI_Citation_Type}.
     *
     * @param  type The GeoAPI interface.
     * @param  impl The implementation class.
     * @return The name of the XML type for the given element, or {@code null} if none.
     *
     * @see #testImplementationAnnotations()
     */
    protected abstract String getExpectedXmlTypeForElement(Class<?> type, Class<?> impl);

    /**
     * Returns the expected namespace for an element defined by the given specification.
     * For example the namespace of any type defined by {@link Specification#ISO_19115}
     * is {@code "http://www.isotc211.org/2005/gmd"}.
     *
     * <p>The default implementation recognizes the
     * {@linkplain Specification#ISO_19115 ISO 19115},
     * {@linkplain Specification#ISO_19115_2 ISO 19115-2},
     * {@linkplain Specification#ISO_19139 ISO 19139} and
     * {@linkplain Specification#ISO_19108 ISO 19108} specifications.
     * Subclasses shall override this method if they need to support more namespaces.</p>
     *
     * <p>The prefix for the given namespace will be fetched by
     * {@link Namespaces#getPreferredPrefix(String, String)}.</p>
     *
     * @param  impl The implementation class, {@link CodeList} or {@link Enum} type.
     * @param  specification The specification that define the type, or {@code null} if unspecified.
     * @return The expected namespace.
     * @throws IllegalArgumentException If the given specification is unknown to this method.
     */
    protected String getExpectedNamespace(final Class<?> impl, final Specification specification) {
        switch (specification) {
            case ISO_19115:   return Namespaces.GMD;
            case ISO_19115_2: return Namespaces.GMI;
            case ISO_19139:   return Namespaces.GMX;
            case ISO_19108:   return Namespaces.GMD;
            default: throw new IllegalArgumentException(specification.toString());
        }
    }

    /**
     * Returns the name of the XML element for the given UML element.
     * This method is invoked in two situations:
     *
     * <ul>
     *   <li>For the root XML element name of an interface, in which case {@code enclosing} is {@code null}.</li>
     *   <li>For the XML element name of a property (field or method) defined by an interface,
     *       in which case {@code enclosing} is the interface containing the property.</li>
     * </ul>
     *
     * The default implementation returns {@link UML#identifier()}. Subclasses shall override this method
     * when mismatches are known to exist between the UML and XML element names.
     *
     * @param  enclosing The GeoAPI interface which contains the property, or {@code null} if none.
     * @param  uml The UML element for which to get the corresponding XML element name.
     * @return The XML element name for the given UML element.
     */
    protected String getExpectedXmlElementName(final Class<?> enclosing, final UML uml) {
        return uml.identifier();
    }

    /**
     * Replaces {@value #DEFAULT} value by the {@link XmlSchema} namespace if needed,
     * then performs validity check on the resulting namespace. This method checks that:
     *
     * <ul>
     *   <li>The namespace is not redundant with the package-level {@link XmlSchema} namespace.</li>
     *   <li>The namespace is declared in a package-level {@link XmlNs} annotation.</li>
     *   <li>The namespace is equals to the {@linkplain #getExpectedNamespace expected namespace}.</li>
     * </ul>
     *
     * @param  namespace The namespace given by the {@code @XmlRootElement} or {@code @XmlElement} annotation.
     * @param  impl      The implementation or wrapper class for which to get the package namespace.
     * @param  uml       The {@code @UML} annotation, or {@code null} if none.
     * @return The actual namespace (same as {@code namespace} if it was not {@value #DEFAULT}).
     */
    private String assertExpectedNamespace(String namespace, final Class<?> impl, final UML uml) {
        assertNotNull("Missing namespace.", namespace);
        assertFalse("Missing namespace.", namespace.trim().isEmpty());
        /*
         * Get the namespace declared at the package level, and ensure the the
         * given namespace is not redundant with that package-level namespace.
         */
        final XmlSchema schema = impl.getPackage().getAnnotation(XmlSchema.class);
        assertNotNull("Missing @XmlSchema package annotation.", schema);
        final String schemaNamespace = schema.namespace();
        assertFalse("Missing namespace in @XmlSchema package annotation.", schemaNamespace.trim().isEmpty());
        assertFalse("Namespace declaration is redundant with @XmlSchema.", namespace.equals(schemaNamespace));
        /*
         * Check that the namespace is declared in the package-level @XmlNs annotation.
         * We do not verify the validity of those @XmlNs annotations, since this is the
         * purpose of the 'testPackageAnnotations()' method.
         */
        if (!DEFAULT.equals(namespace)) {
            boolean found = false;
            for (final XmlNs ns : schema.xmlns()) {
                if (namespace.equals(ns.namespaceURI())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Namespace for " + impl + " is not declared in the package @XmlSchema.xmlns().");
            }
        } else {
            namespace = schemaNamespace;
        }
        if (uml != null) {
            assertEquals("Wrong namespace for the ISO specification.",
                    getExpectedNamespace(impl, uml.specification()), namespace);
        }
        return namespace;
    }

    /**
     * Returns the namespace declared in the {@link XmlSchema} annotation of the given package,
     * or {@code null} if none.
     *
     * @param p The package, or {@code null}.
     * @return The namespace, or {@code null} if none.
     */
    private static String getNamespace(final Package p) {
        if (p != null) {
            final XmlSchema schema = p.getAnnotation(XmlSchema.class);
            if (schema != null) {
                final String namespace = schema.namespace().trim();
                if (!namespace.isEmpty() && !DEFAULT.equals(namespace)) {
                    return namespace;
                }
            }
        }
        return null;
    }

    /**
     * Returns the namespace declared in the {@link XmlRootElement} annotation of the given class,
     * or the package annotation if none is found in the class.
     *
     * @param  impl The implementation class, or {@code null}.
     * @return The namespace, or {@code null} if none.
     */
    private static String getNamespace(final Class<?> impl) {
        if (impl == null) {
            return null;
        }
        final XmlRootElement root = impl.getAnnotation(XmlRootElement.class);
        if (root != null) {
            final String namespace = root.namespace().trim();
            if (!namespace.isEmpty() && !DEFAULT.equals(namespace)) {
                return namespace;
            }
        }
        return getNamespace(impl.getPackage());
    }

    /**
     * Gets the {@link XmlElement} annotation for the no-argument method of the given name
     * in the given implementation class. If the method is not annotated, then fallback on
     * a field having the same name than the UML identifier. If no such field is found or
     * is annotated, returns {@code null}.
     *
     * @param  impl   The implementation class.
     * @param  method The name of the getter method to search for.
     * @param  uml    The UML annotation on the GeoAPI interface, or {@code null} if none.
     * @return The {@code XmlElement}, or {@code null} if none.
     */
    private static XmlElement getXmlElement(final Class<?> impl, final String method, final UML uml) {
        XmlElement element = null;
        try {
            element = impl.getMethod(method, (Class<?>[]) null).getAnnotation(XmlElement.class);
            if (element == null && uml != null) {
                element = impl.getDeclaredField(uml.identifier()).getAnnotation(XmlElement.class);
            }
        } catch (NoSuchMethodException ex) {
            fail("Missing implementation: " + ex);
        } catch (NoSuchFieldException ex) {
            // Ignore - we will consider that there is no annotation.
        }
        return element;
    }

    /**
     * Returns {@code true} if the given method should be ignored.
     * This method returns {@code true} for some standard methods from the JDK.
     */
    private static boolean isIgnored(final Method method) {
        final String name = method.getName();
        return name.equals("equals") || name.equals("hashCode") || name.equals("doubleValue");
    }

    /**
     * Returns {@code true} if the given method is a non-standard extension.
     * If {@code true}, then {@code method} does not need to have UML annotation.
     *
     * @param method The method to verify.
     * @return {@code true} if the given method is an extension, or {@code false} otherwise.
     *
     * @since 0.5
     */
    protected boolean isExtension(final Method method) {
        return false;
    }

    /**
     * Tests the annotations on every GeoAPI interfaces and code lists in the {@link #types} array.
     * More specifically this method tests that:
     *
     * <ul>
     *   <li>All elements in {@link #types} except code lists are interfaces.</li>
     *   <li>All elements in {@code types} have a {@link UML} annotation.</li>
     *   <li>All methods expect deprecated methods and methods overriding JDK methods
     *       have a {@link UML} annotation.</li>
     * </ul>
     */
    @Test
    public void testInterfaceAnnotations() {
        for (final Class<?> type : types) {
            testingMethod = null;
            testingClass = type.getCanonicalName();
            UML uml = type.getAnnotation(UML.class);
            assertNotNull("Missing @UML annotation.", uml);
            if (!CodeList.class.isAssignableFrom(type)) {
                for (final Method method : type.getDeclaredMethods()) {
                    testingMethod = method.getName();
                    if (!isIgnored(method) && !isExtension(method)) {
                        uml = method.getAnnotation(UML.class);
                        if (!method.isAnnotationPresent(Deprecated.class)) {
                            assertNotNull("Missing @UML annotation.", uml);
                        }
                    }
                }
            }
        }
        done();
    }

    /**
     * Tests the annotations in the {@code package-info} files of SIS implementations of the
     * interfaces enumerated in the {@code #types} array. More specifically this method tests that:
     *
     * <ul>
     *   <li>The prefixes declared in the {@link XmlNs} annotations match the
     *       {@linkplain Namespaces#getPreferredPrefix expected prefixes}.</li>
     * </ul>
     */
    @Test
    public void testPackageAnnotations() {
        final Set<Package> packages = new HashSet<Package>();
        for (final Class<?> type : types) {
            if (!CodeList.class.isAssignableFrom(type)) {
                testingClass = type.getCanonicalName();
                final Class<?> impl = getImplementation(type);
                if (impl != null) {
                    testingClass = impl.getCanonicalName();
                    final Package p = impl.getPackage();
                    assertNotNull("Missing package information.", p);
                    packages.add(p);
                }
            }
        }
        for (final Package p : packages) {
            for (final XmlNs ns : p.getAnnotation(XmlSchema.class).xmlns()) {
                testingClass = p.getName();
                final String namespace = ns.namespaceURI();
                assertEquals("Unexpected namespace prefix.", Namespaces.getPreferredPrefix(namespace, null), ns.prefix());
            }
        }
        done();
    }

    /**
     * Tests the annotations on every SIS implementations of the interfaces enumerated
     * in the {@link #types} array. More specifically this method tests that:
     *
     * <ul>
     *   <li>All implementation classes have {@link XmlRootElement} and {@link XmlType} annotations.</li>
     *   <li>The name declared in the {@code XmlType} annotations matches the
     *       {@link #getExpectedXmlTypeForElement expected value}.</li>
     *   <li>The name declared in the {@code XmlRootElement} annotations matches the identifier declared
     *       in the {@link UML} annotation of the GeoAPI interfaces.</li>
     *   <li>The namespace declared in the {@code XmlRootElement} annotations is not redundant with
     *       the {@link XmlSchema} annotation in the package.</li>
     * </ul>
     *
     * This method does not check the method annotations, since it is {@link #testMethodAnnotations()} job.
     */
    @Test
    @DependsOnMethod("testInterfaceAnnotations")
    public void testImplementationAnnotations() {
        for (final Class<?> type : types) {
            if (CodeList.class.isAssignableFrom(type)) {
                // Skip code lists, since they are not the purpose of this test.
                continue;
            }
            testingClass = type.getCanonicalName();
            /*
             * Get the implementation class, which is mandatory (otherwise the
             * subclass shall not include the interface in the 'types' array).
             */
            final Class<?> impl = getImplementation(type);
            assertNotNull("No implementation found.", impl);
            assertNotSame("No implementation found.", type, impl);
            testingClass = impl.getCanonicalName();
            /*
             * Compare the XmlRootElement with the UML annotation, if any. The UML annotation
             * is mandatory in the default implementation of the 'testInterfaceAnnotations()'
             * method, but we don't require the UML to be non-null here since this is not the
             * job of this test method. This is because subclasses may choose to override the
             * 'testInterfaceAnnotations()' method.
             */
            final XmlRootElement root = impl.getAnnotation(XmlRootElement.class);
            assertNotNull("Missing @XmlRootElement annotation.", root);
            final UML uml = type.getAnnotation(UML.class);
            if (uml != null) {
                assertEquals("Wrong @XmlRootElement.name().", getExpectedXmlElementName(null, uml), root.name());
            }
            /*
             * Check that the namespace is the expected one (according subclass)
             * and is not redundant with the package @XmlSchema annotation.
             */
            assertExpectedNamespace(root.namespace(), impl, uml);
            /*
             * Compare the XmlType annotation with the expected value.
             */
            final XmlType xmlType = impl.getAnnotation(XmlType.class);
            assertNotNull("Missing @XmlType annotation.", xmlType);
            String expected = getExpectedXmlTypeForElement(type, impl);
            if (expected == null) {
                expected = DEFAULT;
            }
            assertEquals("Wrong @XmlType.name().", expected, xmlType.name());
        }
        done();
    }

    /**
     * Tests the annotations on every methods of SIS classes.
     * More specifically this method tests that:
     *
     * <ul>
     *   <li>The name declared in {@link XmlElement} matches the UML identifier.</li>
     *   <li>The {@code XmlElement.required()} boolean is consistent with the UML {@linkplain Obligation obligation}.</li>
     *   <li>The namespace declared in {@code XmlElement} is not redundant with the one declared in the package.</li>
     * </ul>
     */
    @Test
    @DependsOnMethod("testImplementationAnnotations")
    public void testMethodAnnotations() {
        for (final Class<?> type : types) {
            if (CodeList.class.isAssignableFrom(type)) {
                // Skip code lists, since they are not the purpose of this test.
                continue;
            }
            testingMethod = null;
            testingClass = type.getCanonicalName();
            final Class<?> impl = getImplementation(type);
            if (impl == null) {
                // Implementation existence are tested by 'testImplementationAnnotations()'.
                // It is not the purpose of this test to verify again their existence.
                continue;
            }
            testingClass = impl.getCanonicalName();
            for (final Method method : type.getDeclaredMethods()) {
                if (isIgnored(method)) {
                    continue;
                }
                testingMethod = method.getName();
                final UML uml = method.getAnnotation(UML.class);
                final XmlElement element = getXmlElement(impl, testingMethod, uml);
                /*
                 * Just display the missing @XmlElement annotation for the method, since we know
                 * that some elements are not yet implemented (and consequently can not yet be
                 * annotated).
                 */
                if (element == null) {
                    // Note: lines with the "[WARNING]" string are highlighted by Jenkins.
                    warning("[WARNING] Missing @XmlElement annotation for ");
                    continue;
                }
                /*
                 * The UML annotation is mandatory in the default implementation of the
                 * 'testInterfaceAnnotations()' method, but we don't require the UML to
                 * be non-null here since this is not the job of this test method. This
                 * is because subclasses may choose to override the above test method.
                 */
                if (uml != null) {
                    assertEquals("Wrong @XmlElement.name().", getExpectedXmlElementName(type, uml), element.name());
                    assertEquals("Wrong @XmlElement.required().", uml.obligation() == Obligation.MANDATORY, element.required());
                }
                /*
                 * Check that the namespace is the expected one (according subclass)
                 * and is not redundant with the package @XmlSchema annotation.
                 */
                assertExpectedNamespace(element.namespace(), impl, uml);
            }
        }
        done();
    }

    /**
     * Tests the annotations on wrappers returned by {@link #getWrapperFor(Class)}.
     * More specifically this method tests that:
     *
     * <ul>
     *   <li>The wrapper have a getter and a setter method declared in the same class.</li>
     *   <li>The getter method is annotated with {@code @XmlElement} or {@code @XmlElementRef}, but not both</li>
     *   <li>{@code @XmlElementRef} is used only in parent classes, not in leaf classes.</li>
     *   <li>The name declared in {@code @XmlElement} matches the {@code @UML} identifier.</li>
     * </ul>
     */
    @Test
    public void testWrapperAnnotations() {
        for (final Class<?> type : types) {
            testingClass = type.getCanonicalName();
            /*
             * Check the annotation on the wrapper, if there is one. If no wrapper is declared
             * specifically for the current type, check if a wrapper is defined for the parent
             * interface. In such case, the getElement() method is required to be annotated by
             * @XmlElementRef, not @XmlElement, in order to let JAXB infer the name from the
             * actual subclass.
             */
            final WrapperClass wrapper;
            try {
                wrapper = getWrapperInHierarchy(type);
            } catch (ClassNotFoundException e) {
                fail(e.toString());
                continue;
            }
            if (wrapper.type == null) {
                // If the wrapper is intentionally undefined, skip it.
                continue;
            }
            /*
             * Now fetch the getter/setter methods, ensure that they are declared in the same class
             * and verify that exactly one of @XmlElement or @XmlElementRef annotation is declared.
             */
            testingClass = wrapper.type.getCanonicalName();
            final XmlElement element;
            if (type.isEnum()) {
                final Field field;
                try {
                    field = wrapper.type.getDeclaredField("value");
                } catch (NoSuchFieldException e) {
                    fail(e.toString());
                    continue;
                }
                element = field.getAnnotation(XmlElement.class);
            } else {
                final Method getter, setter;
                try {
                    getter = wrapper.type.getMethod("getElement", (Class<?>[]) null);
                    setter = wrapper.type.getMethod("setElement", getter.getReturnType());
                } catch (NoSuchMethodException e) {
                    fail(e.toString());
                    continue;
                }
                assertEquals("The setter method must be declared in the same class than the " +
                             "getter method - not in a parent class, to avoid issues with JAXB.",
                             getter.getDeclaringClass(), setter.getDeclaringClass());
                assertEquals("The setter parameter type shall be the same than the getter return type.",
                             getter.getReturnType(), getSingleton(setter.getParameterTypes()));
                element = getter.getAnnotation(XmlElement.class);
                assertEquals("Expected @XmlElement XOR @XmlElementRef.", (element == null),
                             getter.isAnnotationPresent(XmlElementRef.class) ||
                             getter.isAnnotationPresent(XmlElementRefs.class));
            }
            /*
             * If the annotation is @XmlElement, ensure that XmlElement.name() is equals to
             * the UML identifier. Then verify that the
             */
            if (element != null) {
                assertFalse("Expected @XmlElementRef.", wrapper.isInherited);
                final UML uml = type.getAnnotation(UML.class);
                if (uml != null) { // 'assertNotNull' is 'testInterfaceAnnotations()' job.
                    assertEquals("Wrong @XmlElement.", uml.identifier(), element.name());
                }
                final String namespace = assertExpectedNamespace(element.namespace(), wrapper.type, uml);
                if (!CodeList.class.isAssignableFrom(type)) {
                    final String expected = getNamespace(getImplementation(type));
                    if (expected != null) { // 'assertNotNull' is 'testImplementationAnnotations()' job.
                        assertEquals("Inconsistent @XmlRootElement namespace.", expected, namespace);
                    }
                }
            }
        }
        done();
    }

    /**
     * Shall be invoked after every successful test in order
     * to disable the report of failed class or method.
     */
    protected final void done() {
        testingClass  = null;
        testingMethod = null;
    }

    /**
     * Prints the given message followed by the name of the class being tested.
     */
    private void warning(String message) {
        if (testingClass != null) {
            final StringBuilder buffer = new StringBuilder(message);
            buffer.append(testingClass);
            if (testingMethod != null) {
                buffer.append('.').append(testingMethod).append("()");
            }
            message = buffer.toString();
        }
        out.println(message);
    }

    /**
     * If a test failed, reports the class and method names were the failure occurred.
     * The message will be written in the {@link #out} printer.
     *
     * @see #testingClass
     * @see #testingMethod
     */
    @After
    public final void printFailureLocation() {
        if (testingClass != null) {
            warning("TEST FAILURE: ");
        }
    }
}
