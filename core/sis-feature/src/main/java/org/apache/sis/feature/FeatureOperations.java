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

import java.util.Collections;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;


/**
 * A set of pre-defined operations expecting a {@code Feature} as input and producing an {@code Attribute} as output.
 * Those operations can be used for creating <cite>dynamic properties</cite> which compute their value on-the-fly
 * from the values of other properties.
 *
 * <p>A flexible but relatively cumbersome way to define arbitrary computations is to subclass {@link AbstractOperation}.
 * This {@code FeatureOperations} class provides a more convenient way to get a few commonly-used operations.</p>
 *
 * @author  Johann Sorel (Geomatys)
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
     * <div class="section">Read/write behavior</div>
     * Since the {@link AbstractOperation#apply Operation.apply(…)} method returns directly the property
     * identified by the {@code referent} argument, the returned property is writable if the referenced
     * property is also writable.
     *
     * @param  name      name of the property to create.
     * @param  referent  the referenced attribute or feature association.
     * @return an operation which is an alias for the {@code referent} property.
     */
    public static Operation link(final GenericName name, final PropertyType referent) {
        ArgumentChecks.ensureNonNull("referent", referent);
        return new LinkOperation(Collections.singletonMap("name", name), referent);
    }

    /**
     * Creates an operation concatenating the string representations of the values of multiple properties.
     * This operation can be used for creating a <cite>compound key</cite> as a {@link String} that consists
     * of two or more attribute values that uniquely identify a feature instance.
     *
     * <div class="section">Read/write behavior</div>
     * This operation supports both reading and writing. When setting a value on the attribute created by this
     * operation, the given string value will be split around the {@code separator} and each substring will be
     * forwarded to the corresponding single property.
     *
     * @param  name       name of the property to create.
     * @param  prefix     characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix     characters to use at the end of the concatenated string, or {@code null} if none.
     * @param  separator  the characters to use a delimiter between each single attribute value.
     * @param  singleProperties  identification of the single properties to concatenate.
     * @return an operation which concatenates the string representations of all referenced single attribute values.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Compound_key">Compound key on Wikipedia</a>
     */
    public static Operation aggregate(GenericName name, String prefix, String suffix, String separator, GenericName... singleProperties) {
        ArgumentChecks.ensureNonEmpty("separator", separator);
        ArgumentChecks.ensureNonNull("singleProperties", singleProperties);
        return new AggregateOperation(Collections.singletonMap("name", name), prefix, suffix, separator, singleProperties);
    }

    /**
     * Creates a calculate bounds operation type.
     *
     * @param name name of the property
     * @param baseCrs created envelope crs
     * @return Operation
     */
    public static Operation bounds(GenericName name, CoordinateReferenceSystem baseCrs) {
        return new BoundsOperation(Collections.singletonMap("name", name), baseCrs);
    }
}
