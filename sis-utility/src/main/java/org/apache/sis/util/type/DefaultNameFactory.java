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
package org.apache.sis.util.type;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import net.jcip.annotations.ThreadSafe;

import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;

import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.collection.WeakHashSet;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.type.DefaultNameSpace.DEFAULT_SEPARATOR_STRING;


/**
 * A factory for creating {@link AbstractName} objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class DefaultNameFactory extends AbstractFactory implements NameFactory {
    /**
     * Weak references to the name created by this factory.
     */
    private final WeakHashSet<GenericName> pool;

    /**
     * Creates a new factory.
     */
    public DefaultNameFactory() {
        pool = new WeakHashSet<>(GenericName.class);
    }

    /**
     * Creates an international string from a set of strings in different locales.
     */
    @Override
    public InternationalString createInternationalString(final Map<Locale,String> strings) {
        ensureNonNull("strings", strings);
        switch (strings.size()) {
            case 0:  throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyDictionary));
            case 1:  return new SimpleInternationalString(strings.values().iterator().next());
            default: return new DefaultInternationalString(strings);
        }
        // Do not cache in the pool, because not all instances are immutable.
    }

    /**
     * Returns the value for the given key in the given properties map, or {@code null} if none.
     */
    private static String getString(final Map<String,?> properties, final String key) {
        if (properties != null) {
            final Object value = properties.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Creates a namespace having the given name. Despite the "create" name, this method tries
     * to returns an existing instance when possible.
     *
     * <p>This method can receive an optional map of properties. Recognized entries are:</p>
     * <table class="sis">
     *   <tr>
     *     <th nowrap>Property name</th>
     *     <th nowrap>Purpose</th>
     *   </tr>
     *   <tr>
     *     <td>{@code "separator"}</td>
     *     <td>The separator to insert between
     *     {@linkplain AbstractName#getParsedNames() parsed names} in that namespace.
     *     For HTTP namespace, it is {@code "."}.
     *     For URN namespace, it is typically {@code ":"}.</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "separator.head"}</td>
     *     <td>The separator to insert between the namespace and the
     *     {@linkplain AbstractName#head() head}. For HTTP namespace, it is {@code "://"}.
     *     For URN namespace, it is typically {@code ":"}. If this entry is omitted, then
     *     the default is the same value than the {@code "separator"} entry.</td>
     *   </tr>
     * </table>
     *
     * @param name
     *          The name of the namespace to be returned. This argument can be created using
     *          <code>{@linkplain #createGenericName(NameSpace, CharSequence[]) createGenericName}(null, parsedNames)</code>.
     * @param properties
     *          An optional map of properties to be assigned to the namespace, or {@code null} if none.
     *
     * @return A namespace having the given name and separator.
     */
    @Override
    public NameSpace createNameSpace(final GenericName name, final Map<String,?> properties) {
        ensureNonNull("name", name);
        String separator = getString(properties, "separator");
        if (separator == null) {
            separator = DefaultNameSpace.DEFAULT_SEPARATOR_STRING;
        }
        String headSeparator = getString(properties, "separator.head");
        if (headSeparator == null) {
            headSeparator = separator;
        }
        final boolean isEmpty = separator.isEmpty();
        if (isEmpty || headSeparator.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.EmptyProperty_1, isEmpty ? "separator" : "separator.head"));
        }
        return DefaultNameSpace.forName(name.toFullyQualifiedName(), headSeparator, separator);
    }

    /**
     * Creates a type name from the given character sequence.
     * The default implementation returns a new or an existing {@link DefaultTypeName} instance.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the type
     *         name to be created, or {@code null} for a global namespace.
     * @param  name The type name as a string or an international string.
     * @return The type name for the given character sequence.
     * @throws NullArgumentException If the {@code name} argument is null.
     */
    @Override
    public TypeName createTypeName(final NameSpace scope, final CharSequence name) {
        return pool.unique(new DefaultTypeName(scope, name));
    }

    /**
     * Creates a member name from the given character sequence and attribute type.
     * The default implementation returns a new or an existing {@link DefaultMemberName}
     * instance.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the member
     *         name to be created, or {@code null} for a global namespace.
     * @param  name The member name as a string or an international string.
     * @param  attributeType The type of the data associated with the record member.
     * @return The member name for the given character sequence.
     * @throws NullArgumentException If the {@code name} or {@code attributeType} argument is null.
     */
    @Override
    public MemberName createMemberName(final NameSpace scope, final CharSequence name, final TypeName attributeType) {
        return pool.unique(new DefaultMemberName(scope, name, attributeType));
    }

    /**
     * Creates a local name from the given character sequence.
     * The default implementation returns a new or an existing {@link DefaultLocalName} instance.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the local
     *         name to be created, or {@code null} for a global namespace.
     * @param  name The local name as a string or an international string.
     * @return The local name for the given character sequence.
     * @throws NullArgumentException If the {@code name} argument is null.
     */
    @Override
    public LocalName createLocalName(final NameSpace scope, final CharSequence name) {
        if (scope instanceof DefaultNameSpace) {
            // Following may return a cached instance.
            return ((DefaultNameSpace) scope).local(name, null);
        }
        return pool.unique(new DefaultLocalName(scope, name));
    }

    /**
     * Creates a local or scoped name from an array of parsed names. The default implementation
     * returns an instance of {@link DefaultLocalName} if the length of the {@code parsedNames}
     * array is 1, or an instance of {@link DefaultScopedName} if the length of the array is 2
     * or more.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the generic name to
     *         be created, or {@code null} for a global namespace.
     * @param  parsedNames The local names as an array of {@link String} or {@link InternationalString}
     *         instances. This array shall contains at least one element.
     * @return The generic name for the given parsed names.
     * @throws NullArgumentException If the given array is empty.
     */
    @Override
    public GenericName createGenericName(final NameSpace scope, final CharSequence... parsedNames) {
        ensureNonNull("parsedNames", parsedNames);
        switch (parsedNames.length) {
            default: return pool.unique(new DefaultScopedName(scope, Arrays.asList(parsedNames)));
            case 1:  return createLocalName(scope, parsedNames[0]); // User may override.
            case 0:  throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "parsedNames"));
        }
    }

    /**
     * Constructs a generic name from a qualified name.
     * This method splits the given name around a separator inferred from the given scope, or the
     * {@value org.apache.sis.util.typeDefaultNameSpace#DEFAULT_SEPARATOR} separator if the given
     * scope is null.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the generic name to
     *         be created, or {@code null} for a global namespace.
     * @param  name The qualified name, as a sequence of names separated by a scope-dependent
     *         separator.
     * @return A name parsed from the given string.
     */
    @Override
    public GenericName parseGenericName(final NameSpace scope, final CharSequence name) {
        final String separator;
        if (scope instanceof DefaultNameSpace) {
            separator = ((DefaultNameSpace) scope).separator;
        } else {
            separator = DEFAULT_SEPARATOR_STRING;
        }
        final int s = separator.length();
        final List<String> names = new ArrayList<>();
        int lower = 0;
        final String string = name.toString();
        while (true) {
            final int upper = string.indexOf(separator, lower);
            if (upper >= 0) {
                names.add(string.substring(lower, upper));
                lower = upper + s;
            } else {
                names.add(string.substring(lower));
                break;
            }
        }
        if (names.size() == 1) {
            // Preserves the InternationalString (current implementation of
            // the parsing code above has lost the internationalization).
            return createLocalName(scope, name);
        }
        return createGenericName(scope, names.toArray(new String[names.size()]));
    }

    /**
     * Creates a generic name from the given value. The value may be an instance of
     * {@link GenericName}, {@link Identifier} or {@link CharSequence}. If the given
     * object is not recognized, then this method returns {@code null}.
     *
     * @param  value The object to convert.
     * @return The converted object, or {@code null} if {@code value} is not convertible.
     */
    private GenericName createFromObject(final Object value) {
        ensureNonNull("value", value);
        if (value instanceof GenericName) {
            return (GenericName) value;
        }
        if (value instanceof Identifier) {
            return parseGenericName(null, ((Identifier) value).getCode());
        }
        if (value instanceof CharSequence) {
            return parseGenericName(null, (CharSequence) value);
        }
        return null;
    }

    /**
     * Converts the given value to an array of generic names. If the given value is an instance of
     * {@link GenericName}, {@link String} or any other type enumerated below, then it is converted
     * and returned in an array of length 1. If the given value is an array or a collection, then an
     * array of same length is returned where each element has been converted.
     *
     * <p>Allowed types or element types are:</p>
     * <ul>
     *   <li>{@link GenericName}, to be casted and returned as-is.</li>
     *   <li>{@link CharSequence} (usually a {@link String} or an {@link InternationalString}), to
     *       be parsed as a generic name using the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR
     *       default separator}.</li>
     *   <li>{@link Identifier}, its {@linkplain Identifier#getCode() code} to be parsed as a generic name
     *       using the {@value org.apache.sis.util.typeDefaultNameSpace#DEFAULT_SEPARATOR} separator.</li>
     * </ul>
     *
     * @param  value The object to cast into an array of generic names.
     * @return The generic names. May be a direct reference to {@code value}.
     * @throws NullArgumentException if {@code value} is null.
     * @throws ClassCastException if {@code value} can't be casted.
     */
    public GenericName[] toArray(Object value) throws ClassCastException {
        GenericName name = createFromObject(value);
        if (name != null) {
            return new GenericName[] {
                name
            };
        }
        /*
         * Above code checked for a singleton. Now check for a collection or an array.
         */
        final Object[] values;
        if (value instanceof Collection<?>) {
            values = ((Collection<?>) value).toArray();
        } else if (value instanceof Object[]) {
            values = (Object[]) value;
        } else {
            throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                    "value", value.getClass()));
        }
        if (values instanceof GenericName[]) {
            return (GenericName[]) values;
        }
        final GenericName[] names = new GenericName[values.length];
        for (int i=0; i<values.length; i++) {
            value = values[i];
            name = createFromObject(value);
            if (name == null) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                        "value[" + i + ']', value.getClass()));
            }
            names[i] = name;
        }
        return names;
    }
}
