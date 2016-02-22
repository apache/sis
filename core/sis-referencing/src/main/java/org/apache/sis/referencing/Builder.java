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
import java.util.Arrays;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.referencing.DeprecatedCode;
import org.apache.sis.internal.referencing.DeprecatedName;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Base class of builders for various kind of {@link IdentifiedObject}. This class provides convenience methods
 * for filling the {@link #properties} map to be given to an {@link org.opengis.referencing.ObjectFactory}.
 * The main properties are:
 *
 * <ul class="verbose">
 *   <li><b>{@linkplain AbstractIdentifiedObject#getName() Name}:</b><br>
 *       each {@code IdentifiedObject} shall have a name, which can be specified by a call to any of the
 *       {@link #addName(CharSequence) addName(…)} methods defined in this class.</li>
 *
 *   <li><b>{@linkplain AbstractIdentifiedObject#getAlias() Aliases}:</b><br>
 *       {@code IdentifiedObject}s can optionally have an arbitrary amount of aliases, which are also specified
 *       by the {@code addName(…)} methods. Each call after the first one adds an alias.</li>
 *
 *   <li><b>{@linkplain AbstractIdentifiedObject#getIdentifiers() Identifiers}:</b><br>
 *       {@code IdentifiedObject}s can also have an arbitrary amount of identifiers, which are specified by any
 *       of the {@link #addIdentifier(String) addIdentifier(…)} methods. Like names, more than one identifier
 *       can be added by invoking the method many time.</li>
 *
 *   <li><b>{@linkplain ImmutableIdentifier#getCodeSpace() Code space}:</b><br>
 *       {@code IdentifiedObject} names and identifiers can be local to a code space defined by an authority.
 *       Both the authority and code space can be specified by the {@link #setCodeSpace(Citation, String)} method,
 *       and usually (but not necessarily) apply to all {@code Identifier} instances.</li>
 *
 *   <li><b>{@linkplain ImmutableIdentifier#getVersion() Version}:</b><br>
 *       {@code Identifier}s can optionally have a version specified by the {@link #setVersion(String)} method.
 *       The version usually (but not necessarily) applies to all {@code Identifier} instances.</li>
 *
 *   <li><b>{@linkplain ImmutableIdentifier#getDescription() Description}:</b><br>
 *       {@code Identifier}s can optionally have a description specified by the {@link #setDescription(CharSequence)} method.
 *       The description applies only to the next identifier to create.</li>
 *
 *   <li><b>{@linkplain AbstractIdentifiedObject#getRemarks() Remarks}:</b><br>
 *       {@code IdentifiedObject}s can have at most one remark, which is specified by the
 *       {@link #setRemarks(CharSequence) code setRemarks(…)} method.</li>
 * </ul>
 *
 * <div class="section">Namespaces and scopes</div>
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
 * The EPSG database defines a projection named <cite>"Mercator (variant A)"</cite> (EPSG:9804).
 * This projection was named <cite>"Mercator (1SP)"</cite> in older EPSG database versions.
 * The same projection was also named "{@code Mercator_1SP}" by OGC some specifications.
 * If we choose EPSG as our primary naming authority, then those three names can be declared as below:
 *
 * {@preformat java
 *   builder.setCodespace (Citations.EPSG, "EPSG")
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
 * <div class="section">Builder property lifetimes</div>
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
 * <div class="section">Usage examples</div>
 * See {@link org.apache.sis.parameter.ParameterBuilder} class javadoc for more examples with the
 * <cite>Mercator</cite> projection parameters.
 *
 * <div class="section">Note for subclass implementors</div>
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
    private final List<ReferenceIdentifier> identifiers;

    /**
     * The codespace as a {@code NameSpace} object, or {@code null} if not yet created.
     * This object is built from the {@value org.opengis.metadata.Identifier#CODESPACE_KEY} value when first needed.
     */
    private transient NameSpace namespace;

    /**
     * The name factory, fetched when first needed.
     *
     * @see #factory()
     */
    private transient NameFactory nameFactory;

    /**
     * Creates a new builder.
     */
    protected Builder() {
        assert verifyParameterizedType(getClass());
        properties  = new HashMap<String,Object>(8);
        aliases     = new ArrayList<GenericName>();  // Will often stay empty (default constructor handles those cases well).
        identifiers = new ArrayList<ReferenceIdentifier>();
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
     * Creates a new builder initialized to properties of the given object.
     * The properties recognized by this constructor are documented
     * {@linkplain IdentifiedObjects#getProperties(IdentifiedObject, String...) here}.
     *
     * @param object The identified object from which to inherit properties, or {@code null}.
     *
     * @since 0.6
     */
    protected Builder(final IdentifiedObject object) {
        this();
        if (object != null) {
            properties.putAll(IdentifiedObjects.getProperties(object));
            final GenericName[] valueAlias = (GenericName[]) properties.remove(IdentifiedObject.ALIAS_KEY);
            final ReferenceIdentifier[] valueIds = (ReferenceIdentifier[])  properties.remove(IdentifiedObject.IDENTIFIERS_KEY);
            if (valueAlias != null) aliases.addAll(Arrays.asList(valueAlias));
            if (valueIds != null) identifiers.addAll(Arrays.asList(valueIds));
        }
    }

    /**
     * Returns the name factory to use for creating namespaces and local names.
     * The factory will be fetched when first needed, and while not change anymore
     * for the rest of this {@code Builder} lifetime.
     */
    private NameFactory factory() {
        if (nameFactory == null) {
            nameFactory = DefaultFactories.forBuildin(NameFactory.class);
        }
        return nameFactory;
    }

    /**
     * Creates or returns an existing name for the given string in the current namespace.
     * The namespace may be cleared at anytime by a call to {@link #setCodeSpace(Citation, String)}.
     */
    private GenericName createName(final CharSequence name) {
        final NameFactory factory = factory();
        if (namespace == null) {
            final String codespace = getCodeSpace();
            if (codespace != null) {
                namespace = factory.createNameSpace(factory.createLocalName(null, codespace), null);
            }
        }
        return factory.createLocalName(namespace, name);
    }

    /**
     * Creates or returns an existing name for the given string in the given namespace.
     */
    private GenericName createName(final Citation authority, final CharSequence name) {
        if (authority == getAuthority()) {
            return createName(name);
        } else {
            return new NamedIdentifier(authority, name);
        }
    }

    /**
     * Converts the given name into an identifier. Note that {@link NamedIdentifier}
     * implements both {@link GenericName} and {@link Identifier} interfaces.
     */
    private static ReferenceIdentifier toIdentifier(final GenericName name) {
        return (name instanceof ReferenceIdentifier) ? (ReferenceIdentifier) name : new NamedIdentifier(name);
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
        final Object previous = JDK8.putIfAbsent(properties, key, value);
        if (previous != null) {
            if (previous.equals(value)) {
                return false;
            }
            if (properties.get(IdentifiedObject.NAME_KEY) != null) {
                throw new IllegalStateException(Errors.getResources(properties)
                        .getString(Errors.Keys.ValueAlreadyDefined_1, key));
            }
            properties.put(key, value);
        }
        return true;
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
        return (String) properties.get(ReferenceIdentifier.CODESPACE_KEY);
    }

    /**
     * Sets the {@code Identifier} authority and code space.
     * The code space is often the authority's abbreviation, but not necessarily.
     *
     * <div class="note"><b>Example:</b> Coordinate Reference System (CRS) objects identified by codes from the
     * EPSG database are maintained by the <cite>International Association of Oil &amp; Gas producers</cite> (IOGP)
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
        if (!setProperty(ReferenceIdentifier.CODESPACE_KEY, codespace)) {
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
        return (String) properties.get(ReferenceIdentifier.VERSION_KEY);
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
        setProperty(ReferenceIdentifier.VERSION_KEY, version);
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name given by a {@code String} or {@code InternationalString}.
     * The given string will be combined with the authority, {@linkplain #setCodeSpace(Citation, String)
     * code space} and {@linkplain #setVersion(String) version} information for creating the
     * {@link Identifier} or {@link GenericName} object.
     *
     * <div class="section">Name and aliases</div>
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name}, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <div class="section">Deprecated names</div>
     * Some names may exist for historical reasons but have their use discouraged.
     * If <code>{@linkplain #setDeprecated(boolean) setDeprecated}(true)</code> has been invoked, then this
     * method creates a deprecated alias with the current {@linkplain #setRemarks(CharSequence) remarks}.
     * The remark should suggest a replacement, for example with a sentence like
     * <cite>"Superseded by {@literal <new-name>}"</cite>.
     *
     * <p>Note that deprecated names are always added as aliases, never as the primary name of an identified object.</p>
     *
     * <p><b>Lifetime:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The {@code IdentifiedObject} name as a {@link String} or {@link InternationalString} instance.
     * @return {@code this}, for method call chaining.
     */
    public B addName(final CharSequence name) {
        ensureNonNull("name", name);
        if (isDeprecated()) {
            aliases.add(new DeprecatedName(getAuthority(), getCodeSpace(), name, getVersion(), getRemarks()));
        } else if (JDK8.putIfAbsent(properties, IdentifiedObject.NAME_KEY, name.toString()) != null) {
            // A primary name is already present. Add the given name as an alias instead.
            aliases.add(createName(name));
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name in an alternative namespace. This method is typically invoked for
     * {@linkplain AbstractIdentifiedObject#getAlias() aliases} defined after the primary name.
     *
     * <div class="note"><b>Example:</b>
     * The <cite>"Longitude of natural origin"</cite> parameter defined by EPSG is named differently
     * by OGC and GeoTIFF. Those alternative names can be defined as below:
     *
     * {@preformat java
     *   builder.setCodespace(Citations.EPSG, "EPSG")          // Sets the default namespace to "EPSG".
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
        final boolean isDeprecated = isDeprecated();
        if (!isDeprecated && properties.get(IdentifiedObject.NAME_KEY) != null) {
            // A primary name is already present. Add the given name as an alias instead.
            aliases.add(createName(authority, name));
        } else {
            final String codeSpace;
            final String version;
            if (authority == getAuthority()) {
                codeSpace  = getCodeSpace();
                version    = getVersion();
            } else {
                // Do not use the version information since it applies to the default authority rather than the given one.
                codeSpace = Citations.getCodeSpace(authority);
                version   = null;
            }
            if (isDeprecated) {
                aliases.add(new DeprecatedName(authority, codeSpace, name, version, getRemarks()));
            } else {
                properties.put(IdentifiedObject.NAME_KEY,
                        new NamedIdentifier(authority, codeSpace, name, version, getDescription()));
            }
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name fully specified by the given identifier.
     * This method ignores the authority, {@linkplain #setCodeSpace(Citation, String) code space},
     * {@linkplain #setVersion(String) version} and {@linkplain #setDescription(CharSequence) description}
     * specified to this builder (if any), since the given identifier may already contain those information.
     *
     * <div class="section">Name and aliases</div>
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
    public B addName(final ReferenceIdentifier name) {
        ensureNonNull("name", name);
        if (JDK8.putIfAbsent(properties, IdentifiedObject.NAME_KEY, name) != null) {
            // A primary name is already present. Add the given name as an alias instead.
            aliases.add(name instanceof GenericName ? (GenericName) name : new NamedIdentifier(name));
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} name fully specified by the given generic name.
     * This method ignores the authority, {@linkplain #setCodeSpace(Citation, String) code space},
     * {@linkplain #setVersion(String) version} and {@linkplain #setDescription(CharSequence) description}
     * specified to this builder (if any), since the given generic name may already contain those information.
     *
     * <div class="section">Name and aliases</div>
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
            properties.put(IdentifiedObject.NAME_KEY, toIdentifier(name));
        } else {
            aliases.add(name);
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} identifier given by a {@code String}.
     * The given string will be combined with the authority, {@linkplain #setCodeSpace(Citation, String) code space}
     * {@linkplain #setVersion(String) version} and {@linkplain #setDescription(CharSequence) description} information
     * for creating the {@link Identifier} object.
     *
     * <div class="section">Deprecated identifiers</div>
     * Some identifiers may exist for historical reasons but have their use discouraged.
     * If <code>{@linkplain #setDeprecated(boolean) setDeprecated}(true)</code> has been invoked, then this
     * method creates a deprecated identifier with the current {@linkplain #setRemarks(CharSequence) remarks}.
     * The remark should suggest a replacement, for example with a sentence like
     * <cite>"Superseded by {@literal <new-code>}"</cite>.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The {@code IdentifiedObject} identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addIdentifier(final String identifier) {
        ensureNonNull("identifier", identifier);
        addIdentifier(getAuthority(), getCodeSpace(), identifier, getVersion());
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} identifier in an alternative namespace.
     * This method is typically invoked in complement to {@link #addName(Citation, CharSequence)}.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  authority  Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  identifier The {@code IdentifiedObject} identifier as a code in the namespace of the given authority.
     * @return {@code this}, for method call chaining.
     *
     * @see #addName(Citation, CharSequence)
     */
    public B addIdentifier(final Citation authority, final String identifier) {
        ensureNonNull("identifier", identifier);
        final String codeSpace;
        final String version;
        if (authority == getAuthority()) {
            codeSpace  = getCodeSpace();
            version    = getVersion();
        } else {
            // Do not use the version information since it applies to the default authority rather than the given one.
            codeSpace = Citations.getCodeSpace(authority);
            version   = null;
        }
        addIdentifier(authority, codeSpace, identifier, version);
        return self();
    }

    /**
     * Implementation of {@link #addIdentifier(String)} and {@link #addIdentifier(Citation, String)}.
     */
    private void addIdentifier(final Citation authority, final String codeSpace, final String identifier, final String version) {
        final ReferenceIdentifier id;
        if (isDeprecated()) {
            id = new DeprecatedCode(authority, codeSpace, identifier, version, null, getRemarks());
        } else {
            id = new ImmutableIdentifier(authority, codeSpace, identifier, version, getDescription());
        }
        identifiers.add(id);
    }

    /**
     * Adds an {@code IdentifiedObject} identifier fully specified by the given identifier.
     * This method ignores the authority, {@linkplain #setCodeSpace(Citation, String) code space},
     * {@linkplain #setVersion(String) version} and {@linkplain #setDescription(CharSequence) description}
     * specified to this builder (if any), since the given identifier already contains those information.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The {@code IdentifiedObject} identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addIdentifier(final ReferenceIdentifier identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(identifier);
        return self();
    }


    /**
     * Returns {@code true} if the given name or identifier is deprecated.
     *
     * @see #isDeprecated()
     */
    private static boolean isDeprecated(final Object object) {
        return (object instanceof Deprecable) && ((Deprecable) object).isDeprecated();
    }

    /**
     * Adds all non-deprecated names and identifiers from the given object.
     * Other properties like description and remarks are ignored.
     *
     * <p>This is a convenience method for using an existing object as a template, before to modify
     * some names by calls to {@link #rename(Citation, CharSequence[])}.</p>
     *
     * @param  object The object from which to copy the references to names and identifiers.
     * @return {@code this}, for method call chaining.
     *
     * @since 0.6
     */
    public B addNamesAndIdentifiers(final IdentifiedObject object) {
        ensureNonNull("object", object);
        for (final ReferenceIdentifier id : object.getIdentifiers()) {
            if (!isDeprecated(id)) {
                addIdentifier(id);
            }
        }
        ReferenceIdentifier id = object.getName();
        if (!isDeprecated(id)) {
            addName(id);
        }
        for (final GenericName alias : object.getAlias()) {
            if (!isDeprecated(alias)) {
                addName(alias);
            }
        }
        return self();
    }

    /**
     * Replaces the names associated to the given authority by the given new names.
     * More specifically:
     *
     * <ul>
     *   <li>The first occurrence of a name associated to {@code authority} will be replaced by a new name
     *       with the same authority and the local part defined by {@code replacements[0]}.</li>
     *   <li>The second occurrence of a name associated to {@code authority} will be replaced by a new name
     *       with the same authority and the local part defined by {@code replacements[1]}.</li>
     *   <li><i>etc.</i> until one of the following conditions is meet:
     *     <ul>
     *       <li>There is no more name associated to the given authority in this {@code Builder}, in which case
     *           new names are inserted for all remaining elements in the {@code replacements} array.</li>
     *       <li>There is no more elements in the {@code replacements} array, in which case all remaining
     *           names associated to the given authority in this {@code Builder} are removed.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param  authority The authority of the names to replaces.
     * @param  replacements The new local parts for the names to replace,
     *         or {@code null} for removing all identifiers associated to the given authority.
     * @return {@code this}, for method call chaining.
     *
     * @since 0.6
     */
    public B rename(final Citation authority, final CharSequence... replacements) {
        ensureNonNull("authority", authority);
        final int length = (replacements != null) ? replacements.length : 0;
        /*
         * IdentifiedObjects store the "primary name" separately from aliases. Consequently we will start
         * the iteration at index -1 where i=-1 is used as a sentinel value meaning "primary name" before
         * to iterate over the aliases. Note that the type is not the same:
         *
         *   - Primary:   Identifier or String
         *   - Aliases:   Identifier or GenericName
         */
        int next = 0;
        int insertAt = aliases.size();
        for (int i = -1; i < aliases.size(); i++) {
            final Object old = (i < 0) ? properties.get(IdentifiedObject.NAME_KEY) : aliases.get(i);
            if (old == null) {
                continue;       // Actually only the primary name can be null.
            }
            final boolean wasID = (old instanceof Identifier);   // Usually true even for aliases.
            if (!authority.equals(wasID ? ((Identifier) old).getAuthority() : getAuthority())) {
                continue;       // Current name is not for the authority we are looking for.
            }
            /*
             * Found a name associated to the given authority. Process to the replacement if we still
             * have some elements to take in the 'replacements' array, otherwise remove the name.
             */
            if (next < length) {
                final CharSequence name;
                ensureNonNullElement("replacements", next, name = replacements[next++]);
                /*
                 * If the current name matches the specified replacement, we can leave the name as-is.
                 * Only if the name (in its local part) is not the same, proceed to the replacement.
                 */
                final String code = name.toString();
                if (!code.equals(wasID ? ((Identifier) old).getCode() : old.toString())) {
                    if (i < 0) {
                        properties.put(IdentifiedObject.NAME_KEY,
                                (authority != getAuthority()) ? new NamedIdentifier(authority, name) : code);
                    } else {
                        aliases.set(i, createName(authority, name));
                    }
                    insertAt = i + 1;
                }
            } else {
                if (i < 0) {
                    properties.remove(IdentifiedObject.NAME_KEY);
                } else {
                    aliases.remove(i--);
                }
            }
        }
        /*
         * If there is any remaining elements in the 'replacements' array, insert them right after the last
         * element of the given authority that we found (so we keep together the names of the same authority).
         */
        while (next < length) {
            final CharSequence name;
            ensureNonNullElement("replacements", next, name = replacements[next++]);
            aliases.add(insertAt++, createName(authority, name));
        }
        /*
         * If the primary name has been removed as a result of this method execution,
         * take the first alias as the new primary name.
         */
        if (properties.get(IdentifiedObject.NAME_KEY) == null && !aliases.isEmpty()) {
            properties.put(IdentifiedObject.NAME_KEY, toIdentifier(aliases.remove(0)));
        }
        return self();
    }

    /**
     * Returns the parameter description specified by the last call to {@link #setDescription(CharSequence)},
     * or {@code null} if none.
     */
    private InternationalString getDescription() {
        return (InternationalString) properties.get("description");
    }

    /**
     * Sets an {@code Identifier} or {@code IdentifiedObject} description.
     * Descriptions can be used in various contexts:
     *
     * <ul>
     *   <li>Before calls to {@link #addIdentifier(String)} or {@link #addIdentifier(Citation, String)}
     *       for specifying a natural language description of the meaning of the code value.
     *
     *       <div class="note"><b>Example:</b>
     *       {@code setDescription("World Geodetic System 1984").addIdentifier("4326")}</div></li>
     *
     *   <li>Before calls to a {@code createXXX(…)} method for providing a narrative explanation
     *       of the role of the object. Not all {@code IdentifiedObject} supports description.</li>
     * </ul>
     *
     * Calls to this method overwrite any previous value.
     *
     * <p><b>Lifetime:</b>
     * previous descriptions are discarded by calls to {@code setDescription(…)}.
     * Descriptions are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  description The description as a {@link String} or {@link InternationalString} instance, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     *
     * @see ImmutableIdentifier#getDescription()
     */
    public B setDescription(final CharSequence description) {
        /*
         * Convert to InternationalString now in order to share the same instance if
         * the same description is used both for an Identifier and an IdentifiedObject.
         */
        properties.put("description", Types.toInternationalString(description));
        return self();
    }

    /**
     * Returns the remarks specified by the last call to {@link #setRemarks(CharSequence)},
     * or {@code null} if none.
     */
    private InternationalString getRemarks() {
        return (InternationalString) properties.get(IdentifiedObject.REMARKS_KEY);
    }

    /**
     * Sets remarks as a {@code String} or {@code InternationalString} instance.
     * Calls to this method overwrite any previous value.
     *
     * <p><b>Lifetime:</b>
     * previous remarks are discarded by calls to {@code setRemarks(…)}.
     * Remarks are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  remarks The remarks as a {@link String} or {@link InternationalString} instance, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     */
    public B setRemarks(final CharSequence remarks) {
        /*
         * Convert to InternationalString now in order to share the same instance if
         * the same remarks is used both for an Identifier and an IdentifiedObject.
         */
        properties.put(IdentifiedObject.REMARKS_KEY, Types.toInternationalString(remarks));
        return self();
    }

    /**
     * Returns {@code true} if the deprecated flag is set to {@code true}.
     */
    private boolean isDeprecated() {
        return Boolean.TRUE.equals(properties.get(AbstractIdentifiedObject.DEPRECATED_KEY));
    }

    /**
     * Sets whether the next {@code Identifier} or {@code IdentifiedObject}s to create shall be considered deprecated.
     * Deprecated objects exist in some {@linkplain org.opengis.referencing.AuthorityFactory authority factories} like
     * the EPSG database.
     *
     * <p>Note that this method does not apply to name and identifiers, which have their own
     * {@code addDeprecatedFoo(…)} methods.</p>
     *
     * <p><b>Lifetime:</b>
     * Deprecation status is cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  deprecated {@code true} if the next names, identifiers and identified objects should be
     *         considered deprecated, or {@code false} otherwise.
     * @return {@code this}, for method call chaining.
     *
     * @see AbstractIdentifiedObject#isDeprecated()
     *
     * @since 0.6
     */
    public B setDeprecated(final boolean deprecated) {
        properties.put(AbstractIdentifiedObject.DEPRECATED_KEY, deprecated);
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
     * (name, aliases, identifiers, description, remarks and deprecation status) for preparing the
     * builder to the construction of an other object.
     * The authority, codespace and version properties are not cleared by this method.
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
            properties .remove("description");
            properties .remove(AbstractIdentifiedObject.DEPRECATED_KEY);
            aliases    .clear();
            identifiers.clear();
            valueAlias = null;
            valueIds   = null;
        } else {
            valueAlias = aliases    .toArray(new GenericName[aliases    .size()]);
            valueIds   = identifiers.toArray(new ReferenceIdentifier[identifiers.size()]);
        }
        properties.put(IdentifiedObject.ALIAS_KEY,       valueAlias);
        properties.put(IdentifiedObject.IDENTIFIERS_KEY, valueIds);
    }
}
