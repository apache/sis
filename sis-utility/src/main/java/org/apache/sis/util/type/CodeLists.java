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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.opengis.util.CodeList;
import org.opengis.annotation.UML;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;


/**
 * Static methods working on {@link CodeList}.
 * This class provides:
 *
 * <ul>
 *   <li>{@link #getListName(CodeList)} and {@link #getCodeName(CodeList)}
 *       for fetching ISO name if possible, or Java name as a fallback.</li>
 *   <li>{@link #getCodeTitle(CodeList, Locale)} for fetching human-readable names.</li>
 *   <li>A SIS {@link #valueOf(Class, String)} method which can be used instead of the GeoAPI
 *       {@link CodeList#valueOf(Class, String)} when more tolerant search is wanted.
 *       The main difference between the two methods is that the GeoAPI method is strict
 *       while the SIS method ignores cases, whitespaces and the {@code '_'} character.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
public final class CodeLists extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private CodeLists() {
    }

    /**
     * Returns the ISO classname (if available) or the Java classname (as a fallback)
     * of the given code. This method uses the {@link UML} annotation if it exists, or
     * fallback on the {@linkplain Class#getSimpleName() simple class name} otherwise.
     * Examples:
     *
     * <ul>
     *   <li><code>getListName({@linkplain org.opengis.referencing.cs.AxisDirection#NORTH})</code> returns {@code "CS_AxisDirection"}.</li>
     *   <li><code>getListName({@linkplain org.opengis.metadata.identification.CharacterSet#UTF_8})</code> returns {@code "MD_CharacterSetCode"}.</li>
     *   <li><code>getListName({@linkplain org.opengis.metadata.content.ImagingCondition#BLURRED_IMAGE})</code> returns {@code "MD_ImagingConditionCode"}.</li>
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
        final String id = Types.getStandardName(type);
        return (id != null) ? id : type.getSimpleName();
    }

    /**
     * Returns the ISO name (if available) or the Java name (as a fallback) of the given code.
     * If the code has no {@link UML} identifier, then the programmatic name is used as a fallback.
     * Examples:
     *
     * <ul>
     *   <li><code>getCodeName({@linkplain org.opengis.referencing.cs.AxisDirection#NORTH})</code> returns {@code "north"}.</li>
     *   <li><code>getCodeName({@linkplain org.opengis.metadata.identification.CharacterSet#UTF_8})</code> returns {@code "utf8"}.</li>
     *   <li><code>getCodeName({@linkplain org.opengis.metadata.content.ImagingCondition#BLURRED_IMAGE})</code> returns {@code "blurredImage"}.</li>
     * </ul>
     *
     * @param  code The code for which to get the name, or {@code null}.
     * @return The UML identifiers or programmatic name for the given code,
     *         or {@code null} if the given code is null.
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
     *   <li><code>getCodeTitle({@linkplain org.opengis.referencing.cs.AxisDirection#NORTH})</code> returns {@code "North"}.</li>
     *   <li><code>getCodeTitle({@linkplain org.opengis.metadata.identification.CharacterSet#UTF_8})</code> returns {@code "UTF-8"}.</li>
     *   <li><code>getCodeTitle({@linkplain org.opengis.metadata.content.ImagingCondition#BLURRED_IMAGE})</code> returns {@code "Blurred image"}.</li>
     * </ul>
     *
     * @param  code The code from which to get a title, or {@code null}.
     * @return A unlocalized title for the given code, or {@code null} if the given code is null.
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
            return ResourceBundle.getBundle("org.opengis.metadata.CodeLists", locale).getString(key);
        } catch (MissingResourceException e) {
            Logging.recoverableException(CodeLists.class, "getCodeTitle", e);
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
     * @see Types#getDescription(Class, Locale)
     */
    public static String getDescription(final CodeList<?> code, Locale locale) {
        if (code != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            final String key = getListName(code) + '.' + getCodeName(code);
            try {
                return ResourceBundle.getBundle("org.opengis.metadata.Descriptions", locale).getString(key);
            } catch (MissingResourceException e) {
                Logging.recoverableException(CodeLists.class, "getDescription", e);
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
    public static <T extends CodeList<?>> T[] values(final Class<T> codeType) {
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
        } catch (NoSuchMethodException | IllegalAccessException e) {
            values = Array.newInstance(codeType, 0);
        }
        return (T[]) values;
    }

    /**
     * Returns the code of the given type that matches the given name, or returns a new one if none
     * match it. This method performs the same work than the GeoAPI method, except that this method
     * is more tolerant on string comparisons when looking for an existing code:
     *
     * <ul>
     *   <li>Name comparisons are case-insensitive.</li>
     *   <li>Only {@linkplain Character#isLetterOrDigit(int) letter and digit} characters are compared.
     *       Spaces and punctuation characters like {@code '_'} and {@code '-'} are ignored.</li>
     * </ul>
     *
     * @param <T>       The compile-time type given as the {@code codeType} parameter.
     * @param codeType  The type of code list.
     * @param name      The name of the code to obtain, or {@code null}.
     * @return A code matching the given name, or {@code null} if the name is null.
     *
     * @see CodeList#valueOf(Class, String)
     */
    public static <T extends CodeList<T>> T valueOf(final Class<T> codeType, final String name) {
        return valueOf(codeType, name, true);
    }

    /**
     * Returns the code of the given type that matches the given name, as described in the
     * {@link #valueOf(Class, String)} method. If no existing code matches, then this method
     * creates a new code if {@code canCreate} is {@code true}, or returns {@code null} otherwise.
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
    public static <T extends CodeList<T>> T valueOf(final Class<T> codeType, String name, final boolean canCreate) {
        if (name == null || (name = name.trim()).isEmpty()) {
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
}
