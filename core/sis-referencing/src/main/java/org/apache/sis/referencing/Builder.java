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
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.system.DefaultFactories.NAMES;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Base class of builders for various kind of {@link IdentifiedObject}. {@code Builder}s aim to make object creation
 * easier — they do not add any new functionality compared to {@link org.opengis.referencing.ObjectFactory}.
 * Builder methods like {@link #addName(CharSequence)} and {@link #addIdentifier(String)} provide convenient ways
 * to fill the {@link #properties} map, which will be given to the {@code ObjectFactory} methods at
 * {@code IdentifiedObject} creation time. Creations happen when any {@code createXXX(…)} method defined in
 * the builder subclasses is invoked.
 *
 * <p>This base class provides method for defining the following {@link IdentifiedObject} properties:</p>
 * <blockquote><table class="compact" summary="Builder properties.">
 * <tr><td>{@link AbstractIdentifiedObject#getName() Name}:</td>
 *     <td>Each {@code IdentifiedObject} shall have a name, which can be specified by a call to any of the
 *     {@code addName(…)} methods defined in this class.</td></tr>
 *
 * <tr><td>{@link AbstractIdentifiedObject#getAlias() Aliases}:</td>
 *     <td>Identified objects can optionally have an arbitrary amount of aliases, which are also specified
 *     by the {@code addName(…)} methods — each call after the first one adds an alias.</td></tr>
 *
 * <tr><td>{@link AbstractIdentifiedObject#getIdentifiers() Identifiers}:</td>
 *     <td>Identified objects can also have an arbitrary amount of identifiers, which are specified by the
 *     {@code addIdentifier(…)} methods. Like names, more than one identifier can be added by invoking
 *     the method many time.</td></tr>
 *
 * <tr><td>{@link AbstractIdentifiedObject#getRemarks() Remarks}:</td>
 *     <td>Identified objects can have at most one remark, which is specified by the {@code setRemarks(…)}
 *         method.</td></tr>
 * </table></blockquote>
 *
 * {@section Builder property lifetimes}
 * The same builder can be used for creating many objects, since constructing a Coordinate Reference System (CRS)
 * may require constructing many components (coordinate system, datum, ellipsoid, prime meridian, <i>etc.</i>),
 * some of them sharing common properties. In order to simplify the most common usages, identification
 * properties have two different lifetimes in the {@code Builder} class:
 *
 * <ul>
 *   <li>{@linkplain NamedIdentifier#getAuthority() Authority}, {@linkplain NamedIdentifier#getCodeSpace() code space}
 *       and {@linkplain NamedIdentifier#getVersion() version} information specified to this {@code Builder} will stay
 *       active until they are specified again, because those information are typically shared by all components.</li>
 *   <li>Other properties (name, aliases, identifiers and remarks) are cleared after each call to a {@code createXXX(…)}
 *       method, because they are usually specific to a particular {@code IdentifiedObject} instance.</li>
 * </ul>
 *
 * {@section Usage examples}
 * The "<cite>Mercator (variant A)</cite>" projection (EPSG:9804) is also known as "<cite>Mercator (1SP)</cite>".
 * OGC and GeoTIFF use slightly different names, and GeoTIFF has its own code (7).
 * Those information can be specified as below:
 *
 * {@preformat java
 *   Builder builder = new Builder();
 *   builder.setCodespace (Citations.OGP, "EPSG")
 *          .addName      ("Mercator (variant A)")             // Defined in EPSG namespace.
 *          .addName      ("Mercator (1SP)")                   // Defined in EPSG namespace.
 *          .addIdentifier("9804")                             // Defined in EPSG namespace.
 *          .addName      (Citations.OGC,     "Mercator_1SP")
 *          .addName      (Citations.GEOTIFF, "CT_Mercator")
 *          .addIdentifier(Citations.GEOTIFF, "7")
 *          .setRemarks("The “Mercator (1SP)” method name was used prior to October 2010.");
 *   // At this point, the createXXX(…) method to invoke depends on the Builder subclass.
 * }
 *
 * The two first names, which use the default namespace specified by the call to {@code setCodeSpace(…)},
 * will have the {@code "EPSG"} {@linkplain NamedIdentifier#scope() scope}. Since scopes are not shown
 * in {@linkplain NamedIdentifier#toString() string representation of names}, the string representation
 * of the two first names will omit the {@code "EPSG:"} prefix. However the string representation of the
 * two last names will be {@code "OGC:Mercator_1SP"} and {@code "GeoTIFF:CT_Mercator"} respectively.
 *
 * <p>The {@code IdentificationObject} created by this example will have the following properties:</p>
 * <blockquote><table class="compact" summary="IdentifiedObject properties.">
 * <tr><td>{@link AbstractIdentifiedObject#getName() Name}:</td>
 *     <td>{@code "Mercator (variant A)"} as a local name in {@code "EPSG"} scope.</td></tr>
 * <tr><td>{@link AbstractIdentifiedObject#getAlias() Aliases}:</td>
 *     <td>{@code "Mercator (1SP)"} as a local name in {@code "EPSG"} scope,
 *         {@code "OGC:Mercator_1SP"} and {@code "GeoTIFF:CT_Mercator"} as scoped names.</td></tr>
 * <tr><td>{@link AbstractIdentifiedObject#getIdentifiers() Identifiers}:</td>
 *     <td>{@code "EPSG:9804"} and {@code "GeoTIFF:7"}.</td></tr>
 * <tr><td>{@link AbstractIdentifiedObject#getRemarks() Remarks}:</td>
 *     <td>{@code "The “Mercator (1SP)” method name was used prior to October 2010."}</td></tr>
 * </table></blockquote>
 *
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
 * @version 0.4
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
     *
     * @see #namespace()
     */
    private NameSpace namespace;

    /**
     * Creates a new builder.
     */
    protected Builder() {
        assert verifyParameterizedType(getClass());
        properties  = new HashMap<String,Object>(8);
        aliases     = new ArrayList<GenericName>(4);
        identifiers = new ArrayList<ReferenceIdentifier>(4);
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
     * is needed for all keys defined in the {@link ReferenceIdentifier} interface. This check is not needed for
     * other keys, so callers do not need to invoke this method for other keys.
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
            final String codespace = (String) properties.get(ReferenceIdentifier.CODESPACE_KEY);
            if (codespace != null) {
                namespace = NAMES.createNameSpace(NAMES.createLocalName(null, codespace), null);
            }
        }
        return namespace;
    }

    /**
     * Sets the {@code ReferenceIdentifier} authority and code space. This method is typically invoked only once,
     * since a compound object often uses the same code space for all individual components.
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
     */
    public B setCodeSpace(final Citation authority, final String codespace) {
        if (!setProperty(ReferenceIdentifier.CODESPACE_KEY, codespace)) {
            namespace = null;
        }
        setProperty(ReferenceIdentifier.AUTHORITY_KEY, authority);
        return self();
    }

    /**
     * Sets the {@code ReferenceIdentifier} version of object definitions. This method is typically invoked only once,
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
     * The given string will be combined with the authority, {@link #setCodeSpace(Citation, String) code space}
     * and {@link #setVersion(String) version} information for creating the {@link ReferenceIdentifier} or
     * {@link GenericName} object.
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
        final Object old = properties.put(IdentifiedObject.NAME_KEY, name.toString());
        if (old != null) {
            properties.put(IdentifiedObject.NAME_KEY, old); // Restore previous value.
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
        final NamedIdentifier identifier;
        if (name instanceof InternationalString) {
            identifier = new NamedIdentifier(authority, (InternationalString) name);
        } else {
            identifier = new NamedIdentifier(authority, name.toString());
        }
        final Object old = properties.put(IdentifiedObject.NAME_KEY, identifier);
        if (old != null) {
            properties.put(IdentifiedObject.NAME_KEY, old); // Restore previous value.
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
    public B addName(final ReferenceIdentifier name) {
        ensureNonNull("name", name);
        final Object old = properties.put(IdentifiedObject.NAME_KEY, name);
        if (old != null) {
            properties.put(IdentifiedObject.NAME_KEY, old); // Restore previous value.
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
            properties.put(IdentifiedObject.NAME_KEY, new NamedIdentifier(name));
        } else {
            aliases.add(name);
        }
        return self();
    }

    /**
     * Adds an {@code IdentifiedObject} identifier given by a {@code String}.
     * The given string will be combined with the authority, {@link #setCodeSpace(Citation, String) code space}
     * and {@link #setVersion(String) version} information for creating the {@link ReferenceIdentifier} object.
     *
     * <p><b>Lifetime:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The {@code IdentifiedObject} identifier.
     * @return {@code this}, for method call chaining.
     */
    public B addIdentifier(final String identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(new ImmutableIdentifier((Citation) properties.get(ReferenceIdentifier.AUTHORITY_KEY),
                (String) properties.get(ReferenceIdentifier.CODESPACE_KEY), identifier));
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
        identifiers.add(new ImmutableIdentifier(authority, Citations.getIdentifier(authority), identifier));
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
    public B addIdentifier(final ReferenceIdentifier identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(identifier);
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
        properties.put(IdentifiedObject.ALIAS_KEY,       cleanup ? null : aliases    .toArray(new GenericName        [aliases    .size()]));
        properties.put(IdentifiedObject.IDENTIFIERS_KEY, cleanup ? null : identifiers.toArray(new ReferenceIdentifier[identifiers.size()]));
        if (cleanup) {
            properties .put(IdentifiedObject.NAME_KEY, null);
            properties .remove(IdentifiedObject.REMARKS_KEY);
            aliases    .clear();
            identifiers.clear();
        }
    }
}
