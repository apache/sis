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
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ThreadSafe;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.util.CollectionsExt.immutableSet;


/**
 * A base class for metadata applicable to reference system objects.
 * {@code IdentifiedObject} instances are created in two main ways:
 *
 * <ul>
 *   <li>When {@link AuthorityFactory} is used to create an object, the {@linkplain ReferenceIdentifier#getAuthority()
 *       authority} and {@linkplain ReferenceIdentifier#getCode() authority code} values are set to the authority name
 *       of the factory object, and the authority code supplied by the client, respectively.</li>
 *   <li>When {@link ObjectFactory} creates an object, the {@linkplain #getName() name} is set to the value supplied
 *       by the client and all of the other metadata items are left empty.</li>
 * </ul>
 *
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Applications should instead instantiate the most specific subclass having a name starting by {@code Default}.
 * However exceptions to this rule may occur when it is not possible to identify the exact type.
 *
 * {@example It is sometime not possible to infer the exact coordinate system from version 1 of
 *           <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
 *           Known Text</cite></a>, for example when parsing a <code>LOCAL_CS</code> element. In such exceptional
 *           situation, a plain <code>AbstractCS</code> object may be instantiated.}
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
     */
    @XmlElement
    private final ReferenceIdentifier name;

    /**
     * An alternative name by which this object is identified.
     */
    private final Collection<GenericName> alias;

    /**
     * An identifier which references elsewhere the object's defining information.
     * Alternatively an identifier by which this object can be referenced.
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
        name        =         object.getName();
        alias       = nonNull(object.getAlias());
        identifiers = nonNull(object.getIdentifiers());
        remarks     =         object.getRemarks();
        ensureNonNull("object.name", name);
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
     *     <th>Value given to</th>
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
     *     <td>{@link ReferenceIdentifier#getAuthority()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#CODESPACE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link ReferenceIdentifier#getCodeSpace()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link ReferenceIdentifier#getVersion()} on the {@linkplain #getName() name}</td>
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
     * <p>Note that the {@code "authority"} and {@code "version"} properties are ignored if the
     * {@code "name"} property is already a {@link Citation} object instead than a {@link String}.</p>
     *
     * @param  properties The properties to be given to this identified object.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    public AbstractIdentifiedObject(final Map<String,?> properties) throws IllegalArgumentException {
        this(properties, null, null);
    }

    /**
     * Constructs an object from a set of properties and copy unrecognized properties in the given map.
     * The {@code properties} argument is treated as in the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) one argument constructor}.
     * All properties unknown to this {@code AbstractIdentifiedObject} constructor are copied
     * in the {@code subProperties} map.
     *
     * <p>If {@code localizables} is non-null, then all keys listed in this argument are treated as localizable one
     * (i.e. may have a suffix like {@code "_fr"}, {@code "_de"}, <i>etc.</i>). Localizable properties are stored in
     * the {@code subProperties} map as {@link InternationalString} objects.</p>
     *
     * @param  properties    The properties to be given to this identified object.
     * @param  subProperties The map in which to copy unrecognized properties, or {@code null} if none.
     * @param  localizables  Optional list of localized properties, or {@code null} if none.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    protected AbstractIdentifiedObject(final Map<String,?>      properties,
                                       final Map<String,Object> subProperties,
                                       final String[]           localizables)
            throws IllegalArgumentException
    {
        ensureNonNull("properties", properties);

        // -------------------------------------
        // "name": String or ReferenceIdentifier
        // -------------------------------------
        Object value = properties.get(NAME_KEY);
        if (value == null || value instanceof String) {
            name = new NamedIdentifier(PropertiesConverter.convert(properties));
        } else {
            ensureCanCast(NAME_KEY, ReferenceIdentifier.class, value);
            name = (ReferenceIdentifier) value;
        }

        // -------------------------------------------------------------------
        // "alias": CharSequence, CharSequence[], GenericName or GenericName[]
        // -------------------------------------------------------------------
        value = properties.get(ALIAS_KEY);
        try {
            alias = nonNull(immutableSet(Types.toGenericNames(value, null)));
        } catch (ClassCastException e) {
            throw new InvalidParameterValueException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                    ALIAS_KEY, value.getClass()), e, ALIAS_KEY, value);
        }

        // -----------------------------------------------------------
        // "identifiers": ReferenceIdentifier or ReferenceIdentifier[]
        // -----------------------------------------------------------
        value = properties.get(IDENTIFIERS_KEY);
        if (value instanceof ReferenceIdentifier) {
            identifiers = Collections.singleton((ReferenceIdentifier) value);
        } else {
            ensureCanCast(IDENTIFIERS_KEY, ReferenceIdentifier[].class, value);
            identifiers = nonNull(immutableSet((ReferenceIdentifier[]) value));
        }

        // ----------------------------------------
        // "remarks": String or InternationalString
        // ----------------------------------------
        value = properties.get(REMARKS_KEY);
        ensureCanCast(REMARKS_KEY, CharSequence.class, value);
        remarks = Types.toInternationalString((CharSequence) value);
    }

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return The primary name.
     *
     * @see Citations#getName(IdentifiedObject, Citation)
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
        return alias;
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
        return identifiers;
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

    @Override
    public boolean isDeprecated() {
        return false;
    }

    public boolean nameMatches(final String name) {
        return false;
    }

    @Override
    public boolean equals(Object other, ComparisonMode mode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
