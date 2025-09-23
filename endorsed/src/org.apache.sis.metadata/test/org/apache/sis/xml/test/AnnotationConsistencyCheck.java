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
package org.apache.sis.xml.test;

import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.xml.XMLConstants;
import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.annotation.Obligation;
import org.opengis.annotation.Specification;
import org.opengis.util.CodeList;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.cat.CodeListUID;

// Test dependencies
import org.opentest4j.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCaseWithLogs;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.annotation.Classifier;
import org.opengis.annotation.Stereotype;
import org.opengis.util.ControlledVocabulary;


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
 */
public abstract class AnnotationConsistencyCheck extends TestCaseWithLogs {
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
        super(Context.LOGGER);
        this.types = types;     // No need to clone — test classes are normally used only by SIS.
    }

    /**
     * Returns the SIS implementation class for the given GeoAPI interface.
     * For example, the implementation of the {@link org.opengis.metadata.citation.Citation}
     * interface is the {@link org.apache.sis.metadata.iso.citation.DefaultCitation} class.
     *
     * @param  <T>   the type represented by the {@code type} argument.
     * @param  type  the GeoAPI interface (never a {@link CodeList} or {@link Enum} type).
     * @return the SIS implementation for the given interface.
     */
    protected abstract <T> Class<? extends T> getImplementation(Class<T> type);

    /**
     * If the given GeoAPI type, when marshalled to XML, is wrapped into another XML element,
     * returns the class of the wrapper for that XML element. Otherwise returns {@code null}.
     * Such wrappers are unusual in XML (except for lists), but the ISO 19115-3 standard do that
     * systematically for every elements.
     *
     * <p><b>Example:</b> when a {@link org.apache.sis.metadata.iso.citation.DefaultContact}
     * is marshalled to XML inside a {@code ResponsibleParty}, the element is not marshalled
     * directly inside its parent as we usually do in XML. Instead, we have a {@code <CI_Contact>}.
     * inside the {@code <contactInfo>} element as below:</p>
     *
     * {@snippet lang="xml" :
     *   <CI_ResponsibleParty>
     *     <contactInfo>
     *       <CI_Contact>
     *         ...
     *       </CI_Contact>
     *     </contactInfo>
     *   </CI_ResponsibleParty>
     *   }
     *
     * To reflect that fact, this method shall return the internal {@code CI_Contact}
     * wrapper class for the {@link org.apache.sis.metadata.iso.citation.DefaultCitation} argument.
     * If no wrapper is expected for the given class, then this method shall return {@code null}.
     *
     * <p>If a wrapper is expected for the given class but was not found, then this method shall throw
     * {@link ClassNotFoundException}. Note that no wrapper may be defined explicitly for the given type,
     * while a wrapper is defined for a parent of the given type. This method does not need to care about
     * such situation, since the caller will automatically searches for a parent class if
     * {@code ClassNotFoundException} has been thrown.</p>
     *
     * <p>In SIS implementation, most wrappers are also {@link jakarta.xml.bind.annotation.adapters.XmlAdapter}.
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
     * Returns the identifier specified by the given UML, taking only the first one if it compound.
     * For example if the identifier is {@code "defaultLocale+otherLocale"}, then this method returns
     * only {@code "defaultLocale"}.
     */
    private static String firstIdentifier(final UML uml) {
        String identifier = uml.identifier();
        final int s = identifier.indexOf('+');
        return (s >= 0) ? identifier.substring(0, s) : identifier;
    }

    /**
     * Returns the beginning of expected namespace for an element defined by the given UML.
     * For example, the namespace of most types defined by {@link Specification#ISO_19115}
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
                assertEquals(Specification.ISO_19115, uml.specification(), "Unexpected @Specification value.");
                assertEquals((short) 0, uml.version(), "Specification version should be latest ISO 19115.");
                return Namespaces.SRV;
            }
            case "DQ_TemporalAccuracy":                     // Renamed DQ_TemporalQuality
            case "DQ_NonQuantitativeAttributeAccuracy": {   // Renamed DQ_NonQuantitativeAttributeCorrectness
                assertEquals(Specification.ISO_19115, uml.specification(), "Unexpected @Specification value.");
                assertEquals((short) 2003, uml.version(), "Specification version should be legacy ISO 19115.");
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
                    return LegacyNamespaces.GMD;            // Deprecated property after upgrade to ISO 19157.
                }
                break;
            }
            case "errorStatistic": {
                if (org.opengis.metadata.quality.QuantitativeResult.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMD;            // Deprecated property after upgrade to ISO 19157.
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
                    return LegacyNamespaces.GMD;            // Deprecated properties after upgrade to ISO 19157.
                }
                break;
            }
            case "dateTime": {
                if (org.opengis.metadata.quality.Element.class.isAssignableFrom(impl)) {
                    return LegacyNamespaces.GMD;
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
        if (identifier.startsWith("DQ_")) {
            assertEquals(Specification.ISO_19157, uml.specification(), "Unexpected @Specification value.");
            assertEquals((short) 0, uml.version(), "Specification version should be ISO 19157.");
            return Namespaces.MDQ;
        }
        if (identifier.startsWith("DQM_")) {
            assertEquals(Specification.ISO_19157, uml.specification(), "Unexpected @Specification value.");
            assertEquals((short) 0, uml.version(), "Specification version should be ISO 19157.");
            return Namespaces.DQM;
        }
        if (identifier.startsWith("QE_")) {
            assertEquals(Specification.ISO_19115_2, uml.specification(), "Unexpected @Specification value.");
            assertEquals((short) 2009, uml.version(), "Specification version should be legacy ISO 19115-2.");
            return LegacyNamespaces.GMI;
        }
        /*
         * General cases (after we processed all the special cases)
         * based on which standard defines the type or property.
         */
        final short version = uml.version();
        final Specification specification = uml.specification();
        if (version != 0 && version < specification.defaultVersion()) {
            switch (specification) {
                case ISO_19115:   return LegacyNamespaces.GMD;
                case ISO_19115_2: return LegacyNamespaces.GMI;
            }
        }
        switch (specification) {
            case ISO_19115:
            case ISO_19115_2:
            case ISO_19115_3: return CodeListUID.METADATA_ROOT;
            case ISO_19139:   return LegacyNamespaces.GMX;
            case ISO_19108:   return LegacyNamespaces.GMD;
            case ISO_19157: {
                // Case for a method. By contrast, above `identifier.startsWith(…)` checks were for types.
                final UML parent = TestUtilities.getSingleton(impl.getInterfaces()).getAnnotation(UML.class);
                return parent.identifier().startsWith("DQM_") ? Namespaces.DQM : Namespaces.MDQ;
            }
            default: throw new IllegalArgumentException(uml.toString());
        }
    }

    /**
     * Returns the name of the XML type for an interface described by the given UML.
     * For example, in ISO 19115-3, the XML type of {@code CI_Citation} is {@code CI_Citation_Type}.
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
        if (stereotype == Stereotype.ABSTRACT) {
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
        if (stereotype == Stereotype.ABSTRACT) {
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
        String name = firstIdentifier(uml);
        switch (name) {
            case "satisfiedPlan": {
                if (org.opengis.metadata.acquisition.Requirement.class.isAssignableFrom(enclosing)) {
                    name = "satisifiedPlan";                // Misspelling in ISO 19115-3:2016
                }
                break;
            }
            case "meteorologicalConditions": {
                if (org.opengis.metadata.acquisition.EnvironmentalRecord.class.isAssignableFrom(enclosing)) {
                    name = "meterologicalConditions";       // Misspelling in ISO 19115-3:2016
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
        assertNotNull(namespace, "Missing namespace.");
        assertFalse(namespace.trim().isEmpty(), "Missing namespace.");
        /*
         * Get the namespace declared at the package level, and ensure the
         * given namespace is not redundant with that package-level namespace.
         */
        final XmlSchema schema = impl.getPackage().getAnnotation(XmlSchema.class);
        assertNotNull(schema, "Missing @XmlSchema annotation in package-info.");
        final String schemaNamespace = schema.namespace();      // May be XMLConstants.NULL_NS_URI
        assertFalse(namespace.equals(schemaNamespace), "Namespace declaration is redundant with package-info @XmlSchema.");
        /*
         * Resolve the namespace given in argument: using the class-level namespace if needed,
         * or the package-level namespace if the class-level one is not defined.
         */
        if (DEFAULT.equals(namespace)) {
            final XmlType type = impl.getAnnotation(XmlType.class);
            if (type == null || DEFAULT.equals(namespace = type.namespace())) {
                namespace = schemaNamespace;
            }
            assertFalse(XMLConstants.NULL_NS_URI.equals(namespace), "No namespace defined.");
        }
        /*
         * Check that the namespace is declared in the package-level @XmlNs annotation.
         * We do not verify the validity of those @XmlNs annotations, since this is the
         * purpose of the `testPackageAnnotations()` method.
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
        if (uml != null && false) {     // This verification is available only on development branches.
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
     * Returns {@code true} if the given method is public from a GeoAPI point of view.
     */
    private static boolean isPublic(final Method method) {
        return (method.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0;
    }

    /**
     * Returns {@code true} if the given method should be ignored,
     * either because it is a standard method from the JDK or because it is a non-standard extension.
     * If {@code true}, then {@code method} does not need to have {@link UML} or {@link XmlElement} annotation.
     *
     * @param  method  the method to verify.
     * @return {@code true} if the given method should be ignored, or {@code false} otherwise.
     */
    protected boolean isIgnored(final Method method) {
        switch (method.getName()) {
            /*
             * Spelling changed.
             */
            case "getCenterPoint": {
                return true;
            }
            /*
             * Method that override an annotated method in parent class.
             */
            case "getUnits": {
                return org.opengis.metadata.content.Band.class.isAssignableFrom(method.getDeclaringClass());
            }
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
             * `Metaquality` override `Element` with only a change of obligation.
             * We do not duplicate the Java methods only for that.
             */
            case "getDerivedElements": {
                return org.opengis.metadata.quality.Metaquality.class.isAssignableFrom(method.getDeclaringClass());
            }
            /*
             * Property which exists in a deprecated and a non-deprecated version,
             * with the same name but different namespace.
             */
            case "usageDateTime": {
                return method.isAnnotationPresent(Deprecated.class);
            }
            /*
             * - "resultContent" is a property in the ISO 10157 model but not yet in the XML schema.
             * - "resultFormat" and "resultFile" differ in XML schema compared to abstract model (different obligation).
             */
            case "getResultContent":
            case "getResultFormat":
            case "getResultFile": {
                return org.opengis.metadata.quality.CoverageResult.class.isAssignableFrom(method.getDeclaringClass());
            }
            /*
             * GeoAPI addition, not in standard model.
             */
            case "getMeasure": {
                return org.opengis.metadata.quality.Element.class.isAssignableFrom(method.getDeclaringClass());
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
            assertNotNull(uml, "Missing @UML annotation.");
            if (!ControlledVocabulary.class.isAssignableFrom(type)) {
                for (final Method method : type.getDeclaredMethods()) {
                    if (isPublic(method)) {
                        testingMethod = method.getName();
                        if (!isIgnored(method)) {
                            uml = method.getAnnotation(UML.class);
                            if (!method.isAnnotationPresent(Deprecated.class)) {
                                assertNotNull(uml, "Missing @UML annotation.");
                            }
                        }
                    }
                }
            }
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the annotations in the {@code package-info} files of Apache SIS implementations of the
     * interfaces enumerated in the {@link #types} array. More specifically this method tests that:
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
                    assertNotNull(p, "Missing package information.");
                    packages.add(p);
                }
            }
        }
        for (final Package p : packages) {
            for (final XmlNs ns : p.getAnnotation(XmlSchema.class).xmlns()) {
                testingClass = p.getName();
                final String namespace = ns.namespaceURI();
                assertEquals(Namespaces.getPreferredPrefix(namespace, null), ns.prefix(), "Unexpected namespace prefix.");
            }
        }
        loggings.assertNoUnexpectedLog();
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
    public void testImplementationAnnotations() {
        for (final Class<?> type : types) {
            if (ControlledVocabulary.class.isAssignableFrom(type)) {
                // Skip code lists, since they are not the purpose of this test.
                continue;
            }
            testingClass = type.getCanonicalName();
            /*
             * Get the implementation class, which is mandatory (otherwise the
             * subclass shall not include the interface in the `types` array).
             */
            final Class<?> impl = getImplementation(type);
            assertNotNull(impl, "No implementation found.");
            assertNotSame(type, impl, "No implementation found.");
            testingClass = impl.getCanonicalName();
            /*
             * Compare the XmlRootElement with the UML annotation, if any. The UML annotation
             * is mandatory in the default implementation of the `testInterfaceAnnotations()`
             * method, but we don't require the UML to be non-null here since this is not the
             * job of this test method. This is because subclasses may choose to override the
             * `testInterfaceAnnotations()` method.
             */
            final XmlRootElement root = impl.getAnnotation(XmlRootElement.class);
            assertNotNull(root, "Missing @XmlRootElement annotation.");
            final UML uml = type.getAnnotation(UML.class);
            Stereotype stereotype = null;
            if (uml != null) {
                final Classifier c = type.getAnnotation(Classifier.class);
                if (c != null) {
                    stereotype = c.value();
                }
                assertEquals(getExpectedXmlRootElementName(stereotype, uml), root.name(), "Wrong @XmlRootElement.name().");
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
            assertNotNull(xmlType, "Missing @XmlType annotation.");
            String expected = getExpectedXmlTypeName(stereotype, uml);
            if (expected == null) {
                expected = DEFAULT;
            }
            assertEquals(expected, xmlType.name(), "Wrong @XmlType.name().");
        }
        loggings.assertNoUnexpectedLog();
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
                 * Implementation existence are tested by `testImplementationAnnotations()`.
                 * It is not the purpose of this test to verify again their existence.
                 */
                continue;
            }
            testingClass = impl.getCanonicalName();
            for (final Method method : type.getDeclaredMethods()) {
                if (!isPublic(method) || isIgnored(method)) {
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
                    final String identifier = firstIdentifier(uml);
                    for (final Method pm : impl.getDeclaredMethods()) {
                        final XmlElement e = pm.getAnnotation(XmlElement.class);
                        if (e != null && identifier.equals(e.name())) {
                            final boolean isPublic = isPublic(pm);
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
                     * In a few case the annotation is not on a getter method, but directly on the field.
                     * The main case is the "pass" field in DefaultConformanceResult.
                     */
                    if (element == null) try {
                        element = impl.getDeclaredField(identifier).getAnnotation(XmlElement.class);
                        assertNotNull(element, "Missing @XmlElement annotation.");
                    } catch (NoSuchFieldException e) {
                        fail("Missing @XmlElement annotation.");
                        continue;   // As a metter of principle (should never reach this point).
                    }
                }
                /*
                 * The UML annotation is mandatory in the default implementation of the
                 * `testInterfaceAnnotations()` method, but we don't require the UML to
                 * be non-null here since this is not the job of this test method. This
                 * is because subclasses may choose to override the above test method.
                 */
                if (uml != null) {
                    assertEquals(getExpectedXmlElementName(type, uml), element.name(), "Wrong @XmlElement.name().");
                    if (!method.isAnnotationPresent(Deprecated.class) && uml.version() == 0) {
                        assertEquals(uml.obligation() == Obligation.MANDATORY, element.required(), "Wrong @XmlElement.required().");
                    }
                }
                /*
                 * Check that the namespace is the expected one (according subclass)
                 * and is not redundant with the package @XmlSchema annotation.
                 */
                assertExpectedNamespace(element.namespace(), impl, uml);
            }
        }
        loggings.assertNoUnexpectedLog();
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
                assertEquals(getter.getDeclaringClass(), setter.getDeclaringClass(),
                        "The setter method must be declared in the same class as the " +
                        "getter method - not in a parent class, to avoid issues with JAXB.");
                assertEquals(getter.getReturnType(), TestUtilities.getSingleton(setter.getParameterTypes()),
                        "The setter parameter type shall be the same as the getter return type.");
                element = getter.getAnnotation(XmlElement.class);
                assertEquals((element == null),
                             getter.isAnnotationPresent(XmlElementRef.class) ||
                             getter.isAnnotationPresent(XmlElementRefs.class),
                             "Expected @XmlElement XOR @XmlElementRef.");
            }
            /*
             * If the annotation is @XmlElement, ensure that XmlElement.name() is equal
             * to the UML identifier. Then verify that the namespace is the expected one.
             */
            if (element != null) {
                assertFalse(wrapper.isInherited, "Expected @XmlElementRef.");
                final UML uml = type.getAnnotation(UML.class);
                if (uml != null) {                  // `assertNotNull` is `testInterfaceAnnotations()` job.
                    assertEquals(getExpectedXmlRootElementName(null, uml), element.name(), "Wrong @XmlElement.");
                }
                final String namespace = assertExpectedNamespace(element.namespace(), wrapper.type, uml);
                if (!ControlledVocabulary.class.isAssignableFrom(type)) {
                    final String expected = getNamespace(getImplementation(type));
                    if (expected != null) {         // `assertNotNull` is `testImplementationAnnotations()` job.
                        assertEquals(expected, namespace, "Inconsistent @XmlRootElement namespace.");
                    }
                }
            }
        }
        loggings.assertNoUnexpectedLog();
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
     * Unconditionally fails the test. This method is equivalent to the JUnit {@code fail(String)} method
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
     * Fails the test if the given condition is false. This method is equivalent to the JUnit
     * {@link assertTrue(boolean, String)} method except that the error message contains the
     * {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  condition  the condition that must be {@code true}.
     * @param  message    the message in case of failure.
     */
    protected final void assertTrue(final boolean condition, final String message) {
        if (!condition) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given condition is true. This method is equivalent to the JUnit
     * {@code assertFalse(boolean, String)} method except that the error message contains the
     * {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  condition  the condition that must be {@code false}.
     * @param  message    the message in case of failure.
     */
    protected final void assertFalse(final boolean condition, final String message) {
        if (condition) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given object is null. This method is equivalent to the JUnit
     * {@code assertNotNull(Object, String)} method except that the error message contains
     * the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  obj      the object that must be non-null.
     * @param  message  the message in case of failure.
     */
    protected final void assertNotNull(final Object obj, final String message) {
        if (obj == null) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given objects are the same. This method is equivalent to the JUnit
     * {@code assertNotSame(Object, Object, String)} except that the error message contains the
     * {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  o1       the first object (may be null).
     * @param  o2       the second object (may be null).
     * @param  message  the message in case of failure.
     */
    protected final void assertNotSame(final Object o1, final Object o2, final String message) {
        if (o1 == o2) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given objects are not the same. This method is equivalent to JUnit
     * {@code assertSame(Object, Object, String)} method except that the error message contains
     * the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  expected  the first object (may be null).
     * @param  actual    the second object (may be null).
     * @param  message   the message in case of failure.
     */
    protected final void assertSame(final Object expected, final Object actual, final String message) {
        if (expected != actual) throw new AssertionFailedError(location(message));
    }

    /**
     * Fails the test if the given objects are not equal. This method is equivalent to JUnit
     * {@code assertEquals(String, Object, Object)} except that the error
     * message contains the {@link #testingClass} and {@link #testingMethod}.
     *
     * @param  expected  the first object (may be null).
     * @param  actual    the second object (may be null).
     * @param  message   the message in case of failure.
     */
    protected final void assertEquals(final Object expected, final Object actual, final String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionFailedError(location(message) + System.lineSeparator()
                        + "Expected " + expected + " but got " + actual);
        }
    }
}
