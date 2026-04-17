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
package org.apache.sis.util.internal.shared;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.opengis.util.CodeList;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters.Filter;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.lang.reflect.InaccessibleObjectException;
import org.opengis.util.ControlledVocabulary;
import org.apache.sis.util.resources.Errors;


/**
 * Implementation of some {@link org.apache.sis.util.iso.Types} methods needed by {@code org.apache.sis.util} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CodeLists {
    /**
     * Do not allow instantiation of this class.
     */
    private CodeLists() {
    }

    /**
     * Returns {@code true} if the given names matches the name we are looking for.
     */
    private static boolean accept(final String candidate, final String codename) {
        return CharSequences.equalsFiltered(candidate, codename, Filter.LETTERS_AND_DIGITS, true);
    }

    /**
     * Returns the code from the given array that matches the given name.
     *
     * @param  <E>     the type of code.
     * @param  values  the values to test.
     * @param  name    the name of the code to obtain, or {@code null}.
     * @return a code matching the given name, or {@code null} if none.
     */
    public static <E extends ControlledVocabulary> E forName(final E[] values, String name) {
        name = Strings.trimOrNull(name);
        if (name != null) {
            for (final E code : values) {
                for (String candidate : code.names()) {
                    if (accept(candidate, name)) {
                        return code;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the enumeration value of the given type that matches the given name.
     *
     * @param  <T>       the compile-time type given as the {@code enumType} parameter.
     * @param  enumType  the type of enumeration.
     * @param  name      the name of the enumeration value to obtain, or {@code null}.
     * @return a value matching the given name, or {@code null} if the given name was null or blank.
     * @throws IllegalArgumentException if no enumeration value matches the given name.
     *
     * @see org.apache.sis.util.iso.Types#forEnumName(Class, String)
     */
    public static <T extends Enum<T>> T forEnumName(final Class<T> enumType, String name) {
        name = Strings.trimOrNull(name);
        if (name != null) try {
            return Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException e) {
            final T[] values = enumType.getEnumConstants();
            if (values instanceof ControlledVocabulary[]) {
                for (final ControlledVocabulary code : (ControlledVocabulary[]) values) {
                    for (final String candidate : code.names()) {
                        if (accept(candidate, name)) {
                            return enumType.cast(code);
                        }
                    }
                }
            } else if (values != null) {
                for (final Enum<?> code : values) {
                    if (accept(code.name(), name)) {
                        return enumType.cast(code);
                    }
                }
            }
            throw e;
        }
        return null;
    }

    /**
     * Returns the code of the given type that matches the given name.
     *
     * @param  <E>       the compile-time type given as the {@code codeType} parameter.
     * @param  codeType  the type of code list.
     * @param  name      the name of the code to obtain, or {@code null}.
     * @return a code matching the given name, or {@code null} if none.
     */
    public static <E extends CodeList<E>> E forCodeName(final Class<E> codeType, String name) {
        name = Strings.trimOrNull(name);
        if (name != null) {
            for (final E code : values(codeType)) {
                for (String candidate : code.names()) {
                    if (accept(candidate, name)) {
                        return code;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the code of the given type that matches the given name, or creates a new code if none match the name.
     * This method depends on reflection and should be avoided as much as possible.
     * However, at this time we have no way to avoid this method completely.
     *
     * @param  <E>       the compile-time type given as the {@code codeType} parameter.
     * @param  codeType  the type of code list.
     * @param  name      the name of the code to obtain, or {@code null}.
     * @return a code matching the given name, or {@code null} if the given name was null or blank.
     * @throws IllegalArgumentException if no code value value matches the given name and new code cannot be created.
     */
    public static <E extends CodeList<E>> E getOrCreate(final Class<E> codeType, String name) {
        name = Strings.trimOrNull(name);
        if (name == null) {
            return null;
        }
        E code = forCodeName(codeType, name);
        if (code == null) try {
            code = codeType.cast(codeType.getMethod("valueOf", String.class).invoke(null, name));
        } catch (InvocationTargetException e) {
            throw rethrowOrWrap(e.getCause());
        } catch (IllegalAccessException e) {
            throw (InaccessibleObjectException) new InaccessibleObjectException(e.getMessage()).initCause(e);
        } catch (NoSuchMethodException | NullPointerException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementNotFound_1, name), e);
        }
        return code;
    }

    /**
     * Returns all known values for the given type of code list or enumeration.
     * This method delegates to the public static {@code values()} method.
     * If that method is not found, an empty list is returned.
     *
     * <p>Note that this method is fragile: is uses reflection and relies on a convention
     * (expected contract of {@code values()}) which is not enforced by the Java language.
     * Callers should prefer {@link CodeList#family()} as much as possible.</p>
     *
     * @param  <T>       the compile-time type given as the {@code codeType} parameter.
     * @param  codeType  the type of code list or enumeration.
     * @return the list of values for the given code list or enumeration, or an empty array if none.
     */
    @SuppressWarnings("unchecked")
    public static <T extends CodeList<?>> T[] values(final Class<T> codeType) {
        Object values;
        try {
            values = codeType.getMethod("values", (Class<?>[]) null).invoke(null, (Object[]) null);
        } catch (InvocationTargetException e) {
            throw rethrowOrWrap(e.getCause());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            values = Array.newInstance(codeType, 0);
        }
        return (T[]) values;
    }

    /**
     * Re-throws the given exception if it is unchecked, or wraps it otherwise.
     * In the latter case, the wrapper is {@link UndeclaredThrowableException}
     * because this method is invoked in contexts where no checked exception was allowed.
     */
    private static UndeclaredThrowableException rethrowOrWrap(final Throwable cause) {
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        }
        // `CodeList.valueOf(String)` methods are not expected to throw checked exceptions.
        throw new UndeclaredThrowableException(cause);
    }
}
