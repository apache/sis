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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Predicate;
import org.opengis.util.CodeList;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters.Filter;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import java.lang.reflect.Array;


/**
 * Implementation of some {@link org.apache.sis.util.iso.Types} methods needed by {@code org.apache.sis.util} module.
 * This class opportunistically implements {@code CodeList.Filter} interface, but this is an implementation details.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CodeLists implements CodeList.Filter {
    /**
     * The name of bundle resources for code list titles.
     * Keys are {@link CodeList#identifier()}.
     */
    public static final String RESOURCES = "org.apache.sis.metadata.iso.CodeLists";

    /**
     * The name to compare during filtering operation.
     */
    private final String codename;

    /**
     * {@code true} if {@link CodeList#valueOf} is allowed to create new code lists.
     */
    private final boolean canCreate;

    /**
     * Creates a new filter for the specified code name.
     *
     * @param codename the name to compare during filtering operation.
     */
    private CodeLists(final String codename, final boolean canCreate) {
        this.codename  = codename;
        this.canCreate = canCreate;
    }

    /**
     * Returns the name of the code to create, or {@code null} if no new code list shall be created.
     *
     * @return the name specified at construction time.
     */
    @Override
    public String codename() {
        return canCreate ? codename : null;
    }

    /**
     * Returns {@code true} if the given code matches the name we are looking for.
     *
     * @param  code  the code list candidate.
     */
    @Override
    public boolean accept(final CodeList<?> code) {
        for (final String candidate : code.names()) {
            if (accept(candidate, codename)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given names matches the name we are looking for.
     */
    private static boolean accept(final String candidate, final String codename) {
        return CharSequences.equalsFiltered(candidate, codename, Filter.LETTERS_AND_DIGITS, true);
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
            if (values != null) {
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
        return (name != null) ? CodeList.valueOf(codeType, new CodeLists(name, false)) : null;
    }

    /**
     * Returns the code of the given type that matches the filter.
     *
     * @param  <E>       the compile-time type given as the {@code codeType} parameter.
     * @param  codeType  the type of code list.
     * @param  filter    the criterion for selecting a code list.
     * @return a code matching the given name, or {@code null} if none.
     */
    public static <E extends CodeList<E>> E find(final Class<E> codeType, final Predicate<? super CodeList<?>> filter) {
        return CodeList.valueOf(codeType, new CodeList.Filter() {
            @Override public boolean accept(CodeList<?> code) {return filter.test(code);}
            @Override public String codename() {return null;}
        });
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
        return CodeList.valueOf(codeType, new CodeLists(name, true));
    }

    /**
     * Returns all known values for the given type of code list or enumeration.
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
}
