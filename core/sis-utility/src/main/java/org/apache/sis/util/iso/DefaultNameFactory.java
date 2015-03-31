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
package org.apache.sis.util.iso;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakHashSet;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.iso.DefaultNameSpace.DEFAULT_SEPARATOR_STRING;


/**
 * A factory for creating {@link AbstractName} objects.
 * This factory provides the following methods for creating name instances:
 *
 * <ul>
 *   <li>{@link #createTypeName(NameSpace, CharSequence)}</li>
 *   <li>{@link #createMemberName(NameSpace, CharSequence, TypeName)}</li>
 *   <li>{@link #createLocalName(NameSpace, CharSequence)}</li>
 *   <li>{@link #createGenericName(NameSpace, CharSequence[])} – for local or scoped names</li>
 * </ul>
 *
 * The following methods for creating miscellaneous name-related objects:
 *
 * <ul>
 *   <li>{@link #createNameSpace(GenericName, Map)}</li>
 *   <li>{@link #createInternationalString(Map)}</li>
 * </ul>
 *
 * And the following methods for performing some analysis:
 *
 * <ul>
 *   <li>{@link #parseGenericName(NameSpace, CharSequence)}</li>
 *   <li>{@link #toGenericNames(Object)}</li>
 *   <li>{@link #toTypeName(Class)}</li>
 * </ul>
 *
 * <div class="section">Thread safety</div>
 * The same {@code DefaultNameFactory} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see Names
 * @see DefaultNameSpace
 * @see DefaultScopedName
 * @see DefaultLocalName
 * @see DefaultTypeName
 * @see DefaultMemberName
 */
public class DefaultNameFactory extends AbstractFactory implements NameFactory {
    /**
     * The key for name separator.
     */
    static final String SEPARATOR_KEY = "separator";

    /**
     * The key or the separator after the first name.
     */
    static final String HEAD_SEPARATOR_KEY = "separator.head";

    /**
     * Weak references to the name created by this factory.
     */
    private final WeakHashSet<GenericName> pool;

    /**
     * Helper class for mapping {@link Class} to {@link TypeName}, created when first needed.
     */
    private transient volatile TypeNames typeNames;

    /**
     * Creates a new factory.
     */
    public DefaultNameFactory() {
        pool = new WeakHashSet<GenericName>(GenericName.class);
    }

    /**
     * Creates an international string from a set of strings in different locales.
     *
     * @param strings String value for each locale key.
     * @return The international string.
     *
     * @see Types#toInternationalString(CharSequence)
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
     * Returns a namespace having the given name. Despite the "create" name, this method tries to
     * return an existing instance when possible. The namespace is characterized by the given name,
     * and optionally by the following properties:
     *
     * <blockquote><table class="sis">
     *   <caption>Recognized properties</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Purpose</th>
     *   </tr>
     *   <tr>
     *     <td>{@code "separator"}</td>
     *     <td>The separator to insert between {@linkplain AbstractName#getParsedNames() parsed names}
     *         in that namespace.</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "separator.head"}</td>
     *     <td>The separator to insert between the namespace and the {@linkplain AbstractName#head() head}.<br>
     *         If omitted, then the default is the same value than {@code "separator"}.</td>
     *   </tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>For URN namespace, {@code separator} = {@code ":"} is typically sufficient.</li>
     *   <li>For HTTP namespace, {@code separator.head} = {@code "://"} and {@code separator} = {@code "."}.</li>
     * </ul></div>
     *
     * @param name
     *          The name of the namespace to be returned. This argument can be created using
     *          <code>{@linkplain #createGenericName(NameSpace, CharSequence[]) createGenericName}(null, namespace)</code>.
     * @param properties
     *          An optional map of properties to be assigned to the namespace, or {@code null} if none.
     *
     * @return A namespace having the given name and separator.
     *
     * @see Names#createLocalName(CharSequence, String, CharSequence)
     */
    @Override
    public NameSpace createNameSpace(final GenericName name, final Map<String,?> properties) {
        ensureNonNull("name", name);
        String separator = getString(properties, SEPARATOR_KEY);
        if (separator == null) {
            separator = DefaultNameSpace.DEFAULT_SEPARATOR_STRING;
        }
        String headSeparator = getString(properties, HEAD_SEPARATOR_KEY);
        if (headSeparator == null) {
            headSeparator = separator;
        }
        final boolean isEmpty = separator.isEmpty();
        if (isEmpty || headSeparator.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.EmptyProperty_1, isEmpty ? SEPARATOR_KEY : HEAD_SEPARATOR_KEY));
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
     *
     * @see #toTypeName(Class)
     * @see Names#createTypeName(CharSequence, String, CharSequence)
     */
    @Override
    public TypeName createTypeName(final NameSpace scope, final CharSequence name) {
        return pool.unique(new DefaultTypeName(scope, name));
    }

