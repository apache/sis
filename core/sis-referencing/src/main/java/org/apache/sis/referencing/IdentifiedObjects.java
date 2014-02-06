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
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Collection;

import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.CoordinateOperation;

import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.metadata.iso.citation.Citations; // For javadoc.

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.Characters.Filter.LETTERS_AND_DIGITS;
import static org.apache.sis.internal.util.Citations.iterator;
import static org.apache.sis.internal.util.Citations.identifierMatches;


/**
 * Utility methods working on arbitrary implementations of the {@link IdentifiedObject} interface.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see CRS
 * @see org.apache.sis.geometry.Envelopes
 */
public final class IdentifiedObjects extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private IdentifiedObjects() {
    }

    /**
     * Returns the information provided in the specified identified object as a map of properties.
     * The returned map contains the following entries for each key not contained in the {@code excludes} list
     * and for which the corresponding method returns a non-null and non-empty value.
     *
     * <table class="sis">
     *   <tr><th>Key</th> <th>Value</th></tr>
     *   <tr><td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *       <td>{@link IdentifiedObject#getName()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *       <td>{@link IdentifiedObject#getAlias()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *       <td>{@link IdentifiedObject#getIdentifiers()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *       <td>{@link IdentifiedObject#getRemarks()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.operation.CoordinateOperation#SCOPE_KEY}</td>
     *       <td>{@link CoordinateOperation#getScope()} (also in datum and reference systems)</td></tr>
     *   <tr><td>{@value org.opengis.referencing.operation.CoordinateOperation#DOMAIN_OF_VALIDITY_KEY}</td>
     *       <td>{@link CoordinateOperation#getDomainOfValidity()} (also in datum and reference systems)</td></tr>
     *   <tr><td>{@value org.opengis.referencing.operation.CoordinateOperation#OPERATION_VERSION_KEY}</td>
     *       <td>{@link CoordinateOperation#getOperationVersion()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.operation.CoordinateOperation#COORDINATE_OPERATION_ACCURACY_KEY}</td>
     *       <td>{@link CoordinateOperation#getCoordinateOperationAccuracy()}</td></tr>
     * </table>
     *
     * @param  object The identified object to view as a properties map.
     * @param  excludes The keys of properties to exclude from the map.
     * @return An view of the identified object as an immutable map.
     */
    public static Map<String,?> getProperties(final IdentifiedObject object, final String... excludes) {
        ensureNonNull("object", object);
        ensureNonNull("excludes", excludes);
        return new Properties(object, excludes);
    }

    /**
     * Returns every object names and aliases according the given authority. This method performs
     * the same work than {@link #getName(IdentifiedObject, Citation)}, except that it does not
     * stop at the first match. This method is useful in the rare cases where the same authority
     * declares more than one name, and all those names are of interest.
     *
     * @param  object The object to get the names and aliases from, or {@code null}.
     * @param  authority The authority for the names to return, or {@code null} for any authority.
     * @return The object's names and aliases, or an empty set if no name or alias matching the
     *         specified authority has been found.
     */
    public static Set<String> getNames(final IdentifiedObject object, final Citation authority) {
        final Set<String> names = new LinkedHashSet<>(8);
        getName(object, authority, names);
        return names;
    }

    /**
     * Returns an object name according the given authority.
     * This method checks first the {@linkplain AbstractIdentifiedObject#getName() primary name},
     * then all {@linkplain AbstractIdentifiedObject#getAlias() aliases} in their iteration order.
     *
     * <ul>
     *   <li><p>If the name or alias implements the {@link ReferenceIdentifier} interface,
     *       then this method compares the {@linkplain ReferenceIdentifier#getAuthority()
     *       identifier authority} against the specified citation using the
     *       {@link Citations#identifierMatches(Citation, Citation)} method.
     *       If a matching is found, then this method returns the
     *       {@linkplain ReferenceIdentifier#getCode() identifier code} of that object.</p></li>
     *
     *   <li><p>Otherwise, if the alias implements the {@link GenericName} interface, then this method
     *       compares the {@linkplain GenericName#scope() name scope} against the specified citation
     *       using the {@link Citations#identifierMatches(Citation, String)} method.
     *       If a matching is found, then this method returns the
     *       {@linkplain GenericName#tip() name tip} of that object.</p></li>
     * </ul>
     *
     * Note that alias may implement both the {@link ReferenceIdentifier} and {@link GenericName}
     * interfaces (for example {@link NamedIdentifier}). In such cases, the identifier view has
     * precedence.
     *
     * @param  object The object to get the name from, or {@code null}.
     * @param  authority The authority for the name to return, or {@code null} for any authority.
     * @return The object's name (either an {@linkplain ReferenceIdentifier#getCode() identifier code}
     *         or a {@linkplain GenericName#tip() name tip}), or {@code null} if no name matching the
     *         specified authority has been found.
     *
     * @see AbstractIdentifiedObject#getName()
     */
    public static String getName(final IdentifiedObject object, final Citation authority) {
        return getName(object, authority, null);
    }

    /**
     * Returns an object name according the given authority. This method is {@code null}-safe:
     * every properties are checked for null values, even the properties that are supposed to
     * be mandatory (not all implementation defines all mandatory values).
     *
     * @param  object    The object to get the name from, or {@code null}.
     * @param  authority The authority for the name to return, or {@code null} for any authority.
     * @param  addTo     If non-null, the collection where to add all names found.
     * @return The object's name (either an {@linkplain ReferenceIdentifier#getCode() identifier code}
     *         or a {@linkplain GenericName#tip() name tip}), or {@code null} if no name matching the
     *         specified authority has been found.
     */
    private static String getName(final IdentifiedObject object, final Citation authority, final Collection<String> addTo) {
        if (object != null) {
            Identifier identifier = object.getName();
            if (authority == null) {
                if (identifier != null) {
                    final String name = identifier.getCode();
                    if (name != null) {
                        if (addTo == null) {
                            return name;
                        }
                        addTo.add(name);
                    }
                }
                final Iterator<GenericName> it = iterator(object.getAlias());
                if (it != null) while (it.hasNext()) {
                    final GenericName alias = it.next();
                    if (alias != null) {
                        final String name = (alias instanceof Identifier) ?
                                ((Identifier) alias).getCode() : alias.toString();
                        if (name != null) {
                            if (addTo == null) {
                                return name;
                            }
                            addTo.add(name);
                        }
                    }
                }
            } else {
                if (identifier != null) {
                    if (identifierMatches(authority, identifier.getAuthority())) {
                        final String name = identifier.getCode();
                        if (name != null) {
                            if (addTo == null) {
                                return name;
                            }
                            addTo.add(name);
                        }
                    }
                }
                final Iterator<GenericName> it = iterator(object.getAlias());
                if (it != null) while (it.hasNext()) {
                    final GenericName alias = it.next();
                    if (alias != null) {
                        if (alias instanceof Identifier) {
                            identifier = (Identifier) alias;
                            if (identifierMatches(authority, identifier.getAuthority())) {
                                final String name = identifier.getCode();
                                if (name != null) {
                                    if (addTo == null) {
                                        return name;
                                    }
                                    addTo.add(name);
                                }
                            }
                        } else {
                            final NameSpace ns = alias.scope();
                            if (ns != null) {
                                final GenericName scope = ns.name();
                                if (scope != null) {
                                    if (identifierMatches(authority, scope.toString())) {
                                        final String name = alias.toString();
                                        if (name != null) {
                                            if (addTo == null) {
                                                return name;
                                            }
                                            addTo.add(name);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns an identifier for the given object according the given authority.
     * This method checks all {@linkplain AbstractIdentifiedObject#getIdentifiers() identifiers} in their iteration
     * order and returns the first identifier with an {@linkplain NamedIdentifier#getAuthority() authority} citation
     * {@linkplain Citations#identifierMatches(Citation, Citation) matching} the specified authority.
     *
     * @param  object The object to get the identifier from, or {@code null}.
     * @param  authority The authority for the identifier to return, or {@code null} for
     *         the first identifier regardless its authority.
     * @return The object's identifier, or {@code null} if no identifier matching the specified authority
     *         has been found.
     *
     * @see AbstractIdentifiedObject#getIdentifier()
     */
    public static ReferenceIdentifier getIdentifier(final IdentifiedObject object, final Citation authority) {
        if (object != null) {
            final Iterator<ReferenceIdentifier> it = iterator(object.getIdentifiers());
            if (it != null) while (it.hasNext()) {
                final ReferenceIdentifier identifier = it.next();
                if (identifier != null) { // Paranoiac check.
                    if (authority == null || identifierMatches(authority, identifier.getAuthority())) {
                        return identifier;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the string representation of the first identifier, or the object name if there is no identifier.
     * This method searches for the first non-null element in
     * <code>object.{@linkplain AbstractIdentifiedObject#getIdentifiers() getIdentifiers()}</code>. If there is none,
     * then this method fallback on <code>object.{@linkplain AbstractIdentifiedObject#getName() getName()}</code>.
     * The first element found is formatted by {@link #toString(Identifier)}.
     *
     * {@section Recommanded alternatives}
     * <ul>
     *   <li>If the code of a specific authority is wanted (typically EPSG), then consider
     *       using {@link #getIdentifier(IdentifiedObject, Citation)} instead.</li>
     *   <li>In many cases, the identifier is not specified. For an exhaustive scan of the EPSG
     *       database looking for a match, use one of the search methods defined below.</li>
     * </ul>
     *
     * @param  object The identified object, or {@code null}.
     * @return A string representation of the first identifier or name, or {@code null} if none.
     *
     * @see #getIdentifier(IdentifiedObject, Citation)
     * @see #searchIdentifierCode(IdentifiedObject, boolean)
     */
    public static String getIdentifierOrName(final IdentifiedObject object) {
        if (object != null) {
            final Iterator<ReferenceIdentifier> it = iterator(object.getIdentifiers());
            if (it != null) while (it.hasNext()) {
                final String code = toString(it.next());
                if (code != null) { // Paranoiac check.
                    return code;
                }
            }
            final String name = toString(object.getName());
            if (name != null) { // Paranoiac check.
                return name;
            }
        }
        return null;
    }

    /**
     * Returns the first name, alias or identifier which is a
     * {@linkplain CharSequences#isUnicodeIdentifier(CharSequence) valid Unicode identifier}.
     * This method performs the search in the following order:
     *
     * <ul>
     *   <li><code>object.{@linkplain AbstractIdentifiedObject#getName() getName()}</code></li>
     *   <li><code>object.{@linkplain AbstractIdentifiedObject#getAlias() getAlias()}</code> in iteration order</li>
     *   <li><code>object.{@linkplain AbstractIdentifiedObject#getIdentifiers() getIdentifiers()}</code> in iteration order</li>
     * </ul>
     *
     * @param  object The identified object, or {@code null}.
     * @return The first name, alias or identifier which is a valid Unicode identifier, or {@code null} if none.
     */
    public static String getUnicodeIdentifier(final IdentifiedObject object) {
        if (object != null) {
            ReferenceIdentifier identifier = object.getName();
            if (identifier != null) { // Paranoiac check.
                final String code = identifier.getCode();
                if (CharSequences.isUnicodeIdentifier(code)) {
                    return code;
                }
            }
            final Iterator<GenericName> it = iterator(object.getAlias());
            if (it != null) while (it.hasNext()) {
                GenericName alias = it.next();
                if (alias != null && (alias = alias.tip()) != null) {
                    final String code = alias.toString();
                    if (CharSequences.isUnicodeIdentifier(code)) {
                        return code;
                    }
                }
            }
            final Iterator<ReferenceIdentifier> id = iterator(object.getIdentifiers());
            if (id != null) while (id.hasNext()) {
                identifier = id.next();
                if (identifier != null) { // Paranoiac check.
                    final String code = identifier.getCode();
                    if (CharSequences.isUnicodeIdentifier(code)) {
                        return code;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if either the {@linkplain AbstractIdentifiedObject#getName() primary name} or at least
     * one {@linkplain AbstractIdentifiedObject#getAlias() alias} matches the given string according heuristic rules.
     * If the given object is an instance of {@link AbstractIdentifiedObject}, then this method delegates to its
     * {@link AbstractIdentifiedObject#isHeuristicMatchForName(String) isHeuristicMatchForName(String)} method
     * in order to leverage the additional rules implemented by sub-classes.
     * Otherwise the fallback implementation returns {@code true} if the given {@code name} is equal,
     * ignoring aspects documented below, to one of the following names:
     *
     * <ul>
     *   <li>The {@linkplain AbstractIdentifiedObject#getName() primary name}'s {@linkplain NamedIdentifier#getCode() code}
     *       (without {@linkplain NamedIdentifier#getCodeSpace() codespace}).</li>
     *   <li>Any {@linkplain AbstractIdentifiedObject#getAlias() alias}'s {@linkplain NamedIdentifier#tip() tip}
     *       (without {@linkplain NamedIdentifier#scope() scope} and namespace).</li>
     * </ul>
     *
     * The comparison ignores the following aspects:
     * <ul>
     *   <li>Lower/upper cases.</li>
     *   <li>Some Latin diacritical signs (e.g. {@code "Réunion"} and {@code "Reunion"} are considered equal).</li>
     *   <li>All characters that are not {@linkplain Character#isLetterOrDigit(int) letters or digits}
     *       (e.g. {@code "Mercator (1SP)"} and {@code "Mercator_1SP"} are considered equal).</li>
     *   <li>Namespaces or scopes, because this method is typically invoked with either the value of an other
     *       <code>IdentifiedObject.getName().getCode()</code> or with the <cite>Well Known Text</cite> (WKT)
     *       projection or parameter name.</li>
     * </ul>
     *
     * @param  object The object for which to check the name or alias.
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     *
     * @see AbstractIdentifiedObject#isHeuristicMatchForName(String)
     */
    public static boolean isHeuristicMatchForName(final IdentifiedObject object, final String name) {
        if (object instanceof AbstractIdentifiedObject) {
            // DefaultCoordinateSystemAxis overrides this method.
            // We really need to delegate to the overridden method.
            return ((AbstractIdentifiedObject) object).isHeuristicMatchForName(name);
        } else {
            ensureNonNull("object", object);
            return isHeuristicMatchForName(object, object.getAlias(), name);
        }
    }

    /**
     * Returns {@code true} if the {@linkplain AbstractIdentifiedObject#getName() primary name} of the given object
     * or one of the given alias matches the given name. The comparison ignores case, some Latin diacritical signs
     * and any characters that are not letters or digits.
     *
     * @param  object  The object to check.
     * @param  aliases The list of alias in {@code object} (may be {@code null}).
     *                 This method will never modify this list. Consequently, the
     *                 given list can be a direct reference to an internal list.
     * @param  name    The name for which to check for equality.
     * @return {@code true} if the primary name or at least one alias matches the given {@code name}.
     */
    static boolean isHeuristicMatchForName(final IdentifiedObject object, final Collection<GenericName> aliases,
            CharSequence name)
    {
        name = CharSequences.toASCII(name);
        final ReferenceIdentifier id = object.getName();
        if (id != null) { // Paranoiac check.
            final CharSequence code = CharSequences.toASCII(id.getCode());
            if (code != null) { // Paranoiac check.
                if (CharSequences.equalsFiltered(name, code, LETTERS_AND_DIGITS, true)) {
                    return true;
                }
            }
        }
        if (aliases != null) {
            for (final GenericName alias : aliases) {
                if (alias != null) { // Paranoiac check.
                    final CharSequence tip = CharSequences.toASCII(alias.tip().toString());
                    if (CharSequences.equalsFiltered(name, tip, LETTERS_AND_DIGITS, true)) {
                        return true;
                    }
                    /*
                     * Note: a previous version compared also the scoped names. We removed that part,
                     * because experience has shown that this method is used only for the "code" part
                     * of an object name. If we really want to compare scoped name, it would probably
                     * be better to take a GenericName argument instead than String.
                     */
                }
            }
        }
        return false;
    }

    /**
     * Returns a string representation of the given identifier.
     * This method applies the following rules:
     *
     * <ul>
     *   <li>If the given identifier implements the {@link GenericName} interface,
     *       then this method delegates to the {@link GenericName#toString()} method.</li>
     *   <li>Otherwise if the given identifier has a {@linkplain ReferenceIdentifier#getCodeSpace() code space},
     *       then formats the identifier as "{@code codespace:code}".</li>
     *   <li>Otherwise if the given identifier has an {@linkplain Identifier#getAuthority() authority},
     *       then formats the identifier as "{@code authority:code}".</li>
     *   <li>Otherwise returns the {@linkplain Identifier#getCode() identifier code}.</li>
     * </ul>
     *
     * This method is provided because the {@link GenericName#toString()} behavior is specified by its javadoc,
     * while {@link ReferenceIdentifier} has no such contract. For example like most ISO 19115 objects in SIS,
     * the {@link org.apache.sis.metadata.iso.DefaultIdentifier} implementation is formatted as a tree.
     * This static method can be used when a "name-like" representation is needed for any implementation.
     *
     * @param  identifier The identifier, or {@code null}.
     * @return A string representation of the given identifier, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.ImmutableIdentifier#toString()
     * @see NamedIdentifier#toString()
     */
    public static String toString(final Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        if (identifier instanceof GenericName) {
            // The toString() behavior is specified by the GenericName javadoc.
            return identifier.toString();
        }
        final String code = identifier.getCode();
        final String cs;
        if (identifier instanceof ReferenceIdentifier) {
            cs = ((ReferenceIdentifier) identifier).getCodeSpace();
        } else {
            cs = org.apache.sis.internal.util.Citations.getIdentifier(identifier.getAuthority());
        }
        if (cs != null && !cs.isEmpty()) {
            return cs + DefaultNameSpace.DEFAULT_SEPARATOR + code;
        }
        return code;
    }
}
