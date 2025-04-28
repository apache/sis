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
import java.util.function.Function;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.setup.GeometryLibrary;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.filter.Expression;


/**
 * A set of predefined operations expecting a {@code Feature} as input and producing an {@code Attribute} as output.
 * Those operations can be used for creating <dfn>dynamic properties</dfn> which compute their value on-the-fly
 * from the values of other properties.
 *
 * <p>A flexible but relatively cumbersome way to define arbitrary computations is to subclass {@link AbstractOperation}.
 * This {@code FeatureOperations} class provides a more convenient way to get a few commonly-used operations.</p>
 *
 * <h2>Operation name, designation and description</h2>
 * All operations are identified by a programmatic name, but can also have a more human-readable designation
 * for Graphical User Interfaces (GUI). Those identification information are specified in a {@code Map<String,?>}.
 * The recognized entries are the same as the ones documented in {@link AbstractIdentifiedType}, augmented with
 * entries that describe the operation <em>result</em>. Those entries are summarized below:
 *
 * <table class="sis">
 *   <caption>Recognized map entries</caption>
 *   <tr>
 *     <th>Map key</th>
 *     <th>Value type</th>
 *     <th>Returned by</th>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
 *     <td>{@link GenericName} or {@link String}</td>
 *     <td>{@link AbstractOperation#getName() Operation.getName()} (mandatory)</td>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDefinition() Operation.getDefinition()}</td>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDesignation() Operation.getDesignation()}</td>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDescription() Operation.getDescription()}</td>
 *   </tr><tr>
 *     <td>"result.name"</td>
 *     <td>{@link GenericName} or {@link String}</td>
 *     <td>{@link AbstractAttribute#getName() Attribute.getName()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr><tr>
 *     <td>"result.definition"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link DefaultAttributeType#getDefinition() Attribute.getDefinition()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr><tr>
 *     <td>"result.designation"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link DefaultAttributeType#getDesignation() Attribute.getDesignation()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr><tr>
 *     <td>"result.description"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link DefaultAttributeType#getDescription() Attribute.getDescription()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr><tr>
 *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
 *     <td>{@link java.util.Locale}</td>
 *     <td>(none)</td>
 *   </tr>
 * </table>
 *
 * If no {@code "result.*"} entry is provided, then the methods in this class will use some default name, designation
 * and other information for the result type. Those defaults are operation specific; they are often, but not necessarily,
 * the same as the operation name, designation, <i>etc.</i>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.7
 */
public final class FeatureOperations extends Static {
    /**
     * The pool of operations or operation dependencies created so far, for sharing exiting instances.
     */
    static final WeakHashSet<PropertyType> POOL = new WeakHashSet<>(PropertyType.class);

    /**
     * Do not allow instantiation of this class.
     */
    private FeatureOperations() {
    }

    /**
     * Creates an operation which is only an alias for another property.
     *
     * <h4>Example</h4>
     * Features often have a property that can be used as identifier or primary key.
     * But the name of that property may vary between features of different types.
     * For example, features of type <b>Country</b> may have identifiers named “ISO country code”
     * while features of type <b>Car</b> may have identifiers named “license plate number”.
     * In order to simplify identifier usages regardless of their name,
     * an application could choose to add in all features a virtual property named {@code "identifier"}
     * which links to whatever property is used as an identifier in an arbitrary feature.
     * So the definition of the <b>Car</b> feature could contain the following code:
     *
     * {@snippet lang="java" :
     *     AttributeType licensePlateNumber = ...;            // Attribute creation omitted for brevity
     *     FeatureType car = new DefaultFeatureType(...,      // Arguments omitted for brevity
     *             licensePlateNumber, model, owner,
     *             FeatureOperations.link(Map.of(NAME_KEY, "identifier"), licensePlateNumber);
     *     }
     *
     * Since this method does not create new property (it only redirects to an existing property),
     * this method ignores all {@code "result.*"} entries in the given {@code identification} map.
     *
     * <h4>Read/write behavior</h4>
     * Since the {@link AbstractOperation#apply Operation.apply(…)} method returns directly the property
     * identified by the {@code referent} argument, the returned property is writable if the referenced
     * property is also writable.
     *
     * @param  identification  the name and other information to be given to the operation.
     * @param  referent        the referenced attribute or feature association.
     * @return an operation which is an alias for the {@code referent} property.
     *
     * @see Features#getLinkTarget(PropertyType)
     */
    public static Operation link(final Map<String,?> identification, final PropertyType referent) {
        ArgumentChecks.ensureNonNull("referent", referent);
        return POOL.unique(new LinkOperation(identification, referent));
    }

