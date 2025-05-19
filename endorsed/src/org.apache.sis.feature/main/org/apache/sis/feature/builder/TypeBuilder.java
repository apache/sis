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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.PropertyNotFoundException;


/**
 * Information common to all kind of types (feature, association, characteristics).
 * Those information are:
 *
 * <ul>
 *   <li>the name        — a unique name which can be defined within a scope (or namespace).</li>
 *   <li>the definition  — a concise definition of the element.</li>
 *   <li>the designation — a natural language designator for the element for user interfaces.</li>
 *   <li>the description — information beyond that required for concise definition of the element.</li>
 * </ul>
 *
 * The name is mandatory and can be specified as either {@link org.opengis.util.LocalName},
 * {@link org.opengis.util.ScopedName}, {@link String} or {@link InternationalString} instance.
 * All other properties are optional.
 *
 * <h2>Default namespace</h2>
 * In many cases, the names of all {@code AttributeType}s and {@code AssociationRole}s to create
 * within a {@code FeatureType} share the same namespace.
 * For making name creations more convenient, the namespace can be
 * {@linkplain FeatureTypeBuilder#setNameSpace specified once} and applied automatically
 * to all names created by the {@link #setName(CharSequence)} method.
 * Note that namespaces will not be visible in the name {@linkplain org.apache.sis.util.iso.DefaultLocalName#toString()
 * string representation} unless the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toFullyQualifiedName() fully
 * qualified name} is requested.
 * Example:
 *
 * {@snippet lang="java" :
 *     FeatureTypeBuilder builder = new FeatureTypeBuilder().setNameSpace("MyNameSpace").setName("City");
 *     FeatureType city = builder.build();

 *     System.out.println(city.getName());                              // Prints "City"
 *     System.out.println(city.getName().toFullyQualifiedName());       // Prints "MyNameSpace:City"
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.8
 */
public abstract class TypeBuilder implements Localized {
    /**
     * The feature name, definition, designation and description.
     * The name is mandatory; all other information are optional.
     */
    private final Map<String,Object> identification;

    /**
     * Creates a new builder initialized to the values of the given builder.
     * This constructor is for {@link AttributeTypeBuilder#setValueClass(Class)}
     * and {@link CharacteristicTypeBuilder#setValueClass(Class)} implementations.
     *
     * @param builder  the builder from which to copy information.
     */
    TypeBuilder(final TypeBuilder builder) {
        identification = new HashMap<>(builder.identification);
    }

    /**
     * Creates a new builder initialized to the given configuration.
     */
    TypeBuilder(final Locale locale) {
        identification = new HashMap<>(4);
        putIfNonNull(Errors.LOCALE_KEY, locale);
    }

    /**
     * Resets the identification map. After invoking this method, this {@code TypeBuilder}
     * is in same state that after it has been {@linkplain #TypeBuilder(Locale) constructed}.
     *
     * @see #clearCache()
     */
    final void reset() {
        final Object locale = identification.get(Errors.LOCALE_KEY);
        identification.clear();
        putIfNonNull(Errors.LOCALE_KEY, locale);
    }