    /**
     * Creates a member name from the given character sequence and attribute type.
     * The default implementation returns a new or an existing {@link DefaultMemberName} instance.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the member
     *         name to be created, or {@code null} for a global namespace.
     * @param  name The member name as a string or an international string.
     * @param  attributeType The type of the data associated with the record member.
     * @return The member name for the given character sequence.
     */
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
     *
     * @see Names#createLocalName(CharSequence, String, CharSequence)
     */
    @Override
    public LocalName createLocalName(final NameSpace scope, final CharSequence name) {
        /*
         * Maintenance note: if the body of this method is modified (except for the use of the cache),
         * consider updating DefaultLocalName.castOrCopy(LocalName) method accordingly.
         */
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
     * @param  scope The {@linkplain AbstractName#scope() scope} of the generic name to be created,
     *         or {@code null} for a global namespace.
     * @param  parsedNames The local names as an array of {@link String} or {@link InternationalString} instances.
     *         This array shall contain at least one element.
     * @return The generic name for the given parsed names.
     *
     * @see #parseGenericName(NameSpace, CharSequence)
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
     * {@link DefaultNameSpace#DEFAULT_SEPARATOR ':'} separator if the given scope is null.
     *
     * @param  scope The {@linkplain AbstractName#scope() scope} of the generic name to
     *         be created, or {@code null} for a global namespace.
     * @param  name The qualified name, as a sequence of names separated by a scope-dependent
     *         separator.
     * @return A name parsed from the given string.
     *
     * @see Names#parseGenericName(CharSequence, String, CharSequence)
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
        final List<String> names = new ArrayList<String>();
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
     * Converts the given value to an array of generic names. If the given value is an instance of
     * {@link GenericName}, {@link String} or any other type enumerated below, then it is converted
     * and returned in an array of length 1. If the given value is an array or a collection, then an
     * array of same length is returned where each element has been converted.
     *
     * <p>Allowed types or element types are:</p>
     * <ul>
     *   <li>{@link GenericName}, to be casted and returned as-is.</li>
     *   <li>{@link CharSequence} (usually a {@link String} or an {@link InternationalString}),
     *       to be parsed as a generic name using the {@link DefaultNameSpace#DEFAULT_SEPARATOR ':'} separator.</li>
     *   <li>{@link Identifier}, its {@linkplain Identifier#getCode() code} to be parsed as a generic name
     *       using the {@link DefaultNameSpace#DEFAULT_SEPARATOR ':'} separator.</li>
     * </ul>
     *
     * If {@code value} is an array or a collection containing {@code null} elements,
     * then the corresponding element in the returned array will also be {@code null}.
     *
     * @param  value The object to cast into an array of generic names, or {@code null}.
     * @return The generic names, or {@code null} if the given {@code value} was null.
     *         Note that it may be the {@code value} reference itself casted to {@code GenericName[]}.
     * @throws ClassCastException if {@code value} can't be casted.
     *
     * @since 0.5
     */
    public GenericName[] toGenericNames(Object value) throws ClassCastException {
        if (value == null) {
            return null;
        }
        GenericName name = toGenericName(value);
        if (name != null) {
            return new GenericName[] {
                name
            };
        }
        /*
         * Above code checked for a singleton. Now check for a collection or an array.
         */
        final Object[] values;
        if (value instanceof Object[]) {
            values = (Object[]) value;
            if (values instanceof GenericName[]) {
                return (GenericName[]) values;
            }
        } else if (value instanceof Collection<?>) {
            values = ((Collection<?>) value).toArray();
        } else {
            throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                    "value", value.getClass()));
        }
        final GenericName[] names = new GenericName[values.length];
        for (int i=0; i<values.length; i++) {
            value = values[i];
            if (value != null) {
                name = toGenericName(value);
                if (name == null) {
                    throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                            "value[" + i + ']', value.getClass()));
                }
                names[i] = name;
            }
        }
        return names;
    }

    /**
     * Creates a generic name from the given value. The value may be an instance of
     * {@link GenericName}, {@link Identifier}, {@link CharSequence} or {@link Class}.
     * If the given object is not recognized, then this method returns {@code null}.
     *
     * @param  value The object to convert.
     * @return The converted object, or {@code null} if {@code value} is not convertible.
     */
    private GenericName toGenericName(final Object value) {
        if (value instanceof GenericName) {
            return (GenericName) value;
        }
        if (value instanceof Identifier) {
            return parseGenericName(null, ((Identifier) value).getCode());
        }
        if (value instanceof CharSequence) {
            return parseGenericName(null, (CharSequence) value);
        }
        if (value instanceof Class<?>) {
            return toTypeName((Class<?>) value);
        }
        return null;
    }

    /**
     * Suggests a type name for the given class. Apache SIS provides a mapping between {@code Class}
     * and {@code TypeName} objects as documented in the {@link DefaultTypeName} javadoc.
     *
     * <p>In order to protect against potential changes in the {@code Class} ↔ {@code TypeName} mapping, users are
     * encouraged to retrieve the {@code valueClass} by invoking the {@link Names#toClass(TypeName)} method instead
     * than parsing the name.</p>
     *
     * @param  valueClass The Java class for which to get a type name, or {@code null}.
     * @return A suggested type name, or {@code null} if the given class was null.
     *
     * @see DefaultTypeName#toClass()
     * @see Names#toClass(TypeName)
     *
     * @since 0.5
     */
    public TypeName toTypeName(final Class<?> valueClass) {
        if (!TypeNames.isValid(valueClass)) {
            return null;
        }
        /*
         * Note: we do not cache the TypeName for the valueClass argument because:
         *
         *  - It is not needed (at least in the default implementation) for getting unique instance.
         *  - It is not the best place for performance improvement, since TypeName are usually only
         *    a step in the creation of bigger object (typically AttributeType). Users are better to
         *    cache the bigger object instead.
         */
        TypeNames t = typeNames;
        if (t == null) {
            /*
             * Create TypeNames outide the synchronized block because the TypeNames constructor will call back
             * methods from this class. Since those methods are overrideable, this could invoke user's code.
             * Note also that methods in this class use the 'pool', which is itself synchronized, so we are
             * better to avoid double synchronization for reducing the risk of dead-lock.
             */
            final TypeNames c = new TypeNames(this);
            synchronized (this) { // Double-check strategy is ok if 'typeNames' is volatile.
                t = typeNames;
                if (t == null) {
                    typeNames = t = c;
                }
            }
        }
        return t.toTypeName(this, valueClass);
    }
}