    /**
     * Creates an operation concatenating the string representations of the values of multiple properties.
     * This operation can be used for creating a <dfn>compound key</dfn> as a {@link String} that consists
     * of two or more attribute values that uniquely identify a feature instance.
     *
     * <p>The {@code delimiter}, {@code prefix} and {@code suffix} arguments given to this method
     * are used in the same way as {@link java.util.StringJoiner}, except for null values.
     * Null prefix, suffix and property values are handled as if they were empty strings.</p>
     *
     * <p>If the same character sequences as the given delimiter appears in a property value,
     * the {@code '\'} escape character will be inserted before that sequence.
     * If the {@code '\'} character appears in a property value, it will be doubled.</p>
     *
     * <h4>Restrictions</h4>
     * <ul>
     *   <li>The single properties can be either attributes or operations that produce attributes;
     *       feature associations are not allowed, unless they have an {@code "sis:identifier"} property.</li>
     *   <li>Each attribute shall contain at most one value; multi-valued attributes are not allowed.</li>
     *   <li>The delimiter cannot contain the {@code '\'} escape character.</li>
     * </ul>
     *
     * <h4>Read/write behavior</h4>
     * This operation supports both reading and writing. When setting a value on the attribute created by this
     * operation, the given string value will be split around the {@code delimiter} and each substring will be
     * forwarded to the corresponding single property.
     *
     * @param  identification    the name and other information to be given to the operation.
     * @param  delimiter         the characters to use as delimiter between each single property value.
     * @param  prefix            characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix            characters to use at the end of the concatenated string, or {@code null} if none.
     * @param  singleAttributes  identification of the single attributes (or operations producing attributes) to concatenate.
     * @return an operation which concatenates the string representations of all referenced single property values.
     * @throws IllegalArgumentException if {@code singleAttributes} is an empty sequence, or contains a property which
     *         is neither an {@code AttributeType} or an {@code Operation} computing an attribute, or an attribute has
     *         a {@linkplain DefaultAttributeType#getMaximumOccurs() maximum number of occurrences} greater than 1, or
     *         uses a {@linkplain DefaultAttributeType#getValueClass() value class} not convertible from a {@link String}.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Compound_key">Compound key on Wikipedia</a>
     */
    public static Operation compound(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final PropertyType... singleAttributes)
    {
        ArgumentChecks.ensureNonEmpty("delimiter", delimiter);
        if (delimiter.indexOf(StringJoinOperation.ESCAPE) >= 0) {
            throw new IllegalArgumentException(Errors.forProperties(identification).getString(
                    Errors.Keys.IllegalCharacter_2, "delimiter", StringJoinOperation.ESCAPE));
        }
        ArgumentChecks.ensureNonEmpty("singleAttributes", singleAttributes);
        if (singleAttributes.length == 1) {
            if (Strings.isNullOrEmpty(prefix) && Strings.isNullOrEmpty(suffix)) {
                final PropertyType at = singleAttributes[0];
                if (!(at instanceof FeatureAssociationRole)) {
                    return link(identification, at);
                }
            }
        }
        return POOL.unique(new StringJoinOperation(identification, delimiter, prefix, suffix, singleAttributes));
    }

