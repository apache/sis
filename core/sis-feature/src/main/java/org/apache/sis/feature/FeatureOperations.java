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
import org.opengis.util.InternationalString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;

// Branch-dependent imports
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;


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
 *     <td>{@value org.apache.sis.feature.AbstractOperation#NAME_KEY}</td>
 *     <td>{@link GenericName} or {@link String}</td>
 *     <td>{@link AbstractOperation#getName() Operation.getName()} (mandatory)</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractOperation#DEFINITION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDefinition() Operation.getDefinition()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractOperation#DESIGNATION_KEY}</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractOperation#getDesignation() Operation.getDesignation()}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.apache.sis.feature.AbstractOperation#DESCRIPTION_KEY}</td>
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
 *     <td>{@link AbstractAttribute#getDefinition() Attribute.getDefinition()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr>
 *   <tr>
 *     <td>"result.designation"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractAttribute#getDesignation() Attribute.getDesignation()} on the {@linkplain AbstractOperation#getResult() result}</td>
 *   </tr>
 *   <tr>
 *     <td>"result.description"</td>
 *     <td>{@link InternationalString} or {@link String}</td>
 *     <td>{@link AbstractAttribute#getDescription() Attribute.getDescription()} on the {@linkplain AbstractOperation#getResult() result}</td>
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
     * @param  identification  the name and other information to be given to the operation.
     * @param  referent        the referenced attribute or feature association.
     * @return an operation which is an alias for the {@code referent} property.
     */
    public static Operation link(final Map<String,?> identification, final PropertyType referent) {
        ArgumentChecks.ensureNonNull("referent", referent);
        return new LinkOperation(identification, referent);
    }

    /**
     * Creates an operation concatenating the string representations of the values of multiple properties.
     * This operation can be used for creating a <cite>compound key</cite> as a {@link String} that consists
     * of two or more attribute values that uniquely identify a feature instance.
     *
     * <p>The {@code delimiter}, {@code prefix} and {@code suffix} arguments given to this method are used in
     * the same way than {@link java.util.StringJoiner}. Null prefix, suffix and property values are handled
     * as an empty string.</p>
     *
     * <div class="section">Restrictions</div>
     * The single properties can be either attributes or operations that produce attributes;
     * feature associations are not allowed.
     * Furthermore each attribute shall contain at most one value; multi-valued attributes are not allowed.
     *
     * <div class="section">Read/write behavior</div>
     * This operation supports both reading and writing. When setting a value on the attribute created by this
     * operation, the given string value will be split around the {@code delimiter} and each substring will be
     * forwarded to the corresponding single property.
     *
     * @param  identification    the name and other information to be given to the operation.
     * @param  delimiter         the characters to use a delimiter between each single property value.
     * @param  prefix            characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix            characters to use at the end of the concatenated string, or {@code null} if none.
     * @param  singleProperties  identification of the single properties to concatenate.
     * @return an operation which concatenates the string representations of all referenced single property values.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Compound_key">Compound key on Wikipedia</a>
     */
    public static Operation compound(final Map<String,?> identification, final String delimiter,
            final String prefix, final String suffix, final GenericName... singleProperties)
    {
        ArgumentChecks.ensureNonEmpty("delimiter", delimiter);
        ArgumentChecks.ensureNonNull("singleProperties", singleProperties);
        return new StringJoinOperation(identification, delimiter, prefix, suffix, singleProperties);
    }

    /**
     * Creates a calculate bounds operation type.
     *
     * @param  identification  the name and other information to be given to the operation.
     * @param  baseCRS         created envelope CRS.
     * @return Operation
     */
    public static Operation bounds(final Map<String,?> identification, CoordinateReferenceSystem baseCRS) {
        return new BoundsOperation(identification, baseCRS);
    }
}
