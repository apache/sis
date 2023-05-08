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

import java.util.Map;
import java.util.List;
import java.util.Collection;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.QuantitativeResult;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Branch-dependent imports
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Property;


/**
 * Tests common to {@link DenseFeatureTest} and {@link SparseFeatureTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Marc le Bihan
 * @version 0.8
 * @since   0.5
 */
public abstract class FeatureTestCase extends TestCase {
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
     * Asserts that {@link AbstractFeature#getProperty(String)} returns the given instance.
     * This assertion is verified after a call to {@link AbstractFeature#setProperty(Property)}
     * and should be true for all Apache SIS concrete implementations. But it is not guaranteed
     * to be true for non-SIS implementations, for example built on top of {@code AbstractFeature}
     * without overriding {@code setProperty(Property)}.
     * Consequently, this assertion needs to be relaxed by {@link AbstractFeatureTest}.
     *
     * @param  name      the property name to check.
     * @param  expected  the expected property instance.
     * @param  modified  {@code true} if {@code expected} has been modified <strong>after</strong> it has been set
     *                   to the {@link #feature} instance. Not all feature implementations can see such changes.
     * @return {@code true} if the property is the expected instance, or {@code false} if it is another instance.
     */
    boolean assertSameProperty(final String name, final Property expected, final boolean modified) {
        assertSame(name, expected, feature.getProperty(name));
        return true;
    }

    /**
     * Returns the attribute value of the current {@link #feature} for the given name.
     */
    private Object getAttributeValue(final String name) {
        final Object value = feature.getPropertyValue(name);
        if (getValuesFromProperty) {
            /*
             * Verifies consistency with the Attribute instance:
             *   - The AttributeType shall be the same than the one provided by FeatureType for the given name.
             *   - Attribute value shall be the same than the one we got at the beginning of this method.
             *   - Attribute values (as a collection) is either empty or contains the same value.
             */
            final Attribute<?> property = (Attribute<?>) feature.getProperty(name);
            assertSame(name, feature.getType().getProperty(name), property.getType());
            assertSame(name, value, property.getValue());
            final Collection<?> values = property.getValues();
            if (value != null) {
                assertSame(name, value, TestUtilities.getSingleton(values));
            } else {
                assertTrue(name, values.isEmpty());
            }
            /*
             * Invoking getProperty(name) twice should return the same Property instance at least with
             * Apache SIS Feature implementations. Other implementations may relax this requirement.
             */
            assertSameProperty(name, property, false);
        }
        return value;
    }

    /**
     * Sets the attribute of the given name to the given value.
     * First, this method verifies that the previous value is equal to the given one.
     * Then, this method set the attribute to the given value and check if the result.
     *
     * @param  name      the name of the attribute to set.
     * @param  oldValue  the expected old value (may be {@code null}).
     * @param  newValue  the new value to set.
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
     * <h4>Historical note</h4>
     * In a previous SIS version, the first property value was always {@code null}
     * if the implementation was {@link DenseFeature} (see SIS-178).
     * This test reproduced the bug, and now aim to avoid regression.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-178">SIS-178</a>
     */
    @Test
    public void testGetProperty() {
        final DefaultFeatureType type = new DefaultFeatureType(
                Map.of(DefaultFeatureType.NAME_KEY, "My shapefile"), false, null,
                DefaultAttributeTypeTest.attribute("COMMUNE"),
                DefaultAttributeTypeTest.attribute("REF_INSEE"),
                DefaultAttributeTypeTest.attribute("CODE_POSTAL"));

        feature = createFeature(type);
        feature.setPropertyValue("COMMUNE",     "Bagneux");
        feature.setPropertyValue("REF_INSEE",   "92007");
        feature.setPropertyValue("CODE_POSTAL", "92220");

        assertEquals("CODE_POSTAL", "92220",   feature.getProperty("CODE_POSTAL").getValue());
        assertEquals("REF_INSEE",   "92007",   feature.getProperty("REF_INSEE")  .getValue());
        assertEquals("COMMUNE",     "Bagneux", feature.getProperty("COMMUNE")    .getValue());

        assertEquals("CODE_POSTAL", "92220",   feature.getPropertyValue("CODE_POSTAL"));
        assertEquals("REF_INSEE",   "92007",   feature.getPropertyValue("REF_INSEE"));
        assertEquals("COMMUNE",     "Bagneux", feature.getPropertyValue("COMMUNE"));
    }

    /**
     * Tests the {@link AbstractFeature#getPropertyValue(String)} method on a simple feature without super-types.
     * This method:
     *
     * <ul>
     *   <li>Verifies setting attribute values.</li>
     *   <li>Verifies that attempts to set an attribute value of the wrong type throw an exception
     *       and leave the previous value unchanged.</li>
     *   <li>Verifies feature clone.</li>
     *   <li>Verifies serialization.</li>
     * </ul>
     */
    @Test
    @DependsOnMethod("testGetProperty")
    public final void testSimpleValues() {
        feature = createFeature(DefaultFeatureTypeTest.city());
        setAttributeValue("city", "Utopia", "Atlantide");
        /*
         * At this point we have the following "City" feature:
         *   ┌────────────┬─────────┬──────────────┬───────────┐
         *   │ Name       │ Type    │ Multiplicity │ Value     │
         *   ├────────────┼─────────┼──────────────┼───────────┤
         *   │ city       │ String  │   [1 … 1]    │ Atlantide │
         *   │ population │ Integer │   [1 … 1]    │           │
         *   └────────────┴─────────┴──────────────┴───────────┘
         * Verify that attempt to set an illegal value fail.
         */
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
        verifyQualityReports("population");
        setAttributeValue("population", null, 1000);
        verifyQualityReports();
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
         * Set the attribute value on a property having [0 … ∞] multiplicity.
         * The feature implementation should put the value in a list.
         */
        assertEquals("universities", List.of(), getAttributeValue("universities"));
        feature.setPropertyValue("universities", "University of arts");
        assertEquals("universities", List.of("University of arts"), getAttributeValue("universities"));
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
        verifyQualityReports("isGlobal", "temperature");
        setAttributeValue("isGlobal", null, Boolean.TRUE);
        verifyQualityReports("temperature");
        /*
         * Opportunist tests using the existing instance.
         */
        testSerialization();
        try {
            testClone("population", 8405837, 8405838);          // A birth...
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
        final CustomAttribute<String> city = new CustomAttribute<>(Features.cast(
                (AttributeType<?>) feature.getType().getProperty("city"), String.class));

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
        if (assertSameProperty("city", city, true)) {
            /*
             * The quality report is expected to contains a custom element.
             */
            int numOccurrences = 0;
            final DataQuality quality = verifyQualityReports("population");
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
    }

    /**
     * Tests addition of values in a multi-valued property.
     */
    @Test
    @DependsOnMethod("testSimpleProperties")
    public void testAddToCollection() {
        feature = createFeature(new DefaultFeatureType(
                Map.of(DefaultFeatureType.NAME_KEY, "City"),
                false, null, DefaultAttributeTypeTest.universities()));
        /*
         * The value below is an instance of Collection<String>.
         * But the <String> parameterized type cannot be verified at runtime.
         * The best check we can have is Collection<?>, which does not allow addition of new values.
         */
        Collection<?> values = (Collection<?>) feature.getPropertyValue("universities");
        assertTrue("isEmpty", values.isEmpty());
        // Cannot perform values.add("something") here.

        feature.setPropertyValue("universities", List.of("UCAR", "Marie-Curie"));
        values = (Collection<?>) feature.getPropertyValue("universities");
        assertArrayEquals(new String[] {"UCAR", "Marie-Curie"}, values.toArray());
    }

    /**
     * Asserts that {@link AbstractFeature#quality()} reports no anomaly, or only anomalies for the given properties.
     *
     * @param  anomalousProperties  the property for which we expect a report.
     * @return the data quality report.
     */
    private DataQuality verifyQualityReports(final String... anomalousProperties) {
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
     * @param  property  the name of a property to change.
     * @param  oldValue  the old value of the given property.
     * @param  newValue  the new value of the given property.
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
        assertEquals("Tokyo", clone.getProperty("city").getValue());
        assertEquals("hashCode", feature.hashCode(), clone.hashCode());
        assertEquals("equals", feature, clone);
        /*
         * For the other Feature instance to contain full Property object and test again.
         */
        assertEquals("Tokyo", feature.getProperty("city").getValue());
        assertEquals("hashCode", feature.hashCode(), clone.hashCode());
        assertEquals("equals", feature, clone);
    }
}
