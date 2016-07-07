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
package org.apache.sis.internal.feature;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.opengis.util.GenericName;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * Base class of feature and attribute builders.
 * This base class provide the method needed for filling the {@code identification} map.
 *
 * @param <T> the builder subclass. It is subclass responsibility to ensure that {@code this}
 *            is assignable to {@code <T>}; this {@code Builder} class can not verify that.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
abstract class Builder<T extends Builder<T>> implements Localized {
    /**
     * The feature name, definition, designation and description.
     * The name is mandatory; all other information are optional.
     */
    private final Map<String,Object> identification = new HashMap<>(4);

    /**
     * Creates a new builder instance which will format error message using the given locale.
     */
    Builder(final Locale locale) {
        setLocale(locale);
    }

    /**
     * Creates a new builder instance.
     */
    Builder(final Builder<?> parent) {
        setLocale(parent.identification.get(Errors.LOCALE_KEY));
    }

    /**
     * Sets the locale if non-null. This method should be invoked only when the {@link #identification} map is empty.
     *
     * @see #getLocale()
     */
    private void setLocale(final Object locale) {
        if (locale != null) {
            identification.put(Errors.LOCALE_KEY, locale);
        }
    }

    /**
     * Resets this builder to its initial state. After invocation of this method,
     * this builder is in the same state than after construction.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @SuppressWarnings("unchecked")
    public T clear() {
        final Object locale = identification.get(Errors.LOCALE_KEY);
        identification.clear();
        setLocale(locale);
        return (T) this;
    }

    /**
     * Creates a generic name from the given scope and local part.
     * An empty scope means no scope. A {@code null} scope means the
     * {@linkplain FeatureTypeBuilder#setDefaultScope(String) default scope}.
     *
     * @param scope      the scope of the name to create, or {@code null} if the name is local.
     * @param localPart  the local part of the generic name (can not be {@code null}).
     */
    abstract GenericName name(String scope, String localPart);

    /**
     * Returns a default name to use if the user did not specified a name. The first letter will be changed to
     * lower case (unless the name looks like an acronym) for compliance with Java convention on property names.
     */
    String getDefaultName() {
        return null;
    }

    /**
     * Returns the map of properties to give to the {@code FeatureType} or {@code PropertyType} constructor.
     * If the map does not contains a name, a default name may be generated.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<String,Object> identification() {
        if (identification.get(AbstractIdentifiedType.NAME_KEY) == null) {
            String name = getDefaultName();
            if (name != null) {
                final int length = name.length();
                if (length != 0) {
                    final int c  = name.codePointAt(0);
                    final int lc = Character.toLowerCase(c);
                    if (c != lc) {
                        final int n = Character.charCount(c);
                        if (n >= length || Character.isLowerCase(name.codePointAt(n))) {
                            final StringBuilder buffer = new StringBuilder(length);
                            name = buffer.appendCodePoint(lc).append(name, n, length).toString();
                        }
                    }
                    identification.put(AbstractIdentifiedType.NAME_KEY, name(null, name));
                }
            }
        }
        return identification;
    }

    /**
     * Sets the name as a simple string with the default scope.
     * The default scope is the value specified by the last call to
     * {@link FeatureTypeBuilder#setDefaultScope(String)}.
     *
     * <p>The name will be an instance of {@link org.opengis.util.LocalName} if no default scope
     * has been specified, or an instance of {@link org.opengis.util.ScopedName} otherwise.</p>
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param  localPart  the local part of the generic name (can not be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     */
    public T setName(String localPart) {
        ensureNonEmpty("localPart", localPart);
        return setName(name(null, localPart));
    }

    /**
     * Sets the name as a string in the given scope.
     * If a {@linkplain FeatureTypeBuilder#setDefaultScope(String) default scope} was specified,
     * this method override it.
     *
     * <p>The name will be an instance of {@link org.opengis.util.LocalName} if the given scope
     * is {@code null} or empty, or an instance of {@link org.opengis.util.ScopedName} otherwise.</p>
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param  scope      the scope of the name to create, or {@code null} if the name is local.
     * @param  localPart  the local part of the generic name (can not be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     */
    public T setName(String scope, String localPart) {
        ensureNonEmpty("localPart", localPart);
        if (scope == null) {
            scope = "";                                 // For preventing the use of default scope.
        }
        return setName(name(scope, localPart));
    }

