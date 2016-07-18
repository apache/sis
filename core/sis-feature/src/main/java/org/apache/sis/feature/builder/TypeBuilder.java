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
package org.apache.sis.feature.builder;

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

// Branch-dependent imports
import java.util.Objects;
import org.opengis.feature.IdentifiedType;


/**
 * Properties common to all kind of types (feature, association, characteristics).
 * Those properties are:
 *
 * <ul>
 *   <li>the name        — a unique name which can be defined within a scope (or namespace).</li>
 *   <li>the definition  — a concise definition of the element.</li>
 *   <li>the designation — a natural language designator for the element for user interfaces.</li>
 *   <li>the description — information beyond that required for concise definition of the element.</li>
 * </ul>
 *
 * In many cases, the names of all {@code AttributeType}s and {@code AssociationRole}s to create
 * within a {@code FeatureType} share the same namespace.
 * For making name creations more convenient, a default namespace can be
 * {@linkplain FeatureTypeBuilder#setDefaultScope specified once} and applied automatically
 * to all names created by the {@link #setName(String)} method.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class TypeBuilder implements Localized {
    /**
     * The feature name, definition, designation and description.
     * The name is mandatory; all other information are optional.
     */
    private final Map<String,Object> identification = new HashMap<String,Object>(4);

    /**
     * Creates a new builder initialized to the values of an existing type.
     */
    TypeBuilder(final IdentifiedType template, final Locale locale) {
        putIfNonNull(Errors.LOCALE_KEY, locale);
        if (template != null) {
            putIfNonNull(AbstractIdentifiedType.NAME_KEY,        template.getName());
            putIfNonNull(AbstractIdentifiedType.DEFINITION_KEY,  template.getDefinition());
            putIfNonNull(AbstractIdentifiedType.DESIGNATION_KEY, template.getDesignation());
            putIfNonNull(AbstractIdentifiedType.DESCRIPTION_KEY, template.getDescription());
        }
    }

    /**
     * Puts the given value in the {@link #identification} map if the value is non-null.
     * This method should be invoked only when the {@link #identification} map is known
     * to not contain any value for the given key.
     */
    private void putIfNonNull(final String key, final Object value) {
        if (value != null) {
            identification.put(key, value);
        }
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
     * If the object created by the last call to {@code build()} has been cached, clears that cache.
     */
    abstract void clearCache();

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
     * Returns the name of the {@code IdentifiedType} to create, or {@code null} if undefined.
     * This method returns the value built from the last call to a {@code setName(…)} method,
     * or a default name or {@code null} if no name has been explicitely specified.
     *
     * @return the name of the {@code IdentifiedType} to create (may be a default name or {@code null}).
     *
     * @see AbstractIdentifiedType#getName()
     */
    public GenericName getName() {
        return (GenericName) identification().get(AbstractIdentifiedType.NAME_KEY);
    }

    /**
     * Returns a default name to use if the user did not specified a name. The first letter will be changed to
     * lower case (unless the name looks like an acronym) for compliance with Java convention on property names.
     */
    String getDefaultName() {
        return null;
    }

    /**
     * Returns the name to use for displaying error messages.
     */
    final String getDisplayName() {
        final GenericName name = getName();
        return (name != null) ? name.toString() : Vocabulary.getResources(identification).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * Sets the {@code IdentifiedType} name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * <div class="note"><b>Note for subclasses:</b>
     * all {@code setName(…)} convenience methods in this builder delegate to this method.
     * Consequently this method can be used as a central place where to control the creation of all names.</div>
     *
     * @param  name  the generic name (can not be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getName()
     * @see AbstractIdentifiedType#NAME_KEY
     */
    public TypeBuilder setName(final GenericName name) {
        ensureNonNull("name", name);
        if (!name.equals(identification.put(AbstractIdentifiedType.NAME_KEY, name))) {
            clearCache();
        }
        return this;
    }

    /**
     * Sets the {@code IdentifiedType} name as a simple string with the default scope.
     * The default scope is the value specified by the last call to {@link FeatureTypeBuilder#setDefaultScope(String)}.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if no default scope
     * has been specified, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param  localPart  the local part of the generic name (can not be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getName()
     */
    public TypeBuilder setName(final String localPart) {
        ensureNonEmpty("localPart", localPart);
        return setName(name(null, localPart));
    }

    /**
     * Sets the {@code IdentifiedType} name as a string in the given scope.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if the given scope is
     * {@code null} or empty, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     * If a {@linkplain FeatureTypeBuilder#setDefaultScope(String) default scope} has been specified, then the
     * {@code scope} argument overrides it.
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param  scope      the scope of the name to create, or {@code null} if the name is local.
     * @param  localPart  the local part of the generic name (can not be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getName()
     */
    public TypeBuilder setName(String scope, final String localPart) {
        ensureNonEmpty("localPart", localPart);
        if (scope == null) {
            scope = "";                                 // For preventing the use of default scope.
        }
        return setName(name(scope, localPart));
    }

    /**
     * Returns a concise definition of the element.
     *
     * @return concise definition of the element, or {@code null} if none.
     *
     * @see AbstractIdentifiedType#getDefinition()
     */
    public CharSequence getDefinition() {
        return (CharSequence) identification.get(AbstractIdentifiedType.DEFINITION_KEY);
    }

    /**
     * Sets a concise definition of the element.
     *
     * @param  definition a concise definition of the element, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getDefinition()
     * @see AbstractIdentifiedType#DEFINITION_KEY
     */
    public TypeBuilder setDefinition(final CharSequence definition) {
        if (!Objects.equals(definition, identification.put(AbstractIdentifiedType.DEFINITION_KEY, definition))) {
            clearCache();
        }
        return this;
    }

    /**
     * Returns a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @return natural language designator for the element, or {@code null} if none.
     *
     * @see AbstractIdentifiedType#getDesignation()
     */
    public CharSequence getDesignation() {
        return (CharSequence) identification.get(AbstractIdentifiedType.DESIGNATION_KEY);
    }

    /**
     * Sets a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @param  designation a natural language designator for the element, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getDesignation()
     * @see AbstractIdentifiedType#DESIGNATION_KEY
     */
    public TypeBuilder setDesignation(final CharSequence designation) {
        if (!Objects.equals(designation, identification.put(AbstractIdentifiedType.DESIGNATION_KEY, designation))) {
            clearCache();
        }
        return this;
    }

    /**
     * Returns optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     *
     * @return information beyond that required for concise definition of the element, or {@code null} if none.
     *
     * @see AbstractIdentifiedType#getDescription()
     */
    public CharSequence getDescription() {
        return (CharSequence) identification.get(AbstractIdentifiedType.DESCRIPTION_KEY);
    }

    /**
     * Sets optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the feature scope and application.
     *
     * @param  description  information beyond that required for concise definition of the element, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getDescription()
     * @see AbstractIdentifiedType#DESCRIPTION_KEY
     */
    public TypeBuilder setDescription(final CharSequence description) {
        if (!Objects.equals(description, identification.put(AbstractIdentifiedType.DESCRIPTION_KEY, description))) {
            clearCache();
        }
        return this;
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

    /**
     * Returns a string representation of this object.
     * The returned string is for debugging purpose only and may change in any future SIS version.
     *
     * @return a string representation of this object for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return toString(new StringBuilder(Classes.getShortClassName(this))).toString();
    }

    /**
     * Partial implementation of {@link #toString()}. This method assumes that the class name
     * has already been written in the buffer.
     */
    final StringBuilder toString(final StringBuilder buffer) {
        toStringInternal(buffer.append("[“").append(getDisplayName()).append('”'));
        return buffer.append(']');
    }

    /**
     * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
     */
    void toStringInternal(StringBuilder buffer) {
    }
}
