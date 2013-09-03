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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ThreadSafe;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.internal.util.Citations.iterator;
import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.util.CollectionsExt.nonEmpty;
import static org.apache.sis.internal.util.CollectionsExt.immutableSet;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@Immutable
@ThreadSafe
@XmlType(name="IdentifiedObjectType", propOrder={
    "identifier",
    "name"
})
public class AbstractIdentifiedObject extends FormattableObject implements IdentifiedObject,
        LenientComparable, Deprecable, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5173281694258483264L;

    /**
     * The name for this object or code. Should never be {@code null}.
     *
     * @see #getName()
     * @see #getIdentifier()
     */
    @XmlElement
    private final ReferenceIdentifier name;

    /**
     * An alternative name by which this object is identified, or {@code null} if none.
     * We must be prepared to handle either null or an empty set for "no alias" because
     * we may get both on unmarshalling.
     */
    private final Collection<GenericName> alias;

    /**
     * An identifier which references elsewhere the object's defining information.
     * Alternatively an identifier by which this object can be referenced.
     *
     * <p>We must be prepared to handle either null or an empty set for
     * "no identifiers" because we may get both on unmarshalling.</p>
     *
     * @see #getIdentifiers()
     * @see #getIdentifier()
     */
    private final Set<ReferenceIdentifier> identifiers;

    /**
     * Comments on or information about this object, or {@code null} if none.
     */
    private final InternationalString remarks;

    /**
     * The cached hash code value, or 0 if not yet computed. This field is calculated only when
     * first needed. We do not declare it {@code volatile} because it is not a big deal if this
     * field is calculated many time, and the same value should be produced by all computations.
     * The only possible outdated value is 0, which is okay.
     */
    private transient int hashCode;

    /**
     * Constructs a new identified object with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param object The object to shallow copy.
     */
    public AbstractIdentifiedObject(final IdentifiedObject object) {
        ensureNonNull("object", object);
        name        =          object.getName();
        alias       = nonEmpty(object.getAlias()); // Favor null for empty set in case it is not Collections.EMPTY_SET
        identifiers = nonEmpty(object.getIdentifiers());
        remarks     =          object.getRemarks();
    }

    /**
     * Constructs an object from a set of properties. Keys are strings from the table below.
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
     *     <td>{@link String} or {@link ReferenceIdentifier}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link CharSequence}, {@link GenericName} or an array of those</td>
     *     <td>{@link #getAlias()}</td>
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
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or <code>{@linkplain ReferenceIdentifier}[]</code></td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link String} or {@link InternationalString}</td>
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
     * The {@code gml:id}, which is mandatory. The current implementation searches for the first identifier,
     * regardless its authority. If no identifier is found, then the name is used.
     * If no name is found (which should not occur for valid objects), then this method returns {@code null}.
     *
     * <p>When an identifier has been found, this method returns the concatenation of its code space with its code,
     * <em>without separator</em>. For example this method may return {@code "EPSG4326"}, not {@code "EPSG:4326"}.</p>
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
        Iterator<ReferenceIdentifier> it = iterator(identifiers);
        do {
            final ReferenceIdentifier identifier;
            if (it != null && it.hasNext()) {
                identifier = it.next();
            } else {
                it = null;
                identifier = name;
            }
            if (identifier != null) {
                boolean codeSpace = true;
                do { // Executed exactly twice: once for codespace, then once for code.
                    final String part = codeSpace ? identifier.getCodeSpace() : identifier.getCode();
                    if (part != null) {
                        /*
                         * Found a codespace (in the first iteration) or a code (in the second iteration).
                         * Append to the buffer only the characters that are valid for a Unicode identifier.
                         */
                        for (int i=0; i<part.length();) {
                            final int c = part.codePointAt(i);
                            if (id.length() == 0 ? Character.isUnicodeIdentifierStart(c)
                                                 : Character.isUnicodeIdentifierPart(c))
                            {
                                id.appendCodePoint(c);
                            }
                            i += Character.charCount(c);
                        }
                    }
                } while ((codeSpace = !codeSpace) == false);
                if (id.length() != 0) {
                    /*
                     * TODO: If we want to check for ID uniqueness or any other condition before to accept the ID,
                     * we would do that here. If the ID is rejected, then we just need to clear the buffer and let
                     * the iteration continue the search for an other ID.
                     */
                    return id.toString();
                }
            }
        } while (it != null);
        return null;
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
     * Returns the first identifier found, or {@code null} if none.
     * This method is invoked by JAXB at marshalling time.
     *
     * @see #name
     */
    @XmlElement(name = "identifier")
    final ReferenceIdentifier getIdentifier() {
        final Iterator<ReferenceIdentifier> it = iterator(identifiers);
        return (it != null && it.hasNext()) ? it.next() : null;
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
     * one {@linkplain #getAlias() alias} matches the specified string.
     * This method returns {@code true} if the given name is equal to one of the following names,
     * regardless of any authority:
     *
     * <ul>
     *   <li>The {@linkplain #getName() primary name} of this object.</li>
     *   <li>The {@linkplain org.opengis.util.GenericName#toFullyQualifiedName() fully qualified name} of an alias.</li>
     *   <li>The {@linkplain org.opengis.util.ScopedName#tail() tail} of an alias.</li>
     *   <li>The tail of the previous tail, recursively up to the {@linkplain org.opengis.util.ScopedName#tip() tip}.</li>
     * </ul>
     *
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     *
     * @see IdentifiedObjects#nameMatches(IdentifiedObject, String)
     */
    public boolean nameMatches(final String name) {
        return IdentifiedObjects.nameMatches(this, alias, name);
    }

    /**
     * Compares this object with the specified object for equality.
     * The strictness level is controlled by the second argument:
     *
     * <ul>
     *   <li>If {@code mode} is {@link ComparisonMode#STRICT STRICT}, then all available properties
     *       are compared including {@linkplain #getName() name}, {@linkplain #getRemarks() remarks},
     *       {@linkplain #getIdentifiers() identifiers code}, <i>etc.</i></li>
     *   <li>If {@code mode} is {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA},
     *       then this method compares only the properties needed for computing transformations.
     *       In other words, {@code sourceCRS.equals(targetCRS, IGNORE_METADATA)} returns {@code true}
     *       if the transformation from {@code sourceCRS} to {@code targetCRS} is likely to be the
     *       identity transform, no matter what {@link #getName()} said.</li>
     * </ul>
     *
     * {@section Exceptions to the above rules}
     * Some subclasses (especially {@link org.apache.sis.referencing.datum.AbstractDatum}
     * and {@link org.apache.sis.parameter.AbstractParameterDescriptor}) will test for the
     * {@linkplain #getName() name}, since objects with different name have completely
     * different meaning. For example nothing differentiate the {@code "semi_major"} and
     * {@code "semi_minor"} parameters except the name. The name comparison may be loose
     * however, i.e. we may accept a name matching an alias.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     *
     * @see #hashCode(ComparisonMode)
     * @see org.apache.sis.util.Utilities#deepEquals(Object, Object, ComparisonMode)
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == null) {
            return false;
        }
        if (getClass() == object.getClass()) {
            /*
             * If the classes are the same, then the hash codes should be computed in the same
             * way. Since those codes are cached, this is an efficient way to quickly check if
             * the two objects are different. Note that using the hash codes for comparisons
             * that ignore metadata is okay only if the implementation note described in the
             * 'computeHashCode()' javadoc hold (metadata not used in hash code computation).
             */
            if (mode.ordinal() < ComparisonMode.APPROXIMATIVE.ordinal()) {
                final int tc = hashCode;
                if (tc != 0) {
                    final int oc = ((AbstractIdentifiedObject) object).hashCode;
                    if (oc != 0 && tc != oc) {
                        return false;
                    }
                }
            }
        } else {
            if (mode == ComparisonMode.STRICT) { // Same classes was required for this mode.
                return false;
            }
            if (!(object instanceof IdentifiedObject)) {
                return false;
            }
        }
        switch (mode) {
            case STRICT: {
                final AbstractIdentifiedObject that = (AbstractIdentifiedObject) object;
                return Objects.equals(        name,                 that.name)         &&
                       Objects.equals(nonNull(alias),       nonNull(that.alias))       &&
                       Objects.equals(nonNull(identifiers), nonNull(that.identifiers)) &&
                       Objects.equals(        remarks,              that.remarks);
            }
            case BY_CONTRACT: {
                final IdentifiedObject that = (IdentifiedObject) object;
                return deepEquals(        getName(),                 that.getName(),         mode) &&
                       deepEquals(nonNull(getAlias()),       nonNull(that.getAlias()),       mode) &&
                       deepEquals(nonNull(getIdentifiers()), nonNull(that.getIdentifiers()), mode) &&
                       deepEquals(        getRemarks(),              that.getRemarks(),      mode);
            }
            case IGNORE_METADATA:
            case APPROXIMATIVE:
            case DEBUG: {
                return true;
            }
            default: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownEnumValue_1, mode));
            }
        }
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     * This method accepts only the following enumeration values:
     *
     * <ul>
     *   <li>{@link ComparisonMode#STRICT} (the default): this method may use any property,
     *       including implementation-specific ones if any, at implementation choice.</li>
     *   <li>{@link ComparisonMode#BY_CONTRACT}: this method can use any property defined
     *       in the implemented interface (typically a GeoAPI interface).</li>
     *   <li>{@link ComparisonMode#IGNORE_METADATA}: this method ignores the metadata that do not affect
     *       coordinate operations. By default, the ignored properties are the {@linkplain #getName() name},
     *       {@linkplain #getIdentifiers() identifiers} and {@linkplain #getRemarks() remarks}.
     *       However subclasses may ignore a different list of properties.</li>
     * </ul>
     *
     * In the later case, two identified objects will return the same hash value if they are equal in the sense of
     * <code>{@linkplain #equals(Object, ComparisonMode) equals}(object, {@linkplain ComparisonMode#IGNORE_METADATA})</code>.
     * This feature allows users to implement metadata-insensitive {@link java.util.HashMap}.
     *
     * @param  mode Specifies the set of properties that can be used for hash code computation.
     * @return The hash code value. This value may change between different execution of the Apache SIS library.
     * @throws IllegalArgumentException If the given {@code mode} is not one of {@code STRICT}, {@code BY_CONTRACT}
     *         or {@code IGNORE_METADATA} enumeration values.
     */
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        int code = (int) serialVersionUID;
        switch (mode) {
            case STRICT: {
                code ^= Objects.hash(name, nonNull(alias), nonNull(identifiers), remarks);
                break;
            }
            case BY_CONTRACT: {
                code ^= Objects.hash(getName(), getAlias(), getIdentifiers(), getRemarks());
                break;
            }
            case IGNORE_METADATA: {
                break;
            }
            default: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "mode", mode));
            }
        }
        return code;
    }

    /**
     * Compares the specified object with this object for equality.
     * This method is implemented as below (omitting assertions):
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
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
     * Returns a hash value for this identified object.
     * This method invokes <code>{@linkplain #hashCode(ComparisonMode) hashCode}(ComparisonMode.STRICT)</code>
     * when first needed and caches the value for future invocations.
     * Subclasses shall override {@link #hashCode(ComparisonMode)} instead than this method.
     *
     * @return The hash code value. This value may change between different execution of the Apache SIS library.
     */
    @Override
    public final int hashCode() { // No need to synchronize; ok if invoked twice.
        int hash = hashCode;
        if (hash == 0) {
            hash = hashCode(ComparisonMode.STRICT);
            if (hash == 0) {
                hash = -1;
            }
            hashCode = hash;
        }
        assert hash == -1 || hash == hashCode(ComparisonMode.STRICT) : this;
        return hash;
    }
}
