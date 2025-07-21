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
package org.apache.sis.feature.privy;

import java.util.Optional;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.Static;
import org.apache.sis.util.iso.Names;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.geometry.wrapper.Geometries;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;


/**
 * Defines the names of some properties or characteristics for which we assign a conventional usage.
 * Properties with the names defined in this {@code AttributeConvention} class are often aliases generated
 * by the SIS implementation of various file readers. Those synthetic properties redirect to the most
 * appropriate "real" property in the feature.
 *
 * <div class="note"><b>Example:</b>
 * one of the most frequently used synthetic property is {@code "sis:identifier"}, which contains a unique
 * identifier (or primary key) for the feature. This property is usually (but not necessarily)
 * a {@linkplain org.apache.sis.feature.FeatureOperations#link link to an existing attribute}.
 * By using the {@code "sis:identifier"} alias, users do not need to know the name of the "real" attribute.
 * </div>
 *
 * This class defines names for two kinds of usage:
 * <ul>
 *   <li>Names ending with {@code "_PROPERTY"} are used for attributes or operations that are members of the
 *       collection returned by {@link org.apache.sis.feature.DefaultFeatureType#getProperties(boolean)}.</li>
 *   <li>Names ending with {@code "_CHARACTERISTIC"} are used for characteristics that are entries of the
 *       map returned by {@link org.apache.sis.feature.DefaultAttributeType#characteristics()}.</li>
 * </ul>
 *
 * <h2>Mixing with other conventions</h2>
 * The conventions defined in this class are specific to Apache SIS.
 * Current implementation does not support any other convention than the SIS one,
 * but we may refactor this class in future SIS versions if there is a need to support different conventions.
 *
 * <p>In order to reduce the risk of name collision with properties in user-defined features
 * (e.g. the user may already have an attribute named {@code "identifier"} for his own purpose),
 * all names defined in this class begin with the {@code "@"} character.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AttributeConvention extends Static {
    /**
     * Scope of all names defined by SIS convention.
     */
    private static final LocalName SCOPE = Names.createLocalName("Apache", null, "sis");

    /**
     * Conventional name for a property used as a unique identifier.
     * The identifier should be unique in the {@link org.apache.sis.storage.DataStore} instance containing the feature
     * (for example a {@code DataStore} opened for a XML file), but does not need to be unique between two independent
     * {@code DataStore} instances.
     *
     * <p>Properties of this name are usually
     * {@linkplain org.apache.sis.feature.FeatureOperations#link aliases for existing attributes}, or
     * {@linkplain org.apache.sis.feature.FeatureOperations#compound compound keys} made by concatenation
     * of two or more other attributes.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} is usually
     * {@link String}, {@link Integer}, {@link java.util.UUID} or other types commonly used as identifiers.</p>
     */
    public static final ScopedName IDENTIFIER_PROPERTY = Names.createScopedName(SCOPE, null, "identifier");

    /**
     * Conventional name for a property containing the geometric object to use by default.
     * Some features may contain more than one geometric object; this property tells which
     * geometry to render on a map for example.
     *
     * <p>Properties of this name are usually {@linkplain org.apache.sis.feature.FeatureOperations#link
     * operations acting as a redirection to another attribute}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} can be
     * the {@link com.esri.core.geometry.Geometry} class from ESRI's API, or the {@code Geometry} class from
     * <cite>Java Topology Suite</cite> (JTS) library, or any other class defined in future SIS versions.
     * See {@link #isGeometryAttribute(IdentifiedType)} for testing whether the value is a supported type.</p>
     *
     * @see #isGeometryAttribute(IdentifiedType)
     */
    public static final ScopedName GEOMETRY_PROPERTY = Names.createScopedName(SCOPE, null, "geometry");

    /**
     * Conventional name for fetching the envelope encompassing all geometries in a feature.
     * Most {@code FeatureType}s have at most one geometry, which is also the {@link #GEOMETRY_PROPERTY default geometry}.
     * But if several geometries exist, then the value for this synthetic property is the union of all geometries.
     *
     * <p>Properties of this name are usually
     * {@linkplain org.apache.sis.feature.FeatureOperations#envelope operations}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link org.opengis.geometry.Envelope}.</p>
     */
    public static final ScopedName ENVELOPE_PROPERTY = Names.createScopedName(SCOPE, null, "envelope");

    /**
     * Conventional name for fetching the Coordinate Reference System (CRS) of a geometry or a coverage.
     * This characteristic is typically an entry in the map returned by a call to the
     * {@link org.apache.sis.feature.DefaultAttributeType#characteristics()} method
     * on the attribute referenced by {@link #GEOMETRY_PROPERTY}.
     *
     * <p>While it is technically possible to have different CRS for different feature instances,
     * in most cases the CRS is the same for all geometries found in {@code GEOMETRY_PROPERTY}.
     * In such cases, the CRS can be specified only once as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value}
     * of this {@code CRS_CHARACTERISTIC}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link org.opengis.referencing.crs.CoordinateReferenceSystem}.</p>
     *
     * @see #getCRSCharacteristic(Feature, String)
     */
    public static final ScopedName CRS_CHARACTERISTIC = Names.createScopedName(SCOPE, null, "crs");

    /**
     * Conventional name for fetching the unit of measurement of a property.
     * This characteristic is typically an entry in the map returned by a call to the
     * {@link org.apache.sis.feature.DefaultAttributeType#characteristics()} method
     * on any attribute of numeric type.
     *
     * <p>While it is technically possible to have different units of measurement for the same property
     * on different feature instances, in most cases the unit is the same for all feature instances.
     * In such cases, the unit can be specified only once as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link javax.measure.Unit}.</p>
     */
    public static final ScopedName UNIT_CHARACTERISTIC = Names.createScopedName(SCOPE, null, "unit");

    /**
     * Conventional name for fetching the maximal length of string values.
     * The maximal length is stored as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value} of the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#characteristics() characteristic} associated
     * to the attribute on which the maximal length applies.
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link Integer}.</p>
     *
     * @see #getMaximalLengthCharacteristic(Feature, String)
     */
    public static final ScopedName MAXIMAL_LENGTH_CHARACTERISTIC = Names.createScopedName(SCOPE, null, "maximalLength");

    /**
     * Conventional name for fetching the enumeration of valid values.
     * The set of valid values is stored stored as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value} of the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#characteristics() characteristic} associated
     * to the attribute on which the restriction applies.
     */
    public static final GenericName VALID_VALUES_CHARACTERISTIC = Names.createScopedName(SCOPE, null, "validValues");

    /**
     * String representation of the {@link #IDENTIFIER_PROPERTY} name.
     * This can be used in calls to {@link Feature#getPropertyValue(String)}.
     */
    public static final String IDENTIFIER = "sis:identifier";

    /**
     * String representation of the {@link #GEOMETRY_PROPERTY} name.
     * This can be used in calls to {@link Feature#getPropertyValue(String)}.
     */
    public static final String GEOMETRY = "sis:geometry";

    /**
     * String representation of the {@link #ENVELOPE_PROPERTY} name.
     * This can be used in calls to {@link Feature#getPropertyValue(String)}.
     */
    public static final String ENVELOPE = "sis:envelope";

    /**
     * String representation of the {@link #CRS_CHARACTERISTIC} name.
     */
    public static final String CRS = "sis:crs";

    /**
     * String representation of the {@link #UNIT_CHARACTERISTIC} name.
     */
    public static final String UNIT = "sis:unit";

    /**
     * String representation of the {@link #MAXIMAL_LENGTH_CHARACTERISTIC} name.
     */
    public static final String MAXIMAL_LENGTH = "sis:maximalLength";

    /**
     * Prefix to insert before sequential number for name disambiguation.
     * This is used when attribute name collisions are detected in a file.
     */
    public static final String DISAMBIGUATION_SEQUENTIAL_NUMBER_PREFIX = " #";

    /**
     * Do not allow instantiation of this class.
     */
    private AttributeConvention() {
    }

    /**
     * Returns {@code true} if the given name stands for one of the synthetic properties defined by convention.
     * Conventional properties are properties added by the {@code DataStore} to the {@code FeatureType} in order
     * to provide a uniform way to access commonly used information.
     *
     * <p>Synthetic properties should generally not be written by the user.
     * Those properties are calculated most of the time and have only a meaning within SIS.</p>
     *
     * <p>Current implementation returns {@code true} if the given name is in the SIS namespace.</p>
     *
     * @param  name  the name of the property or characteristic to test, or {@code null}.
     * @return {@code true} if the given name is non-null and in the SIS namespace.
     */
    public static boolean contains(GenericName name) {
        while (name instanceof ScopedName) {
            if (SCOPE.equals(((ScopedName) name).path())) {
                return true;
            }
            name = ((ScopedName) name).tail();
        }
        return false;
    }

    /**
     * Returns {@code true} if the given feature type is non-null and has a {@value #IDENTIFIER} property.
     *
     * @param  feature  the feature type to test, or {@code null}.
     * @return whether the given feature type is non-null and has a {@value #IDENTIFIER} property.
     */
    public static boolean hasIdentifier(final FeatureType feature) {
        if (feature != null) try {
            return feature.getProperty(IDENTIFIER) != null;
        } catch (PropertyNotFoundException e) {
            // Ignore
        }
        return false;
    }

    /**
     * Returns {@code true} if the given type is an {@link AttributeType} or an {@link Operation} computing
     * an attribute, and the attribute value is one of the geometry types recognized by SIS.
     * The types currently recognized by SIS are:
     *
     * <ul>
     *   <li>{@link com.esri.core.geometry.Geometry} of the ESRI's API.</li>
     * </ul>
     *
     * The above list may be expanded in any future SIS version.
     *
     * @param  type  the type to test, or {@code null}.
     * @return {@code true} if the given type is (directly or indirectly) an attribute type
     *         for one of the recognized geometry types.
     *
     * @see #GEOMETRY_PROPERTY
     */
    public static boolean isGeometryAttribute(final IdentifiedType type) {
        final Optional<AttributeType<?>> at = Features.toAttribute(type);
        return at.isPresent() && Geometries.isKnownType(at.get().getValueClass());
    }

    /**
     * Returns whether the given operation or attribute type is characterized by a coordinate reference system.
     * This method verifies whether a characteristic named {@link #CRS_CHARACTERISTIC} with values assignable to
     * {@link CoordinateReferenceSystem} exists (directly or indirectly) for the given type.
     *
     * @param  type  the operation or attribute type for which to get the CRS, or {@code null}.
     * @return {@code true} if a characteristic for Coordinate Reference System has been found.
     */
    public static boolean characterizedByCRS(final IdentifiedType type) {
        return hasCharacteristic(type, CRS, CoordinateReferenceSystem.class);
    }

    /**
     * Returns the Coordinate Reference Systems characteristic for the specified attribute, or {@code null} if none.
     * This method gets the value or default value from the characteristic named {@link #CRS_CHARACTERISTIC}.
     *
     * @param  feature   the feature instance from which to get the <abbr>CRS</abbr> of an attribute.
     * @param  property  name of the property for which to get the <abbr>CRS</abbr>.
     * @return the Coordinate Reference System characteristic of the specified property, or {@code null} if none.
     * @throws PropertyNotFoundException if the {@code property} argument is not the name of a property of the given feature.
     * @throws ClassCastException if {@link #CRS_CHARACTERISTIC} has been found but is associated
     *         to an object which is not a {@link CoordinateReferenceSystem} instance.
     *
     * @see org.apache.sis.feature.builder.AttributeTypeBuilder#setCRS(CoordinateReferenceSystem)
     */
    public static CoordinateReferenceSystem getCRSCharacteristic(final Feature feature, final String property) {
        return (CoordinateReferenceSystem) getCharacteristic(feature, property, CRS);
    }

    /**
     * Returns the Coordinate Reference Systems characteristic for the given property type, or {@code null} if none.
     * This method gets the default value from the characteristic named {@link #CRS_CHARACTERISTIC}.
     * If the given property is a link, then this method follows the link in the given feature type (if non-null).
     *
     * <p>This method should be used only when the actual property instance is unknown.
     * Otherwise, {@link #getCRSCharacteristic(Feature, String)} should be used because
     * the <abbr>CRS</abbr> may vary for each property instance.</p>
     *
     * @param  feature    the feature type in which to follow links, or {@code null} if none.
     * @param  attribute  the attribute type for which to get the CRS, or {@code null}.
     * @return the Coordinate Reference System characteristic of the given property type, or {@code null} if none.
     * @throws ClassCastException if {@link #CRS_CHARACTERISTIC} has been found but is associated
     *         to an object which is not a {@link CoordinateReferenceSystem} instance.
     */
    public static CoordinateReferenceSystem getCRSCharacteristic(final FeatureType feature, final PropertyType attribute) {
        return (CoordinateReferenceSystem) getCharacteristic(feature, attribute, CRS);
    }

    /**
     * Returns whether the given operation or attribute type is characterized by a maximal length.
     * This method verifies whether a characteristic named {@link #MAXIMAL_LENGTH_CHARACTERISTIC}
     * with values of class {@link Integer} exists (directly or indirectly) for the given type.
     *
     * @param  type  the operation or attribute type for which to get the maximal length, or {@code null}.
     * @return {@code true} if a characteristic for maximal length has been found.
     */
    public static boolean characterizedByMaximalLength(final IdentifiedType type) {
        return hasCharacteristic(type, MAXIMAL_LENGTH, Integer.class);
    }

    /**
     * Returns the maximal length characteristic for the given attribute, or {@code null} if none.
     * This method gets the value or default value from the characteristic named {@link #MAXIMAL_LENGTH_CHARACTERISTIC}.
     *
     * @param  feature   the feature instance from which to get the maximal length of an attribute.
     * @param  property  the name of the property for which to get the maximal length.
     * @return the maximal length characteristic of the specified property, or {@code null} if none.
     * @throws PropertyNotFoundException if the {@code property} argument is not the name of a property of the given feature.
     * @throws ClassCastException if {@link #MAXIMAL_LENGTH_CHARACTERISTIC} has been found but is associated
     *         to an object which is not an {@link Integer} instance.
     *
     * @see org.apache.sis.feature.builder.AttributeTypeBuilder#setMaximalLength(Integer)
     */
    public static Integer getMaximalLengthCharacteristic(final Feature feature, final String property) {
        return (Integer) getCharacteristic(feature, property, MAXIMAL_LENGTH);
    }

    /**
     * Returns the maximal length characteristic for the given property type, or {@code null} if none.
     * This method gets the default value from the characteristic named {@link #MAXIMAL_LENGTH_CHARACTERISTIC}.
     * If the given property is a link, then this method follows the link in the given feature type (if non-null).
     *
     * <p>This method should be used only when the actual property instance is unknown.
     * Otherwise, {@link #getMaximalLengthCharacteristic(Feature, String)} should be used
     * because the maximal length may vary for each property instance.</p>
     *
     * @param  feature    the feature type in which to follow links, or {@code null} if none.
     * @param  attribute  the attribute type for which to get the maximal length, or {@code null}.
     * @return the maximal length characteristic of the given property type, or {@code null} if none.
     * @throws ClassCastException if {@link #MAXIMAL_LENGTH_CHARACTERISTIC} has been found but is associated
     *         to an object which is not a {@link CoordinateReferenceSystem} instance.
     */
    public static Integer getMaximalLengthCharacteristic(final FeatureType feature, final PropertyType attribute) {
        return (Integer) getCharacteristic(feature, attribute, MAXIMAL_LENGTH);
    }

    /**
     * Returns {@code true} if the given operation or attribute type has a characteristic of the given name,
     * and the values of that characteristic are assignable to the given {@code valueClass}.
     *
     * @param  type        the operation or attribute type for which to test the existence of a characteristic.
     * @param  name        the name of the characteristic to test.
     * @param  valueClass  the expected characteristic values.
     * @return {@code true} if a characteristic of the given name exists and has values assignable to the given class.
     */
    private static boolean hasCharacteristic(IdentifiedType type, final String name, final Class<?> valueClass) {
        final Optional<AttributeType<?>> at = Features.toAttribute(type);
        if (at.isPresent()) {
            final AttributeType<?> ct = at.get().characteristics().get(name);
            if (ct != null) {
                return valueClass.isAssignableFrom(ct.getValueClass());
            }
        }
        return false;
    }

    /**
     * Fetches from the specified property the value or default value of the named characteristic.
     * If the specified property is null, or is not an attribute, or does not have characteristics
     * of the given name, then this method returns {@code null}.
     *
     * @param  feature         the feature instance from which to get the characteristic.
     * @param  property        name of the property for which to get the characteristic.
     * @param  characteristic  name of the characteristic from which to get the value or default value.
     * @return the value or default value of the specified characteristic in the specified property, or {@code null} if none.
     * @throws PropertyNotFoundException if the {@code property} argument is not the name of a property of the given feature.
     */
    private static Object getCharacteristic(final Feature feature, final String property, final String characteristic) {
        if (feature instanceof AbstractFeature) {
            return ((AbstractFeature) feature).getCharacteristicValue(property, characteristic).orElse(null);
        }
        final Property attribute = feature.getProperty(property);
        if (attribute instanceof Attribute<?>) {
            final Attribute<?> at = ((Attribute<?>) attribute).characteristics().get(characteristic);
            if (at != null) {
                final Object value = at.getValue();
                if (value != null) {
                    return value;
                }
            }
            final AttributeType<?> type = ((Attribute<?>) attribute).getType().characteristics().get(characteristic);
            if (type != null) {
                return type.getDefaultValue();
            }
        }
        return null;
    }

    /**
     * Fetches from the given property the default value of the characteristic of the given name.
     * If the given property is a link, then this method follows the link in the given feature type
     * (unless that feature type is null).
     *
     * @param  feature         the feature type in which to follow links, or {@code null} if none.
     * @param  property        the property from which to get the characteristic value, or {@code null}.
     * @param  characteristic  name of the characteristic from which to get the default value.
     * @return the default value of the named characteristic in the given property, or {@code null} if none.
     */
    private static Object getCharacteristic(final FeatureType feature, PropertyType property, final String characteristic) {
        final Optional<String> referent = Features.getLinkTarget(property);
        if (referent.isPresent() && feature != null) {
            property = feature.getProperty(referent.get());
        }
        if (property instanceof AttributeType<?>) {
            final AttributeType<?> type = ((AttributeType<?>) property).characteristics().get(characteristic);
            if (type != null) {
                return type.getDefaultValue();
            }
        }
        return null;
    }
}
