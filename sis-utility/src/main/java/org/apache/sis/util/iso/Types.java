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
import java.util.HashMap;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.util.DefaultFactories;


/**
 * Static methods working on GeoAPI types and {@link CodeList} values.
 * This class provides:
 *
 * <ul>
 *   <li>{@link #toInternationalString(CharSequence)} and {@link #toGenericName(Object, NameFactory)}
 *       for creating name-related objects from various objects.</li>
 *   <li>{@link #getStandardName(Class)}, {@link #getListName(CodeList)} and {@link #getCodeName(CodeList)}
 *       for fetching ISO names if possible.</li>
 *   <li>{@link #getCodeTitle(CodeList, Locale)}, {@link #getDescription(CodeList, Locale)} and
 *       {@link #getDescription(Class, Locale)} for fetching human-readable descriptions.</li>
 *   <li>{@link #forStandardName(String)} and {@link #forCodeName(Class, String, boolean)} for
 *       fetching an instance from a name (converse of above {@code get} methods).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
public final class Types extends Static {
    /**
     * The class loader to use for fetching GeoAPI resources.
     * Since the resources are bundled in the GeoAPI JAR file,
     * we use the instance that loaded GeoAPI for more determinist behavior.
     */
    private static final ClassLoader CLASSLOADER = UML.class.getClassLoader();

    /**
     * The types for ISO 19115 UML identifiers. The keys are UML identifiers. Values
     * are either class names as {@link String} objects, or the {@link Class} instances.
     * This map will be built only when first needed.
     *
     * @see #forName(String)
     */
    private static Map<Object,Object> typeForNames;

    /**
     * Do not allow instantiation of this class.
     */
    private Types() {
    }

    /**
     * Returns the ISO name for the given class, or {@code null} if none.
     * This method can be used for GeoAPI interfaces or {@link CodeList}.
     * Examples:
     *
     * <ul>
     *   <li><code>getStandardName({@linkplain org.opengis.metadata.citation.Citation}.class)</code>
     *       (an interface) returns {@code "CI_Citation"}.</li>
     *   <li><code>getStandardName({@linkplain org.opengis.referencing.cs.AxisDirection}.class)</code>
     *       (a code list) returns {@code "CS_AxisDirection"}.</li>
     * </ul>
     *
     * This method looks for the {@link UML} annotation on the given type. It does not search for
     * parent classes or interfaces if the given type is not directly annotated (i.e. {@code @UML}
     * annotations are not inherited). If no annotation is found, then this method does not fallback
     * on the Java name since, as the name implies, this method is about standard names.
     *
     * @param  type The GeoAPI interface or code list from which to get the ISO name, or {@code null}.
     * @return The ISO name for the given type, or {@code null} if none or if the given type is {@code null}.
     *
     * @see #forStandardName(String)
     */
    public static String getStandardName(final Class<?> type) {
        if (type != null) {
            final UML uml = type.getAnnotation(UML.class);
            if (uml != null) {
                final String id = uml.identifier();
                if (id != null && !id.isEmpty()) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * Returns the ISO classname (if available) or the Java classname (as a fallback)
     * of the given code. This method uses the {@link UML} annotation if it exists, or
     * fallback on the {@linkplain Class#getSimpleName() simple class name} otherwise.
     * Examples:
     *
     * <ul>
     *   <li>{@code getListName(AxisDirection.NORTH)} returns {@code "CS_AxisDirection"}.</li>
     *   <li>{@code getListName(CharacterSet.UTF_8)} returns {@code "MD_CharacterSetCode"}.</li>
     *   <li>{@code getListName(ImagingCondition.BLURRED_IMAGE)} returns {@code "MD_ImagingConditionCode"}.</li>
     * </ul>
     *
     * @param  code The code for which to get the class name, or {@code null}.
     * @return The ISO (preferred) or Java (fallback) class name, or {@code null} if the given code is null.
     */
    public static String getListName(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        final Class<?> type = code.getClass();
        final String id = getStandardName(type);
        return (id != null) ? id : type.getSimpleName();
    }

    /**
     * Returns the ISO name (if available) or the Java name (as a fallback) of the given code.
     * If the code has no {@link UML} identifier, then the programmatic name is used as a fallback.
     * Examples:
     *
     * <ul>
     *   <li>{@code getCodeName(AxisDirection.NORTH)} returns {@code "north"}.</li>
     *   <li>{@code getCodeName(CharacterSet.UTF_8)} returns {@code "utf8"}.</li>
     *   <li>{@code getCodeName(ImagingCondition.BLURRED_IMAGE)} returns {@code "blurredImage"}.</li>
     * </ul>
     *
     * @param  code The code for which to get the name, or {@code null}.
     * @return The UML identifiers or programmatic name for the given code,
     *         or {@code null} if the given code is null.
     *
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(CodeList, Locale)
     * @see #forCodeName(Class, String, boolean)
     */
    public static String getCodeName(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        final String id = code.identifier();
        return (id != null && !id.isEmpty()) ? id : code.name();
    }

    /**
     * Returns a unlocalized title for the given code.
     * This method builds a title using heuristics rules, which should give reasonable
     * results without the need of resource bundles. For better results, consider using
     * {@link #getCodeTitle(CodeList, Locale)} instead.
     *
     * <p>The current heuristic implementation iterates over {@linkplain CodeList#names() all
     * code names}, selects the longest one excluding the {@linkplain CodeList#name() field name}
     * if possible, then {@linkplain CharSequences#camelCaseToSentence(CharSequence) makes a sentence}
     * from that name. Examples:</p>
     *
     * <ul>
     *   <li>{@code getCodeTitle(AxisDirection.NORTH)} returns {@code "North"}.</li>
     *   <li>{@code getCodeTitle(CharacterSet.UTF_8)} returns {@code "UTF-8"}.</li>
     *   <li>{@code getCodeTitle(ImagingCondition.BLURRED_IMAGE)} returns {@code "Blurred image"}.</li>
     * </ul>
     *
     * @param  code The code from which to get a title, or {@code null}.
     * @return A unlocalized title for the given code, or {@code null} if the given code is null.
     *
     * @see #getCodeName(CodeList)
     * @see #getDescription(CodeList, Locale)
     */
    public static String getCodeTitle(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        String id = code.identifier();
        final String name = code.name();
        if (id == null) {
            id = name;
        }
        for (final String candidate : code.names()) {
            if (!candidate.equals(name) && candidate.length() >= id.length()) {
                id = candidate;
            }
        }
        return CharSequences.camelCaseToSentence(id).toString();
    }

    /**
     * Returns the localized title of the given code.
     * Special cases:
     *
     * <ul>
     *   <li>If {@code code} is {@code null}, then this method returns {@code null}.</li>
     *   <li>If {@code locale} is {@code null}, then this method uses {@link Locale#US}
     *       as a close approximation of "unlocalized" strings since OGC standards are
     *       defined in English.</li>
     *   <li>If there is no resources for the given code in the given language, then this method
     *       fallback on other languages as described in {@link ResourceBundle} javadoc.</li>
     *   <li>If there is no localized resources for the given code, then this method fallback
     *       on {@link #getCodeTitle(CodeList)}.</li>
     * </ul>
     *
     * @param  code   The code for which to get the localized name, or {@code null}.
     * @param  locale The local, or {@code null} if none.
     * @return The localized title, or {@code null} if the given code is null.
     *
     * @see #getDescription(CodeList, Locale)
     */
    public static String getCodeTitle(final CodeList<?> code, Locale locale) {
        if (code == null) {
            return null;
        }
        if (locale == null) {
            locale = Locale.US;
        }
        /*
         * The code below is a duplicated - in a different way - of CodeListProxy(CodeList)
         * constructor (org.apache.sis.internal.jaxb.code package). This duplication exists
         * because CodeListProxy constructor stores more information in an opportunist way.
         * If this method is updated, please update CodeListProxy(CodeList) accordingly.
         */
        final String key = getListName(code) + '.' + getCodeName(code);
        try {
            return ResourceBundle.getBundle("org.opengis.metadata.CodeLists", locale, CLASSLOADER).getString(key);
        } catch (MissingResourceException e) {
            Logging.recoverableException(Types.class, "getCodeTitle", e);
            return getCodeTitle(code);
        }
    }

    /**
     * Returns the localized description of the given code, or {@code null} if none.
     * Special cases:
     *
     * <ul>
     *   <li>If {@code code} is {@code null}, then this method returns {@code null}.</li>
     *   <li>If {@code locale} is {@code null}, then this method uses the
     *       {@linkplain Locale#getDefault() default locale} - there is no such thing
     *       like "unlocalized" description.</li>
     *   <li>If there is no resources for the given code in the given language, then this method
     *       fallback on other languages as described in {@link ResourceBundle} javadoc.</li>
     *   <li>If there is no localized resources for the given code, then this method returns
     *       {@code null} - there is no fallback.</li>
     * </ul>
     *
     * For a description of the code list as a whole instead than a particular code,
     * see {@link Types#getDescription(Class, Locale)}.
     *
     * @param  code   The code for which to get the localized description, or {@code null}.
     * @param  locale The desired local, or {@code null} for the default locale.
     * @return The localized description, or {@code null} if the given code is null.
     *
     * @see #getCodeTitle(CodeList, Locale)
     * @see #getDescription(Class, Locale)
     */
    public static String getDescription(final CodeList<?> code, final Locale locale) {
        return (code != null) ? getDescription(getListName(code) + '.' + getCodeName(code), locale) : null;
    }

    /**
     * Returns a localized description for the given class, or {@code null} if none.
     * This method can be used for GeoAPI interfaces or {@link CodeList}.
     * Special cases:
     *
     * <ul>
     *   <li>If {@code code} is {@code null}, then this method returns {@code null}.</li>
     *   <li>If {@code locale} is {@code null}, then this method uses the
     *       {@linkplain Locale#getDefault() default locale} - there is no such thing
     *       like "unlocalized" description.</li>
     *   <li>If there is no resources for the given type in the given language, then this method
     *       fallback on other languages as described in {@link ResourceBundle} javadoc.</li>
     *   <li>If there is no localized resources for the given type, then this method returns
     *       {@code null} - there is no fallback.</li>
     * </ul>
     *
     * @param  type The GeoAPI interface or code list from which to get the description, or {@code null}.
     * @param  locale The desired local, or {@code null} for the default locale.
     * @return The ISO name for the given type, or {@code null} if none or if the type is {@code null}.
     *
     * @see #getDescription(CodeList, Locale)
     */
    public static String getDescription(final Class<?> type, final Locale locale) {
        return getDescription(getStandardName(type), locale);
    }

    /**
     * Returns the descriptions for the given key in the given locale.
     *
     * @param  key     The ISO identifier of a class, or a class property, or a code list value.
     * @param  locale  The locale in which to get the description.
     * @return The description, or {@code null} if none.
     */
    private static String getDescription(final String key, Locale locale) {
        if (key != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            try {
                return ResourceBundle.getBundle("org.opengis.metadata.Descriptions", locale, CLASSLOADER).getString(key);
            } catch (MissingResourceException e) {
                Logging.recoverableException(Types.class, "getDescription", e);
            }
        }
        return null;
    }

    /**
     * Returns all known values for the given type of code list.
     * Note that the size of the returned array may growth between different invocations of this method,
     * since users can add their own codes to an existing list.
     *
     * @param <T> The compile-time type given as the {@code codeType} parameter.
     * @param codeType The type of code list.
     * @return The list of values for the given code list, or an empty array if none.
     */
    @SuppressWarnings("unchecked")
    public static <T extends CodeList<?>> T[] getCodeValues(final Class<T> codeType) {
        Object values;
        try {
            values = codeType.getMethod("values", (Class<?>[]) null).invoke(null, (Object[]) null);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new UndeclaredThrowableException(cause);
        } catch (NoSuchMethodException e) {
            values = Array.newInstance(codeType, 0);
        } catch (IllegalAccessException e) {
            values = Array.newInstance(codeType, 0);
        }
        return (T[]) values;
    }

    /**
     * Returns the GeoAPI interface for the given ISO name, or {@code null} if none.
     * The identifier argument shall be the value documented in the {@link UML#identifier()}
     * annotation associated with the GeoAPI interface.
     * Examples:
     *
     * <ul>
     *   <li>{@code forStandardName("CI_Citation")}      returns <code>{@linkplain org.opengis.metadata.citation.Citation}.class</code></li>
     *   <li>{@code forStandardName("CS_AxisDirection")} returns <code>{@linkplain org.opengis.referencing.cs.AxisDirection}.class</code></li>
     * </ul>
     *
     * Only identifiers for the stable part of GeoAPI are recognized. This method does not handle
     * the identifiers for the {@code geoapi-pending} module.
     *
     * @param  identifier The ISO {@linkplain UML} identifier, or {@code null}.
     * @return The GeoAPI interface, or {@code null} if the given identifier is {@code null} or unknown.
     */
    public static synchronized Class<?> forStandardName(final String identifier) {
        if (identifier == null) {
            return null;
        }
        if (typeForNames == null) {
            final Class<UML> c = UML.class;
            final InputStream in = c.getResourceAsStream("class-index.properties");
            if (in == null) {
                throw new MissingResourceException("class-index.properties", c.getName(), identifier);
            }
            final Properties props = new Properties();
            try {
                props.load(in);
                in.close();
            } catch (IOException e) {
                throw new BackingStoreException(e);
            } catch (IllegalArgumentException e) {
                throw new BackingStoreException(e);
            }
            typeForNames = new HashMap<Object,Object>(props);
        }
        final Object value = typeForNames.get(identifier);
        if (value == null || value instanceof Class<?>) {
            return (Class<?>) value;
        }
        final Class<?> type;
        try {
            type = Class.forName((String) value);
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException((String) value, e);
        }
        typeForNames.put(identifier, type);
        return type;
    }

    /**
     * Returns the code of the given type that matches the given name, or optionally returns a new
     * one if none match it. This method performs the same work than the GeoAPI {@code valueOf(…)}
     * method, except that this method is more tolerant on string comparisons when looking for an
     * existing code:
     *
     * <ul>
     *   <li>Name comparisons are case-insensitive.</li>
     *   <li>Only {@linkplain Character#isLetterOrDigit(int) letter and digit} characters are compared.
     *       Spaces and punctuation characters like {@code '_'} and {@code '-'} are ignored.</li>
     * </ul>
     *
     * If no match is found, then a new code is created only if the {@code canCreate} argument is
     * {@code true}. Otherwise this method returns {@code null}.
     *
     * @param <T>        The compile-time type given as the {@code codeType} parameter.
     * @param codeType   The type of code list.
     * @param name       The name of the code to obtain, or {@code null}.
     * @param canCreate  {@code true} if this method is allowed to create new code.
     * @return A code matching the given name, or {@code null} if the name is null
     *         or if no matching code is found and {@code canCreate} is {@code false}.
     *
     * @see CodeList#valueOf(Class, String)
     */
    public static <T extends CodeList<T>> T forCodeName(final Class<T> codeType, String name, final boolean canCreate) {
        name = CharSequences.trimWhitespaces(name);
        if (name == null || name.isEmpty()) {
            return null;
        }
        final String typeName = codeType.getName();
        try {
            // Forces initialization of the given class in order
            // to register its list of static final constants.
            Class.forName(typeName, true, codeType.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException(typeName, e); // Should never happen.
        }
        return CodeList.valueOf(codeType, new CodeListFilter(name, canCreate));
    }

    /**
     * Returns the given characters sequence as an international string. If the given sequence is
     * null or an instance of {@link InternationalString}, this this method returns it unchanged.
     * Otherwise, this method copies the {@link InternationalString#toString()} value in a new
     * {@link SimpleInternationalString} instance and returns it.
     *
     * @param  string The characters sequence to convert, or {@code null}.
     * @return The given sequence as an international string,
     *         or {@code null} if the given sequence was null.
     *
     * @see DefaultNameFactory#createInternationalString(Map)
     */
    public static InternationalString toInternationalString(final CharSequence string) {
        if (string == null || string instanceof InternationalString) {
            return (InternationalString) string;
        }
        return new SimpleInternationalString(string.toString());
    }

    /**
     * Returns the given array of {@code CharSequence}s as an array of {@code InternationalString}s.
     * If the given array is null or an instance of {@code InternationalString[]}, then this method
     * returns it unchanged. Otherwise a new array of type {@code InternationalString[]} is created
     * and every elements from the given array is copied or
     * {@linkplain #toInternationalString(CharSequence) converted} in the new array.
     *
     * <p>If a defensive copy of the {@code strings} array is wanted, then the caller needs to check
     * if the returned array is the same instance than the one given in argument to this method.</p>
     *
     * @param  strings The characters sequences to convert, or {@code null}.
     * @return The given array as an array of type {@code InternationalString[]},
     *         or {@code null} if the given array was null.
     */
    public static InternationalString[] toInternationalStrings(final CharSequence... strings) {
        if (strings == null || strings instanceof InternationalString[]) {
            return (InternationalString[]) strings;
        }
        final InternationalString[] copy = new InternationalString[strings.length];
        for (int i=0; i<strings.length; i++) {
            copy[i] = toInternationalString(strings[i]);
        }
        return copy;
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
     *       to be parsed as a generic name using the
     *       {@value org.apache.sis.util.iso.DefaultNameSpace#DEFAULT_SEPARATOR} separator.</li>
     *   <li>{@link Identifier}, its {@linkplain Identifier#getCode() code} to be parsed as a generic name
     *       using the {@value org.apache.sis.util.iso.DefaultNameSpace#DEFAULT_SEPARATOR} separator.</li>
     * </ul>
     *
     * If {@code value} is an array or a collection containing {@code null} elements,
     * then the corresponding element in the returned array will also be {@code null}.
     *
     * @param  value The object to cast into an array of generic names, or {@code null}.
     * @param  factory The factory to use for creating names, or {@code null} for the default.
     * @return The generic names, or {@code null} if the given {@code value} was null.
     *         Note that it may be the {@code value} reference itself casted to {@code GenericName[]}.
     * @throws ClassCastException if {@code value} can't be casted.
     */
    public static GenericName[] toGenericNames(Object value, NameFactory factory) throws ClassCastException {
        if (value == null) {
            return null;
        }
        if (factory == null) {
            factory = DefaultFactories.NAMES;
        }
        GenericName name = toGenericName(value, factory);
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
                name = toGenericName(value, factory);
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
     * {@link GenericName}, {@link Identifier} or {@link CharSequence}. If the given
     * object is not recognized, then this method returns {@code null}.
     *
     * @param  value The object to convert.
     * @param  factory The factory to use for creating names.
     * @return The converted object, or {@code null} if {@code value} is not convertible.
     */
    private static GenericName toGenericName(final Object value, final NameFactory factory) {
        if (value instanceof GenericName) {
            return (GenericName) value;
        }
        if (value instanceof Identifier) {
            return factory.parseGenericName(null, ((Identifier) value).getCode());
        }
        if (value instanceof CharSequence) {
            return factory.parseGenericName(null, (CharSequence) value);
        }
        return null;
    }
}
