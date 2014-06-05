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
import java.util.Locale;
import java.io.Serializable;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.feature.IdentifiedType;


/**
 * Identification and description information inherited by property types and feature types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class AbstractIdentifiedType implements IdentifiedType, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 277130188958446740L;

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getName()}.
     *
     * @see #getName()
     */
    public static final String NAME_KEY = "name";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDefinition()}.
     *
     * @see #getDefinition()
     */
    public static final String DEFINITION_KEY = "definition";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDesignation()}.
     *
     * @see #getDesignation()
     */
    public static final String DESIGNATION_KEY = "designation";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getDescription()}.
     *
     * @see #getDescription()
     */
    public static final String DESCRIPTION_KEY = "description";

    /**
     * The name of this type.
     *
     * @see #getName()
     * @see #NAME_KEY
     */
    private final GenericName name;

    /**
     * Concise definition of the element.
     *
     * @see #getDefinition()
     * @see #DEFINITION_KEY
     */
    private final InternationalString definition;

    /**
     * Natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #name} in user interfaces.
     *
     * @see #getDesignation()
     * @see #DESIGNATION_KEY
     */
    private final InternationalString designation;

    /**
     * Optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * @see #getDescription()
     * @see #DESCRIPTION_KEY
     */
    private final InternationalString description;

    /**
     * Constructs a type from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the {@value #NAME_KEY}.
     * Other entries listed in the table below are optional.
     *
     * <table class="sis">
     *   <caption>Recognized map entries</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value #NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #DESIGNATION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #DESCRIPTION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
     *     <td>{@link Locale}</td>
     *     <td>(none)</td>
     *   </tr>
     * </table>
     *
     * {@section Localization}
     * All localizable attributes like {@code "definition"} may have a language and country code suffix.
     * For example the {@code "definition_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
     * the {@code "definition_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     * They are convenience properties for building the {@code InternationalString} value.
     *
     * <p>The {@code "locale"} property applies only in case of exception for formatting the error message, and
     * is used only on a <cite>best effort</cite> basis. The locale is discarded after successful construction
     * since localizations are applied by the {@link InternationalString#toString(Locale)} method.</p>
     *
     * @param  identification The name and other information to be given to this identified type.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    protected AbstractIdentifiedType(final Map<String,?> identification)
            throws IllegalArgumentException
    {
        ensureNonNull("identification", identification);
        Object value = identification.get(NAME_KEY);
        if (value == null) {
            throw new IllegalArgumentException(Errors.getResources(identification)
                    .getString(Errors.Keys.MissingValueForProperty_1, NAME_KEY));
        } else if (value instanceof String) {
            name = createName(DefaultFactories.NAMES, (String) value);
        } else if (value instanceof GenericName) {
            name = (GenericName) value;
        } else {
            throw new IllegalArgumentException(Errors.getResources(identification).getString(
                    Errors.Keys.IllegalPropertyClass_2, NAME_KEY, value.getClass()));
        }
        definition  = Types.toInternationalString(identification, DEFINITION_KEY );
        designation = Types.toInternationalString(identification, DESIGNATION_KEY);
        description = Types.toInternationalString(identification, DESCRIPTION_KEY);
    }

    /**
     * Creates a name from the given string. This method is invoked at construction time,
     * so it should not use any field in this {@code AbtractIdentifiedObject} instance.
     */
    GenericName createName(final NameFactory factory, final String value) {
        return factory.createLocalName(null, value);
    }

    /**
     * Returns the name of this type.
     * The namespace can be either explicit
     * ({@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name}) or implicit
     * ({@linkplain org.apache.sis.util.iso.DefaultLocalName local name}).
     *
     * <p>For {@linkplain DefaultFeatureType feature types}, the name is mandatory and shall be unique
     * in the unit processing the data (e.g. a {@link org.apache.sis.storage.DataStore} reading a file).</p>
     *
     * @return The type name.
     */
    @Override
    public GenericName getName() {
        return name;
    }

    /**
     * Returns a concise definition of the element.
     *
     * @return Concise definition of the element.
     */
    @Override
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Returns a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @return Natural language designator for the element.
     */
    @Override
    public InternationalString getDesignation() {
        return designation;
    }

    /**
     * Returns optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * @return Information beyond that required for concise definition of the element, or {@code null} if none.
     */
    @Override
    public InternationalString getDescription() {
        return description;
    }

    /*
     * ISO 19109 properties omitted for now:
     *
     *   - constrainedBy : CharacterString
     *
     * Rational: a CharacterString is hardly programmatically usable. A Range would be better but too specific.
     * We could follow the GeoAPI path and define a "restrictions : Filter" property. That would be more generic,
     * but we are probably better to wait for Filter to be implemented in SIS.
     *
     * Reference: https://issues.apache.org/jira/browse/SIS-175
     */

    /**
     * Returns a hash code value for this type.
     *
     * @return The hash code for this type.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, definition, designation, description);
    }

    /**
     * Compares this type with the given object for equality.
     *
     * @param  obj The object to compare with this type.
     * @return {@code true} if the given object is equals to this type.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && getClass() == obj.getClass()) {
            final AbstractIdentifiedType that = (AbstractIdentifiedType) obj;
            return Objects.equals(name,        that.name) &&
                   Objects.equals(definition,  that.definition) &&
                   Objects.equals(designation, that.designation) &&
                   Objects.equals(description, that.description);
        }
        return false;
    }
}
