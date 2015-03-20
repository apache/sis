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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.system.DefaultFactories.NAMES;

// Branch-dependent imports
import java.util.Objects;


/**
 * Base class of builders for various kind of {@link IdentifiedObject}. {@code Builder}s aim to make object creation
 * easier — they do not add any new functionality compared to {@link org.opengis.referencing.ObjectFactory}.
 * Builder methods like {@link #addName(CharSequence)} and {@link #addIdentifier(String)} provide convenient ways
 * to fill the {@link #properties} map, which will be given to the {@code ObjectFactory} methods when any
 * {@code createXXX(…)} method is invoked.
 *
 * <p>This base class provides methods for defining the {@link IdentifiedObject} properties shown below:</p>
 *
 * <ul class="verbose">
 *   <li><b>{@linkplain AbstractIdentifiedObject#getName() Name}:</b>
 *       each {@code IdentifiedObject} shall have a name, which can be specified by a call to any of the
 *       {@link #addName(CharSequence) addName(…)} methods defined in this class.</li>
 *
 *   <li><b>{@linkplain AbstractIdentifiedObject#getAlias() Aliases}:</b>
 *       identified objects can optionally have an arbitrary amount of aliases, which are also specified
 *       by the {@code addName(…)} methods. Each call after the first one adds an alias.</li>
 *
 *   <li><b>{@linkplain AbstractIdentifiedObject#getIdentifiers() Identifiers}:</b>
 *       identified objects can also have an arbitrary amount of identifiers, which are specified by any
 *       of the {@link #addIdentifier(String) addIdentifier(…)} methods. Like names, more than one identifier
 *       can be added by invoking the method many time.</li>
 *
 *   <li><b>{@linkplain AbstractIdentifiedObject#getRemarks() Remarks}:</b>
 *       identified objects can have at most one remark, which is specified by the {@code setRemarks(…)}
 *       method.</li>
 * </ul>
 *
 * The names and identifiers cited in the above table can be built from {@link CharSequence} given to the
 * {@code addName(…)} or {@code addIdentifier(…)} methods combined with the following properties:
 *
 * <ul class="verbose">
 *   <li><b>{@linkplain ImmutableIdentifier#getCodeSpace() Code space}:</b>
 *       each {@code Identifier} name or code can be local to a code space defined by an authority.
 *       Both the authority and code space can be specified by the {@link #setCodeSpace(Citation, String)} method,
 *       and usually (but not necessarily) apply to all {@code Identifier} instances.</li>
 *
 *   <li><b>{@linkplain ImmutableIdentifier#getVersion() Version}:</b>
 *       identifiers can optionally have a version specified by the {@link #setVersion(String)} method.
 *       The version usually (but not necessarily) applies to all {@code Identifier} instances.</li>
 *
 *   <li><b>{@linkplain ImmutableIdentifier#getDescription() Description}:</b>
 *       identifiers can optionally have a description specified by the {@link #setDescription(CharSequence)} method.
 *       The description applies only to the next identifier to create.</li>
 * </ul>
 *
 * {@section Namespaces and scopes}
 * The {@code addName(…)} and {@code addIdentifier(…)} methods come in three flavors:
 *
 * <ul class="verbose">
 *   <li>The {@link #addIdentifier(String)} and {@link #addName(CharSequence)} methods combine the given argument
 *       with the above-cited authority, code space, version and description information.
 *       The result is a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} or identifier,
 *       in which the code space information is stored but not shown by the {@code toString()} method.</li>
 *
 *   <li>The {@link #addIdentifier(Citation, String)} and {@link #addName(Citation, CharSequence)} methods use the given
 *       {@link Citation} argument, ignoring any authority or code space information given to this {@code Builder}.
 *       The result is a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} or identifier,
 *       in which the code space information is shown by the {@code toString()} method.</li>
 *
 *   <li>The {@link #addIdentifier(Identifier)}, {@link #addName(Identifier)} and {@link #addName(GenericName)}
 *       methods take the given object <cite>as-is</cite>. Any authority, code space, version or description
 *       information given to the {@code Builder} are ignored.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b>
 * The EPSG database defines a projection named "<cite>Mercator (variant A)</cite>" (EPSG:9804).
 * This projection was named "<cite>Mercator (1SP)</cite>" in older EPSG database versions.
 * The same projection was also named "{@code Mercator_1SP}" by OGC some specifications.
 * If we choose EPSG as our primary naming authority, then those three names can be declared as below:
 *
 * {@preformat java
 *   builder.setCodespace (Citations.OGP, "EPSG")
 *          .addName("Mercator (variant A)")
 *          .addName("Mercator (1SP)")
 *          .addName(Citations.OGC, "Mercator_1SP")
 * }
 *
 * The {@code toString()} representation of those three names are {@code "Mercator (variant A)"},
 * {@code "Mercator (1SP)"} (note the absence of {@code "EPSG:"} prefix, which is stored as the
 * name {@linkplain org.apache.sis.util.iso.DefaultLocalName#scope() scope} but not shown) and
 * <code>"<b>OGC:</b>Mercator_1SP"</code> respectively.</div>
 *
 *
 * {@section Builder property lifetimes}
 * Some complex objects require the creation of many components. For example constructing a
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} (CRS) may require constructing a
 * {@linkplain org.apache.sis.referencing.cs.AbstractCS coordinate system}, a
 * {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum} and an
 * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoid} among other components.
 * However all those components often (but not necessarily) share the same authority, code space and version information.
 * In order to simplify that common usage, two groups of properties have different lifetimes in the {@code Builder} class:
 *
 * <ul>
 *   <li>
 *     {@linkplain NamedIdentifier#getAuthority() Authority},
 *     {@linkplain NamedIdentifier#getCodeSpace() code space} and
 *     {@linkplain NamedIdentifier#getVersion()   version}:<br>
 *     Kept until they are specified again, because those properties are typically shared by all components.
 *   </li><li>
 *     {@linkplain AbstractIdentifiedObject#getName()        Name},
 *     {@linkplain AbstractIdentifiedObject#getAlias()       aliases},
 *     {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers},
 *     {@linkplain ImmutableIdentifier#getDescription()      description} and
 *     {@linkplain AbstractIdentifiedObject#getRemarks()     remarks}:<br>
 *     Cleared after each call to a {@code createXXX(…)} method, because those properties are usually specific
 *     to a particular {@code IdentifiedObject} or {@code Identifier} instance.
 *   </li>
 * </ul>
 *
 * {@section Usage examples}
 * See {@link org.apache.sis.parameter.ParameterBuilder} class javadoc for more examples with the
 * <cite>Mercator</cite> projection parameters.
 *
 * {@section Note for subclass implementors}
 * <ul>
 *   <li>The type {@code <B>} shall be exactly the subclass type.
 *       For performance reasons, this is verified only if Java assertions are enabled.</li>
 *   <li>All {@code createXXX(…)} methods shall invoke {@link #onCreate(boolean)} before and after
 *       usage of {@link #properties} map by the factory.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b>
 * {@preformat java
 *     public class MyBuilder extends Builder<MyBuilder> {
 *         public Foo createFoo() {
 *             onCreate(false);
 *             Foo foo = factory.createFoo(properties);
 *             onCreate(true);
 *             return foo;
 *         }
 *     }
 * }
 * </div>
 *
 * @param <B> The builder subclass.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public abstract class Builder<B extends Builder<B>> {
    /**
     * The properties to be given to {@link org.opengis.referencing.ObjectFactory} methods.
     * This map may contain values for the
     * {@value org.opengis.referencing.IdentifiedObject#NAME_KEY},
     * {@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY},
     * {@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY} and
     * {@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY} keys.
     * Subclasses may add other entries like
     * {@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY} and
     * {@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY} keys.
     *
     * <p>See <cite>Notes for subclass implementors</cite> in class javadoc for usage conditions.</p>
     *
     * @see #onCreate(boolean)
     */
    protected final Map<String,Object> properties;

    /**
     * A temporary list for aliases, before to assign them to the {@link #properties}.
     */
    private final List<GenericName> aliases;

    /**
     * A temporary list for identifiers, before to assign them to the {@link #properties}.
     */
    private final List<Identifier> identifiers;

    /**
     * The codespace as a {@code NameSpace} object, or {@code null} if not yet created.
     *
     * @see #namespace()
     */
    private NameSpace namespace;

    /**
     * Creates a new builder.
     */
    protected Builder() {
        assert verifyParameterizedType(getClass());
        properties  = new HashMap<>(8);
        aliases     = new ArrayList<>();  // Will often stay empty (default constructor handles those cases well).
        identifiers = new ArrayList<>();
    }

    /**
     * Verifies that {@code B} in {@code <B extends Builder<B>} is the expected class.
     * This method is for assertion purposes only.
     */
    private static boolean verifyParameterizedType(final Class<?> expected) {
        for (Class<?> c = expected; c != null; c = c.getSuperclass()) {
            Type type = c.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                final ParameterizedType p = (ParameterizedType) type;
                if (p.getRawType() == Builder.class) {
                    type = p.getActualTypeArguments()[0];
                    if (type == expected) return true;
                    throw new AssertionError(type);
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code this} casted to {@code <B>}. The cast is valid if the assertion performed
     * at construction time passes. Since the {@code <B>} type is hard-coded in the source code,
     * if the JUnit test passes then the cast should always be valid for all instances of the
     * same builder class.
     */
    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    /**
     * Sets the property value for the given key, if a change is still possible. The check for change permission
     * is needed for all keys defined in the {@link Identifier} interface. This check is not needed for other keys,
     * so callers do not need to invoke this method for other keys.
     *
     * @param  key The key of the property to set.
     * @param  value The value to set.
     * @return {@code true} if the property changed as a result of this method call.
     * @throws IllegalStateException if a new value is specified in a phase where the value can not be changed.
     */
    private boolean setProperty(final String key, final Object value) throws IllegalStateException {
        if (Objects.equals(properties.get(key), value)) {
            return false;
        }
        if (properties.get(IdentifiedObject.NAME_KEY) != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, key));
        }
        properties.put(key, value);
        return true;
    }

    /**
     * Returns the namespace, creating it when first needed.
     */
    private NameSpace namespace() {
        if (namespace == null) {
            final String codespace = getCodeSpace();
            if (codespace != null) {
                namespace = NAMES.createNameSpace(NAMES.createLocalName(null, codespace), null);
            }
        }
        return namespace;
    }

    /**
     * Returns the value of the first argument given by the last call to {@link #setCodeSpace(Citation, String)},
     * or {@code null} if none. The default value is {@code null}.
     *
     * @return The citation specified by the last call to {@code setCodeSpace(…)}, or {@code null} if none.
     *
     * @since 0.6
     */
    private Citation getAuthority() {
        return (Citation) properties.get(Identifier.AUTHORITY_KEY);
    }

    /**
     * Returns the value of the last argument given by the last call to {@link #setCodeSpace(Citation, String)},
     * or {@code null} if none. The default value is {@code null}.
     *
     * @return The string specified by the last call to {@code setCodeSpace(…)}, or {@code null} if none.
     *
     * @since 0.6
     */
    private String getCodeSpace() {
        return (String) properties.get(Identifier.CODESPACE_KEY);
    }

    /**
     * Sets the {@code Identifier} authority and code space.
     * The code space is often the authority's abbreviation, but not necessarily.
     *
     * <div class="note"><b>Example:</b> Coordinate Reference System (CRS) objects identified by codes from the
     * EPSG database are maintained by the {@linkplain org.apache.sis.metadata.iso.citation.Citations#OGP OGP}
     * authority, but the code space is {@code "EPSG"} for historical reasons.</div>
     *
     * This method is typically invoked only once, since a compound object often uses the same code space
     * for all individual components.
     *
     * <p><b>Condition:</b>
     * this method can not be invoked after one or more names or identifiers have been added (by calls to the
     * {@code addName(…)} or {@code addIdentifier(…)} methods) for the next object to create. This method can be
     * invoked again after the name, aliases and identifiers have been cleared by a call to {@code createXXX(…)}.</p>
     *
     * <p><b>Lifetime:</b>
     * this property is kept unchanged until this {@code setCodeSpace(…)} method is invoked again.</p>
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  codespace The {@code IdentifiedObject} codespace, or {@code null} for inferring it from the authority.
     * @return {@code this}, for method call chaining.
     * @throws IllegalStateException if {@code addName(…)} or {@code addIdentifier(…)} has been invoked at least
     *         once since builder construction or since the last call to a {@code createXXX(…)} method.
     *
     * @see ImmutableIdentifier#getAuthority()
     * @see ImmutableIdentifier#getCodeSpace()
     */
    public B setCodeSpace(final Citation authority, final String codespace) {
        if (!setProperty(Identifier.CODESPACE_KEY, codespace)) {
            namespace = null;
        }
        setProperty(Identifier.AUTHORITY_KEY, authority);
        return self();
    }

    /**
     * Returns the value given by the last call to {@link #setVersion(String)}, or {@code null} if none.
     * The default value is {@code null}.
     *
     * @return The value specified by the last call to {@code setVersion(…)}, or {@code null} if none.
     *
     * @since 0.6
     */
    private String getVersion() {
        return (String) properties.get(Identifier.VERSION_KEY);
    }

    /**
     * Sets the {@code Identifier} version of object definitions. This method is typically invoked only once,
     * since a compound object often uses the same version for all individual components.
     *
     * <p><b>Condition:</b>
     * this method can not be invoked after one or more names or identifiers have been added (by calls to the
     * {@code addName(…)} or {@code addIdentifier(…)} methods) for the next object to create. This method can be
     * invoked again after the name, aliases and identifiers have been cleared by a call to {@code createXXX(…)}.</p>
     *
     * <p><b>Lifetime:</b>
     * this property is kept unchanged until this {@code setVersion(…)} method is invoked again.</p>
     *
     * @param  version The version of code definitions, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     * @throws IllegalStateException if {@code addName(…)} or {@code addIdentifier(…)} has been invoked at least
     *         once since builder construction or since the last call to a {@code createXXX(…)} method.
     */
    public B setVersion(final String version) {
        setProperty(Identifier.VERSION_KEY, version);
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name given by a {@code String} or {@code InternationalString}.
     * The given string will be combined with the authority, {@link #setCodeSpace(Citation, String) code space}
     * and {@link #setVersion(String) version} information for creating the {@link Identifier} or {@link GenericName}
     * object.
     *
     * {@section Name and aliases}
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name}, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <p><b>Lifetime:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The {@code IdentifiedObject} name.
     * @return {@code this}, for method call chaining.
     */
    public B addName(final CharSequence name) {
        ensureNonNull("name", name);
        if (properties.putIfAbsent(IdentifiedObject.NAME_KEY, name.toString()) != null) {
            // A primary name is already present. Add the given name as an alias instead.
            aliases.add(name instanceof GenericName ? (GenericName) name : NAMES.createLocalName(namespace(), name));
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name in an alternative namespace. This method is typically invoked for
     * {@linkplain AbstractIdentifiedObject#getAlias() aliases} defined after the primary name.
     *
     * <div class="note"><b>Example:</b>
     * The "<cite>Longitude of natural origin</cite>" parameter defined by EPSG is named differently
     * by OGC and GeoTIFF. Those alternative names can be defined as below:
     *
     * {@preformat java
     *   builder.setCodespace(Citations.OGP, "EPSG")           // Sets the default namespace to "EPSG".
     *          .addName("Longitude of natural origin")        // Primary name in builder default namespace.
     *          .addName(Citations.OGC, "central_meridian")    // First alias in "OGC" namespace.
     *          .addName(Citations.GEOTIFF, "NatOriginLong");  // Second alias in "GeoTIFF" namespace.
     * }
     *
     * In this example, {@code "central_meridian"} will be the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#tip() tip} and {@code "OGC"} will be the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#head() head} of the first alias.</div>
     *
     * <p><b>Lifetime:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  name The {@code IdentifiedObject} alias as a name in the namespace of the given authority.
     * @return {@code this}, for method call chaining.
     *
     * @see #addIdentifier(Citation, String)
     */
    public B addName(final Citation authority, final CharSequence name) {
        ensureNonNull("name", name);
        final NamedIdentifier identifier = new NamedIdentifier(authority, name);
        if (properties.putIfAbsent(IdentifiedObject.NAME_KEY, identifier) != null) {
            // A primary name is already present. Add the given name as an alias instead.
            aliases.add(identifier);
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name fully specified by the given identifier.
     * This method ignores the authority, {@link #setCodeSpace(Citation, String) code space} or
     * {@link #setVersion(String) version} specified to this builder (if any), since the given
     * identifier already contains those information.
     *
     * {@section Name and aliases}
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name} to the given value, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <p><b>Lifetime:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The {@code IdentifiedObject} name as an identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addName(final Identifier name) {
        ensureNonNull("name", name);
        if (properties.putIfAbsent(IdentifiedObject.NAME_KEY, name) != null) {
            // A primary name is already present. Add the given name as an alias instead.
            aliases.add(name instanceof GenericName ? (GenericName) name : new NamedIdentifier(name));
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name fully specified by the given generic name.
     * This method ignores the authority, {@link #setCodeSpace(Citation, String) code space} or
     * {@link #setVersion(String) version} specified to this builder (if any), since the given
     * generic name already contains those information.
     *
     * {@section Name and aliases}
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name} to the given value, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <p><b>Lifetime:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The {@code IdentifiedObject} name as an identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addName(final GenericName name) {
        ensureNonNull("name", name);
        if (properties.get(IdentifiedObject.NAME_KEY) == null) {
            properties.put(IdentifiedObject.NAME_KEY, (name instanceof Identifier) ? name : new NamedIdentifier(name));
        } else {
            aliases.add(name);
        }
        return self();
    }

    /**
     * Adds a deprecated name given by a {@code CharSequence}. Some objects have deprecated names for historical reasons.
     * The deprecated name typically has a replacement, which can be given by the {@code supersededBy} argument.
     * The later, if non-null, shall be a name specified by a previous call to an {@code addName(…)} method.
     *
     * <p>The given string will be combined with the authority, {@link #setCodeSpace(Citation, String) code space} and
     * {@link #setVersion(String) version} information for creating the deprecated {@link NamedIdentifier} object.</p>
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The {@code IdentifiedObject} deprecated name.
     * @param  supersededBy The name to use instead of this one, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     *
     * @since 0.6
     */
    public B addDeprecatedName(final CharSequence name, final CharSequence supersededBy) {
        ensureNonNull("name", name);
        final DeprecatedName dn = new DeprecatedName(getAuthority(), getCodeSpace(), name, getVersion(), supersededBy);
        if (properties.putIfAbsent(IdentifiedObject.NAME_KEY, dn) != null) {
            aliases.add(dn);
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} identifier given by a {@code String}.
     * The given string will be combined with the authority, {@link #setCodeSpace(Citation, String) code space}
     * and {@link #setVersion(String) version} information for creating the {@link Identifier} object.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The {@code IdentifiedObject} identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addIdentifier(final String identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(new ImmutableIdentifier(getAuthority(), getCodeSpace(), identifier, getVersion(), null));
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} identifier in an alternative namespace.
     * This method is typically invoked in complement to {@link #addName(Citation, CharSequence)}.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  identifier The {@code IdentifiedObject} identifier as a code in the namespace of the given authority.
     * @return {@code this}, for method call chaining.
     *
     * @see #addName(Citation, CharSequence)
     */
    public B addIdentifier(final Citation authority, final String identifier) {
        ensureNonNull("identifier", identifier);
        // Do not use the version information since it applies to the default authority rather than the given one.
        identifiers.add(new ImmutableIdentifier(authority, Citations.getUnicodeIdentifier(authority), identifier));
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} identifier fully specified by the given identifier.
     * This method ignores the authority, {@link #setCodeSpace(Citation, String) code space} or
     * {@link #setVersion(String) version} specified to this builder (if any), since the given
     * identifier already contains those information.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The {@code IdentifiedObject} identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addIdentifier(final Identifier identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(identifier);
        return self();
    }

    /**
     * Adds a deprecated identifier given by a {@code String}. Some objects have deprecated identifiers for
     * historical reasons. The deprecated identifier typically has a replacement, which can be given by the
     * {@code supersededBy} argument. The later, if non-null, shall be an identifier specified by a previous
     * call to an {@code addIdentifier(…)} method.
     *
     * <p>The given string will be combined with the authority, {@link #setCodeSpace(Citation, String) code space}
     * and {@link #setVersion(String) version} information for creating the deprecated {@link Identifier} object.</p>
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier   The {@code IdentifiedObject} deprecated identifier.
     * @param  supersededBy The identifier to use instead of this one, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     *
     * @since 0.6
     */
    public B addDeprecatedIdentifier(final String identifier, final String supersededBy) {
        ensureNonNull("identifier", identifier);
        identifiers.add(new DeprecatedCode(getAuthority(), getCodeSpace(), identifier, getVersion(), supersededBy));
        return self();
    }

    /**
     * Sets the parameter description as a {@code String} or {@code InternationalString} instance.
     * Calls to this method overwrite any previous value.
     *
     * <p><b>Lifetime:</b>
     * previous descriptions are discarded by calls to {@code setDescription(…)}.
     * Descriptions are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  description The description, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     */
    public B setDescription(final CharSequence description) {
        properties.put(Identifier.DESCRIPTION_KEY, description);
        return self();
    }

    /**
     * Sets remarks as a {@code String} or {@code InternationalString} instance.
     * Calls to this method overwrite any previous value.
     *
     * <p><b>Lifetime:</b>
     * previous remarks are discarded by calls to {@code setRemarks(…)}.
     * Remarks are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  remarks The remarks, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     */
    public B setRemarks(final CharSequence remarks) {
        properties.put(IdentifiedObject.REMARKS_KEY, remarks);
        return self();
    }

    /**
     * Initializes/cleanups the {@link #properties} map before/after a {@code createXXX(…)} execution.
     * Subclasses shall invoke this method in their {@code createXXX(…)} methods as below:
     *
     * {@preformat java
     *     public Foo createFoo() {
     *         final Foo foo;
     *         onCreate(false);
     *         try {
     *             foo = factory.createFoo(properties);
     *         } finally {
     *             onCreate(true);
     *         }
     *         return foo;
     *     }
     * }
     *
     * If {@code cleanup} is {@code true}, then this method clears the identification information
     * (name, aliases, identifiers and remarks) for preparing the builder to the construction of
     * an other object. The authority, codespace and version properties are not cleared by this method.
     *
     * @param cleanup {@code false} when this method is invoked before object creation, and
     *                {@code true} when this method is invoked after object creation.
     *
     * @see #properties
     */
    protected void onCreate(final boolean cleanup) {
        final GenericName[] valueAlias;
        final Identifier[]  valueIds;
        if (cleanup) {
            properties .put(IdentifiedObject.NAME_KEY, null);
            properties .remove(IdentifiedObject.REMARKS_KEY);
            aliases    .clear();
            identifiers.clear();
            valueAlias = null;
            valueIds   = null;
        } else {
            valueAlias = aliases    .toArray(new GenericName[aliases    .size()]);
            valueIds   = identifiers.toArray(new Identifier [identifiers.size()]);
        }
        properties.put(IdentifiedObject.ALIAS_KEY,       valueAlias);
        properties.put(IdentifiedObject.IDENTIFIERS_KEY, valueIds);
    }
}
