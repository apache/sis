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
package org.apache.sis.feature.builder;

import java.util.Map;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import org.opengis.geometry.Envelope;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.feature.DefaultFeatureTypeTest;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;


/**
 * Tests {@link FeatureTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Michael Hausegger
 */
@SuppressWarnings("exports")
public final class FeatureTypeBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public FeatureTypeBuilderTest() {
    }

    /**
     * Verifies that {@link FeatureTypeBuilder#setSuperTypes(FeatureType...)} ignores null parents.
     * This method tests only the builder state without creating feature type.
     */
    @Test
    public void testNullParents() {
        var builder = new FeatureTypeBuilder(null);
        assertSame(builder, builder.setSuperTypes(new FeatureType[6]));
        assertEquals(0, builder.getSuperTypes().length);
    }

    /**
     * Verifies {@link FeatureTypeBuilder#setAbstract(boolean)}.
     * This method tests only the builder state without creating feature type.
     */
    @Test
    public void testSetAbstract() {
        var builder = new FeatureTypeBuilder(null);
        assertFalse(builder.isAbstract());
        assertSame (builder, builder.setAbstract(true));
        assertTrue (builder.isAbstract());
    }

    /**
     * Verifies {@link FeatureTypeBuilder#setDeprecated(boolean)}.
     * This method tests only the builder state without creating feature type.
     */
    @Test
    public void testSetDeprecated() {
        var builder = new FeatureTypeBuilder();
        assertFalse(builder.isDeprecated());
        builder.setDeprecated(true);
        assertTrue(builder.isDeprecated());
    }

    /**
     * Verifies {@link FeatureTypeBuilder#setNameSpace(CharSequence)}.
     */
    @Test
    public void testSetNameSpace() {
        var builder = new FeatureTypeBuilder();
        assertNull(builder.getNameSpace());
        assertSame(builder, builder.setNameSpace("myNameSpace"));
        assertEquals("myNameSpace", builder.getNameSpace());
    }

    /**
     * Tests with the minimum number of parameters (no property and no super type).
     */
    @Test
    public void testInitialization() {
        var builder = new FeatureTypeBuilder();
        var e = assertThrows(IllegalArgumentException.class, () -> builder.build(),
                "Builder should have failed if there is not at least a name set.");
        assertMessageContains(e, "name");

        assertSame(builder, builder.setName("scope", "test"));
        final var feature = builder.build();
        assertEquals("scope:test", feature.getName().toString());
        assertFalse (feature.isAbstract());
        assertEquals(0, feature.getProperties(true).size());
        assertEquals(0, feature.getSuperTypes().size());
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)}.
     */
    @Test
    public void testAddAttribute() {
        final var builder = new FeatureTypeBuilder();
        assertSame(builder, builder.setName("myScope", "myName"));
        assertSame(builder, builder.setDefinition ("test definition"));
        assertSame(builder, builder.setDesignation("test designation"));
        assertSame(builder, builder.setDescription("test description"));
        assertSame(builder, builder.setAbstract(true));
        builder.addAttribute(String .class).setName("name");
        builder.addAttribute(Integer.class).setName("age");
        builder.addAttribute(Point  .class).setName("location").setCRS(HardCodedCRS.WGS84);
        builder.addAttribute(Double .class).setName("score").setDefaultValue(10.0).setMinimumOccurs(5).setMaximumOccurs(50);

        final var feature = builder.build();
        assertEquals("myScope:myName",   feature.getName().toString());
        assertEquals("test definition",  feature.getDefinition().toString());
        assertEquals("test description", feature.getDescription().orElseThrow().toString());
        assertEquals("test designation", feature.getDesignation().orElseThrow().toString());
        assertTrue  (                    feature.isAbstract());

        final var it = feature.getProperties(true).iterator();
        final var a0 = attributeType(it.next());
        final var a1 = attributeType(it.next());
        final var a2 = attributeType(it.next());
        final var a3 = attributeType(it.next());
        assertFalse(it.hasNext());

        assertEquals("name",     a0.getName().toString());
        assertEquals("age",      a1.getName().toString());
        assertEquals("location", a2.getName().toString());
        assertEquals("score",    a3.getName().toString());

        assertEquals(String.class,  a0.getValueClass());
        assertEquals(Integer.class, a1.getValueClass());
        assertEquals(Point.class,   a2.getValueClass());
        assertEquals(Double.class,  a3.getValueClass());

        assertEquals(1, a0.getMinimumOccurs());
        assertEquals(1, a1.getMinimumOccurs());
        assertEquals(1, a2.getMinimumOccurs());
        assertEquals(5, a3.getMinimumOccurs());

        assertEquals( 1, a0.getMaximumOccurs());
        assertEquals( 1, a1.getMaximumOccurs());
        assertEquals( 1, a2.getMaximumOccurs());
        assertEquals(50, a3.getMaximumOccurs());

        assertEquals(null, a0.getDefaultValue());
        assertEquals(null, a1.getDefaultValue());
        assertEquals(null, a2.getDefaultValue());
        assertEquals(10.0, a3.getDefaultValue());

        assertFalse(AttributeConvention.characterizedByCRS(a0));
        assertFalse(AttributeConvention.characterizedByCRS(a1));
        assertTrue (AttributeConvention.characterizedByCRS(a2));
        assertFalse(AttributeConvention.characterizedByCRS(a3));
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)} where one property is an identifier
     * and another property is the geometry.
     */
    @Test
    public void testAddIdentifierAndGeometry() {
        final var builder = new FeatureTypeBuilder();
        assertSame(builder, builder.setName("scope", "test"));
        assertSame(builder, builder.setIdentifierDelimiters("-", "pref.", null));
        builder.addAttribute(String.class).setName("name")
                .addRole(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(Geometry.class).setName("shape")
                .setCRS(HardCodedCRS.WGS84)
                .addRole(AttributeRole.DEFAULT_GEOMETRY);

        final var feature = builder.build();
        assertEquals("scope:test", feature.getName().toString());
        assertFalse(feature.isAbstract());

        final var it = feature.getProperties(true).iterator();
        final var a0 = it.next();
        final var a1 = it.next();
        final var a2 = it.next();
        final var a3 = it.next();
        final var a4 = it.next();
        assertFalse(it.hasNext());

        assertEquals(AttributeConvention.IDENTIFIER_PROPERTY, a0.getName());
        assertEquals(AttributeConvention.ENVELOPE_PROPERTY,   a1.getName());
        assertEquals(AttributeConvention.GEOMETRY_PROPERTY,   a2.getName());
        assertEquals("name",                                  a3.getName().toString());
        assertEquals("shape",                                 a4.getName().toString());
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)} where one attribute is an identifier that already has
     * the {@code "sis:identifier"} name. This is called "anonymous" because identifiers with an explicit name in
     * the data file should use that name instead in the feature type.
     */
    @Test
    public void testAddAnonymousIdentifier() {
        final var builder = new FeatureTypeBuilder();
        assertSame(builder, builder.setName("City"));
        builder.addAttribute(String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY).addRole(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(Integer.class).setName("population");
        final var feature = builder.build();
        final var it = feature.getProperties(true).iterator();
        final var a0 = attributeType(it.next());
        final var a1 = attributeType(it.next());
        assertFalse(it.hasNext());
        assertEquals(AttributeConvention.IDENTIFIER_PROPERTY, a0.getName());
        assertEquals(String.class, a0.getValueClass());
        assertEquals("population", a1.getName().toString());
        assertEquals(Integer.class, a1.getValueClass());
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)} where one attribute is a geometry that already has
     * the {@code "sis:geometry"} name. This is called "anonymous" because geometries with an explicit name in
     * the data file should use that name instead in the feature type.
     */
    @Test
    public void testAddAnonymousGeometry() {
        final var builder = new FeatureTypeBuilder();
        assertSame(builder, builder.setName("City"));
        builder.addAttribute(Point.class).setName(AttributeConvention.GEOMETRY_PROPERTY).addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.addAttribute(Integer.class).setName("population");
        final var feature = builder.build();
        final var it = feature.getProperties(true).iterator();
        final var a0 = /*operation*/(it.next());
        final var a1 = attributeType(it.next());
        final var a2 = attributeType(it.next());
        assertFalse(it.hasNext());
        assertEquals(AttributeConvention.ENVELOPE_PROPERTY, a0.getName());
        assertEquals(AttributeConvention.GEOMETRY_PROPERTY, a1.getName());
        assertEquals(Point.class,   a1.getValueClass());
        assertEquals("population",  a2.getName().toString());
        assertEquals(Integer.class, a2.getValueClass());
    }

    /**
     * Tests {@link PropertyTypeBuilder#replaceBy(PropertyTypeBuilder)}.
     */
    @Test
    public void testReplace() {
        final var builder = new FeatureTypeBuilder();
        assertSame(builder, builder.setName("City"));
        final AttributeTypeBuilder<?> toReplace, replacement;
        /* unmodified */ builder.addAttribute(String.class).setName("name");
        toReplace      = builder.addAttribute(String.class).setName("someKey");
        /* unmodified */ builder.addAttribute(Integer.class).setName("population");
        replacement    = builder.addAttribute(Integer.class).setName("someId");

        var it = builder.build().getProperties(true).iterator();
        assertPropertyEquals("name",       String.class,  it.next());
        assertPropertyEquals("someKey",    String.class,  it.next());
        assertPropertyEquals("population", Integer.class, it.next());
        assertPropertyEquals("someId",     Integer.class, it.next());
        assertFalse(it.hasNext());

        toReplace.replaceBy(replacement);
        assertMessageContains(assertThrows(IllegalStateException.class, () -> toReplace.setName("Foo")));
        it = builder.build().getProperties(true).iterator();
        assertPropertyEquals("name",       String.class,  it.next());
        assertPropertyEquals("someId",     Integer.class, it.next());
        assertPropertyEquals("population", Integer.class, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Tests creation of a builder from an existing feature type.
     * This method also acts as a test of {@code FeatureTypeBuilder} getter methods.
     */
    @Test
    public void testCreateFromTemplate() {
        final var builder = new FeatureTypeBuilder(DefaultFeatureTypeTest.capital());
        assertEquals("Capital", builder.getName().toString());
        assertEquals("City",    assertSingleton(builder.getSuperTypes()).getName().toString());
        assertFalse (           builder.isAbstract());

        // The list of properties does not include super-type properties.
        final var a0 = attributeTypeBuilder(assertSingleton(builder.properties()));
        assertEquals("parliament",  a0.getName().toString());
        assertEquals(String.class,  a0.getValueClass());
        assertTrue  (               a0.roles().isEmpty());
    }

    /**
     * Tests creation of a builder from an existing feature type with some attributes having {@link AttributeRole}s.
     */
    @Test
    public void testCreateFromTemplateWithRoles() {
        var builder = new FeatureTypeBuilder().setName("City");
        builder.addAttribute(String  .class).setName("name").roles().add(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(Integer .class).setName("population");
        builder.addAttribute(Geometry.class).setName("area").roles().add(AttributeRole.DEFAULT_GEOMETRY);

        final var feature = builder.build();
        builder = new FeatureTypeBuilder(feature);
        assertEquals("City", builder.getName().toString());
        assertEquals(0, builder.getSuperTypes().length);

        final var it = builder.properties().iterator();
        final var a0 = attributeTypeBuilder(it.next());
        final var a1 = attributeTypeBuilder(it.next());
        final var a2 = attributeTypeBuilder(it.next());
        assertFalse(it.hasNext());
        assertEquals("name",       a0.getName().toString());
        assertEquals("population", a1.getName().toString());
        assertEquals("area",       a2.getName().toString());

        assertEquals(String.class,   a0.getValueClass());
        assertEquals(Integer.class,  a1.getValueClass());
        assertEquals(Geometry.class, a2.getValueClass());

        assertTrue  (a1.roles().isEmpty());
        assertEquals(AttributeRole.IDENTIFIER_COMPONENT, assertSingleton(a0.roles()));
        assertEquals(AttributeRole.DEFAULT_GEOMETRY,     assertSingleton(a2.roles()));
    }

    /**
     * Verifies that {@code build()} method returns the previously created instance when possible.
     * See {@link AttributeTypeBuilder#build()} javadoc for a rational.
     */
    @Test
    public void testBuildCache() {
        final var builder   = new FeatureTypeBuilder().setName("City");
        final var attribute = builder.addAttribute(String.class).setName("name").build();
        final var feature   = builder.build();
        assertSame(attribute, feature.getProperty("name"), "Should return the existing AttributeType.");
        assertSame(feature, builder.build(), "Should return the existing FeatureType.");

        assertSame(attribute, builder.getProperty("name").build(),
                "Should return the existing AttributeType since we didn't changed anything.");

        assertNotSame(attribute, builder.getProperty("name").setDescription("Name of the city").build(),
                "Should return a new AttributeType since we changed something.");

        assertNotSame(feature, builder.build(),
                "Should return a new FeatureType since we changed an attribute.");
    }

    /**
     * Tests overriding the "sis:envelope" property. This may happen when the user wants to specify
     * envelope himself instead of relying on the automatically computed value.
     */
    @Test
    public void testEnvelopeOverride() {
        var builder = new FeatureTypeBuilder().setName("CoverageRecord").setAbstract(true);
        builder.addAttribute(Geometry.class).setName(AttributeConvention.GEOMETRY_PROPERTY).addRole(AttributeRole.DEFAULT_GEOMETRY);
        final var parentFeature = builder.build();

        builder = new FeatureTypeBuilder().setName("Record").setSuperTypes(parentFeature);
        builder.addAttribute(Envelope.class).setName(AttributeConvention.ENVELOPE_PROPERTY);
        final var childFeature = builder.build();

        final var it = childFeature.getProperties(true).iterator();
        assertPropertyEquals("sis:envelope", Envelope.class, it.next());
        assertPropertyEquals("sis:geometry", Geometry.class, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Tests overriding an attribute by an operation.
     * This is the converse of {@link #testEnvelopeOverride()}.
     */
    @Test
    public void testOverrideByOperation() {
        var builder   = new FeatureTypeBuilder().setName("Parent").setAbstract(true);
        var attribute = builder.addAttribute(Integer.class).setName("A").build();
        /* no local */  builder.addAttribute(Integer.class).setName("B");
        final var parentFeature = builder.build();

        builder = new FeatureTypeBuilder().setName("Child").setSuperTypes(parentFeature);
        builder.addProperty(FeatureOperations.link(Map.of(AbstractOperation.NAME_KEY, "B"), attribute));
        final var childFeature = builder.build();

        final var it = childFeature.getProperties(true).iterator();
        assertPropertyEquals("A", Integer.class, it.next());
        assertPropertyEquals("B", Integer.class, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verifies that the given property is an attribute with the given name and value class.
     */
    private static void assertPropertyEquals(final String name, final Class<?> valueClass, IdentifiedType property) {
        assertEquals(name, property.getName().toString());
        if (property instanceof Operation op) {
            property = op.getResult();
        }
        assertEquals(valueClass, attributeType(property).getValueClass());
    }

    /**
     * Casts a property to an attribute.
     */
    private static AttributeType<?> attributeType(final IdentifiedType property) {
        return assertInstanceOf(AttributeType.class, property);
    }

    /**
     * Casts a property builder to an attribute builder.
     */
    private static AttributeTypeBuilder<?> attributeTypeBuilder(final PropertyTypeBuilder builder) {
        return assertInstanceOf(AttributeTypeBuilder.class, builder);
    }
}
