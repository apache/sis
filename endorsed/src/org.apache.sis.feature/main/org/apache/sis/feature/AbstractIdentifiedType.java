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
import java.util.Objects;
import java.util.Optional;
import java.util.Collections;
import java.util.logging.Logger;
import java.io.Serializable;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.IdentifiedType;


/**
 * Identification and description information inherited by property types and feature types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.5
 */
public class AbstractIdentifiedType implements IdentifiedType, Deprecable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 277130188958446740L;

    /**
     * The logger used by feature implementations.
     */
    static final Logger LOGGER = Logger.getLogger(Modules.FEATURE);

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
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #isDeprecated()}.
     *
     * <p>If this property is set to {@code true}, then the value associated to {@link #DESCRIPTION_KEY}
     * should give the replacement (e.g. <q>superceded by …</q>).</p>
     *
     * @see #isDeprecated()
     *
     * @since 0.8
     */
    public static final String DEPRECATED_KEY = "deprecated";

    /**
     * Optional key which can be given to the constructor for inheriting values from an existing identified type.
     * If a value exists, then any property that is not defined by one of the above-cited keys will inherit its
     * value from the given {@link IdentifiedType}.
     *
     * <p>This property is useful when creating a new property derived from an existing property.
     * An example of such derivation is {@link AbstractOperation#updateDependencies(Map)}.</p>
     *
     * @since 1.5
     */
    public static final String INHERIT_FROM_KEY = "inheritFrom";

    /**
     * The name of this type.
     *
     * @see #getName()
     * @see #NAME_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final GenericName name;

    /**
     * Concise definition of the element.
     *
     * @see #getDefinition()
     * @see #DEFINITION_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final InternationalString definition;

    /**
     * Natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #name} in user interfaces.
     *
     * @see #getDesignation()
     * @see #DESIGNATION_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final InternationalString designation;

    /**
     * Optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * @see #getDescription()
     * @see #DESCRIPTION_KEY
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private final InternationalString description;

    /**
     * {@code true} if this type is deprecated.
     *
     * @see #isDeprecated()
     * @see #DEPRECATED_KEY
     */
    final boolean deprecated;

    /**
     * Constructs a type from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the {@value #NAME_KEY} key,
     * unless a fallback is specified with the {@value #INHERIT_FROM_KEY} key.
     * Other entries listed in the table below are optional.
     *
     * <table class="sis">
     *   <caption>Recognized map entries</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value #NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value #DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr><tr>
     *     <td>{@value #DESIGNATION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr><tr>
     *     <td>{@value #DESCRIPTION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr><tr>
     *     <td>{@value #DEPRECATED_KEY}</td>
     *     <td>{@link Boolean}</td>
     *     <td>{@link #isDeprecated()}</td>
     *   </tr><tr>
     *     <td>{@value #INHERIT_FROM_KEY}</td>
     *     <td>{@link IdentifiedType}</td>
     *     <td>(various)</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
     *     <td>{@link Locale}</td>
     *     <td>(none)</td>
     *   </tr>
     * </table>
     *
     * <h4>Localization</h4>
     * All localizable attributes like {@code "definition"} may have a language and country code suffix.
     * For example, the {@code "definition_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
     * the {@code "definition_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     * They are convenience properties for building the {@code InternationalString} value.
     *
     * <p>The {@code "locale"} property applies only in case of exception for formatting the error message,
     * and is used only on a <em>best effort</em> basis. The locale is discarded after successful construction
     * because localizations are applied by the {@link InternationalString#toString(Locale)} method.</p>
     *
     * @param  identification  the name and other information to be given to this identified type.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    @SuppressWarnings("this-escape")
    protected AbstractIdentifiedType(final Map<String,?> identification) throws IllegalArgumentException {
        final IdentifiedType inheritFrom = Containers.property(identification, INHERIT_FROM_KEY, IdentifiedType.class);
        Object value = identification.get(NAME_KEY);    // Implicit null value check.
        if (value == null) {
            if (inheritFrom == null || (name = inheritFrom.getName()) == null) {
                throw new IllegalArgumentException(Errors.forProperties(identification)
                        .getString(Errors.Keys.MissingValueForProperty_1, NAME_KEY));
            }
        } else if (value instanceof String) {
            name = createName(DefaultNameFactory.provider(), (String) value);
        } else if (value instanceof GenericName) {
            name = (GenericName) value;
        } else {
            throw illegalPropertyType(identification, NAME_KEY, value);
        }
        definition  = toInternationalString(identification, DEFINITION_KEY,  inheritFrom);
        designation = toInternationalString(identification, DESIGNATION_KEY, inheritFrom);
        description = toInternationalString(identification, DESCRIPTION_KEY, inheritFrom);
        value = identification.get(DEPRECATED_KEY);
        if (value == null) {
            deprecated = (inheritFrom instanceof Deprecable) ? ((Deprecable) inheritFrom).isDeprecated() : false;
        } else if (value instanceof Boolean) {
            deprecated = (Boolean) value;
        } else {
            throw illegalPropertyType(identification, DEPRECATED_KEY, value);
        }
    }

    /**
     * Returns an international string for the values in the given properties map, or {@code null} if none.
     *
     * @param  identification  the map from which to get the string values for an international string.
     * @param  prefix          the prefix of keys to use for creating the international string.
     * @param  inheritFrom     the type from which to inherit a value if none is specified in the map, or {@code null}.
     * @return the international string, or {@code null} if the given map is null or does not contain values
     *         associated to keys starting with the given prefix.
     */
    private static InternationalString toInternationalString(
            final Map<String,?> identification, final String prefix, final IdentifiedType inheritFrom)
    {
        InternationalString i18n = Types.toInternationalString(identification, prefix);
        if (i18n == null && inheritFrom != null) {
            switch (prefix) {
                case DEFINITION_KEY:  i18n = inheritFrom.getDefinition(); break;
                case DESIGNATION_KEY: i18n = inheritFrom.getDesignation().orElse(null); break;
                case DESCRIPTION_KEY: i18n = inheritFrom.getDescription().orElse(null); break;
            }
        }
        return i18n;
    }

    /**
     * Returns a more compact representation of the given map. This method is similar to
     * {@link Containers#unmodifiable(Map)} except that it does not wrap the map in an unmodifiable view.
     * The intent is to avoid one level of indirection for performance and memory reasons.
     * This is okay only if the map is kept in a private field and never escape outside that class.
     *
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to compact, or {@code null}.
     * @return a potentially compacted map, or {@code null} if the given map was null.
     *
     * @see Containers#unmodifiable(Map)
     */
    static <K,V> Map<K,V> compact(final Map<K,V> map) {
        if (map != null) {
            switch (map.size()) {
                case 0: return Collections.emptyMap();
                case 1: final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                        return Collections.singletonMap(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Returns the exception to be thrown when a property is of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(final Map<String,?> identification,
            final String key, final Object value)
    {
        return new IllegalArgumentException(Errors.forProperties(identification).getString(
                Errors.Keys.IllegalPropertyValueClass_2, key, value.getClass()));
    }

    /**
     * Convenience method for subclasses that create new types derived from this type.
     * The purpose is more to improve readability than to save a few byte codes.
     */
    final Map<String,?> inherit() {
        return Map.of(INHERIT_FROM_KEY, this);
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
     * <h4>API design note</h4>
     * This method is final because it is invoked (indirectly) by subclass constructors,
     * and invoking a user-overrideable method at construction time is not recommended.
     * Furthermore, this attribute is often used as the primary key for {@code IdentifiedType} instances
     * and need some guarantees about its stability.
     *
     * @return the type name.
     */
    @Override
    public final GenericName getName() {
        return name;
    }

    /**
     * Returns a concise definition of the element.
     *
     * @return concise definition of the element.
     */
    @Override
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Returns a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @return natural language designator for the element.
     */
    @Override
    public Optional<InternationalString> getDesignation() {
        return Optional.ofNullable(designation);
    }

    /**
     * Returns optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * <p>If this type {@linkplain #isDeprecated() is deprecated}, then the description should give
     * indication about the replacement (e.g. <q>superceded by …</q>).</p>
     *
     * @return information beyond that required for concise definition of the element, or {@code null} if none.
     */
    @Override
    public Optional<InternationalString> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns comments on or information about this type.
     * The default implementation performs the following choice:
     *
     * <ul>
     *   <li>If this type {@linkplain #isDeprecated() is deprecated}, returns the
     *       {@linkplain #getDescription() description}. The description of deprecated types
     *       should give indication about the replacement (e.g. <q>superceded by …</q>).</li>
     *   <li>Otherwise returns {@code null} since remarks are not part of the ISO 19109 feature model.</li>
     * </ul>
     *
     * @return the remarks, or {@code null} if none.
     *
     * @since 0.8
     */
    @Override
    public Optional<InternationalString> getRemarks() {
        return Optional.ofNullable(deprecated ? description : null);
    }

    /**
     * Returns {@code true} if this type is deprecated.
     * If this method returns {@code true}, then the {@linkplain #getRemarks() remarks} should give
     * indication about the replacement (e.g. <q>superceded by …</q>).
     *
     * @return whether this type is deprecated.
     *
     * @since 0.8
     */
    @Override
    public boolean isDeprecated() {
        return deprecated;
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
     * @return the hash code for this type.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, definition, designation, description, deprecated);
    }

    /**
     * Compares this type with the given object for equality.
     *
     * @param  obj  the object to compare with this type.
     * @return {@code true} if the given object is equal to this type.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && getClass() == obj.getClass()) {
            final var that = (AbstractIdentifiedType) obj;
            return Objects.equals(name,        that.name) &&
                   Objects.equals(definition,  that.definition) &&
                   Objects.equals(designation, that.designation) &&
                   Objects.equals(description, that.description) &&
                   deprecated == that.deprecated;
        }
        return false;
    }

    /**
     * Returns the string representation of the given name, making sure that the name is non-null
     * and the string non-empty. This method is used for checking argument validity.
     *
     * @param  name       the name for which to get the string representation.
     * @param  container  the feature or attribute which contains the named characteristics.
     * @param  argument   the name of the argument ({@code "properties"} or {@code "characterizedBy"}).
     * @param  index      index of the characteristics having the given name.
     * @throws IllegalArgumentException if the given name is null or have an empty string representation.
     */
    static String toString(final GenericName name, final IdentifiedType container, final String argument, final int index) {
        short key = Errors.Keys.MissingValueForProperty_1;
        if (name != null) {
            final String s = name.toString();
            if (!s.isEmpty()) {
                return s;
            }
            key = Errors.Keys.EmptyProperty_1;
        }
        final var b = new StringBuilder(40).append("Type[“").append(container.getName()).append("”].")
                .append(argument).append('[').append(index).append("].name");
        throw new IllegalArgumentException(Errors.format(key, b.toString()));
    }
}
