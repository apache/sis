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
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.CoordinateOperation;

import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.metadata.NameMeaning;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;

import static org.apache.sis.internal.util.Citations.iterator;
import static org.apache.sis.internal.util.Citations.identifierMatches;


/**
 * Utility methods working on arbitrary implementations of the {@link IdentifiedObject} interface.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.4
 * @version 0.7
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
     *   <caption>Provided properties</caption>
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
     *   <tr><td>{@value org.opengis.referencing.operation.OperationMethod#FORMULA_KEY}</td>
     *       <td>{@link org.opengis.referencing.operation.OperationMethod#getFormula()}</td></tr>
     *   <tr><td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#DEPRECATED_KEY}</td>
     *       <td>{@link AbstractIdentifiedObject#isDeprecated()}</td></tr>
     * </table>
     *
     * <div class="note"><b>Note:</b>
     * the current implementation does not provide
     * {@value org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#MINIMUM_VALUE_KEY},
     * {@value org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#MAXIMUM_VALUE_KEY} or
     * {@value org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#RANGE_MEANING_KEY} entry for
     * {@link org.opengis.referencing.cs.CoordinateSystemAxis} instances because the minimum and maximum
     * values depend on the {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getUnit()
     * units of measurement}.</div>
     *
     * @param  object The identified object to view as a properties map.
     * @param  excludes The keys of properties to exclude from the map.
     * @return A view of the identified object properties as an immutable map.
     */
    public static Map<String,?> getProperties(final IdentifiedObject object, final String... excludes) {
        ArgumentChecks.ensureNonNull("object", object);
        ArgumentChecks.ensureNonNull("excludes", excludes);
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
        final Set<String> names = new LinkedHashSet<String>(8);
        getName(object, authority, names);
        return names;
    }

    /**
     * Returns an object name according the given authority.
     * This method checks first the {@linkplain AbstractIdentifiedObject#getName() primary name},
     * then all {@linkplain AbstractIdentifiedObject#getAlias() aliases} in their iteration order.
     *
     * <ul class="verbose">
     *   <li>If the name or alias implements the {@link Identifier} interface,
     *       then this method compares the {@linkplain Identifier#getAuthority()
     *       identifier authority} against the specified citation using the
     *       {@link Citations#identifierMatches(Citation, Citation)} method.
     *       If a matching is found, then this method returns the
     *       {@linkplain Identifier#getCode() identifier code} of that object.</li>
     *
     *   <li>Otherwise, if the alias implements the {@link GenericName} interface, then this method
     *       compares the {@linkplain GenericName#scope() name scope} against the specified citation
     *       using the {@link Citations#identifierMatches(Citation, String)} method.
     *       If a matching is found, then this method returns the
     *       {@linkplain GenericName#tip() name tip} of that object.</li>
     * </ul>
     *
     * Note that alias may implement both the {@link Identifier} and {@link GenericName}
     * interfaces (for example {@link NamedIdentifier}). In such cases, the identifier view has
     * precedence.
     *
     * @param  object The object to get the name from, or {@code null}.
     * @param  authority The authority for the name to return, or {@code null} for any authority.
     * @return The object's name (either an {@linkplain Identifier#getCode() identifier code}
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
     * @return The object's name (either an {@linkplain Identifier#getCode() identifier code}
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
                                    if (identifierMatches(authority, null, scope.toString())) {
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
    public static Identifier getIdentifier(final IdentifiedObject object, final Citation authority) {
        if (object != null) {
            final Iterator<? extends Identifier> it = iterator(object.getIdentifiers());
            if (it != null) while (it.hasNext()) {
                final Identifier identifier = it.next();
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
     * <div class="section">Recommended alternatives</div>
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
     * @see #lookupURN(IdentifiedObject, Citation)
     */
    public static String getIdentifierOrName(final IdentifiedObject object) {
        if (object != null) {
            final Iterator<? extends Identifier> it = iterator(object.getIdentifiers());
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
     *
     * @see org.apache.sis.metadata.iso.ImmutableIdentifier
     * @see org.apache.sis.metadata.iso.citation.Citations#getUnicodeIdentifier(Citation)
     * @see org.apache.sis.util.CharSequences#isUnicodeIdentifier(CharSequence)
     */
    public static String getUnicodeIdentifier(final IdentifiedObject object) {
        if (object != null) {
            Identifier identifier = object.getName();
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
            final Iterator<? extends Identifier> id = iterator(object.getIdentifiers());
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
     * Looks up a URN, such as {@code "urn:ogc:def:crs:EPSG:8.2:4326"}, of the specified object.
     * This method searches in all {@linkplain org.apache.sis.referencing.factory.GeodeticAuthorityFactory geodetic
     * authority factories} known to SIS for an object {@linkplain org.apache.sis.util.ComparisonMode#APPROXIMATIVE
     * approximatively equals} to the specified object. If such an object is found, then the URN for the given
     * authority is returned. Otherwise or if there is ambiguity, this method returns {@code null}.
     *
     * <p><strong>Note that this method checks the identifier validity.</strong>
     * If the given object declares explicitly an identifier, then this method will instantiate an object from the
     * authority factory using that identifier and compare it with the given object. If the comparison fails, then
     * this method returns {@code null}. Consequently this method may return {@code null} even if the given object
     * declares explicitly its identifier. If the declared identifier is wanted unconditionally,
     * one can use the following pattern instead:
     *
     * {@preformat java
     *     String urn = toURN(object.getClass(), getIdentifier(object, authority));
     * }
     *
     * This method can be seen as a converse of {@link CRS#forCode(String)}.
     *
     * @param  object The object (usually a {@linkplain org.apache.sis.referencing.crs.AbstractCRS
     *         coordinate reference system}) whose identifier is to be found, or {@code null}.
     * @param  authority The authority for the identifier to return, or {@code null} for
     *         the first identifier regardless its authority.
     * @return The identifier, or {@code null} if none was found without ambiguity or if the given object was null.
     * @throws FactoryException if an error occurred during the search.
     *
     * @see #newFinder(String)
     * @see #toURN(Class, Identifier)
     *
     * @since 0.7
     */
    public static String lookupURN(final IdentifiedObject object, final Citation authority) throws FactoryException {
        String urn = null;
        if (object != null) {
            for (final IdentifiedObject candidate : newFinder(null).find(object)) {
                String c = toURN(candidate.getClass(), getIdentifier(candidate, authority));
                if (c == null && authority == null) {
                    /*
                     * If 'authority' was null, then getIdentifier(candidate, authority) returned the identifier
                     * for the first authority.  But not all authorities can be formatted as a URN. So try other
                     * authorities.
                     */
                    for (final Identifier id : candidate.getIdentifiers()) {
                        c = toURN(candidate.getClass(), id);
                        if (c != null) break;
                    }
                }
                if (c != null) {
                    if (urn != null && !urn.equals(c)) {
                        return null;
                    }
                    urn = c;
                }
            }
        }
        return urn;
    }

    /**
     * Looks up an EPSG code, such as {@code 4326}, of the specified object. This method searches in EPSG factories
     * known to SIS for an object {@linkplain org.apache.sis.util.ComparisonMode#APPROXIMATIVE approximatively equals}
     * to the specified object. If such an object is found, then its EPSG identifier is returned.
     * Otherwise or if there is ambiguity, this method returns {@code null}.
     *
     * <p><strong>Note that this method checks the identifier validity.</strong>
     * If the given object declares explicitly an identifier, then this method will instantiate an object from the
     * EPSG factory using that identifier and compare it with the given object. If the comparison fails, then this
     * method returns {@code null}. Consequently this method may return {@code null} even if the given object
     * declares explicitly its identifier. If the declared identifier is wanted unconditionally,
     * one can use the following pattern instead:
     *
     * {@preformat java
     *     String code = toString(getIdentifier(object, Citations.EPSG));
     * }
     *
     * This method can be seen as a converse of {@link CRS#forCode(String)}.
     *
     * @param  object The object (usually a {@linkplain org.apache.sis.referencing.crs.AbstractCRS
     *         coordinate reference system}) whose EPSG code is to be found, or {@code null}.
     * @return The EPSG code, or {@code null} if none was found without ambiguity or if the given object was null.
     * @throws FactoryException if an error occurred during the search.
     *
     * @see #newFinder(String)
     *
     * @since 0.7
     */
    public static Integer lookupEPSG(final IdentifiedObject object) throws FactoryException {
        Integer code = null;
        if (object != null) {
            for (final IdentifiedObject candidate : newFinder(Constants.EPSG).find(object)) {
                final Identifier id = getIdentifier(candidate, Citations.EPSG);
                if (id != null) try {
                    Integer previous = code;
                    code = Integer.valueOf(id.getCode());
                    if (previous != null && !previous.equals(code)) {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    Logging.recoverableException(Logging.getLogger(Loggers.CRS_FACTORY), IdentifiedObjects.class, "lookupEPSG", e);
                }
            }
        }
        return code;
    }

    /**
     * Creates a finder which can be used for looking up unidentified objects.
     * This method is an alternative to {@code lookup(…)} methods when more control are desired.
     *
     * <div class="note"><b>Example 1: be lenient regarding axis order</b><br>
     * By default, {@code lookup(…)} methods require that objects in the dataset have their axes in the
     * same order than the given object. For relaxing this condition, one can use the following Java code.
     * This example assumes that at most one object from the dataset will match the given object.
     * If more than one object may match, then the call to {@code findSingleton(…)} should be replaced
     * by {@code find(…)}.
     *
     * {@preformat java
     *     IdentifiedObjectFinder finder = IdentifiedObjects.newFinder(null);
     *     finder.setIgnoringAxes(true);
     *     IdentifiedObject found = finder.findSingleton(object);
     * }</div>
     *
     * <div class="note"><b>Example 2: extend the search to deprecated definitions</b><br>
     * By default, {@code lookup(…)} methods exclude deprecated objects from the search.
     * To search also among deprecated objects, one can use the following Java code:
     * This example does not use the {@code findSingleton(…)} convenience method on the assumption
     * that the search may find both deprecated and non-deprecated objects.
     *
     * {@preformat java
     *     IdentifiedObjectFinder finder = IdentifiedObjects.newFinder(null);
     *     finder.setSearchDomain(IdentifiedObjectFinder.Domain.ALL_DATASET);
     *     Set<IdentifiedObject> found = finder.find(object);
     * }</div>
     *
     * @param  authority The authority of the objects to search (typically {@code "EPSG"} or {@code "OGC"}),
     *         or {@code null} for searching among the objects created by all authorities.
     * @return A finder to use for looking up unidentified objects.
     * @throws NoSuchAuthorityFactoryException if the given authority is not found.
     * @throws FactoryException if the finder can not be created for another reason.
     *
     * @see #lookupEPSG(IdentifiedObject)
     * @see #lookupURN(IdentifiedObject, Citation)
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#newIdentifiedObjectFinder()
     * @see IdentifiedObjectFinder#find(IdentifiedObject)
     */
    public static IdentifiedObjectFinder newFinder(final String authority)
            throws NoSuchAuthorityFactoryException, FactoryException
    {
        final GeodeticAuthorityFactory factory;
        if (authority == null) {
            factory = AuthorityFactories.ALL;
        } else {
            factory = AuthorityFactories.ALL.getAuthorityFactory(GeodeticAuthorityFactory.class, authority, null);
        }
        return factory.newIdentifiedObjectFinder();
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
     * If the {@code object} argument is {@code null}, then this method returns {@code false}.
     *
     * @param  object The object for which to check the name or alias, or {@code null}.
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name or at least one alias matches the specified {@code name}.
     *
     * @see AbstractIdentifiedObject#isHeuristicMatchForName(String)
     */
    public static boolean isHeuristicMatchForName(final IdentifiedObject object, final String name) {
        ArgumentChecks.ensureNonNull("name", name);
        if (object == null) {
            return false;
        }
        if (object instanceof AbstractIdentifiedObject) {
            // DefaultCoordinateSystemAxis overrides this method.
            // We really need to delegate to the overridden method.
            return ((AbstractIdentifiedObject) object).isHeuristicMatchForName(name);
        } else {
            return NameToIdentifier.isHeuristicMatchForName(object.getName(), object.getAlias(), name,
                    NameToIdentifier.Simplifier.DEFAULT);
        }
    }

    /**
     * Returns the URN of the given identifier, or {@code null} if no valid URN can be formed.
     * This method builds a URN from the {@linkplain NamedIdentifier#getCodeSpace() codespace},
     * {@linkplain NamedIdentifier#getVersion() version} and {@linkplain NamedIdentifier#getCode() code}
     * of the given identifier, completed by the given {@link Class} argument.
     *
     * <p>First, this method starts the URN with {@code "urn:"} followed by a namespace determined
     * from the identifier {@linkplain NamedIdentifier#getCodeSpace() codespace} (which is usually
     * an abbreviation of the identifier {@linkplain NamedIdentifier#getAuthority() authority}).
     * The recognized namespaces are listed in the following table
     * (note that the list of authorities than can be used in the {@code "urn:ogc:def"} namespace
     * is specified by the <a href="http://www.opengeospatial.org/ogcna">OGC Naming Authority</a>).
     * If this method can not determine a namespace for the given identifier, it returns {@code null}.</p>
     *
     * <table class="sis">
     *   <caption>Valid values for the authority component in URN</caption>
     *   <tr><th>Namespace</th>           <th>Authority in URN</th> <th>Description</th></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code EPSG}</td>     <td>EPSG dataset</td></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code OGC}</td>      <td>Open Geospatial Consortium</td></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code OGC-WFS}</td>  <td>OGC Web Feature Service</td></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code SI}</td>       <td>Système International d'Unités</td></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code UCUM}</td>     <td>Unified Code for Units of Measure</td></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code UNSD}</td>     <td>United Nations Statistics Division</td></tr>
     *   <tr><td>{@code urn:ogc:def}</td> <td>{@code USNO}</td>     <td>United States Naval Observatory</td></tr>
     * </table>
     *
     * The namespace is followed by the authority, then by a type determined from the given {@link Class} argument.
     * That class is usually determined simply by {@code IdentifiedObject.getClass()}.
     * The given class shall be assignable to one of the following types, otherwise this method returns {@code null}:
     *
     * <table class="sis">
     *   <caption>Valid values for the type component in URN</caption>
     *   <tr><th>Interface</th>                                                     <th>Type in URN</th>                 <th>Description</th></tr>
     *   <tr><td>{@link org.opengis.referencing.cs.CoordinateSystemAxis}</td>       <td>{@code axis}</td>                <td>Coordinate system axe definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.operation.CoordinateOperation}</td> <td>{@code coordinateOperation}</td> <td>Coordinate operation definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.crs.CoordinateReferenceSystem}</td> <td>{@code crs}</td>                 <td>Coordinate reference system definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.cs.CoordinateSystem}</td>           <td>{@code cs}</td>                  <td>Coordinate system definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.datum.Datum}</td>                   <td>{@code datum}</td>               <td>Datum definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.datum.Ellipsoid}</td>               <td>{@code ellipsoid}</td>           <td>Ellipsoid definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.datum.PrimeMeridian}</td>           <td>{@code meridian}</td>            <td>Prime meridian definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.operation.OperationMethod}</td>     <td>{@code method}</td>              <td>Operation method definition</td></tr>
     *   <tr><td>{@link org.opengis.parameter.ParameterDescriptor}</td>             <td>{@code parameter}</td>           <td>Operation parameter definition</td></tr>
     *   <tr><td>{@link org.opengis.referencing.ReferenceSystem}</td>               <td>{@code referenceSystem}</td>     <td>Value reference system definition</td></tr>
     *   <tr><td>{@link javax.measure.unit.Unit}</td>                               <td>{@code uom}</td>                 <td>Unit of measure definition</td></tr>
     * </table>
     *
     * The type is followed by the {@linkplain NamedIdentifier#getVersion() codespace version} if available,
     * and finally by the {@linkplain NamedIdentifier#getCode() code} value.
     *
     * <p>The above tables may be expanded in any future SIS version.</p>
     *
     * @param  type A type assignable to one of the types listed in above table.
     * @param  identifier The identifier for which to format a URN, or {@code null}.
     * @return The URN for the given identifier, or {@code null} if the given identifier was null
     *         or can not be formatted by this method.
     *
     * @see #lookupURN(IdentifiedObject, Citation)
     *
     * @since 0.7
     */
    public static String toURN(final Class<?> type, final Identifier identifier) {
        ArgumentChecks.ensureNonNull("type", type);
        if (identifier == null) {
            return null;
        }
        String cs = null;
        if (identifier instanceof ReferenceIdentifier) {
            cs = ((ReferenceIdentifier) identifier).getCodeSpace();
        }
        if (cs == null || cs.isEmpty()) {
            cs = org.apache.sis.internal.util.Citations.getIdentifier(identifier.getAuthority(), true);
        }
        return NameMeaning.toURN(type, cs,
                (identifier instanceof ReferenceIdentifier) ? ((ReferenceIdentifier) identifier).getVersion() : null,
                identifier.getCode());
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
     * while {@link Identifier} has no such contract. For example like most ISO 19115 objects in SIS,
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
        String cs = null;
        if (identifier instanceof ReferenceIdentifier) {
            cs = ((ReferenceIdentifier) identifier).getCodeSpace();
        }
        if (cs == null || cs.isEmpty()) {
            cs = org.apache.sis.internal.util.Citations.getIdentifier(identifier.getAuthority(), true);
        }
        if (cs != null) {
            return cs + DefaultNameSpace.DEFAULT_SEPARATOR + code;
        }
        return code;
    }
}