    /**
     * Initializes this builder to the value of the given type.
     * The caller is responsible to invoke {@link #reset()} (if needed) before this method.
     */
    final void initialize(final IdentifiedType template) {
        putIfNonNull(AbstractIdentifiedType.NAME_KEY,        template.getName());
        putIfNonNull(AbstractIdentifiedType.DEFINITION_KEY,  template.getDefinition());
        putIfNonNull(AbstractIdentifiedType.DESIGNATION_KEY, template.getDesignation().orElse(null));
        putIfNonNull(AbstractIdentifiedType.DESCRIPTION_KEY, template.getDescription().orElse(null));
        if (template instanceof Deprecable && ((Deprecable) template).isDeprecated()) {
            identification.put(AbstractIdentifiedType.DEPRECATED_KEY, Boolean.TRUE);
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
                    identification.put(AbstractIdentifiedType.NAME_KEY, createLocalName(name));
                }
            }
        }
        return identification;
    }

    /**
     * If the object created by the last call to {@code build()} has been cached, clears that cache.
     *
     * @see #reset()
     */
    abstract void clearCache();

    /**
     * Creates a local name in the {@linkplain FeatureTypeBuilder#setNameSpace feature namespace}.
     */
    abstract GenericName createLocalName(final CharSequence name);

    /**
     * Creates a generic name in the {@linkplain FeatureTypeBuilder#setNameSpace feature namespace}.
     */
    abstract GenericName createGenericName(final CharSequence... names);

    /**
     * Returns the name of the {@code IdentifiedType} to create, or {@code null} if undefined.
     * This method returns the value built from the last call to a {@code setName(…)} method,
     * or a default name, or {@code null} if no name has been explicitly specified.
     *
     * @return the name of the {@code IdentifiedType} to create (may be a default name or {@code null}).
     *
     * @see #setName(GenericName)
     * @see AbstractIdentifiedType#getName()
     * @see FeatureTypeBuilder#getNameSpace()
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
        return (name != null) ? name.toString() : Vocabulary.forProperties(identification).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * Sets the {@code IdentifiedType} name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * <h4>Note for subclasses</h4>
     * All {@code setName(…)} convenience methods in this builder delegate to this method.
     * Consequently, this method can be used as a central place where to control the creation of all names.
     *
     * @param  name  the generic name (cannot be {@code null}).
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getName()
     * @see #setName(CharSequence)
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
     * Sets the {@code IdentifiedType} name as a simple string (local name).
     * The namespace will be the value specified by the last call to {@link FeatureTypeBuilder#setNameSpace(CharSequence)},
     * but that namespace will not be visible in the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toString()
     * string representation} unless the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toFullyQualifiedName()
     * fully qualified name} is requested.
     *
     * <p>This convenience method creates a {@link org.opengis.util.LocalName} instance from
     * the given {@code CharSequence}, then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param  localPart  the local part of the generic name as a {@link String} or {@link InternationalString}.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getName()
     * @see #setName(CharSequence...)
     * @see FeatureTypeBuilder#getNameSpace()
     */
    public TypeBuilder setName(final CharSequence localPart) {
        ensureNonEmpty("localPart", localPart);
        return setName(createLocalName(localPart));
    }

    /**
     * Sets the {@code IdentifiedType} name as a string in the given scope.
     * The {@code components} array must contain at least one element.
     * The last component (the {@linkplain org.apache.sis.util.iso.DefaultScopedName#tip() tip}) will be sufficient
     * in many cases for calls to the {@link org.apache.sis.feature.AbstractFeature#getProperty(String)} method.
     * The other elements before the last one are optional and can be used for resolving ambiguity.
     * They will be visible as the name {@linkplain org.apache.sis.util.iso.DefaultScopedName#path() path}.
     *
     * <div class="note"><b>Example:</b>
     * a call to {@code setName("A", "B", "C")} will create a "A:B:C" name.
     * A property built with this name can be obtained from a feature by a call to {@code feature.getProperty("C")}
     * if there is no ambiguity, or otherwise by a call to {@code feature.getProperty("B:C")} (if non-ambiguous) or
     * {@code feature.getProperty("A:B:C")}.</div>
     *
     * In addition to the path specified by the {@code components} array, the name may also contain
     * a namespace specified by the last call to {@link FeatureTypeBuilder#setNameSpace(CharSequence)}.
     * But contrarily to the specified components, the namespace will not be visible in the name
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#toString() string representation} unless the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#toFullyQualifiedName() fully qualified name} is requested.
     *
     * <p>This convenience method creates a {@link org.opengis.util.LocalName} or {@link org.opengis.util.ScopedName}
     * instance depending on whether the {@code names} array contains exactly 1 element or more than 1 element, then
     * delegates to {@link #setName(GenericName)}.</p>
     *
     * @param  components  the name components as an array of {@link String} or {@link InternationalString} instances.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getName()
     * @see #setName(CharSequence)
     * @see FeatureTypeBuilder#getNameSpace()
     */
    public TypeBuilder setName(final CharSequence... components) {
        ensureNonNull("components", components);
        if (components.length == 0) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.EmptyArgument_1, "components"));
        }
        return setName(createGenericName(components));
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
     * @param  definition  a concise definition of the element, or {@code null} if none.
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
     * @param  designation  a natural language designator for the element, or {@code null} if none.
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
     * If the type {@linkplain #isDeprecated() is deprecated}, then the description should
     * give indication about the replacement (e.g. <q>superceded by …</q>).
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
     * Returns {@code true} if the type is deprecated.
     * If this method returns {@code true}, then the {@linkplain #getDescription() description} should give
     * indication about the replacement (e.g. <q>superceded by …</q>).
     *
     * @return whether this type is deprecated.
     *
     * @see AbstractIdentifiedType#isDeprecated()
     */
    public boolean isDeprecated() {
        return Boolean.TRUE.equals(identification.get(AbstractIdentifiedType.DEPRECATED_KEY));
    }

    /**
     * Sets whether the type is deprecated.
     * If the type is deprecated, then the {@linkplain #setDescription(CharSequence) description}
     * should be set to an indication about the replacement (e.g. <q>superceded by …</q>).
     *
     * @param  deprecated  whether this type is deprecated.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #isDeprecated()
     * @see AbstractIdentifiedType#DEPRECATED_KEY
     */
    public TypeBuilder setDeprecated(final boolean deprecated) {
        final Boolean wrapper = deprecated;
        Object previous = identification.put(AbstractIdentifiedType.DEPRECATED_KEY, wrapper);
        if (previous == null) {
            previous = Boolean.FALSE;
        }
        if (!Objects.equals(wrapper, previous)) {
            clearCache();
        }
        return this;
    }

    /**
     * Returns the element of the given name in the given list. The given name does not need to contains
     * all elements of a {@link ScopedName}; it can be only the tip (for example {@code "myName"} instead
     * of {@code "myScope:myName"}) provided that ignoring the name head does not create ambiguity.
     *
     * @param  types         the collection where to search for an element of the given name.
     * @param  name          name of the element to search.
     * @param  nonAmbiguous  whether to throw an exception if the given name is ambiguous.
     * @return element of the given name, or {@code null} if none were found.
     * @throws IllegalArgumentException if the given name is ambiguous.
     */
    final <E extends TypeBuilder> E forName(final List<E> types, final String name, final boolean nonAmbiguous) {
        E best      = null;                     // Best type found so far.
        E ambiguity = null;                     // If two types are found at the same depth, the other type.
        int depth   = Integer.MAX_VALUE;        // Number of path elements that we had to ignore in the GenericName.
        for (final E type : types) {
            GenericName candidate = type.getName();
            for (int d=0; candidate != null; d++) {
                if (name.equals(candidate.toString())) {
                    if (d < depth) {
                        best      = type;
                        ambiguity = null;
                        depth     = d;
                        break;
                    }
                    if (d == depth) {
                        ambiguity = type;
                        break;
                    }
                }
                if (!(candidate instanceof ScopedName)) break;
                candidate = ((ScopedName) candidate).tail();
            }
        }
        if (ambiguity != null && nonAmbiguous) {
            throw new PropertyNotFoundException(errors().getString(
                    Errors.Keys.AmbiguousName_3, best.getName(), ambiguity.getName(), name));
        }
        return best;
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
        return Errors.forProperties(identification);
    }

    /**
     * Returns the {@code org.apache.sis.feature} specific resources for error messages.
     */
    final Resources resources() {
        return Resources.forProperties(identification);
    }

    /**
     * Same as {@link org.apache.sis.util.ArgumentChecks#ensureNonNull(String, Object)},
     * but uses the current locale in case of error.
     *
     * @param  name   the name of the argument to be checked. Used only if an exception is thrown.
     * @param  value  the user argument to check against null value.
     * @throws NullPointerException if {@code object} is null.
     */
    final void ensureNonNull(final String name, final Object value) {
        if (value == null) {
            throw new NullPointerException(errors().getString(Errors.Keys.NullArgument_1, name));
        }
    }

    /**
     * Ensures that this instance is still alive.
     *
     * @param owner  the owner of this instance. A value of null means that this instance should not be used any more.
     */
    final void ensureAlive(final TypeBuilder owner) {
        if (owner == null) {
            throw new IllegalStateException(errors().getString(Errors.Keys.DisposedInstanceOf_1, getClass()));
        }
    }

    /**
     * Same as {@link org.apache.sis.util.ArgumentChecks#ensureNonEmpty(String, CharSequence)},
     * but uses the current locale in case of error.
     *
     * @param  name  the name of the argument to be checked. Used only if an exception is thrown.
     * @param  text  the user argument to check against null value and empty sequences.
     * @throws NullPointerException if {@code text} is null.
     * @throws IllegalArgumentException if {@code text} is empty.
     */
    final void ensureNonEmpty(final String name, final CharSequence text) {
        if (text == null) {
            throw new NullPointerException(errors().getString(Errors.Keys.NullArgument_1, name));
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
    @Override
    public String toString() {
        return appendStringTo(new StringBuilder(Classes.getShortClassName(this))).toString();
    }

    /**
     * Partial implementation of {@link #toString()}. This method assumes that the class name
     * has already be written in the buffer.
     */
    final StringBuilder appendStringTo(final StringBuilder buffer) {
        toStringInternal(buffer.append("[“").append(getDisplayName()).append('”'));
        return buffer.append(']');
    }

    /**
     * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
     */
    void toStringInternal(StringBuilder buffer) {
    }

    /**
     * Invoked when a type builder has been removed from its parent.
     * Subclasses should override this method in a way that flag the builder as not usable anymore.
     */
    void remove() {
    }

    /**
     * Builds the feature or property type from the information specified to this builder.
     * If a type has already been built and this builder state has not changed since the type creation,
     * then the previously created {@code IdentifiedType} instance is returned.
     *
     * @return the feature or property type.
     * @throws IllegalStateException if the builder contains inconsistent information.
     */
    public abstract IdentifiedType build() throws IllegalStateException;
}
