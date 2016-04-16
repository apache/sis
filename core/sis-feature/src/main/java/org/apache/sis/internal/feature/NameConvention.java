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
package org.apache.sis.internal.feature;

import com.esri.core.geometry.Geometry;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.Static;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;


/**
 * Defines the names of some properties or characteristics for which we assign a conventional usage.
 * Properties with the names defined in this {@code NameConvention} class are often aliases generated
 * by the SIS implementation of various file readers. Those synthetic properties redirect to the most
 * appropriate "real" property in the feature.
 *
 * <div class="note"><b>Example:</b>
 * one of the most frequently used synthetic property is {@code "@id"}, which contains a unique
 * identifier (or primary key) for the feature. This property is usually (but not necessarily)
 * a {@linkplain org.apache.sis.feature.FeatureOperations#link link to an existing attribute}.
 * By using the {@code "@id"} alias, users do not need to know the name of the "real" attribute.
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
 * <div class="section">Mixing with other conventions</div>
 * The conventions defined in this class are specific to Apache SIS.
 * Current implementation does not support any other convention than the SIS one,
 * but we may refactor this class in future SIS versions if there is a need to support different conventions.
 *
 * <p>In order to reduce the risk of name collision with properties in user-defined features
 * (e.g. the user may already have an attribute named {@code "id"} for his own purpose),
 * all names defined in this class begin with the {@code "@"} character.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class NameConvention extends Static {
    /**
     * Namespace of all names defined by SIS convention.
     */
    private static final GenericName NAMESPACE;

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
    public static final LocalName ID_PROPERTY;

    /**
     * Conventional name for a property containing the geometric object to use by default.
     * Some features may contain more than one geometric object; this property tells which
     * geometry to render on a map for example.
     *
     * <p>Properties of this name are usually {@linkplain org.apache.sis.feature.FeatureOperations#link
     * operations acting as a redirection to another attribute}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} can be the
     * {@link Geometry} class from ESRI's API, or the {@code Geometry} class from <cite>Java Topology Suite</cite>
     * (JTS) library, or any other class defined in future SIS versions. See {@link #isGeometryAttribute(PropertyType)}
     * for testing whether the value is a supported type.</p>
     *
     * @see #isGeometryAttribute(PropertyType)
     */
    public static final LocalName DEFAULT_GEOMETRY_PROPERTY;

    /**
     * Conventional name for fetching the envelope encompassing all geometries in a feature. Most {@code FeatureType}s
     * have at most one geometry, which is also the {@linkplain #DEFAULT_GEOMETRY_PROPERTY default geometry}.
     * But if several geometries exist, then the value for this synthetic property is the union of all geometries.
     *
     * <p>Properties of this name are usually
     * {@linkplain org.apache.sis.feature.FeatureOperations#envelope operations}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link org.opengis.geometry.Envelope}.</p>
     */
    public static final LocalName ENVELOPE_PROPERTY;

    /**
     * Conventional name for fetching the Coordinate Reference System (CRS) of a geometry or a coverage.
     * This characteristic is typically an entry in the map returned by a call to the
     * {@link org.apache.sis.feature.DefaultAttributeType#characteristics()} method
     * on the attribute referenced by {@link #DEFAULT_GEOMETRY_PROPERTY}.
     *
     * <p>While it is technically possible to have different CRS for different feature instances,
     * in most cases the CRS is the same for all geometries found in {@code DEFAULT_GEOMETRY_PROPERTY}.
     * In such cases, the CRS can be specified only once as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value}
     * of this {@code CRS_CHARACTERISTIC}.</p>
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link org.opengis.referencing.crs.CoordinateReferenceSystem}.</p>
     */
    public static final LocalName CRS_CHARACTERISTIC;

    /**
     * Conventional name for fetching the maximal length of string values.
     * The maximal length is stored as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value} of the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#characteristics() characteristic} associated
     * to the attribute on which the maximal length applies.
     *
     * <p>The {@linkplain org.apache.sis.feature.DefaultAttributeType#getValueClass() value class} should be
     * {@link Integer}.</p>
     */
    public static final LocalName MAXIMAL_LENGTH_CHARACTERISTIC;

    /**
     * Conventional name for fetching the enumeration of valid values.
     * The set of valid values is stored stored as the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#getDefaultValue() default value} of the
     * {@linkplain org.apache.sis.feature.DefaultAttributeType#characteristics() characteristic} associated
     * to the attribute on which the restriction applies.
     */
    public static final LocalName VALID_VALUES_CHARACTERISTIC;

    static {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        NAMESPACE                     = factory.createGenericName(null, "Apache", Constants.SIS);
        NameSpace ns                  = factory.createNameSpace(NAMESPACE, null);
        ID_PROPERTY                   = factory.createLocalName(ns, "@identifier");
        DEFAULT_GEOMETRY_PROPERTY     = factory.createLocalName(ns, "@defaultGeometry");
        ENVELOPE_PROPERTY             = factory.createLocalName(ns, "@envelope");
        CRS_CHARACTERISTIC            = factory.createLocalName(ns, "@crs");
        MAXIMAL_LENGTH_CHARACTERISTIC = factory.createLocalName(ns, "@maximalLength");
        VALID_VALUES_CHARACTERISTIC   = factory.createLocalName(ns, "@validValues");
    }

    /**
     * Do not allow instantiation of this class.
     */
    private NameConvention() {
    }

    /**
     * Returns {@code true} if the given name stands for one of the synthetic properties defined by convention.
     * Conventional properties are properties added by the {@code DataStore} to the {@code FeatureType} in order
     * to provide a uniform way to access commonly used informations.
     *
     * <p>Synthetic properties should generally not be written by the user.
     * Those properties are calculated most of the time and have only a meaning within SIS.</p>
     *
     * <p>Current implementation returns {@code true} if the given name is in the SIS namespace.</p>
     *
     * @param  name  the name of the property or characteristic to test, or {@code null}.
     * @return {@code true} if the given name is non-null and in the SIS namespace.
     */
    public static boolean contains(final GenericName name) {
        if (name == null) {
            return false;
        }
        final GenericName scope;
        if (name instanceof ScopedName) {
            scope = ((ScopedName) name).path().toFullyQualifiedName();
        } else {
            scope = name.scope().name();
        }
        return NAMESPACE.equals(scope);
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
     * @see #DEFAULT_GEOMETRY_PROPERTY
     */
    public static boolean isGeometryAttribute(IdentifiedType type) {
        while (type instanceof Operation) {
            type = ((Operation) type).getResult();
        }
        if (type instanceof AttributeType<?>) {
            final Class<?> valueClass = ((AttributeType<?>) type).getValueClass();
            return Geometry.class.isAssignableFrom(valueClass);
        }
        return false;
    }

    /**
     * Returns the coordinate reference systems associated to the given operation or attribute type,
     * or {@code null} if none. This method searches for a characteristic named {@link #CRS_CHARACTERISTIC}.
     *
     * @param  type  the operation or attribute type for which to get the CRS, or {@code null}.
     * @return The Coordinate Reference System for the given type, or {@code null} if none.
     * @throws ClassCastException if {@link #CRS_CHARACTERISTIC} has been found but is associated
     *         to an object which is not a {@link CoordinateReferenceSystem} instance.
     */
    public static CoordinateReferenceSystem getCoordinateReferenceSystem(IdentifiedType type) {
        while (type instanceof Operation) {
            type = ((Operation) type).getResult();
        }
        if (type instanceof AttributeType<?>) {
            final AttributeType<?> at = ((AttributeType<?>) type).characteristics().get(CRS_CHARACTERISTIC.toString());
            if (at != null) {
                return (CoordinateReferenceSystem) at.getDefaultValue();
            }
        }
        return null;
    }

    /**
     * Returns the maximal string length associated to the given attribute type, or {@code null} if none.
     * This method searches for a characteristic named {@link #MAXIMAL_LENGTH_CHARACTERISTIC}.
     *
     * @param  type  the attribute type for which to get the maximal string length, or {@code null}.
     * @return The maximal string length for the given type, or {@code null} if none.
     * @throws ClassCastException if {@link #MAXIMAL_LENGTH_CHARACTERISTIC} has been found
     *         but is associated to an object which is not an {@link Integer} instance.
     */
    public static Integer getMaximalLength(final AttributeType<?> type){
        if (type != null) {
            final AttributeType<?> at = type.characteristics().get(MAXIMAL_LENGTH_CHARACTERISTIC.toString());
            if (at != null) {
                return (Integer) at.getDefaultValue();
            }
        }
        return null;
    }
}