    /**
     * Sets the name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * <div class="note"><b>Note for subclasses:</b>
     * all {@code setName(…)} convenience methods in this builder delegate to this method.
     * Consequently this method can be used as a central place where to control the creation of all names.</div>
     *
     * @param  name  the generic name (can not be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     *
     * @see AbstractIdentifiedType#NAME_KEY
     */
    @SuppressWarnings("unchecked")
    public T setName(GenericName name) {
        ensureNonNull("name", name);
        identification.put(AbstractIdentifiedType.NAME_KEY, name);
        return (T) this;
    }

    /**
     * Returns the current name, or {@code null} if undefined.
     * This method returns the value built from the last call to a {@code setName(…)} method,
     * or a default name or {@code null} if no name has been explicitely specified.
     *
     * @return the current name (may be a default name or {@code null}).
     */
    public GenericName getName() {
        return (GenericName) identification().get(AbstractIdentifiedType.NAME_KEY);
    }

    /**
     * Returns the name to use for displaying error messages.
     */
    final String getDisplayName() {
        final GenericName name = getName();
        return (name != null) ? name.toString() : Vocabulary.getResources(identification).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * Sets a concise definition of the element.
     *
     * @param  definition a concise definition of the element, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see AbstractIdentifiedType#DEFINITION_KEY
     */
    @SuppressWarnings("unchecked")
    public T setDefinition(CharSequence definition) {
        identification.put(AbstractIdentifiedType.DEFINITION_KEY, definition);
        return (T) this;
    }

    /**
     * Sets a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @param  designation a natural language designator for the element, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see AbstractIdentifiedType#DESIGNATION_KEY
     */
    @SuppressWarnings("unchecked")
    public T setDesignation(CharSequence designation) {
        identification.put(AbstractIdentifiedType.DESIGNATION_KEY, designation);
        return (T) this;
    }

    /**
     * Sets optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the feature scope and application.
     *
     * @param  description  information beyond that required for concise definition of the element, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see AbstractIdentifiedType#DESCRIPTION_KEY
     */
    @SuppressWarnings("unchecked")
    public T setDescription(CharSequence description) {
        identification.put(AbstractIdentifiedType.DESCRIPTION_KEY, description);
        return (T) this;
    }

    /**
     * Returns the locale used for formatting error messages, or {@code null} if unspecified.
     * If unspecified, the system default locale will be used.
     *
     * @return the locale used for formatting error messages, or {@code null} if unspecified.
     */
    @Override
    public Locale getLocale() {
        return (Locale) identification.get(Errors.LOCALE_KEY);
    }

    /**
     * Returns a string representation of this object.
     * The returned string is for debugging purpose only and may change in any future SIS version.
     *
     * @return a string representation of this object for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this));
        toStringInternal(buffer.append("[“").append(getDisplayName()).append('”'));
        return buffer.append(']').toString();
    }

    /**
     * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
     */
    void toStringInternal(StringBuilder buffer) {
    }

    /**
     * Returns the resources for error messages.
     */
    final Errors errors() {
        return Errors.getResources(identification);
    }

    /**
     * Same as {@link org.apache.sis.util.ArgumentChecks#ensureNonNull(String, Object)},
     * but uses the current locale in case of error.
     *
     * @param  name the name of the argument to be checked. Used only if an exception is thrown.
     * @param  object the user argument to check against null value.
     * @throws NullArgumentException if {@code object} is null.
     */
    final void ensureNonNull(final String name, final Object value) {
        if (value == null) {
            throw new NullArgumentException(errors().getString(Errors.Keys.NullArgument_1, name));
        }
    }

    /**
     * Same as {@link org.apache.sis.util.ArgumentChecks#ensureNonEmpty(String, CharSequence)},
     * but uses the current locale in case of error.
     *
     * @param  name the name of the argument to be checked. Used only if an exception is thrown.
     * @param  text the user argument to check against null value and empty sequences.
     * @throws NullArgumentException if {@code text} is null.
     * @throws IllegalArgumentException if {@code text} is empty.
     */
    final void ensureNonEmpty(final String name, final String text) {
        if (text == null) {
            throw new NullArgumentException(errors().getString(Errors.Keys.NullArgument_1, name));
        }
        if (text.length() == 0) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.EmptyArgument_1, name));
        }
    }
}
