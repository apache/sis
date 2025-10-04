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
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.DefinitionURI;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.pending.jdk.JDK21;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.metadata.internal.shared.Identifiers;
import org.apache.sis.metadata.internal.shared.NameMeaning;
import org.apache.sis.metadata.internal.shared.NameToIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Utility methods working on arbitrary implementations of the {@link IdentifiedObject} interface.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.5
 *
 * @see CRS
 * @see org.apache.sis.geometry.Envelopes
 *
 * @since 0.4
 */
public final class IdentifiedObjects {
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
     *   <tr><td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *       <td>{@link IdentifiedObject#getDomains()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *       <td>{@link IdentifiedObject#getRemarks()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.ObjectDomain#SCOPE_KEY}</td>
     *       <td>{@link ObjectDomain#getScope()}</td></tr>
     *   <tr><td>{@value org.opengis.referencing.ObjectDomain#DOMAIN_OF_VALIDITY_KEY}</td>
     *       <td>{@link ObjectDomain#getDomainOfValidity()}</td></tr>
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
     * <h4>Implementation note</h4>
     * The current implementation does not provide
     * {@value org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#MINIMUM_VALUE_KEY},
     * {@value org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#MAXIMUM_VALUE_KEY} or
     * {@value org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#RANGE_MEANING_KEY} entry for
     * {@link org.opengis.referencing.cs.CoordinateSystemAxis} instances because the minimum and maximum
     * values depend on the {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getUnit()
     * units of measurement}.
     *
     * @param  object    the identified object to view as a properties map.
     * @param  excludes  the keys of properties to exclude from the map.
     * @return a view of the identified object properties as an immutable map.
     */
    public static Map<String,?> getProperties(final IdentifiedObject object, final String... excludes) {
        ArgumentChecks.ensureNonNull("object", object);
        ArgumentChecks.ensureNonNull("excludes", excludes);
        return new Properties(object, excludes);
    }

    /**
     * Returns every object names and aliases according the given authority. This method performs
     * the same work as {@link #getName(IdentifiedObject, Citation)}, except that it does not
     * stop at the first match. This method is useful in the rare cases where the same authority
     * declares more than one name, and all those names are of interest.
     *
     * @param  object     the object to get the names and aliases from, or {@code null}.
     * @param  authority  the authority for the names to return, or {@code null} for any authority.
     * @return the object's names and aliases, or an empty set if no name or alias matching the
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
     *       {@linkplain GenericName#toString() string representation} of that name.</li>
     * </ul>
     *
     * Note that alias may implement both the {@link Identifier} and {@link GenericName}
     * interfaces (for example {@link NamedIdentifier}). In such cases, the identifier view has
     * precedence.
     *
     * @param  object     the object to get the name from, or {@code null}.
     * @param  authority  the authority for the name to return, or {@code null} for any authority.
     * @return the object's name (either an {@linkplain Identifier#getCode() identifier code}
     *         or a {@linkplain GenericName#toString() generic name}),
     *         or {@code null} if no name matching the specified authority has been found.
     *
     * @see AbstractIdentifiedObject#getName()
     */
    @OptionalCandidate
    public static String getName(final IdentifiedObject object, final Citation authority) {
        return getName(object, authority, null);
    }

    /**
     * Returns an object name according the given authority. This method is {@code null}-safe:
     * every properties are checked for null values, even the properties that are supposed to
     * be mandatory (not all implementations define all mandatory values).
     *
     * @param  object     the object to get the name from, or {@code null}.
     * @param  authority  the authority for the name to return, or {@code null} for any authority.
     * @param  addTo      if non-null, the collection where to add all names found.
     * @return the object's name (either an {@linkplain Identifier#getCode() identifier code}
     *         or a {@linkplain GenericName#toString() generic name}),
     *         or {@code null} if no name matching the specified authority has been found.
     */
    private static String getName(final IdentifiedObject object, final Citation authority, final Collection<String> addTo) {
        if (object != null) {
            Identifier identifier = object.getName();
            if (identifier != null) {
                if (authority == null || Citations.identifierMatches(authority, identifier.getAuthority())) {
                    final String name = identifier.getCode();
                    if (name != null) {
                        if (addTo == null) {
                            return name;
                        }
                        addTo.add(name);
                    }
                }
            }
            /*
             * If we do not found a primary name for the specified authority,
             * or if the user requested all names, search among aliases.
             */
            for (final GenericName alias : CollectionsExt.nonNull(object.getAlias())) {
                if (alias != null) {
                    final String name;
                    if (alias instanceof Identifier) {
                        identifier = (Identifier) alias;
                        if (authority != null && !Citations.identifierMatches(authority, identifier.getAuthority())) {
                            continue;               // Authority does not match. Search another alias.
                        }
                        name = identifier.getCode();
                    } else {
                        if (authority != null) {
                            final NameSpace ns = alias.scope();  if (ns    == null) continue;
                            final GenericName scope = ns.name(); if (scope == null) continue;
                            if (!Citations.identifierMatches(authority, scope.toString())) {
                                continue;           // Authority does not match. Search another alias.
                            }
                        }
                        name = alias.toString();
                    }
                    if (name != null) {
                        if (addTo == null) {
                            return name;
                        }
                        addTo.add(name);
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
     * If the specified authority implements {@link IdentifierSpace}, then the authority space name
     * is also compared to the {@linkplain NamedIdentifier#getCodeSpace() code space} of each identifier.
     *
     * @param  object     the object to get the identifier from, or {@code null}.
     * @param  authority  the authority for the identifier to return, or {@code null} for
     *         the first identifier regardless its authority.
     * @return the object's identifier, or {@code null} if no identifier matching the specified authority
     *         has been found.
     *
     * @see AbstractIdentifiedObject#getIdentifier()
     */
    @OptionalCandidate
    public static Identifier getIdentifier(final IdentifiedObject object, final Citation authority) {
        if (object != null) {
            String cs = null;
            if (authority instanceof IdentifierSpace<?>) {
                cs = ((IdentifierSpace<?>) authority).getName();
            }
            for (final Identifier identifier : CollectionsExt.nonNull(object.getIdentifiers())) {
                if (identifier != null) {                       // Paranoiac check.
                    if (cs != null && cs.equalsIgnoreCase(identifier.getCodeSpace())) {
                        return identifier;      // Match based on codespace.
                    }
                    if (authority == null || Citations.identifierMatches(authority, identifier.getAuthority())) {
                        return identifier;      // Match based on citation.
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
     * <h4>Recommended alternatives</h4>
     * <ul>
     *   <li>If the code of a specific authority is wanted (typically EPSG), then consider
     *       using {@link #getIdentifier(IdentifiedObject, Citation)} instead.</li>
     *   <li>In many cases, the identifier is not specified. For an exhaustive scan of the EPSG
     *       database looking for a match, use one of the search methods defined below.</li>
     * </ul>
     *
     * @param  object  the identified object, or {@code null}.
     * @return a string representation of the first identifier or name, or {@code null} if none.
     *
     * @see #getIdentifier(IdentifiedObject, Citation)
     * @see #lookupURN(IdentifiedObject, Citation)
     */
    public static String getIdentifierOrName(final IdentifiedObject object) {
        if (object == null) {
            return null;
        }
        for (final Identifier id : CollectionsExt.nonNull(object.getIdentifiers())) {
            final String code = toString(id);
            if (code != null) {                                 // Paranoiac check.
                return code;
            }
        }
        return toString(object.getName());
    }

    /**
     * Returns the first name, alias or identifier which is a valid Unicode identifier. This method considers a
     * name or identifier as valid if {@link CharSequences#isUnicodeIdentifier(CharSequence)} returns {@code true}.
     * This method performs the search in the following order:
     *
     * <ul>
     *   <li><code>object.{@linkplain AbstractIdentifiedObject#getName() getName()}</code></li>
     *   <li><code>object.{@linkplain AbstractIdentifiedObject#getAlias() getAlias()}</code> in iteration order</li>
     *   <li><code>object.{@linkplain AbstractIdentifiedObject#getIdentifiers() getIdentifiers()}</code> in iteration order</li>
     * </ul>
     *
     * This method is can be used for fetching a more human-friendly identifier than the numerical values
     * typically returned by {@link IdentifiedObject#getIdentifiers()}. However, the returned value is not
     * guaranteed to be unique.
     *
     * @param  object  the identified object, or {@code null}.
     * @return the first name, alias or identifier which is a valid Unicode identifier, or {@code null} if none.
     *
     * @see ImmutableIdentifier
     * @see Citations#toCodeSpace(Citation)
     * @see CharSequences#isUnicodeIdentifier(CharSequence)
     *
     * @since 1.0
     */
    @OptionalCandidate
    public static String getSimpleNameOrIdentifier(final IdentifiedObject object) {
        if (object != null) {
            Identifier identifier = object.getName();
            if (identifier != null) {                                       // Paranoiac check.
                final String code = identifier.getCode();
                if (CharSequences.isUnicodeIdentifier(code)) {
                    return code;
                }
            }
            for (GenericName alias : CollectionsExt.nonNull(object.getAlias())) {
                if (alias != null && (alias = alias.tip()) != null) {
                    final String code = alias.toString();
                    if (CharSequences.isUnicodeIdentifier(code)) {
                        return code;
                    }
                }
            }
            for (final Identifier id : CollectionsExt.nonNull(object.getIdentifiers())) {
                if (id != null) {                                           // Paranoiac check.
                    final String code = id.getCode();
                    if (CharSequences.isUnicodeIdentifier(code)) {
                        return code;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a name that can be used for display purposes.
     * This method checks the non-blank
     * {@linkplain AbstractIdentifiedObject#getName() name},
     * {@linkplain AbstractIdentifiedObject#getAlias() alias} or
     * {@linkplain AbstractIdentifiedObject#getIdentifiers() identifier}, in that order.
     * If the primary name seems to be the {@linkplain CharSequences#isAcronymForWords acronym} of an alias,
     * then the alias is returned. For example if the name is <q>WGS 84</q> and an alias is
     * <q>World Geodetic System 1984</q>, then that later alias is returned.
     *
     * <p>The name should never be missing, but this method nevertheless
     * fallbacks on identifiers as a safety against incomplete implementations.
     * If an identifier implements {@link GenericName} (as with {@link NamedIdentifier}),
     * its {@link GenericName#toInternationalString() toInternationalString()} method will be used.</p>
     *
     * @param  object  the identified object, or {@code null}.
     * @return a name for human reading, or {@code null} if the given object is null.
     *
     * @see #getDisplayName(IdentifiedObject, Locale)
     *
     * @since 1.5
     */
    public static InternationalString getDisplayName(final IdentifiedObject object) {
        return (object == null) ? null : new DisplayName(object);
    }

    /**
     * Returns a name that can be used for display purposes in the specified locale.
     * Invoking this method is equivalent to invoking {@code getDisplayName(object).toString(locale)},
     * except that the {@code object} argument can be null and the creation of an intermediate
     * {@link InternationalString} object is avoided.
     *
     * @param  object  the identified object, or {@code null}.
     * @param  locale  the locale for the name to return, or {@code null} for the default.
     * @return a name for human reading, or {@code null} if none were found.
     *
     * @see #getDisplayName(IdentifiedObject)
     *
     * @since 1.1
     */
    public static String getDisplayName(final IdentifiedObject object, final Locale locale) {
        if (object == null) {
            return null;
        }
        String name = toString(object.getName(), locale);
        for (final GenericName c : CollectionsExt.nonNull(object.getAlias())) {
            final String alias = toString(c, locale);
            if (alias != null) {
                if (name == null || CharSequences.isAcronymForWords(name, alias)) {
                    return alias;
                }
                final String unlocalized = c.toString();
                if (!alias.equals(unlocalized) && CharSequences.isAcronymForWords(name, unlocalized)) {
                    return alias;           // Select the localized version instead of `unlocalized`.
                }
            }
        }
        if (name == null) {
            for (final Identifier id : CollectionsExt.nonNull(object.getIdentifiers())) {
                name = toString(id, locale);
                if (name != null) break;
            }
        }
        return name;
    }

    /**
     * Returns the domain of validity of the given object.
     * If the object specifies more than one domain of validity,
     * then this method computes their intersection (note that this is the opposite of
     * {@link #getGeographicBoundingBox(IdentifiedObject)}, which computes the union).
     * If there is no intersection, then the returned object implements {@link Emptiable}
     * and the {@link Emptiable#isEmpty()} method returns {@code true}.
     *
     * @param  object  the object for which to get the domain of validity, or {@code null}.
     * @return the domain of validity where the object is valid, or empty if unspecified.
     *
     * @since 1.5
     */
    public static Optional<Extent> getDomainOfValidity(final IdentifiedObject object) {
        Extent domain = null;
        if (object != null) {
            for (ObjectDomain obj : object.getDomains()) {
                domain = Extents.intersection(domain, obj.getDomainOfValidity());
            }
        }
        return Optional.ofNullable(domain);
    }

    /**
     * Returns the geographic bounding box computed from the domains of the given object.
     * If the given object contains more than one domain, then this method computes their union.
     * Note that this is the opposite of {@link #getDomainOfValidity(IdentifiedObject)},
     * which computes the intersection.
     *
     * @param  object  the object for which to get the domain of validity, or {@code null}.
     * @return the geographic area where the object is valid, or empty if unspecified.
     *
     * @see IdentifiedObject#getDomains()
     * @see Extents#getGeographicBoundingBox(Stream)
     *
     * @since 1.5
     */
    public static Optional<GeographicBoundingBox> getGeographicBoundingBox(final IdentifiedObject object) {
        if (object == null) {
            return Optional.empty();
        }
        return Extents.getGeographicBoundingBox(object.getDomains().stream().map(ObjectDomain::getDomainOfValidity));
    }

    /**
     * Looks up a URN, such as {@code "urn:ogc:def:crs:EPSG:9.1:4326"}, of the specified object.
     * This method searches in all {@linkplain org.apache.sis.referencing.factory.GeodeticAuthorityFactory geodetic
     * authority factories} known to SIS for an object {@linkplain org.apache.sis.util.ComparisonMode#APPROXIMATE
     * approximately equals} to the specified object. Then there is a choice:
     *
     * <ul>
     *   <li>If a single matching object is found in the specified authority factory, then its URN is returned.</li>
     *   <li>Otherwise if the given object is a {@link CompoundCRS} or {@link ConcatenatedOperation}
     *       and all components have an URN, then this method returns a combined URN.</li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * <p><strong>Note that this method checks the identifier validity.</strong>
     * If the given object declares explicitly an identifier, then this method will instantiate an object from the
     * authority factory using that identifier and compare it with the given object. If the comparison fails, then
     * this method returns {@code null}. Consequently, this method may return {@code null} even if the given object
     * declares explicitly its identifier. If the declared identifier is wanted unconditionally,
     * one can use the following pattern instead:
     *
     * {@snippet lang="java" :
     *     String urn = toURN(object.getClass(), getIdentifier(object, authority));
     *     }
     *
     * This method can be seen as a converse of {@link CRS#forCode(String)}.
     *
     * @param  object  the object (usually a {@linkplain org.apache.sis.referencing.crs.AbstractCRS
     *         coordinate reference system}) whose identifier is to be found, or {@code null}.
     * @param  authority  the authority for the identifier to return, or {@code null} for
     *         the first identifier regardless its authority.
     * @return the identifier, or {@code null} if none was found without ambiguity or if the given object was null.
     * @throws UnavailableFactoryException if the factory for the authority identified in the URN is not available.
     * @throws FactoryException if the lookup failed for another reason.
     *
     * @see #newFinder(String)
     * @see #toURN(Class, Identifier)
     *
     * @since 0.7
     */
    @OptionalCandidate
    public static String lookupURN(final IdentifiedObject object, final Citation authority) throws FactoryException {
        if (object == null) {
            return null;
        }
        IdentifiedObjectFinder finder;
        try {
            finder = newFinder(Citations.toCodeSpace(authority));
        } catch (NoSuchAuthorityFactoryException e) {
            warning("lookupURN", e);
            finder = newFinder(null);
        }
        String urn = lookupURN(object, authority, finder);
        if (urn != null) {
            return urn;
        }
        /*
         * If we didn't found a URN but the given object is made of smaller components, build a combined URN.
         * Example: "urn:ogc:def:crs, crs:EPSG::27700, crs:EPSG::5701" (without spaces actually).
         */
        final List<? extends IdentifiedObject> components;
        if (object instanceof CompoundCRS) {
            components = ((CompoundCRS) object).getSingleComponents();
        } else if (object instanceof ConcatenatedOperation) {
            final var cop = (ConcatenatedOperation) object;
            final List<? extends CoordinateOperation> steps = cop.getOperations();
            if (CRS.equivalent(cop.getSourceCRS(), JDK21.getFirst(steps).getSourceCRS()) &&
                CRS.equivalent(cop.getTargetCRS(), JDK21.getLast (steps).getTargetCRS()))
            {
                components = steps;
            } else {
                return null;
            }
        } else {
            return null;
        }
        StringBuilder buffer = null;
        for (final IdentifiedObject component : components) {
            urn = lookupURN(component, authority, finder);
            if (urn == null) {
                return null;
            }
            assert urn.startsWith(DefinitionURI.PREFIX) : urn;
            if (buffer == null) {
                buffer = new StringBuilder(40).append(DefinitionURI.PREFIX).append(DefinitionURI.SEPARATOR)
                                              .append(NameMeaning.toObjectType(object.getClass()));
            }
            buffer.append(DefinitionURI.COMPONENT_SEPARATOR)
                  .append(urn, DefinitionURI.PREFIX.length() + 1, urn.length());
        }
        return (buffer != null) ? buffer.toString() : null;
    }

    /**
     * Implementation of {@link #lookupURN(IdentifiedObject, Citation)}, possibly invoked many times
     * if the identified object is a {@link CompoundCRS} or {@link ConcatenatedOperation}.
     */
    private static String lookupURN(final IdentifiedObject object, final Citation authority,
                                    final IdentifiedObjectFinder finder) throws FactoryException
    {
        String urn = null;
        if (object != null) try {
            for (final IdentifiedObject candidate : finder.find(object)) {
                String c = toURN(candidate.getClass(), getIdentifier(candidate, authority));
                if (c == null && authority == null) {
                    /*
                     * If `authority` was null, then getIdentifier(candidate, authority) returned the identifier
                     * for the first authority.  But not all authorities can be formatted as a URN. So try other
                     * authorities.
                     */
                    for (final Identifier id : candidate.getIdentifiers()) {
                        c = toURN(candidate.getClass(), id);
                        if (c != null) break;
                    }
                }
                /*
                 * We should find at most one URN. But if we find many, verify that all of them are consistent.
                 */
                if (c != null) {
                    if (urn != null && !urn.equals(c)) {
                        return null;
                    }
                    urn = c;
                }
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        return urn;
    }

    /**
     * Looks up an EPSG code, such as {@code 4326}, of the specified object. This method searches in EPSG factories
     * known to SIS for an object {@linkplain org.apache.sis.util.ComparisonMode#APPROXIMATE approximately equals}
     * to the specified object. If such an object is found, then its EPSG identifier is returned.
     * Otherwise or if there is ambiguity, this method returns {@code null}.
     *
     * <p><strong>Note that this method checks the identifier validity.</strong>
     * If the given object declares explicitly an identifier, then this method will instantiate an object from the
     * EPSG factory using that identifier and compare it with the given object. If the comparison fails, then this
     * method returns {@code null}. Consequently, this method may return {@code null} even if the given object
     * declares explicitly its identifier. If the declared identifier is wanted unconditionally,
     * one can use the following pattern instead:
     *
     * {@snippet lang="java" :
     *     String code = toString(getIdentifier(object, Citations.EPSG));
     *     }
     *
     * This method can be seen as a converse of {@link CRS#forCode(String)}.
     *
     * @param  object  the object (usually a {@linkplain org.apache.sis.referencing.crs.AbstractCRS
     *         coordinate reference system}) whose EPSG code is to be found, or {@code null}.
     * @return the EPSG code, or {@code null} if none was found without ambiguity or if the given object was null.
     * @throws UnavailableFactoryException if the EPSG factory is not available.
     * @throws FactoryException if the lookup failed for another reason.
     *
     * @see #newFinder(String)
     *
     * @since 0.7
     */
    @OptionalCandidate
    public static Integer lookupEPSG(final IdentifiedObject object) throws FactoryException {
        Integer code = null;
        if (object != null) try {
            for (final IdentifiedObject candidate : newFinder(Constants.EPSG).find(object)) {
                final Identifier id = getIdentifier(candidate, Citations.EPSG);
                if (id != null) try {
                    Integer previous = code;
                    code = Integer.valueOf(id.getCode());
                    if (previous != null && !previous.equals(code)) {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    warning("lookupEPSG", e);
                }
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        return code;
    }

    /**
     * Logs a warning for a non-critical error. The callers should have a fallback.
     */
    private static void warning(final String method, final Exception e) {
        Logging.recoverableException(CRS.LOGGER, IdentifiedObjects.class, method, e);
    }

    /**
     * Creates a finder which can be used for looking up unidentified objects.
     * This method is an alternative to {@code lookup(…)} methods when more control are desired.
     *
     * <h4>Example 1: be lenient regarding axis order</h4>
     * By default, {@code lookup(…)} methods require that objects in the dataset have their axes in the
     * same order as the given object. For relaxing this condition, one can use the following Java code.
     * This example assumes that at most one object from the dataset will match the given object.
     * If more than one object may match, then the call to {@code findSingleton(…)} should be replaced
     * by {@code find(…)}.
     *
     * {@snippet lang="java" :
     *     IdentifiedObjectFinder finder = IdentifiedObjects.newFinder(null);
     *     finder.setIgnoringAxes(true);
     *     IdentifiedObject found = finder.findSingleton(object);
     *     }
     *
     * <h4>Example 2: extend the search to deprecated definitions</h4>
     * By default, {@code lookup(…)} methods exclude deprecated objects from the search.
     * To search also among deprecated objects, one can use the following Java code:
     *
     * {@snippet lang="java" :
     *     IdentifiedObjectFinder finder = IdentifiedObjects.newFinder(null);
     *     finder.setSearchDomain(IdentifiedObjectFinder.Domain.ALL_DATASET);
     *     Set<IdentifiedObject> found = finder.find(object);
     *     }
     *
     * @param  authority  the authority of the objects to search (typically {@code "EPSG"} or {@code "OGC"}),
     *         or {@code null} for searching among the objects created by all authorities.
     * @return a finder to use for looking up unidentified objects.
     * @throws NoSuchAuthorityFactoryException if the given authority is not found.
     * @throws FactoryException if the finder cannot be created for another reason.
     *
     * @see #lookupEPSG(IdentifiedObject)
     * @see #lookupURN(IdentifiedObject, Citation)
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#newIdentifiedObjectFinder()
     * @see IdentifiedObjectFinder#find(IdentifiedObject)
     */
    public static IdentifiedObjectFinder newFinder(final String authority) throws NoSuchAuthorityFactoryException, FactoryException {
        final GeodeticAuthorityFactory factory;
        if (authority == null) {
            factory = AuthorityFactories.ALL;
        } else if (authority.equalsIgnoreCase(Constants.EPSG)) {
            return AuthorityFactories.finderForEPSG();
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
     *   <li>Namespaces or scopes, because this method is typically invoked with either the value of another
     *       <code>IdentifiedObject.getName().getCode()</code> or with the <i>Well Known Text</i> (WKT)
     *       projection or parameter name.</li>
     * </ul>
     *
     * If the {@code object} argument is {@code null}, then this method returns {@code false}.
     *
     * @param  object  the object for which to check the name or alias, or {@code null}.
     * @param  name    the name to compare with the object name or aliases.
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
            /*
             * DefaultCoordinateSystemAxis overrides this method.
             * We really need to delegate to the overridden method.
             */
            return ((AbstractIdentifiedObject) object).isHeuristicMatchForName(name);
        } else {
            return NameToIdentifier.isHeuristicMatchForName(
                    object.getName(), object.getAlias(), name,
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
     * (note that the list of authorities that can be used in the {@code "urn:ogc:def"} namespace
     * is specified by the <a href="https://www.ogc.org/ogcna">OGC Naming Authority</a>).
     * If this method cannot determine a namespace for the given identifier, it returns {@code null}.</p>
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
     *   <tr><td>{@link javax.measure.Unit}</td>                                    <td>{@code uom}</td>                 <td>Unit of measure definition</td></tr>
     * </table>
     *
     * The type is followed by the {@linkplain NamedIdentifier#getVersion() codespace version} if available,
     * and finally by the {@linkplain NamedIdentifier#getCode() code} value.
     *
     * <p>The above tables may be expanded in any future SIS version.</p>
     *
     * @param  type  a type assignable to one of the types listed in above table.
     * @param  identifier  the identifier for which to format a URN, or {@code null}.
     * @return the URN for the given identifier, or {@code null} if the given identifier was null
     *         or cannot be formatted by this method.
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
        String cs = identifier.getCodeSpace();
        if (Strings.isNullOrEmpty(cs)) {
            cs = Identifiers.getIdentifier(identifier.getAuthority(), true);
        }
        return NameMeaning.toURN(type, cs, identifier.getVersion(), identifier.getCode());
    }

    /**
     * Returns a string representation of the given identifier.
     * This method applies the following rules:
     *
     * <ul>
     *   <li>If the given identifier implements the {@link GenericName} interface,
     *       then this method delegates to the {@link GenericName#toString()} method.</li>
     *   <li>Otherwise if the given identifier has a {@linkplain Identifier#getCodeSpace() code space},
     *       then formats the identifier as "{@code codespace:code}".</li>
     *   <li>Otherwise if the given identifier has an {@linkplain Identifier#getAuthority() authority},
     *       then formats the identifier as "{@code authority:code}".</li>
     *   <li>Otherwise returns the {@linkplain Identifier#getCode() identifier code}.</li>
     * </ul>
     *
     * This method is provided because the {@link GenericName#toString()} behavior is specified by its javadoc,
     * while {@link Identifier} has no such contract. For example, like most ISO 19115 objects in SIS,
     * the {@link org.apache.sis.metadata.iso.DefaultIdentifier} implementation is formatted as a tree.
     * This static method can be used when a "name-like" representation is needed for any implementation.
     *
     * @param  identifier  the identifier, or {@code null}.
     * @return a string representation of the given identifier, or {@code null}.
     *
     * @see ImmutableIdentifier#toString()
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
        String cs = identifier.getCodeSpace();
        if (Strings.isNullOrEmpty(cs)) {
            cs = Citations.toCodeSpace(identifier.getAuthority());
        }
        if (cs != null) {
            return cs + Constants.DEFAULT_SEPARATOR + code;
        }
        return code;
    }

    /**
     * Returns a localized name for the given identifier if possible, or the identifier code otherwise.
     * This method performs paranoiac checks against null or empty values. We do not provides this method
     * in public API because those aggressive checks may be unexpected. It is okay when we merely want to
     * provide a label for human reading.
     *
     * @param  identifier  the identifier for which to get a localized string representation.
     * @param  locale      the desired locale, or {@code null} for the default.
     * @return string representation, or {@code null} if none.
     */
    private static String toString(final Identifier identifier, final Locale locale) {
        if (identifier == null) return null;
        if (identifier instanceof GenericName) {
            final String name = toString(((GenericName) identifier).tip(), locale);
            if (name != null) return name;
        }
        return Strings.trimOrNull(identifier.getCode());
    }

    /**
     * Returns a string representation of the given name in the given locale.
     * This method performs paranoiac checks against null or empty values.
     * We do not provides this method in public API because those aggressive checks may be unexpected.
     * It is okay when we merely want to provide a label for human reading.
     *
     * @param  name    the name for which to get a localized string representation.
     * @param  locale  the desired locale, or {@code null} for the default.
     * @return localized string representation, or {@code null} if none.
     */
    private static String toString(final GenericName name, final Locale locale) {
        if (name == null) {
            return null;
        }
        if (locale != null) {
            final InternationalString i18n = name.toInternationalString();
            if (i18n != null) {
                final String s = Strings.trimOrNull(i18n.toString(locale));
                if (s != null) return s;
            }
        }
        return Strings.trimOrNull(name.toString());
    }
}