    /**
     * Creates an operation computing the envelope that encompass all geometries found in the given attributes.
     * Geometries can be in different coordinate reference systems; they will be transformed to the first non-null
     * CRS in the following choices:
     *
     * <ol>
     *   <li>the CRS specified to this method,</li>
     *   <li>the CRS of the default geometry, or</li>
     *   <li>the CRS of the first non-empty geometry.</li>
     * </ol>
     *
     * The {@linkplain AbstractOperation#getResult() result} of this operation is an {@code Attribute}
     * with values of type {@link org.opengis.geometry.Envelope}. If the {@code crs} argument given to
     * this method is non-null, then the
     * {@linkplain org.apache.sis.geometry.GeneralEnvelope#getCoordinateReferenceSystem() envelope CRS}
     * will be that CRS.
     *
     * <h4>Limitations</h4>
     * If a geometry contains other geometries, this operation queries only the envelope of the root geometry.
     * It is the root geometry responsibility to take in account the envelope of all its children.
     *
     * <h4>Read/write behavior</h4>
     * This operation is read-only. Calls to {@code Attribute.setValue(Envelope)} will result in an
     * {@link UnsupportedOperationException} to be thrown.
     *
     * @param  identification      the name and other information to be given to the operation.
     * @param  crs                 the Coordinate Reference System in which to express the envelope, or {@code null}.
     * @param  geometryAttributes  the operation or attribute type from which to get geometry values.
     *                             Any element which is {@code null} or has a non-geometric value class will be ignored.
     * @return an operation which will compute the envelope encompassing all geometries in the given attributes.
     * @throws FactoryException if a coordinate operation to the target CRS cannot be created.
     */
    public static Operation envelope(final Map<String,?> identification, final CoordinateReferenceSystem crs,
            final PropertyType... geometryAttributes) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("geometryAttributes", geometryAttributes);
        return POOL.unique(new EnvelopeOperation(identification, crs, geometryAttributes));
    }

    /**
     * Creates a single geometry from a sequence of points or polylines stored in another property.
     * When evaluated, this operation reads a feature property containing a sequence of {@code Point}s or {@code Polyline}s.
     * Those geometries shall be instances of the specified geometry library (e.g. JTS or ESRI).
     * The merged geometry is usually a {@code Polyline},
     * unless the sequence of source geometries is empty or contains a single element.
     * The merged geometry is re-computed every time that the operation is evaluated.
     *
     * <h4>Examples</h4>
     * <p><i>Polylines created from points:</i>
     * a boat that record it's position every hour.
     * The input is a list of all positions stored in an attribute with [0 … ∞] multiplicity.
     * This operation will extract each position and create a line as a new attribute.</p>
     *
     * <p><i>Polylines created from other polylines:</i>
     * a boat that record track every hour.
     * The input is a list of all tracks stored in an attribute with [0 … ∞] multiplicity.
     * This operation will extract each track and create a polyline as a new attribute.</p>
     *
     * <h4>Read/write behavior</h4>
     * This operation is read-only. Calls to {@code Attribute.setValue(…)}
     * will result in an {@link UnsupportedOperationException} to be thrown.
     *
     * @param  identification  the name of the operation, together with optional information.
     * @param  library         the library providing the implementations of geometry objects to read and write.
     * @param  components      attribute, association or operation providing the geometries to group as a polyline.
     * @return a feature operation which computes its values by merging points or polylines.
     *
     * @since 1.4
     */
    public static Operation groupAsPolyline(final Map<String,?> identification, final GeometryLibrary library,
                                            final PropertyType components)
    {
        ArgumentChecks.ensureNonNull("library", library);
        ArgumentChecks.ensureNonNull("components", components);
        return POOL.unique(GroupAsPolylineOperation.create(identification, library, components));
    }

    /**
     * Creates an operation which delegates the computation to a given expression.
     * The {@code expression} argument should generally be an instance of
     * {@link org.opengis.filter.Expression},
     * but more generic functions are accepted as well.
     *
     * <h4>Read/write behavior</h4>
     * This operation is read-only. Calls to {@code Attribute.setValue(…)}
     * will result in an {@link UnsupportedOperationException} to be thrown.
     *
     * @param  <V>             the type of values computed by the expression and assigned to the feature property.
     * @param  identification  the name of the operation, together with optional information.
     * @param  expression      the expression to evaluate on feature instances.
     * @param  resultType      type of values computed by the expression and assigned to the feature property.
     * @return a feature operation which computes its values using the given expression.
     *
     * @since 1.4
     */
    public static <V> Operation function(final Map<String,?> identification,
                                         final Function<? super Feature, ? extends V> expression,
                                         final AttributeType<? super V> resultType)
    {
        ArgumentChecks.ensureNonNull("expression", expression);
        ArgumentChecks.ensureNonNull("resultType", resultType);
        return POOL.unique(ExpressionOperation.create(identification, expression, resultType));
    }

    /**
     * Creates an operation which delegates the computation to a given expression producing values of unknown type.
     * This method can be used as an alternative to {@link #function function(…)} when the constraint on the
     * parameterized type {@code <V>} between {@code expression} and {@code result} cannot be enforced at compile time.
     * This method casts or converts the expression to the expected type by a call to
     * {@link Expression#toValueType(Class)}.
     *
     * <h4>Read/write behavior</h4>
     * This operation is read-only. Calls to {@code Attribute.setValue(…)}
     * will result in an {@link UnsupportedOperationException} to be thrown.
     *
     * @param  <V>             the type of values computed by the expression and assigned to the feature property.
     * @param  identification  the name of the operation, together with optional information.
     * @param  expression      the expression to evaluate on feature instances.
     * @param  resultType      type of values computed by the expression and assigned to the feature property.
     * @return a feature operation which computes its values using the given expression.
     * @throws ClassCastException if the result type is not a target type supported by the expression.
     *
     * @since 1.4
     */
    public static <V> Operation expression(final Map<String,?> identification,
                                           final Expression<? super Feature, ?> expression,
                                           final AttributeType<V> resultType)
    {
        return function(identification, expression.toValueType(resultType.getValueClass()), resultType);
    }
}
