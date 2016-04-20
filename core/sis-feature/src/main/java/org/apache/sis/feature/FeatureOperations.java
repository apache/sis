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
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports


/**
 * A set of pre-defined operations expecting a {@code Feature} as input and producing an {@code Attribute} as output.
 * Those operations can be used for creating <cite>dynamic properties</cite> which compute their value on-the-fly
 * from the values of other properties.
 *
 * <p>A flexible but relatively cumbersome way to define arbitrary computations is to subclass {@link AbstractOperation}.
 * This {@code FeatureOperations} class provides a more convenient way to get a few commonly-used operations.</p>
 *
 * <div class="section">Operation name, designation and description</div>
 * All operations are identified by a programmatic name, but can also have a more human-readable designation
 * for Graphical User Interfaces (GUI). Those identification information are specified in a {@code Map<String,?>}.
 * The recognized entries are the same than the ones documented in {@link AbstractIdentifiedType}, augmented with
 * entries that describe the operation <em>result</em>. Those entries are summarized below:
 *
 * <table class="sis">
 *   <caption>Recognized map entries</caption>
 *   <tr>
 *     <th>Map key</th>
 *     <th>Value type</th>
 *     <th>Returned by</th>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
 *     <td>{@link GenericName} or {@link String}</td>
 *     <td>{@link AbstractOperation#getName() Operation.getName()} (mandatory)</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDefinition() Operation.getDefinition()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDesignation() Operation.getDesignation()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDescription() Operation.getDescription()}</td>
 *   </tr>
 *   <tr>
 *     <td>"result.name"</td>
 *     <td>{@link GenericName} or {@link String}</td>
 *     <td>{@link AbstractAttribute#getName() Attribute.getName()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr>
 *   <tr>
 *     <td>"result.definition"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link DefaultAttributeType#getDefinition() Attribute.getDefinition()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr>
 *   <tr>
 *     <td>"result.designation"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link DefaultAttributeType#getDesignation() Attribute.getDesignation()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr>
 *   <tr>
 *     <td>"result.description"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link DefaultAttributeType#getDescription() Attribute.getDescription()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
 *     <td>{@link Locale}</td>
 *     <td>(none)</td>
 *   </tr>
 * </table>
 *
 * If no {@code "result.*"} entry is provided, then the methods in this class will use some default name, designation
 * and other information for the result type. Those defaults are operation specific; they are often, but not necessarily,
 * the same than the operation name, designation, <i>etc.</i>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class FeatureOperations extends Static {
    /**
     * The pool of operations or operation dependencies created so far, for sharing exiting instances.
     */
    static final WeakHashSet<AbstractIdentifiedType> POOL = new WeakHashSet<AbstractIdentifiedType>(AbstractIdentifiedType.class);

    /**
     * Do not allow instantiation of this class.
     */
    private FeatureOperations() {
    }

    /**
     * Creates an operation which is only an alias for another property.
     *
     * <div class="note"><b>Example:</b>
     * features often have a property that can be used as identifier or primary key.
     * But the name of that property may vary between features of different types.
     * For example features of type <b>Country</b> may have identifiers named “ISO country code”
     * while features of type <b>Car</b> may have identifiers named “license plate number”.
     * In order to simplify identifier usages regardless of their name,
     * an application could choose to add in all features a virtual property named {@code "@id"}
     * which links to whatever property is used as an identifier in an arbitrary feature.
     * So the definition of the <b>Car</b> feature could contain the following code:
     *
     * {@preformat java
     *   AttributeType licensePlateNumber = ...;            // Attribute creation omitted for brevity
     *   FeatureType car = new DefaultFeatureType(...,      // Arguments omitted for brevity
     *           licensePlateNumber, model, owner,
     *           FeatureOperations.link(singletonMap(NAME_KEY, "@id"), licensePlateNumber);
     * }
     * </div>
     *
     * Since this method does not create new property (it only redirects to an existing property),
     * this method ignores all {@code "result.*"} entries in the given {@code identification} map.
     *
     * <div class="section">Read/write behavior</div>
     * Since the {@link AbstractOperation#apply Operation.apply(…)} method returns directly the property
     * identified by the {@code referent} argument, the returned property is writable if the referenced
     * property is also writable.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of {@code referent} parameter will be changed to {@code PropertyType}
     * if and when such interface will be defined in GeoAPI.</div>
     *
     * @param  identification  the name and other information to be given to the operation.
     * @param  referent        the referenced attribute or feature association.
     * @return an operation which is an alias for the {@code referent} property.
     */
    public static AbstractOperation link(final Map<String,?> identification, final AbstractIdentifiedType referent) {
        ArgumentChecks.ensureNonNull("referent", referent);
        return POOL.unique(new LinkOperation(identification, referent));
    }

    /**
     * Creates an operation concatenating the string representations of the values of multiple properties.
     * This operation can be used for creating a <cite>compound key</cite> as a {@link String} that consists
     * of two or more attribute values that uniquely identify a feature instance.
     *
     * <p>The {@code delimiter}, {@code prefix} and {@code suffix} arguments given to this method
     * are used in the same way than {@link java.util.StringJoiner}, except for null values.
     * Null prefix, suffix and property values are handled as if they were empty strings.</p>
     *
     * <p>If the same character sequences than the given delimiter appears in a property value,
     * the {@code '\'} escape character will be inserted before that sequence.
     * If the {@code '\'} character appears in a property value, it will be doubled.</p>
     *
     * <p><b>Restrictions:</b></p>
     * <ul>
     *   <li>The single properties can be either attributes or operations that produce attributes;
     *       feature associations are not allowed.</li>
     *   <li>Each attribute shall contain at most one value; multi-valued attributes are not allowed.</li>
     *   <li>The delimiter can not contain the {@code '\'} escape character.</li>
     * </ul>
     *
     * <div class="section">Read/write behavior</div>
     * This operation supports both reading and writing. When setting a value on the attribute created by this
     * operation, the given string value will be split around the {@code delimiter} and each substring will be
     * forwarded to the corresponding single property.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of {@code singleAttributes} elements will be changed to {@code PropertyType}
     * if and when such interface will be defined in GeoAPI.</div>
     *
     * @param  identification    the name and other information to be given to the operation.
     * @param  delimiter         the characters to use as delimiter between each single property value.
     * @param  prefix            characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix            characters to use at the end of the concatenated string, or {@code null} if none.
     * @param  singleAttributes  identification of the single attributes (or operations producing attributes) to concatenate.
     * @return an operation which concatenates the string representations of all referenced single property values.
     * @throws UnconvertibleObjectException if at least one of the given {@code singleAttributes} uses a
     *         {@linkplain DefaultAttributeType#getValueClass() value class} which is not convertible from a {@link String}.
     * @throws IllegalArgumentException if {@code singleAttributes} is an empty sequence, or contains a property which
     *         is neither an {@code AttributeType} or an {@code Operation} computing an attribute, or an attribute has
     *         a {@linkplain DefaultAttributeType#getMaximumOccurs() maximum number of occurrences} greater than 1.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Compound_key">Compound key on Wikipedia</a>
     */
    public static AbstractOperation compound(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final AbstractIdentifiedType... singleAttributes)
            throws UnconvertibleObjectException
    {
        ArgumentChecks.ensureNonEmpty("delimiter", delimiter);
        if (delimiter.indexOf(StringJoinOperation.ESCAPE) >= 0) {
            throw new IllegalArgumentException(Errors.getResources(identification).getString(
                    Errors.Keys.IllegalCharacter_2, "delimiter", StringJoinOperation.ESCAPE));
        }
        ArgumentChecks.ensureNonNull("singleAttributes", singleAttributes);
        switch (singleAttributes.length) {
            case 0: {
                throw new IllegalArgumentException(Errors.getResources(identification)
                        .getString(Errors.Keys.EmptyArgument_1, "singleAttributes"));
            }
            case 1: {
                if ((prefix == null || prefix.isEmpty()) && (suffix == null || suffix.isEmpty())) {
                    return link(identification, singleAttributes[0]);
                }
                break;
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
     * <div class="section">Limitations</div>
     * If a geometry contains other geometries, this operation queries only the envelope of the root geometry.
     * It is the root geometry responsibility to take in account the envelope of all its children.
     *
     * <div class="section">Read/write behavior</div>
     * This operation is read-only. Calls to {@code Attribute.setValue(Envelope)} will result in an
     * {@link IllegalStateException} to be thrown.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of {@code geometryAttributes} elements will be changed to {@code PropertyType}
     * if and when such interface will be defined in GeoAPI.</div>
     *
     * @param  identification     the name and other information to be given to the operation.
     * @param  crs                the Coordinate Reference System in which to express the envelope, or {@code null}.
     * @param  geometryAttributes the operation or attribute type from which to get geometry values.
     *                            Any element which is {@code null} or has a non-geometric value class will be ignored.
     * @return an operation which will compute the envelope encompassing all geometries in the given attributes.
     * @throws FactoryException if a coordinate operation to the target CRS can not be created.
     */
    public static AbstractOperation envelope(final Map<String,?> identification, final CoordinateReferenceSystem crs,
            final AbstractIdentifiedType... geometryAttributes) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("geometryAttributes", geometryAttributes);
        return POOL.unique(new EnvelopeOperation(identification, crs, geometryAttributes));
    }
}
