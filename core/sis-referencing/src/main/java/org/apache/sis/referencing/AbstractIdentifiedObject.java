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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.jaxb.referencing.Code;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.util.CollectionsExt.nonEmpty;
import static org.apache.sis.internal.util.CollectionsExt.immutableSet;
import static org.apache.sis.internal.util.Utilities.appendUnicodeIdentifier;

// Related to JDK7
import java.util.Objects;


/**
 * Base class for objects identified by a name or a code. Those objects are typically
 * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic datum}   (e.g. "<cite>World Geodetic System 1984</cite>"),
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} (e.g. "<cite>WGS 84 / World Mercator</cite>") or
 * {@linkplain org.apache.sis.referencing.operation.DefaultProjection map projection}  (e.g. "<cite>Mercator (variant A)</cite>").
 * Those names, or a code (e.g. {@code "EPSG:3395"}), can be used for fetching an object from a database.
 * However it is not sufficient to know the object name. We also need to know who define that name
 * (the {@linkplain NamedIdentifier#getAuthority() authority}) since the same objects are often named differently
 * depending on the providers, or conversely the same name is used for different objects depending on the provider.
 *
 * <p>The main information stored in an {@code IdentifiedObject} are:</p>
 * <ul>
 *   <li>a primary {@linkplain #getName() name}, considered by the object creator as the preferred name,</li>
 *   <li>an arbitrary amount of {@linkplain #getAlias() aliases}, for example a list of names used by other providers,</li>
 *   <li>an arbitrary amount of {@linkplain #getIdentifiers() identifiers}, typically primary keys in the provider database,</li>
 *   <li>optional {@linkplain #getRemarks() remarks}.</li>
 * </ul>
 *
 * {@section Instantiation}
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Applications should instead instantiate the most specific subclass having a name starting by {@code Default}.
 * However exceptions to this rule may occur when it is not possible to identify the exact type.
 *
 * {@example It is sometime not possible to infer the exact coordinate system from version 1 of
 *           <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
 *           Known Text</cite></a>, for example when parsing a <code>LOCAL_CS</code> element. In such exceptional
 *           situation, a plain <code>AbstractCS</code> object may be instantiated.}
 *
 * {@code IdentifiedObject} instances are created in two main ways:
 *
 * <ul>
 *   <li>Using an {@link ObjectFactory}, in which case all properties can be explicitely specified.</li>
 *   <li>Using an {@link AuthorityFactory}, in which case only a code (typically a primary key) is specified.
 *       The {@linkplain NamedIdentifier#getAuthority() authority}
 *       and {@linkplain NamedIdentifier#getCode() authority code} values are set to the authority name
 *       of the factory object, and the authority code supplied by the client, respectively.
 *       All other information are fetched from the database.</li>
 * </ul>
 *
 * {@section Immutability and thread safety}
 * This base class is immutable if the {@link Citation}, {@link ReferenceIdentifier}, {@link GenericName} and
 * {@link InternationalString} instances given to the constructor are also immutable. Most SIS subclasses and
 * related classes are immutable under similar conditions. This means that unless otherwise noted in the javadoc,
 * {@code IdentifiedObject} instances created using only SIS factories and static constants can be shared by many
 * objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@XmlType(name="IdentifiedObjectType", propOrder={
    "identifier",
    "names",
    "remarks"
})
@XmlSeeAlso({
    AbstractReferenceSystem.class,
    org.apache.sis.referencing.datum.AbstractDatum.class,
    org.apache.sis.referencing.datum.DefaultEllipsoid.class,
    org.apache.sis.referencing.datum.DefaultPrimeMeridian.class,
    org.apache.sis.referencing.cs.AbstractCS.class
})
public class AbstractIdentifiedObject extends FormattableObject implements IdentifiedObject,
        LenientComparable, Deprecable, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5173281694258483264L;

    /**
     * The name for this object or code. Shall never be {@code null}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setNames(Collection)}</p>
     *
     * @see #getName()
     * @see #getNames()
     */
    private ReferenceIdentifier name;

    /**
     * An alternative name by which this object is identified, or {@code null} if none.
     * We must be prepared to handle either null or an empty set for "no alias" because
     * we may get both on unmarshalling.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setNames(Collection)}</p>
     */
    private Collection<GenericName> alias;

    /**
     * An identifier which references elsewhere the object's defining information.
     * Alternatively an identifier by which this object can be referenced.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setIdentifier(ReferenceIdentifier)}</p>
     *
     * @see #getIdentifiers()
     * @see #getIdentifier()
     */
    private Set<ReferenceIdentifier> identifiers;

    /**
     * Comments on or information about this object, or {@code null} if none.
     */
    @XmlElement
    private final InternationalString remarks;

    /**
     * The cached hash code value, or 0 if not yet computed. This field is calculated only when
     * first needed. We do not declare it {@code volatile} because it is not a big deal if this
     * field is calculated many time, and the same value should be produced by all computations.
     * The only possible outdated value is 0, which is okay.
     */
    private transient int hashCode;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractIdentifiedObject() {
        identifiers = null;
        remarks     = null;
    }

    /**
     * Constructs an object from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the
     * {@value org.opengis.referencing.IdentifiedObject#NAME_KEY} key.
     * Other properties listed in the table below are optional.
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#AUTHORITY_KEY}</td>
     *     <td>{@link String} or {@link Citation}</td>
     *     <td>{@link NamedIdentifier#getAuthority()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#CODE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link NamedIdentifier#getCode()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#CODESPACE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link NamedIdentifier#getCodeSpace()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link NamedIdentifier#getVersion()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * Additionally, all localizable attributes like {@code "remarks"} may have a language and country code suffix.
     * For example the {@code "remarks_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
     * the {@code "remarks_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     *
     * <p>Note that the {@code "authority"} and {@code "version"} properties are ignored if the {@code "name"}
     * property is already a {@link ReferenceIdentifier} object instead than a {@link String}.</p>
     *
     * @param  properties The properties to be given to this identified object.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    public AbstractIdentifiedObject(final Map<String,?> properties) throws IllegalArgumentException {
        ensureNonNull("properties", properties);

        // -------------------------------------
        // "name": String or ReferenceIdentifier
        // -------------------------------------
        Object value = properties.get(NAME_KEY);
        if (value == null || value instanceof String) {
            name = new NamedIdentifier(PropertiesConverter.convert(properties));
        } else if (value instanceof ReferenceIdentifier) {
            name = (ReferenceIdentifier) value;
        } else {
            throw illegalPropertyType(NAME_KEY, value);
        }

        // -------------------------------------------------------------------
        // "alias": CharSequence, CharSequence[], GenericName or GenericName[]
        // -------------------------------------------------------------------
        value = properties.get(ALIAS_KEY);
        try {
            alias = immutableSet(true, Types.toGenericNames(value, null));
        } catch (ClassCastException e) {
            throw (IllegalArgumentException) illegalPropertyType(ALIAS_KEY, value).initCause(e);
        }

        // -----------------------------------------------------------
        // "identifiers": ReferenceIdentifier or ReferenceIdentifier[]
        // -----------------------------------------------------------
        value = properties.get(IDENTIFIERS_KEY);
        if (value == null) {
            identifiers = null;
        } else if (value instanceof ReferenceIdentifier) {
            identifiers = Collections.singleton((ReferenceIdentifier) value);
        } else if (value instanceof ReferenceIdentifier[]) {
            identifiers = immutableSet(true, (ReferenceIdentifier[]) value);
        } else {
            throw illegalPropertyType(IDENTIFIERS_KEY, value);
        }

        // ----------------------------------------
        // "remarks": String or InternationalString
        // ----------------------------------------
        remarks = Types.toInternationalString(properties, REMARKS_KEY);
    }

    /**
     * Returns the exception to be thrown when a property if of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(final String key, final Object value) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyClass_2, key, value.getClass()));
    }

    /**
     * Constructs a new identified object with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param object The object to shallow copy.
     */
    protected AbstractIdentifiedObject(final IdentifiedObject object) {
        ensureNonNull("object", object);
        name        =          object.getName();
        alias       = nonEmpty(object.getAlias()); // Favor null for empty set in case it is not Collections.EMPTY_SET
        identifiers = nonEmpty(object.getIdentifiers());
        remarks     =          object.getRemarks();
    }

    /**
     * Returns a SIS identified object implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is is an instance of
     *       {@link org.opengis.referencing.datum.Datum},
     *       {@link org.opengis.referencing.datum.Ellipsoid} or
     *       {@link org.opengis.referencing.datum.PrimeMeridian},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractIdentifiedObject}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractIdentifiedObject} instance is created using the
     *       {@linkplain #AbstractIdentifiedObject(IdentifiedObject) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractIdentifiedObject castOrCopy(final IdentifiedObject object) {
        return SubTypes.castOrCopy(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * This information is part of the data compared by {@link #equals(Object, ComparisonMode)}.
     *
     * <p>The default implementation returns {@code IdentifiedObject.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.</p>
     *
     * {@section Invariants}
     * The following invariants must hold for all {@code AbstractIdentifiedObject} instances:
     * <ul>
     *   <li><code>getInterface().{@linkplain Class#isInstance(Object) isInstance}(this)</code>
     *       shall return {@code true}.</li>
     *   <li>If {@code A.getClass() == B.getClass()} is {@code true}, then
     *       {@code A.getInterface() == B.getInterface()} shall be {@code true}.
     *       Note that the converse does not need to hold.</li>
     * </ul>
     *
     * @return The GeoAPI interface implemented by this class.
     */
    public Class<? extends IdentifiedObject> getInterface() {
        return IdentifiedObject.class;
    }

    /**
     * The {@code gml:id}, which is mandatory. The current implementation searches for the first identifier,
     * regardless its authority. If no identifier is found, then the name is used.
     * If no name is found (which should not occur for valid objects), then this method returns {@code null}.
     *
     * <p>If an identifier has been found, this method returns the concatenation of the following elements
     * separated by hyphens:</p>
     * <ul>
     *   <li>The code space in lower case, retaining only characters that are valid for Unicode identifiers.</li>
     *   <li>The object type as defined in OGC's URN (see {@link org.apache.sis.internal.util.DefinitionURI})</li>
     *   <li>The object code, retaining only characters that are valid for Unicode identifiers.</li>
     * </ul>
     *
     * Example: {@code "epsg-crs-4326"}.
     *
     * <p>The returned ID needs to be unique only in the XML document being marshalled.
     * Consecutive invocations of this method do not need to return the same value,
     * since it may depends on the marshalling context.</p>
     */
    @XmlID
    @XmlAttribute(name = "id", namespace = Namespaces.GML, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    final String getID() {
        final StringBuilder id = new StringBuilder();
        /*
         * We will iterate over the identifiers first. Only after the iteration is over,
         * if we found no suitable ID, then we will use the primary name as a last resort.
         */
        if (identifiers != null) {
            for (final ReferenceIdentifier identifier : identifiers) {
                if (appendUnicodeIdentifier(id, '-', identifier.getCodeSpace(), ":", true) | // Really |, not ||
                    appendUnicodeIdentifier(id, '-', ReferencingUtilities.toURNType(getClass()), ":", false) |
                    appendUnicodeIdentifier(id, '-', identifier.getCode(), ":", true))
                {
                    /*
                     * TODO: If we want to check for ID uniqueness or any other condition before to accept the ID,
                     * we would do that here. If the ID is rejected, then we just need to clear the buffer and let
                     * the iteration continue the search for an other ID.
                     */
                    return id.toString();
                }
                id.setLength(0); // Clear the buffer for an other try.
            }
        }
        // In last ressort, append code without codespace since the name are often verbose.
        return appendUnicodeIdentifier(id, '-', name.getCode(), ":", false) ? id.toString() : null;
    }

    /**
     * Returns a single element from the {@code Set<ReferenceIdentifier>} collection, or {@code null} if none.
     * We have to define this method because ISO 19111 defines the {@code identifiers} property as a collection
     * while GML 3.2 defines it as a singleton.
     *
     * <p>This method searches for the following identifiers, in preference order:</p>
     * <ul>
     *   <li>The first identifier having a code that begin with {@code "urn:"}.</li>
     *   <li>The first identifier having a code that begin with {@code "http:"}.</li>
     *   <li>The first identifier, converted to the {@code "urn:} syntax if possible.</li>
     * </ul>
     */
    @XmlElement(name = "identifier")
    final Code getIdentifier() {
        return Code.forIdentifiedObject(getClass(), identifiers);
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the identifier.
     */
    private void setIdentifier(final Code identifier) {
        if (identifier != null) {
            final ReferenceIdentifier id = identifier.getIdentifier();
            if (id != null) {
                identifiers = Collections.singleton(id);
            }
        }
    }

    /**
     * Returns the {@link #name} and all aliases which are also instance of {@lik ReferenceIdentifier}.
     * The later happen often in SIS implementation since many aliases are instance of {@link NamedIdentifier}.
     */
    @XmlElement(name = "name", required = true)
    final Collection<ReferenceIdentifier> getNames() {
        // Unconditionally creates a modifiable list because some JAXB implementations modify it.
        final Collection<ReferenceIdentifier> names = new ArrayList<>(nonNull(alias).size() + 1);
        names.add(name);
        if (alias != null) {
            for (final GenericName c : alias) {
                if (c != name && (c instanceof ReferenceIdentifier)) {
                    names.add((ReferenceIdentifier) c);
                }
            }
        }
        return names;
    }

    /**
     * Sets the first element as the {@link #name} and all remaining elements as {@link #alias}.
     * This method is invoked by JAXB at unmarshalling time. It should not be invoked anymore
     * after the object has been made available to the user.
     */
    private void setNames(final Collection<ReferenceIdentifier> names) {
        if (names != null) {
            final Iterator<ReferenceIdentifier> it = names.iterator();
            if (it.hasNext()) {
                name = it.next();
                if (it.hasNext()) {
                    alias = new ArrayList<>(4); // There is generally few aliases.
                    do {
                        alias.add(new NamedIdentifier(it.next()));
                    } while (it.hasNext());
                }
            }
        }
    }

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return The primary name.
     *
     * @see IdentifiedObjects#getName(IdentifiedObject, Citation)
     */
    @Override
    public ReferenceIdentifier getName() {
        return name;
    }

    /**
     * Returns alternative names by which this object is identified.
     *
     * @return The aliases, or an empty collection if there is none.
     *
     * @see #getName()
     */
    @Override
    public Collection<GenericName> getAlias() {
        return nonNull(alias); // Needs to be null-safe because we may have a null value on unmarshalling.
    }

    /**
     * Returns identifiers which references elsewhere the object's defining information.
     * Alternatively identifiers by which this object can be referenced.
     *
     * @return This object identifiers, or an empty set if there is none.
     *
     * @see IdentifiedObjects#getIdentifier(IdentifiedObject, Citation)
     */
    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        return nonNull(identifiers); // Needs to be null-safe because we may have a null value on unmarshalling.
    }

    /**
     * Returns comments on or information about this object, including data source information.
     *
     * @return The remarks, or {@code null} if none.
     */
    @Override
    public InternationalString getRemarks(){
        return remarks;
    }

    /**
     * Returns {@code true} if this object is deprecated. Deprecated objects exist in some
     * {@linkplain org.opengis.referencing.AuthorityFactory authority factories} like the
     * EPSG database. Deprecated objects are usually obtained from a deprecated authority code.
     * For this reason, the default implementation applies the following rules:
     *
     * <ul>
     *   <li>If the {@linkplain #getName() name} is deprecated, then returns {@code true}.</li>
     *   <li>Otherwise if <strong>all</strong> {@linkplain #getIdentifiers() identifiers}
     *       are deprecated, ignoring the identifiers that are not instance of {@link Deprecable}
     *       (because they can not be tested), then returns {@code true}.</li>
     *   <li>Otherwise returns {@code false}.</li>
     * </ul>
     *
     * @return {@code true} if this object is deprecated.
     *
     * @see org.apache.sis.metadata.iso.ImmutableIdentifier#isDeprecated()
     */
    @Override
    public boolean isDeprecated() {
        if (name instanceof Deprecable) {
            if (((Deprecable) name).isDeprecated()) {
                return true;
            }
        }
        boolean isDeprecated = false;
        for (final ReferenceIdentifier identifier : nonNull(identifiers)) {
            if (identifier instanceof Deprecable) {
                if (!((Deprecable) identifier).isDeprecated()) {
                    return false;
                }
                isDeprecated = true;
            }
        }
        return isDeprecated;
    }

    /**
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the given string according heuristic rules.
     * The default implementation returns {@code true} if the given {@code name} is equal,
     * ignoring aspects documented below, to one of the following names:
     *
     * <ul>
     *   <li>The {@linkplain #getName() primary name}'s {@linkplain NamedIdentifier#getCode() code}
     *       (without {@linkplain NamedIdentifier#getCodeSpace() codespace}).</li>
     *   <li>Any {@linkplain #getAlias() alias}'s {@linkplain NamedIdentifier#tip() tip}
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
     * {@section Usage}
     * This method is invoked by SIS when comparing in {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} mode
     * two objects that can be differentiated only by some identifier (name or alias), like
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis coordinate system axes},
     * {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum},
     * {@linkplain org.apache.sis.parameter.AbstractParameterDescriptor parameters} and
     * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation methods}.
     * See {@link #equals(Object, ComparisonMode)} for more information.
     *
     * <p>This method is also invoked when searching a parameter or operation method for a given name.
     * For example the same projection is known as {@code "Mercator (variant A)"} (the primary name according EPSG)
     * and {@code "Mercator (1SP)"} (the legacy name prior EPSG 7.6). Since the later is still in frequent use, SIS
     * accepts it as an alias of the <cite>Mercator (variant A)</cite> projection.</p>
     *
     * {@section Overriding by subclasses}
     * Some subclasses add more flexibility to the comparisons:
     * <ul>
     *   <li>{@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#isHeuristicMatchForName(String)
     *       Comparisons of coordinate system axis names} consider {@code "Lat"}, {@code "Latitude"} and
     *       {@code "Geodetic latitude"} as synonymous, and likewise for longitude.</li>
     *   <li>{@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#isHeuristicMatchForName(String)
     *       Comparisons of geodetic datum names} ignore the {@code "D_"} prefix, if any.
     *       This prefix appears in ESRI datum name (e.g. {@code "D_WGS_1984"}).</li>
     * </ul>
     *
     * {@section Future evolutions}
     * This method implements heuristic rules learned from experience while trying to provide inter-operability
     * with different data producers. Those rules may be adjusted in any future SIS version according experience
     * gained while working with more data producers.
     *
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     *
     * @see IdentifiedObjects#isHeuristicMatchForName(IdentifiedObject, String)
     */
    public boolean isHeuristicMatchForName(final String name) {
        return IdentifiedObjects.isHeuristicMatchForName(this, alias, name);
    }

    /**
     * Compares this object with the specified object for equality.
     * The strictness level is controlled by the second argument:
     *
     * <ul>
     *   <li>If {@code mode} is {@link ComparisonMode#STRICT STRICT}, then this method verifies if the two
     *       objects are of the same {@linkplain #getClass() class} and compares all public properties,
     *       including SIS-specific (non standard) properties.</li>
     *   <li>If {@code mode} is {@link ComparisonMode#BY_CONTRACT}, then this method verifies if the the two
     *       object implements the same {@linkplain #getInterface() GeoAPI interface} and compares all properties
     *       defined by that interface ({@linkplain #getName() name}, {@linkplain #getRemarks() remarks},
     *       {@linkplain #getIdentifiers() identifiers}, <i>etc</i>).</li>
     *   <li>If {@code mode} is {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA},
     *       then this method compares only the properties needed for computing transformations.
     *       In other words, {@code sourceCRS.equals(targetCRS, IGNORE_METADATA)} returns {@code true}
     *       if the transformation from {@code sourceCRS} to {@code targetCRS} would be the
     *       identity transform, no matter what {@link #getName()} said.</li>
     * </ul>
     *
     * {@section Exceptions to the above rules}
     * Some subclasses (especially
     * {@link org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis},
     * {@link org.apache.sis.referencing.datum.AbstractDatum} and
     * {@link org.apache.sis.parameter.AbstractParameterDescriptor}) will compare the
     * {@linkplain #getName() name} even in {@code IGNORE_METADATA} mode,
     * because objects of those types with different names have completely different meaning.
     * For example nothing differentiate the {@code "semi_major"} and {@code "semi_minor"} parameters except the name.
     * The name comparison may be lenient however, i.e. the rules may accept a name matching an alias.
     * See {@link #isHeuristicMatchForName(String)} for more information.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @see #computeHashCode()
     * @see org.apache.sis.util.Utilities#deepEquals(Object, Object, ComparisonMode)
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == null) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                if (getClass() != object.getClass()) {
                    return false;
                }
                final AbstractIdentifiedObject that = (AbstractIdentifiedObject) object;
                /*
                 * If the hash codes were cached for both objects, opportunistically compare them.
                 * This is an efficient way to quickly check if the two objects are different
                 * before the more extensive check below.
                 */
                if (mode == ComparisonMode.STRICT) {
                    final int tc = hashCode;
                    if (tc != 0) {
                        final int oc = that.hashCode;
                        if (oc != 0 && tc != oc) {
                            return false;
                        }
                    }
                }
                return Objects.equals(name, that.name) &&
                       nonNull(alias).equals(nonNull(that.alias)) &&
                       nonNull(identifiers).equals(nonNull(that.identifiers)) &&
                       Objects.equals(remarks, that.remarks);
            }
            case BY_CONTRACT: {
                if (!implementsSameInterface(object)) {
                    return false;
                }
                final IdentifiedObject that = (IdentifiedObject) object;
                return deepEquals(getName(),        that.getName(),        mode) &&
                       deepEquals(getAlias(),       that.getAlias(),       mode) &&
                       deepEquals(getIdentifiers(), that.getIdentifiers(), mode) &&
                       deepEquals(getRemarks(),     that.getRemarks(),     mode);
            }
            case IGNORE_METADATA:
            case APPROXIMATIVE:
            case DEBUG: {
                return implementsSameInterface(object);
            }
            default: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownEnumValue_1, mode));
            }
        }
    }

    /**
     * Returns {@code true} if the given object implements the same GeoAPI interface than this object.
     */
    private boolean implementsSameInterface(final Object object) {
        final Class<? extends IdentifiedObject> type = getInterface();
        if (object instanceof AbstractIdentifiedObject) {
            return ((AbstractIdentifiedObject) object).getInterface() == type;
        }
        /*
         * Fallback for non-SIS implementations.
         */
        if (type.isInstance(object)) {
            final Class<? extends IdentifiedObject>[] t = Classes.getLeafInterfaces(object.getClass(), type);
            if (t.length == 1 && t[0] == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares the specified object with this object for equality.
     * This method is implemented as below (omitting assertions):
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
     *
     * Subclasses shall override {@link #equals(Object, ComparisonMode)} instead than this method.
     *
     * @param  object The other object (may be {@code null}).
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        final boolean eq = equals(object, ComparisonMode.STRICT);
        // If objects are equal, then they must have the same hash code value.
        assert !eq || hashCode() == object.hashCode() : this;
        return eq;
    }

    /**
     * Returns a hash value for this identified object. Two {@code AbstractIdentifiedObject} instances
     * for which {@link #equals(Object)} returns {@code true} shall have the same hash code value, if
     * the hash codes are computed on the same JVM instance for both objects. The hash code value is
     * <em>not</em> guaranteed to be stable between different versions of the Apache SIS library, or
     * between libraries running on different JVM.
     *
     * {@section Implementation note}
     * This method invokes {@link #computeHashCode()} when first needed, then caches the result.
     * Subclasses shall override {@link #computeHashCode()} instead than this method.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    public final int hashCode() { // No need to synchronize; ok if invoked twice.
        int hash = hashCode;
        if (hash == 0) {
            hash = Numerics.hashCode(computeHashCode());
            if (hash == 0) {
                hash = -1;
            }
            hashCode = hash;
        }
        assert hash == -1 || hash == Numerics.hashCode(computeHashCode()) : hash;
        return hash;
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code when first needed.
     * This method is invoked at most once in normal execution, or an arbitrary amount of times if Java
     * assertions are enabled. The hash code value shall never change during the whole lifetime of this
     * object in a JVM. The hash code value does not need to be the same in two different executions of
     * the JVM.
     *
     * {@section Overriding}
     * Subclasses can override this method for using more properties in hash code calculation.
     * All {@code computeHashCode()} methods shall invoke {@code super.computeHashCode()},
     * <strong>not</strong> {@code hashCode()}. Example:
     *
     * {@preformat java
     *     &#64;Override
     *     protected long computeHashCode() {
     *         return super.computeHashCode() + 31 * Objects.hash(myProperties);
     *     }
     * }
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    protected long computeHashCode() {
        return Objects.hash(name, nonNull(alias), nonNull(identifiers), remarks) ^ getInterface().hashCode();
    }
}
