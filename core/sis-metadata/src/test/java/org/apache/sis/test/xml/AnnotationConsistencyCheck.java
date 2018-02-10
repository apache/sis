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

import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.annotation.Classifier;
import org.opengis.annotation.Stereotype;
import org.opengis.annotation.Obligation;
import org.opengis.annotation.Specification;
import org.opengis.util.CodeList;
import org.opengis.util.ControlledVocabulary;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import junit.framework.AssertionFailedError;


/**
 * Verifies consistency between {@link UML}, {@link XmlElement} and other annotations.
 * Some tests performed by this class are:
 *
 * <ul>
 *   <li>All implementation classes have {@link XmlRootElement} and {@link XmlType} annotations.</li>
 *   <li>The name declared in the {@code XmlType} annotations matches the
 *       {@link #getExpectedXmlTypeName expected value}.</li>
 *   <li>The name declared in the {@code XmlRootElement} (classes) or {@link XmlElement} (methods)
 *       annotations matches the identifier declared in the {@link UML} annotation of the GeoAPI interfaces.
 *       The UML - XML name mapping can be changed by overriding {@link #getExpectedXmlElementName(Class, UML)}.</li>
 *   <li>The {@code XmlElement.required()} boolean is consistent with the UML {@linkplain Obligation obligation}.</li>
 *   <li>The namespace declared in the {@code XmlRootElement} or {@code XmlElement} annotations
 *       is not redundant with the {@link XmlSchema} annotation in the package.</li>
 *   <li>The prefixes declared in the {@link XmlNs} annotations match the
 *       {@linkplain Namespaces#getPreferredPrefix expected prefixes}.</li>
 *   <li>The {@linkplain #getWrapperFor wrapper}, if any, is consistent.</li>
 * </ul>
 *
 * This class does not verify JAXB annotations against a XSD file.
 * For such verification, see {@link SchemaCompliance}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public abstract strictfp class AnnotationConsistencyCheck extends TestCase {
    /**
     * The {@value} string used in JAXB annotations for default names or namespaces.
     */
    static final String DEFAULT = "##default";

    /**
     * The GeoAPI interfaces, {@link CodeList} or {@link Enum} types to test.
     * This array is specified at construction time. Each test iterates over
     * all types in this array.
     */
    protected final Class<?>[] types;

    /**
     * The type being tested, or {@code null} if none. In case of test failure, this information
     * will be used by the {@code assert(…)} methods for formatting a message giving the name of
     * class and method where the failure occurred.
     *
     * @see #fail(String)
     */
    protected String testingClass;

    /**
     * The method being tested, or {@code null} if none. In case of test failure, this information
     * will be used by the {@code assert(…)} methods for formatting a message giving the name of
     * class and method where the failure occurred.
     *
     * @see #fail(String)
     */
    protected String testingMethod;

    /**
     * Creates a new test suite for the given types.
     * The given sequence of types is assigned to the {@link #types} field.
     *
     * @param  types  the GeoAPI interfaces, {@link CodeList} or {@link Enum} types to test.
     */
    protected AnnotationConsistencyCheck(final Class<?>... types) {
        this.types = types;     // No need to clone — test classes are normally used only by SIS.
    }

    /**
     * Returns the SIS implementation class for the given GeoAPI interface.
     * For example the implementation of the {@link org.opengis.metadata.citation.Citation}
     * interface is the {@link org.apache.sis.metadata.iso.citation.DefaultCitation} class.
     *
     * @param  <T>   the type represented by the {@code type} argument.
     * @param  type  the GeoAPI interface (never a {@link CodeList} or {@link Enum} type).
     * @return the SIS implementation for the given interface.
     */
    protected abstract <T> Class<? extends T> getImplementation(Class<T> type);

    /**
     * If the given GeoAPI type, when marshalled to XML, is wrapped into an other XML element,
     * returns the class of the wrapper for that XML element. Otherwise returns {@code null}.
     * Such wrappers are unusual in XML (except for lists), but the ISO 19115-3 standard do that
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
     * @param  type  the GeoAPI interface, {@link CodeList} or {@link Enum} type.
     * @return the wrapper for the given type, or {@code null} if none.
     * @throws ClassNotFoundException if a wrapper was expected but not found.
     */
    protected abstract Class<?> getWrapperFor(Class<?> type) throws ClassNotFoundException;

    /**
     * The value returned by {@link #getWrapperFor(Class)}, together with a boolean telling
     * whether the wrapper has been found in the tested class or in one of its parent classes.
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
     * @param  type  the GeoAPI interface, {@link CodeList} or {@link Enum} type.
     * @return the wrapper for the given type.
     *         {@link WrapperClass#type} is {@code null} if no wrapper has been found.
     * @throws ClassNotFoundException if a wrapper was expected but not found in the
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
                    e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    /**
     * Returns the beginning of expected namespace for an element defined by the given UML.
     * For example the namespace of most types defined by {@link Specification#ISO_19115}
     * starts with is {@code "http://standards.iso.org/iso/19115/-3/"}.
     *
     * <p>The default implementation recognizes the
     * {@linkplain Specification#ISO_19115   ISO 19115},
     * {@linkplain Specification#ISO_19115_2 ISO 19115-2},
     * {@linkplain Specification#ISO_19115_3 ISO 19115-3},
     * {@linkplain Specification#ISO_19139   ISO 19139} and
     * {@linkplain Specification#ISO_19108   ISO 19108} specifications,
     * with a hard-coded list of exceptions to the general rule.
     * Subclasses shall override this method if they need to support more namespaces.</p>
     *
     * <p>Note that a more complete verification is done by {@link SchemaCompliance}.
     * But the test done in this {@link AnnotationConsistencyCheck} class can be run without network access.</p>
     *
     * <p>The prefix for the given namespace will be fetched by
     * {@link Namespaces#getPreferredPrefix(String, String)}.</p>
     *
     * @param  impl  the implementation class ({@link CodeList} or {@link Enum} type).
     * @param  uml   the UML associated to the class or the method.
     * @return the expected namespace.
     * @throws IllegalArgumentException if the given UML is unknown to this method.
     */
    @SuppressWarnings("deprecation")
    protected String getExpectedNamespaceStart(final Class<?> impl, final UML uml) {
        final String identifier = uml.identifier();
        switch (identifier) {
            case "SV_CoupledResource":
            case "SV_OperationMetadata":
            case "SV_OperationChainMetadata":
            case "SV_ServiceIdentification": {              // Historical reasons (other standard integrated into ISO 19115)
                assertEquals("Unexpected @Specification value.", Specification.ISO_19115, uml.specification());
                assertEquals("Specification version should be latest ISO 19115.", (short) 0, uml.version());
                return Namespaces.SRV;
            }
            case "DQ_TemporalAccuracy":                     // Renamed DQ_TemporalQuality
            case "DQ_NonQuantitativeAttributeAccuracy": {   // Renamed DQ_NonQuantitativeAttributeCorrectness
                assertEquals("Unexpected @Specification value.", Specification.ISO_19115, uml.specification());
                assertEquals("Specification version should be legacy ISO 19115.", (short) 2003, uml.version());
                return LegacyNamespaces.GMD;
            }
            case "role": {
                if (org.opengis.metadata.citation.ResponsibleParty.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMD;            // Override a method defined in Responsibility
                }
                break;
            }
            case "lineage": {
                if (org.opengis.metadata.quality.DataQuality.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMD;            // Deprecated property in a type not yet upgraded.
                }
                break;
            }
            case "errorStatistic": {
                if (org.opengis.metadata.quality.QuantitativeResult.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMD;            // Deprecated property in a type not yet upgraded.
                }
                break;
            }
            case "nameOfMeasure":
            case "measureIdentification":
            case "measureDescription":
            case "evaluationMethodType":
            case "evaluationMethodDescription":
            case "evaluationProcedure": {
                if (org.opengis.metadata.quality.Element.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMD;            // Deprecated property in a type not yet upgraded.
                }
                break;
            }
            case "dateTime": {
                if (org.opengis.metadata.quality.Element.class.isAssignableFrom(impl)) {
                    return Namespaces.DQC;
                }
                break;
            }
            case "fileFormat": {
                if (org.opengis.metadata.distribution.DataFile.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMX;            // Deprecated method (removed from ISO 19115-3:2016)
                }
                break;
            }
        }
        /*
         * GeoAPI has not yet been upgraded to ISO 19157. Interfaces in the "org.opengis.metadata.quality"
         * package are still defined according the old specification. Those types have the "DQ_" or "QE_"
         * prefix. This issue applies also to properties (starting with a lower case).
         */
        if (identifier.startsWith("DQ_")) {
            assertEquals("Unexpected @Specification value.", Specification.ISO_19115, uml.specification());
            assertEquals("Specification version should be legacy ISO 19115.", (short) 2003, uml.version());
            return Namespaces.MDQ;
        }
        if (identifier.startsWith("QE_")) {
            assertEquals("Unexpected @Specification value.", Specification.ISO_19115_2, uml.specification());
            switch (uml.version()) {
                case 0:    return Namespaces.MDQ;
                case 2009: return LegacyNamespaces.GMI;
                default: fail("Unexpected version number in " + uml);
            }
        }
        if (org.opengis.metadata.quality.DataQuality.class.isAssignableFrom(impl) ||    // For properties in those types.
            org.opengis.metadata.quality.Element.class.isAssignableFrom(impl) ||
            org.opengis.metadata.quality.Result.class.isAssignableFrom(impl))
        {
            return Namespaces.MDQ;
        }
        /*
         * General cases (after we processed all the special cases)
         * based on which standard defines the type or property.
         */
        if (uml.version() != 0) {
            switch (uml.specification()) {
                case ISO_19115:   return LegacyNamespaces.GMD;
                case ISO_19115_2: return LegacyNamespaces.GMI;
            }
        }
        switch (uml.specification()) {
            case ISO_19115:
            case ISO_19115_2:
            case ISO_19115_3: return Schemas.METADATA_ROOT;
            case ISO_19139:   return LegacyNamespaces.GMX;
            case ISO_19108:   return LegacyNamespaces.GMD;
            default: throw new IllegalArgumentException(uml.toString());
        }
    }

    /**
     * Returns the name of the XML type for an interface described by the given UML.
     * For example in ISO 19115-3, the XML type of {@code CI_Citation} is {@code CI_Citation_Type}.
     * The default implementation returns {@link UML#identifier()}, possibly with {@code "Abstract"} prepended,
     * and unconditionally with {@code "_Type"} appended.
     * Subclasses shall override this method when mismatches are known to exist between the UML and XML type names.
     *
     * @param  stereotype  the stereotype of the interface, or {@code null} if none.
     * @param  uml         the UML of the interface for which to get the corresponding XML type name.
     * @return the name of the XML type for the given element, or {@code null} if none.
     *
     * @see #testImplementationAnnotations()
     */
    protected String getExpectedXmlTypeName(final Stereotype stereotype, final UML uml) {
        final String rootName = uml.identifier();
        final StringBuilder buffer = new StringBuilder(rootName.length() + 13);
        if (Stereotype.ABSTRACT.equals(stereotype)) {
            buffer.append("Abstract");
        }
        return buffer.append(rootName).append("_Type").toString();
    }

    /**
     * Returns the name of the XML root element for an interface described by the given UML.
     * The default implementation returns {@link UML#identifier()}, possibly with {@code "Abstract"} prepended.
     * Subclasses shall override this method when mismatches are known to exist between the UML and XML element names.
     *
     * @param  stereotype  the stereotype of the interface, or {@code null} if none.
     * @param  uml         the UML of the interface for which to get the corresponding XML root element name.
     * @return the name of the XML root element for the given UML.
     *
     * @see #testImplementationAnnotations()
     */
    protected String getExpectedXmlRootElementName(final Stereotype stereotype, final UML uml) {
        String name = uml.identifier();
        if (Stereotype.ABSTRACT.equals(stereotype)) {
            name = "Abstract".concat(name);
        }
        return name;
    }

    /**
     * Returns the name of the XML element for a method described by the given UML.
     * This method is invoked for a property (field or method) defined by an interface.
     * The {@code enclosing} argument is the interface containing the property.
     *
     * <p>The default implementation returns {@link UML#identifier()}. Subclasses shall override this method
     * when mismatches are known to exist between the UML and XML element names.</p>
     *
     * @param  enclosing  the GeoAPI interface which contains the property, or {@code null} if none.
     * @param  uml        the UML element for which to get the corresponding XML element name.
     * @return the XML element name for the given UML element.
     *
     * @see #testMethodAnnotations()
     */
    protected String getExpectedXmlElementName(final Class<?> enclosing, final UML uml) {
        String name = uml.identifier();
        switch (name) {
            case "stepDateTime": {
                if (org.opengis.metadata.lineage.ProcessStep.class.isAssignableFrom(enclosing)) {
                    name = "dateTime";
                }
                break;
            }
            case "satisfiedPlan": {
                if (org.opengis.metadata.acquisition.Requirement.class.isAssignableFrom(enclosing)) {
                    name = "satisifiedPlan";                // Mispelling in ISO 19115-3:2016
                }
                break;
            }
            case "meteorologicalConditions": {
                if (org.opengis.metadata.acquisition.EnvironmentalRecord.class.isAssignableFrom(enclosing)) {
                    name = "meterologicalConditions";       // Mispelling in ISO 19115-3:2016
                }
                break;
            }
            case "detectedPolarization": {
                if (org.opengis.metadata.content.Band.class.isAssignableFrom(enclosing)) {
                    name = "detectedPolarisation";          // Spelling change in XSD files
                }
                break;
            }
            case "transmittedPolarization": {
                if (org.opengis.metadata.content.Band.class.isAssignableFrom(enclosing)) {
                    name = "transmittedPolarisation";       // Spelling change in XSD files
                }
                break;
            }
            case "featureType": {
                if (org.opengis.metadata.distribution.DataFile.class.isAssignableFrom(enclosing)) {
                    name = "featureTypes";                  // Spelling change in XSD files
                }
                break;
            }
            case "valueType": {
                if (org.opengis.metadata.quality.Result.class.isAssignableFrom(enclosing)) {
                    return "valueRecordType";
                }
                break;
            }
        }
        return name;
    }

    /**
     * Replaces {@value #DEFAULT} value by the {@link XmlSchema} namespace if needed,
     * then performs validity check on the resulting namespace. This method checks that:
     *
     * <ul>
     *   <li>The namespace is not redundant with the package-level {@link XmlSchema} namespace.</li>
     *   <li>The namespace is declared in a package-level {@link XmlNs} annotation.</li>
     *   <li>The namespace starts with the {@linkplain #getExpectedNamespaceStart expected namespace}.</li>
     * </ul>
     *
     * @param  namespace  the namespace given by the {@code @XmlRootElement} or {@code @XmlElement} annotation.
     * @param  impl       the implementation or wrapper class from which to get the package namespace.
     * @param  uml        the {@code @UML} annotation, or {@code null} if none.
     * @return the actual namespace (same as {@code namespace} if it was not {@value #DEFAULT}).
     */
    private String assertExpectedNamespace(String namespace, final Class<?> impl, final UML uml) {
        assertNotNull("Missing namespace.", namespace);
        assertFalse("Missing namespace.", namespace.trim().isEmpty());
        /*
         * Get the namespace declared at the package level, and ensure the the
         * given namespace is not redundant with that package-level namespace.
         */
        final XmlSchema schema = impl.getPackage().getAnnotation(XmlSchema.class);
        assertNotNull("Missing @XmlSchema annotation in package-info.", schema);
        final String schemaNamespace = schema.namespace();      // May be XMLConstants.NULL_NS_URI
        assertFalse("Namespace declaration is redundant with package-info @XmlSchema.", namespace.equals(schemaNamespace));
        /*
         * Resolve the namespace given in argument: using the class-level namespace if needed,
         * or the package-level namespace if the class-level one is not defined.
         */
        if (DEFAULT.equals(namespace)) {
            final XmlType type = impl.getAnnotation(XmlType.class);
            if (type == null || DEFAULT.equals(namespace = type.namespace())) {
                namespace = schemaNamespace;
            }
            assertFalse("No namespace defined.", XMLConstants.NULL_NS_URI.equals(namespace));
        }
        /*
         * Check that the namespace is declared in the package-level @XmlNs annotation.
         * We do not verify the validity of those @XmlNs annotations, since this is the
         * purpose of the 'testPackageAnnotations()' method.
         */
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
        /*
         * Check that the namespace is one of the namespaces controlled by the specification.
         * We check only the namespace start, since some specifications define many namespaces
         * under a common root (e.g. "http://standards.iso.org/iso/19115/-3/").
         */
        if (uml != null) {
            final String expected = getExpectedNamespaceStart(impl, uml);
            if (!namespace.startsWith(expected)) {
                fail("Expected " + expected + "… namespace for that ISO specification but got " + namespace);
            }
        }
        return namespace;
    }

    /**
     * Returns the namespace declared in the {@link XmlSchema} annotation of the given package,
     * or {@code null} if none.
     *
     * @param  p  the package, or {@code null}.
     * @return the namespace, or {@code null} if none.
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
     * @param  impl  the implementation class, or {@code null}.
     * @return the namespace, or {@code null} if none.
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
     * Returns {@code true} if the given method should be ignored,
     * either because it is a standard method from the JDK or because it is a non-standard extension.
     * If {@code true}, then {@code method} does not need to have {@link UML} or {@link XmlElement} annotation.
     *
     * @param  method  the method to verify.
     * @return {@code true} if the given method should be ignored, or {@code false} otherwise.
     *
     * @since 0.5
     */
    protected boolean isIgnored(final Method method) {
        switch (method.getName()) {
            /*
             * Types for which JAXB binding has not yet implemented.
             */
            case "getGeographicCoordinates": {
                return org.opengis.metadata.spatial.GCP.class.isAssignableFrom(method.getDeclaringClass());
            }
            /*
             * GeoAPI extension for inter-operability with JDK API, not defined in ISO specification.
             */
            case "getCurrency": {
                return org.opengis.metadata.distribution.StandardOrderProcess.class.isAssignableFrom(method.getDeclaringClass());
            }
            /*
             * ISO 19115-2 properties moved from MI_Band to MD_SampleDimension by GeoAPI.
             * Must be taken in account only when checking the Band subtype.
             */
            case "getNominalSpatialResolution":
            case "getTransferFunctionType": {
                final Class<?> dc = method.getDeclaringClass();
                return org.opengis.metadata.content.SampleDimension.class.isAssignableFrom(dc)
                        && !org.opengis.metadata.content.Band.class.isAssignableFrom(dc);
            }
            /*
             * Standard Java methods overridden in some GeoAPI interfaces for Javadoc purposes.
             */
            case "equals":
            case "hashCode":
            case "doubleValue": return true;
        }
        return false;
    }

    /**
     * Tests the annotations on every GeoAPI interfaces and code lists in the {@link #types} array.
     * More specifically this method tests that:
     *
     * <ul>
     *   <li>All elements in {@link #types} except code lists are interfaces.</li>
     *   <li>All elements in {@code types} have a {@link UML} annotation.</li>
     *   <li>All methods except deprecated methods and methods overriding JDK methods
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
            if (!ControlledVocabulary.class.isAssignableFrom(type)) {
                for (final Method method : type.getDeclaredMethods()) {
                    testingMethod = method.getName();
                    if (!isIgnored(method)) {
                        uml = method.getAnnotation(UML.class);
                        if (!method.isAnnotationPresent(Deprecated.class)) {
                            assertNotNull("Missing @UML annotation.", uml);
                        }
                    }
                }
            }
        }
    }

    /**
     * Tests the annotations in the {@code package-info} files of Apache SIS implementations of the
     * interfaces enumerated in the {@code #types} array. More specifically this method tests that:
     *
     * <ul>
     *   <li>The prefixes declared in the {@link XmlNs} annotations match the
     *       {@linkplain Namespaces#getPreferredPrefix expected prefixes}.</li>
     * </ul>
     */
    @Test
    public void testPackageAnnotations() {
        final Set<Package> packages = new HashSet<>();
        for (final Class<?> type : types) {
            if (!ControlledVocabulary.class.isAssignableFrom(type)) {
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
    }

    /**
     * Tests the annotations on every SIS implementations of the interfaces enumerated
     * in the {@link #types} array. More specifically this method tests that:
     *
     * <ul>
     *   <li>All implementation classes have {@link XmlRootElement} and {@link XmlType} annotations.</li>
     *   <li>The name declared in the {@code XmlType} annotations matches the
     *       {@link #getExpectedXmlTypeName expected value}.</li>
     *   <li>The name declared in the {@code XmlRootElement} annotations matches the identifier declared
     *       in the {@link UML} annotation of the GeoAPI interfaces, with {@code "Abstract"} prefix added
     *       if needed.</li>
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
            if (ControlledVocabulary.class.isAssignableFrom(type)) {
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
            Stereotype stereotype = null;
            if (uml != null) {
                final Classifier c = type.getAnnotation(Classifier.class);
                if (c != null) {
                    stereotype = c.value();
                }
                assertEquals("Wrong @XmlRootElement.name().", getExpectedXmlRootElementName(stereotype, uml), root.name());
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
            String expected = getExpectedXmlTypeName(stereotype, uml);
            if (expected == null) {
                expected = DEFAULT;
            }
            assertEquals("Wrong @XmlType.name().", expected, xmlType.name());
        }
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
            if (ControlledVocabulary.class.isAssignableFrom(type)) {
                // Skip code lists, since they are not the purpose of this test.
                continue;
            }
            testingMethod = null;
            testingClass = type.getCanonicalName();
            final Class<?> impl = getImplementation(type);
            if (impl == null) {
                /*
                 * Implementation existence are tested by 'testImplementationAnnotations()'.
                 * It is not the purpose of this test to verify again their existence.
                 */
                continue;
            }
            testingClass = impl.getCanonicalName();
            for (final Method method : type.getDeclaredMethods()) {
                if (isIgnored(method)) {
                    continue;
                }
                testingMethod = method.getName();
                final UML uml = method.getAnnotation(UML.class);
                XmlElement element;
                try {
                    element = impl.getMethod(testingMethod).getAnnotation(XmlElement.class);
                } catch (NoSuchMethodException e) {
                    fail(e.toString());
                    continue;
                }
                if (element == null) {
                    if (uml == null) {
                        continue;
                    }
                    /*
                     * If the method does not have a @XmlElement annotation, search for a private method having the
                     * @XmlElement annotation with expected name. This situation happens when metadata object needs
                     * to perform some extra step at XML marshalling time only (not when using directly the API),
                     * for example verifying whether we are marshalling ISO 19139:2007 or ISO 19115-3:2016.
                     */
                    boolean wasPublic = false;
                    final String identifier = uml.identifier();
                    for (final Method pm : impl.getDeclaredMethods()) {
                        final XmlElement e = pm.getAnnotation(XmlElement.class);
                        if (e != null && identifier.equals(e.name())) {
                            final boolean isPublic = Modifier.isPublic(pm.getModifiers());
                            if (element != null) {
                                if (isPublic & !wasPublic) continue;            // Give precedence to private methods.
                                if (isPublic == wasPublic) {
                                    fail("Duplicated @XmlElement for \"" + identifier + "\".");
                                }
                            }
                            wasPublic = isPublic;
                            element = e;
                        }
                    }
                    /*
                     * If a few case the annotation is not on a getter method, but directly on the field.
                     * The main case is the "pass" field in DefaultConformanceResult.
                     */
                    if (element == null) try {
                        element = impl.getDeclaredField(identifier).getAnnotation(XmlElement.class);
                        assertNotNull("Missing @XmlElement annotation.", element);
                    } catch (NoSuchFieldException e) {
                        fail("Missing @XmlElement annotation.");
                        continue;   // As a metter of principle (should never reach this point).
                    }
                }
                /*
                 * The UML annotation is mandatory in the default implementation of the
                 * 'testInterfaceAnnotations()' method, but we don't require the UML to
                 * be non-null here since this is not the job of this test method. This
                 * is because subclasses may choose to override the above test method.
                 */
                if (uml != null) {
                    assertEquals("Wrong @XmlElement.name().", getExpectedXmlElementName(type, uml), element.name());
                    if (!method.isAnnotationPresent(Deprecated.class) && uml.version() == 0) {
                        assertEquals("Wrong @XmlElement.required().", uml.obligation() == Obligation.MANDATORY, element.required());
                    }
                }
                /*
                 * Check that the namespace is the expected one (according subclass)
                 * and is not redundant with the package @XmlSchema annotation.
                 */
                assertExpectedNamespace(element.namespace(), impl, uml);
            }
        }
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
                             getter.getReturnType(), TestUtilities.getSingleton(setter.getParameterTypes()));
                element = getter.getAnnotation(XmlElement.class);
                assertEquals("Expected @XmlElement XOR @XmlElementRef.", (element == null),
                             getter.isAnnotationPresent(XmlElementRef.class) ||
                             getter.isAnnotationPresent(XmlElementRefs.class));
            }
            /*
             * If the annotation is @XmlElement, ensure that XmlElement.name() is equals
             * to the UML identifier. Then verify that the namespace is the expected one.
             */
            if (element != null) {
                assertFalse("Expected @XmlElementRef.", wrapper.isInherited);
                final UML uml = type.getAnnotation(UML.class);
                if (uml != null) {                  // 'assertNotNull' is 'testInterfaceAnnotations()' job.
                    assertEquals("Wrong @XmlElement.", uml.identifier(), element.name());
                }
                final String namespace = assertExpectedNamespace(element.namespace(), wrapper.type, uml);
                if (!ControlledVocabulary.class.isAssignableFrom(type)) {
                    final String expected = getNamespace(getImplementation(type));
                    if (expected != null) {         // 'assertNotNull' is 'testImplementationAnnotations()' job.
                        assertEquals("Inconsistent @XmlRootElement namespace.", expected, namespace);
                    }
                }
            }
        }
    }

    /**
     * Prepends the {@link #testingClass} and {@link #testingMethod} before the given message.
     * This is used by {@code assertFoo(…)} methods in case of failure.
     */
    private String location(String message) {
        if (testingClass != null) {
            final StringBuilder buffer = new StringBuilder(100).append("Error with ").append(testingClass);
            if (testingMethod != null) {
                buffer.append('.').append(testingMethod).append("()");
            }
            message = buffer.append(": ").append(message).toString();
        }
        return message;
    }

    /**
     * Unconditionally fails the test. This method is equivalent to JUnit {@link org.junit.Assert#fail(String)}
     * except that the error message contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message  the failure message.
     *
     * @see #testingClass
     * @see #testingMethod
     */
    protected final void fail(final String message) {
        throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given condition is false. This method is equivalent to JUnit
     * {@link org.junit.Assert#assertTrue(String, boolean)} except that the error message
     * contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message    the message in case of failure.
     * @param  condition  the condition that must be {@code true}.
     */
    protected final void assertTrue(final String message, final boolean condition) {
        if (!condition) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given condition is true. This method is equivalent to JUnit
     * {@link org.junit.Assert#assertFalse(String, boolean)} except that the error message
     * contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message    the message in case of failure.
     * @param  condition  the condition that must be {@code false}.
     */
    protected final void assertFalse(final String message, final boolean condition) {
        if (condition) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given object is null. This method is equivalent to JUnit
     * {@link org.junit.Assert#assertNotNull(String, Object)} except that the error
     * message contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message  the message in case of failure.
     * @param  obj      the object that must be non-null.
     */
    protected final void assertNotNull(final String message, final Object obj) {
        if (obj == null) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given objects are the same. This method is equivalent to JUnit
     * {@link org.junit.Assert#assertNotSame(String, Object, Object)} except that the error
     * message contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message  the message in case of failure.
     * @param  o1       the first object (may be null).
     * @param  o2       the second object (may be null).
     */
    protected final void assertNotSame(final String message, final Object o1, final Object o2) {
        if (o1 == o2) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given objects are not the same. This method is equivalent to JUnit
     * {@link org.junit.Assert#assertSame(String, Object, Object)} except that the error message
     * contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message   the message in case of failure.
     * @param  expected  the first object (may be null).
     * @param  actual    the second object (may be null).
     */
    protected final void assertSame(final String message, final Object expected, final Object actual) {
        if (expected != actual) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given objects are not equal. This method is equivalent to JUnit
     * {@link org.junit.Assert#assertEquals(String, Object, Object)} except that the error
     * message contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  message   the message in case of failure.
     * @param  expected  the first object (may be null).
     * @param  actual    the second object (may be null).
     */
    protected final void assertEquals(final String message, final Object expected, final Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionFailedError(location(message) + System.lineSeparator()
                        + "Expected " + expected + " but got " + actual);
        }
    }
}
