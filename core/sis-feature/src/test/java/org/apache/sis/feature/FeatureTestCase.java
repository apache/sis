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
package org.apache.sis.feature;

import java.util.Collection;
import java.util.Collections;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.QuantitativeResult;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests common to {@link DenseFeatureTest} and {@link SparseFeatureTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Marc le Bihan
 * @since   0.5
 * @version 0.6
 * @module
 */
public abstract strictfp class FeatureTestCase extends TestCase {
    /**
     * The feature being tested.
     */
    AbstractFeature feature;

    /**
     * {@code true} if {@link #getAttributeValue(String)} should invoke {@link AbstractFeature#getProperty(String)},
     * or {@code false} for invoking directly {@link AbstractFeature#getPropertyValue(String)}.
     */
    private boolean getValuesFromProperty;

    /**
     * For sub-class constructors only.
     */
    FeatureTestCase() {
    }

    /**
     * Creates a new feature for the given type.
     */
    abstract AbstractFeature createFeature(final DefaultFeatureType type);

    /**
     * Clones the {@link #feature} instance.
     */
    abstract AbstractFeature cloneFeature() throws CloneNotSupportedException;

    /**
     * Returns the attribute value of the current {@link #feature} for the given name.
     */
    private Object getAttributeValue(final String name) {
        final Object value = feature.getPropertyValue(name);
        if (getValuesFromProperty) {
            final AbstractAttribute<?> property = (AbstractAttribute<?>) feature.getProperty(name);

            // The AttributeType shall be the same than the one provided by FeatureType for the given name.
            assertSame(name, feature.getType().getProperty(name), property.getType());

            // Attribute value shall be the same than the one provided by FeatureType convenience method.
            assertSame(name, value, property.getValue());

            // Collection view shall contains the same value, or be empty.
            final Collection<?> values = property.getValues();
            if (value != null) {
                assertSame(name, value, TestUtilities.getSingleton(values));
            } else {
                assertTrue(name, values.isEmpty());
            }

            // Invoking getProperty(name) twice shall return the same Property instance.
            assertSame(name, property, feature.getProperty(name));
        }
        return value;
    }

    /**
     * Sets the attribute of the given name to the given value.
     * First, this method verifies that the previous value is equals to the given one.
     * Then, this method set the attribute to the given value and check if the result.
     *
     * @param name     The name of the attribute to set.
     * @param oldValue The expected old value (may be {@code null}).
     * @param newValue The new value to set.
     */
    private void setAttributeValue(final String name, final Object oldValue, final Object newValue) {
        assertEquals(name, oldValue, getAttributeValue(name));
        feature.setPropertyValue(name, newValue);
        assertEquals(name, newValue, getAttributeValue(name));
    }

    /**
     * Tests the {@link AbstractFeature#getProperty(String)} method. This test uses a very simple and
     * straightforward {@code FeatureType} similar to the ones obtained when reading a ShapeFile.
     *
     * <div class="note">In a previous SIS version, the first property value was always {@code null}
     * if the implementation was {@link DenseFeature} (see SIS-178). This test reproduced the bug,
     * and now aim to avoid regression.</div>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-178">SIS-178</a>
     */
    @Test
    public void testGetProperty() {
        final DefaultFeatureType type = new DefaultFeatureType(
                Collections.singletonMap(DefaultFeatureType.NAME_KEY, "My shapefile"), false, (DefaultFeatureType[]) null,
                DefaultAttributeTypeTest.attribute("COMMUNE"),
                DefaultAttributeTypeTest.attribute("REF_INSEE"),
                DefaultAttributeTypeTest.attribute("CODE_POSTAL"));

        feature = createFeature(type);
        feature.setPropertyValue("COMMUNE",     "Bagneux");
        feature.setPropertyValue("REF_INSEE",   "92007");
        feature.setPropertyValue("CODE_POSTAL", "92220");

        assertEquals("CODE_POSTAL", "92220",   ((AbstractAttribute) feature.getProperty("CODE_POSTAL")).getValue());
        assertEquals("REF_INSEE",   "92007",   ((AbstractAttribute) feature.getProperty("REF_INSEE"))  .getValue());
        assertEquals("COMMUNE",     "Bagneux", ((AbstractAttribute) feature.getProperty("COMMUNE"))    .getValue());

        assertEquals("CODE_POSTAL", "92220",   feature.getPropertyValue("CODE_POSTAL"));
        assertEquals("REF_INSEE",   "92007",   feature.getPropertyValue("REF_INSEE"));
        assertEquals("COMMUNE",     "Bagneux", feature.getPropertyValue("COMMUNE"));
    }

    /**
     * Tests the {@link AbstractFeature#getPropertyValue(String)} method on a simple feature without super-types.
     * This method also tests that attempts to set a value of the wrong type throw an exception and leave the
     * previous value unchanged, that the feature is cloneable and that serialization works.
     */
    @Test
    @DependsOnMethod("testGetProperty")
    public void testSimpleValues() {
        feature = createFeature(DefaultFeatureTypeTest.city());
        setAttributeValue("city", "Utopia", "Atlantide");
        try {
            feature.setPropertyValue("city", 2000);
            fail("Shall not be allowed to set a value of the wrong type.");
        } catch (ClassCastException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("city"));
            assertTrue(message, message.contains("Integer"));
        }
        assertEquals("Property shall not have been modified.", "Atlantide", getAttributeValue("city"));
        /*
         * Before we set the population attribute, the feature should be considered invalid.
         * After we set it, the feature should be valid since all mandatory attributes are set.
         */
        assertQualityReports("population");
        setAttributeValue("population", null, 1000);
        assertQualityReports();
        /*
         * Opportunist tests using the existing instance.
         */
        testSerialization();
        try {
            testClone("population", 1000, 1500);
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Tests the {@link AbstractFeature#getProperty(String)} method on a simple feature without super-types.
     * This method also tests that attempts to set a value of the wrong type throw an exception and leave the
     * previous value unchanged.
     */
    @Test
    @DependsOnMethod("testSimpleValues")
    public void testSimpleProperties() {
        getValuesFromProperty = true;
        testSimpleValues();
    }

    /**
     * Tests {@link AbstractFeature#getProperty(String)} and {@link AbstractFeature#getPropertyValue(String)}
     * on a "complex" feature, involving multi-valued properties, inheritances and property overriding.
     */
    @Test
    @DependsOnMethod({"testSimpleValues", "testSimpleProperties"})
    public void testComplexFeature() {
        feature = createFeature(DefaultFeatureTypeTest.worldMetropolis());
        setAttributeValue("city", "Utopia", "New York");
        setAttributeValue("population", null, 8405837); // Estimation for 2013.
        /*
         * Set the attribute value on a property having [0 … ∞] cardinality.
         * The feature implementation should put the value in a list.
         */
        assertEquals("universities", Collections.emptyList(), getAttributeValue("universities"));
        feature.setPropertyValue("universities", "University of arts");
        assertEquals("universities", Collections.singletonList("University of arts"), getAttributeValue("universities"));
        /*
         * Switch to 'getProperty' mode only after we have set at least one value,
         * in order to test the conversion of existing values to property instances.
         */
        getValuesFromProperty = true;
        final SimpleInternationalString region = new SimpleInternationalString("State of New York");
        setAttributeValue("region", null, region);
        /*
         * Adds more universities.
         */
        @SuppressWarnings("unchecked")
        final Collection<String> universities = (Collection<String>) feature.getPropertyValue("universities");
        assertTrue(universities.add("University of sciences"));
        assertTrue(universities.add("University of international development"));
        /*
         * In our 'metropolis' feature type, the region can be any CharSequence. But 'worldMetropolis'
         * feature type overrides the region property with a restriction to InternationalString.
         * Verifiy that this restriction is checked.
         */
        try {
            feature.setPropertyValue("region", "State of New York");
            fail("Shall not be allowed to set a value of the wrong type.");
        } catch (ClassCastException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("region"));
            assertTrue(message, message.contains("String"));
        }
        assertSame("region", region, getAttributeValue("region"));
        /*
         * Before we set the 'isGlobal' attribute, the feature should be considered invalid.
         * After we set it, the feature should be valid since all mandatory attributes are set.
         */
        assertQualityReports("isGlobal", "temperature");
        setAttributeValue("isGlobal", null, Boolean.TRUE);
        assertQualityReports("temperature");
        /*
         * Opportunist tests using the existing instance.
         */
        testSerialization();
        try {
            testClone("population", 8405837, 8405838); // A birth...
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Tests the possibility to plugin custom attributes via {@link AbstractFeature#setProperty(Property)}.
     */
    @Test
    @DependsOnMethod({"testSimpleValues", "testSimpleProperties"})
    public void testCustomAttribute() {
        feature = createFeature(DefaultFeatureTypeTest.city());
        final AbstractAttribute<String> wrong = SingletonAttributeTest.parliament();
        final CustomAttribute<String> city = new CustomAttribute<String>(Features.cast(
                (DefaultAttributeType<?>) feature.getType().getProperty("city"), String.class));

        feature.setProperty(city);
        setAttributeValue("city", "Utopia", "Atlantide");
        try {
            feature.setProperty(wrong);
            fail("Shall not be allowed to set a property of the wrong type.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("parliament"));
            assertTrue(message, message.contains("City"));
        }
        assertSame(city, feature.getProperty("city"));
        /*
         * The quality report is expected to contains a custom element.
         */
        int numOccurrences = 0;
        final DataQuality quality = assertQualityReports("population");
        for (final Element report : quality.getReports()) {
            final String identifier = report.getMeasureIdentification().toString();
            if (identifier.equals("city")) {
                numOccurrences++;
                final Result result = TestUtilities.getSingleton(report.getResults());
                assertInstanceOf("result", QuantitativeResult.class, result);
                assertEquals("quality.report.result.errorStatistic",
                        CustomAttribute.ADDITIONAL_QUALITY_INFO,
                        String.valueOf(((QuantitativeResult) result).getErrorStatistic()));
            }
        }
        assertEquals("Number of reports.", 1, numOccurrences);
    }

    /**
     * Asserts that {@link AbstractFeature#quality()} reports no anomaly, or only anomalies for the given properties.
     *
     * @param  anomalousProperties The property for which we expect a report.
     * @return The data quality report.
     */
    private DataQuality assertQualityReports(final String... anomalousProperties) {
        int anomalyIndex  = 0;
        final DataQuality quality = feature.quality();
        for (final Element report : quality.getReports()) {
            for (final Result result : report.getResults()) {
                if (result instanceof ConformanceResult && !((ConformanceResult) result).pass()) {
                    assertTrue("Too many reports", anomalyIndex < anomalousProperties.length);
                    final String propertyName = anomalousProperties[anomalyIndex];
                    final String identifier   = report.getMeasureIdentification().toString();
                    final String explanation  = ((ConformanceResult) result).getExplanation().toString();
                    assertEquals("quality.report.measureIdentification", propertyName, identifier);
                    assertTrue  ("quality.report.result.explanation", explanation.contains(propertyName));
                    anomalyIndex++;
                }
            }
        }
        assertEquals("Number of reports.", anomalousProperties.length, anomalyIndex);
        return quality;
    }

    /**
     * Tests the {@link AbstractFeature#clone()} method on the current {@link #feature} instance.
     * This method is invoked from other test methods using the existing feature instance in an opportunist way.
     *
     * @param  property The name of a property to change.
     * @param  oldValue The old value of the given property.
     * @param  newValue The new value of the given property.
     * @throws CloneNotSupportedException Should never happen.
     */
    private void testClone(final String property, final Object oldValue, final Object newValue)
            throws CloneNotSupportedException
    {
        final AbstractFeature clone = cloneFeature();
        assertNotSame("clone",      clone, feature);
        assertTrue   ("equals",     clone.equals(feature));
        assertTrue   ("hashCode",   clone.hashCode() == feature.hashCode());
        setAttributeValue(property, oldValue, newValue);
        assertEquals (property,     oldValue, clone  .getPropertyValue(property));
        assertEquals (property,     newValue, feature.getPropertyValue(property));
        assertFalse  ("equals",     clone.equals(feature));
        assertFalse  ("hashCode",   clone.hashCode() == feature.hashCode());
    }

    /**
     * Tests serialization of current {@link #feature} instance.
     * This method is invoked from other test methods using the existing feature instance in an opportunist way.
     */
    private void testSerialization() {
        assertNotSame(feature, assertSerializedEquals(feature));
    }

    /**
     * Tests {@code equals(Object)}.
     *
     * @throws CloneNotSupportedException Should never happen.
     */
    @Test
    @DependsOnMethod("testSimpleProperties")
    public void testEquals() throws CloneNotSupportedException {
        feature = createFeature(DefaultFeatureTypeTest.city());
        feature.setPropertyValue("city", "Tokyo");
        final AbstractFeature clone = cloneFeature();
        assertEquals(feature, clone);
        /*
         * Force the conversion of a property value into a full Property object on one and only one of
         * the Features to be compared. The implementation shall be able to wrap or unwrap the values.
         */
        assertEquals("Tokyo", ((AbstractAttribute) clone.getProperty("city")).getValue());
        assertEquals("hashCode", feature.hashCode(), clone.hashCode());
        assertEquals("equals", feature, clone);
        /*
         * For the other Feature instance to contain full Property object and test again.
         */
        assertEquals("Tokyo", ((AbstractAttribute) feature.getProperty("city")).getValue());
        assertEquals("hashCode", feature.hashCode(), clone.hashCode());
        assertEquals("equals", feature, clone);
    }
}
