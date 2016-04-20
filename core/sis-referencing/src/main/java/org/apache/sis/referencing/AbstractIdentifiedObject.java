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
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.referencing.Code;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.util.CollectionsExt.nonEmpty;
import static org.apache.sis.internal.util.CollectionsExt.immutableSet;

// Branch-dependent imports
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.jdk7.Objects;


/**
 * Base class for objects identified by a name or a code. Those objects are typically
 * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic datum}   (e.g. <cite>"World Geodetic System 1984"</cite>),
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} (e.g. <cite>"WGS 84 / World Mercator"</cite>) or
 * {@linkplain org.apache.sis.referencing.operation.DefaultProjection map projection}  (e.g. <cite>"Mercator (variant A)"</cite>).
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
 * <div class="section">Instantiation</div>
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Applications should instead instantiate the most specific subclass having a name starting by {@code Default}.
 * However exceptions to this rule may occur when it is not possible to identify the exact type.
 *
 * <div class="note"><b>Example:</b>
 * It is sometime not possible to infer the exact coordinate system type from
 * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">version 1
 * of <cite>Well Known Text format</cite></a>, for example when parsing a {@code LOCAL_CS} element.
 * In such exceptional situation, a plain {@code AbstractCS} object may be instantiated.</div>
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
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable if the {@link Citation}, {@link ReferenceIdentifier}, {@link GenericName} and
 * {@link InternationalString} instances given to the constructor are also immutable. Most SIS subclasses and
 * related classes are immutable under similar conditions. This means that unless otherwise noted in the javadoc,
 * {@code IdentifiedObject} instances created using only SIS factories and static constants can be shared by many
 * objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlType(name = "IdentifiedObjectType", propOrder = {
    "description",
    "identifier",
    "names",
    "remarks"
})
@XmlSeeAlso({
    org.apache.sis.referencing.crs.AbstractCRS.class,
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
     * Optional key which can be given to the {@linkplain #AbstractIdentifiedObject(Map) constructor} for specifying
     * the locale to use for producing error messages. Notes:
     *
     * <ul>
     *   <li>The locale is not stored in any {@code AbstractIdentifiedObject} property;
     *       its value is ignored if no error occurred at construction time.</li>
     *   <li>The locale is used on a <cite>best effort</cite> basis;
     *       not all error messages may be localized.</li>
     * </ul>
     */
    public static final String LOCALE_KEY = Errors.LOCALE_KEY;

    /**
     * Optional key which can be given to the {@linkplain #AbstractIdentifiedObject(Map) constructor}
     * for specifying the object is deprecated. If deprecated, then the replacement should be specified
     * in the {@linkplain #getRemarks() remarks}.
     *
     * <div class="note"><b>Example:</b> "superseded by code XYZ".</div>
     *
     * @since 0.6
     */
    public static final String DEPRECATED_KEY = "deprecated";

    /**
     * The name for this object or code. Shall never be {@code null}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link Names#add(Identifier)}.</p>
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
     * This field is modified only at unmarshalling time by {@link Names#add(Identifier)}.</p>
     */
    private Collection<GenericName> alias;

    /**
     * An identifier which references elsewhere the object's defining information.
     * Alternatively an identifier by which this object can be referenced.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setIdentifier(Code)}</p>
     *
     * @see #getIdentifiers()
     * @see #getIdentifier()
     */
    private Set<ReferenceIdentifier> identifiers;

    /**
     * Comments on or information about this object, or {@code null} if none.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setRemarks(InternationalString)}</p>
     *
     * @see #getRemarks()
     */
    private InternationalString remarks;

    /**
     * {@code true} if this object is deprecated.
     *
     * @since 0.6
     */
    private final boolean deprecated;

    /**
     * The cached hash code value, or 0 if not yet computed. This field is calculated only when
     * first needed. We do not declare it {@code volatile} because it is not a big deal if this
     * field is calculated many time, and the same value should be produced by all computations.
     * The only possible outdated value is 0, which is okay.
     */
    private transient int hashCode;

    /**
     * Constructs an object from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the
     * {@value org.opengis.referencing.IdentifiedObject#NAME_KEY} or
     * {@value org.opengis.metadata.Identifier#CODE_KEY} key.
     * Other properties listed in the table below are optional.
     * In particular, {@code "authority"}, {@code "code"}, {@code "codespace"} and {@code "version"}
     * are convenience properties for building a name, and are ignored if the {@code "name"} property
     * is already a {@link ReferenceIdentifier} object instead than a {@link String}.
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
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
     *     <td>"description"</td>
     *     <td>{@link String}</td>
     *     <td>{@link NamedIdentifier#getDescription()} on the {@linkplain #getName() name}</td>
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
     *   <tr>
     *     <td>{@value #DEPRECATED_KEY}</td>
     *     <td>{@link Boolean}</td>
     *     <td>{@link #isDeprecated()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #LOCALE_KEY}</td>
     *     <td>{@link Locale}</td>
     *     <td>(none)</td>
     *   </tr>
     * </table>
     *
     * <div class="section">Localization</div>
     * All localizable attributes like {@code "remarks"} may have a language and country code suffix.
     * For example the {@code "remarks_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
     * the {@code "remarks_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     * They are convenience properties for building the {@code InternationalString} value.
     *
     * <p>The {@code "locale"} property applies only in case of exception for formatting the error message, and
     * is used only on a <cite>best effort</cite> basis. The locale is discarded after successful construction
     * since localizations are applied by the {@link InternationalString#toString(Locale)} method.</p>
     *
     * <div class="section">Properties map versus explicit arguments</div>
     * Generally speaking, information provided in the {@code properties} map are considered ignorable metadata
     * while information provided in explicit arguments to the sub-class constructors have an impact on coordinate
     * transformation results. See {@link #equals(Object, ComparisonMode)} for more information.
     *
     * @param  properties The properties to be given to this identified object.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    public AbstractIdentifiedObject(final Map<String,?> properties) throws IllegalArgumentException {
        ensureNonNull("properties", properties);

        // ----------------------------
        // "name": String or Identifier
        // ----------------------------
        Object value = properties.get(NAME_KEY);
        if (value == null || value instanceof String) {
            if (value == null && properties.get(Identifier.CODE_KEY) == null) {
                throw new IllegalArgumentException(Errors.getResources(properties)
                        .getString(Errors.Keys.MissingValueForProperty_1, NAME_KEY));
            }
            name = new NamedIdentifier(PropertiesConverter.convert(properties));
        } else if (value instanceof ReferenceIdentifier) {
            name = (ReferenceIdentifier) value;
        } else {
            throw illegalPropertyType(properties, NAME_KEY, value);
        }

        // -------------------------------------------------------------------
        // "alias": CharSequence, CharSequence[], GenericName or GenericName[]
        // -------------------------------------------------------------------
        value = properties.get(ALIAS_KEY);
        final GenericName[] names;
        try {
            /*
             * Note: DefaultFactories.forBuildin(Class) filters the implementations in order to guarantee an
             * Apache SIS implementation. We need to revisit this mechanism for a real "dependency injection"
             * mechanism in the future.
             */
            final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
            names = factory.toGenericNames(value);
        } catch (ClassCastException e) {
            throw (IllegalArgumentException) illegalPropertyType(properties, ALIAS_KEY, value).initCause(e);
        }
        alias = immutableSet(true, names);

        // -----------------------------------------
        // "identifiers": Identifier or Identifier[]
        // -----------------------------------------
        value = properties.get(IDENTIFIERS_KEY);
        if (value == null) {
            identifiers = null;
        } else if (value instanceof ReferenceIdentifier) {
            identifiers = Collections.singleton((ReferenceIdentifier) value);
        } else if (value instanceof ReferenceIdentifier[]) {
            identifiers = immutableSet(true, (ReferenceIdentifier[]) value);
        } else {
            throw illegalPropertyType(properties, IDENTIFIERS_KEY, value);
        }

        // ----------------------------------------
        // "remarks": String or InternationalString
        // ----------------------------------------
        remarks = Types.toInternationalString(properties, REMARKS_KEY);

        // ---------------------
        // "deprecated": Boolean
        // ---------------------
        value = properties.get(DEPRECATED_KEY);
        if (value == null) {
            deprecated = false;
        } else if (value instanceof Boolean) {
            deprecated = (Boolean) value;
        } else {
            throw illegalPropertyType(properties, DEPRECATED_KEY, value);
        }
    }

    /**
     * Returns the exception to be thrown when a property is of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(
            final Map<String,?> properties, final String key, final Object value)
    {
        return new IllegalArgumentException(Errors.getResources(properties)
                .getString(Errors.Keys.IllegalPropertyValueClass_2, key, value.getClass()));
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
        deprecated  = (object instanceof Deprecable) ? ((Deprecable) object).isDeprecated() : false;
    }

    /**
     * Returns a SIS identified object implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
     *       {@link org.opengis.referencing.crs.CoordinateReferenceSystem},
     *       {@link org.opengis.referencing.cs.CoordinateSystem},
     *       {@link org.opengis.referencing.cs.CoordinateSystemAxis},
     *       {@link org.opengis.referencing.datum.Datum},
     *       {@link org.opengis.referencing.datum.Ellipsoid},
     *       {@link org.opengis.referencing.datum.PrimeMeridian},
     *       {@link org.opengis.referencing.operation.OperationMethod},
     *       {@link org.opengis.referencing.operation.CoordinateOperation},
     *       {@link org.opengis.parameter.ParameterDescriptor} or
     *       {@link org.opengis.parameter.ParameterDescriptorGroup},
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
     * <div class="section">Invariants</div>
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
        return nonNull(alias);          // Needs to be null-safe because we may have a null value on unmarshalling.
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
        return nonNull(identifiers);    // Needs to be null-safe because we may have a null value on unmarshalling.
    }

    /**
     * Returns a narrative explanation of the role of this object.
     *
     * <div class="section">Default value</div>
     * The default implementation returns the
     * {@linkplain org.apache.sis.metadata.iso.ImmutableIdentifier#getDescription() description}
     * provided by this object's {@linkplain #getName() name}.
     *
     * @return A narrative explanation of the role of this object, or {@code null} if none.
     *
     * @see org.apache.sis.metadata.iso.ImmutableIdentifier#getDescription()
     *
     * @since 0.6
     */
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        final ReferenceIdentifier name = getName();
        if (name instanceof ImmutableIdentifier) {
            return ((ImmutableIdentifier) name).getDescription();
        }
        if (name instanceof DefaultIdentifier) {
            return ((DefaultIdentifier) name).getDescription();
        }
        return null;
    }

    /**
     * Returns comments on or information about this object, including data source information.
     * If this object {@linkplain #isDeprecated() is deprecated}, then the remarks should give
     * indication about the replacement (e.g. <cite>"superceded by …"</cite>).
     *
     * @return The remarks, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "remarks")
    public InternationalString getRemarks() {
        return remarks;
    }

    /**
     * Returns {@code true} if this object is deprecated. Deprecated objects exist in some
     * {@linkplain org.opengis.referencing.AuthorityFactory authority factories} like the EPSG database.
     * If this method returns {@code true}, then the {@linkplain #getRemarks() remarks} should give
     * indication about the replacement (e.g. <cite>"superceded by …"</cite>).
     *
     * @return {@code true} if this object is deprecated.
     */
    @Override
    public boolean isDeprecated() {
        return deprecated;
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
     * <div class="section">Usage</div>
     * This method is invoked by SIS when comparing in {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} mode
     * two objects that can be differentiated only by some identifier (name or alias), like
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis coordinate system axes},
     * {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum},
     * {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor parameters} and
     * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation methods}.
     * See {@link #equals(Object, ComparisonMode)} for more information.
     *
     * <p>This method is also invoked when searching a parameter or operation method for a given name.
     * For example the same projection is known as {@code "Mercator (variant A)"} (the primary name according EPSG)
     * and {@code "Mercator (1SP)"} (the legacy name prior EPSG 7.6). Since the later is still in frequent use, SIS
     * accepts it as an alias of the <cite>Mercator (variant A)</cite> projection.</p>
     *
     * <div class="section">Overriding by subclasses</div>
     * Some subclasses add more flexibility to the comparisons:
     * <ul>
     *   <li>{@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#isHeuristicMatchForName(String)
     *       Comparisons of coordinate system axis names} consider {@code "Lat"}, {@code "Latitude"} and
     *       {@code "Geodetic latitude"} as synonymous, and likewise for longitude.</li>
     *   <li>{@linkplain org.apache.sis.referencing.datum.AbstractDatum#isHeuristicMatchForName(String)
     *       Comparisons of datum names} ignore the {@code "D_"} prefix, if any.
     *       This prefix appears in ESRI datum name (e.g. {@code "D_WGS_1984"}).</li>
     *   <li>{@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#isHeuristicMatchForName(String)
     *       Comparisons of geodetic datum names} may ignore the prime meridian name, if any.
     *       Example: <cite>"(Paris)"</cite> in <cite>"Nouvelle Triangulation Française (Paris)"</cite>.</li>
     * </ul>
     *
     * <div class="section">Future evolutions</div>
     * This method implements recommendations from the
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#139">WKT 2 specification §B.5.2</a>,
     * together with heuristic rules learned from experience while trying to provide inter-operability
     * with different data producers. Those rules may be adjusted in any future SIS version according experience
     * gained while working with more data producers.
     *
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name or at least one alias matches the specified {@code name}.
     *
     * @see IdentifiedObjects#isHeuristicMatchForName(IdentifiedObject, String)
     * @see org.apache.sis.util.Characters.Filter#LETTERS_AND_DIGITS
     */
    public boolean isHeuristicMatchForName(final String name) {
        return NameToIdentifier.isHeuristicMatchForName(this.name, alias, name, NameToIdentifier.Simplifier.DEFAULT);
    }

    /**
     * Compares this object with the given object for equality.
     * The strictness level is controlled by the second argument,
     * from stricter to more permissive values:
     *
     * <table class="compact" summary="Description of comparison modes.">
     *   <tr><td>{@link ComparisonMode#STRICT STRICT}:</td>
     *        <td>Verifies if the two objects are of the same {@linkplain #getClass() class}
     *            and compares all public properties, including SIS-specific (non standard) properties.</td></tr>
     *   <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}:</td>
     *       <td>Verifies if the two objects implement the same {@linkplain #getInterface() GeoAPI interface}
     *           and compares all properties defined by that interface ({@linkplain #getName() name},
     *           {@linkplain #getIdentifiers() identifiers}, {@linkplain #getRemarks() remarks}, <i>etc</i>).
     *           The two objects do not need to be instances of the same implementation class
     *           and SIS-specific properties are ignored.</td></tr>
     *   <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}:</td>
     *       <td>Compares only the properties relevant to coordinate transformations. Generally speaking, the content
     *           of the {@code properties} map given at {@linkplain #AbstractIdentifiedObject(Map) construction time}
     *           is considered ignorable metadata while the explicit arguments given to the constructor (if any) are
     *           considered non-ignorable. Note that there is some exceptions to this rule of thumb — see
     *           <cite>When object name matter</cite> below.</td></tr>
     *   <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}:</td>
     *       <td>Same as {@code IGNORE_METADATA}, with some tolerance threshold on numerical values.</td></tr>
     *   <tr><td>{@link ComparisonMode#ALLOW_VARIANT ALLOW_VARIANT}:</td>
     *       <td>Same as {@code APPROXIMATIVE}, but ignores coordinate system axes.</td></tr>
     *   <tr><td>{@link ComparisonMode#DEBUG DEBUG}:</td>
     *        <td>Special mode for figuring out why two objects expected to be equal are not.</td></tr>
     * </table>
     *
     * The main guideline is that if {@code sourceCRS.equals(targetCRS, IGNORE_METADATA)} returns {@code true},
     * then the transformation from {@code sourceCRS} to {@code targetCRS} should be the identity transform
     * even if the two CRS do not have the same name.
     *
     * <div class="section">When object name matter</div>
     * Some subclasses (especially
     * {@link org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis},
     * {@link org.apache.sis.referencing.datum.AbstractDatum} and
     * {@link org.apache.sis.parameter.DefaultParameterDescriptor}) will compare the
     * {@linkplain #getName() name} even in {@code IGNORE_METADATA} mode,
     * because objects of those types with different names have completely different meaning.
     * For example nothing differentiate the {@code "semi_major"} and {@code "semi_minor"} parameters except the name.
     * The name comparison may be lenient however, i.e. the rules may accept a name matching an alias.
     * See {@link #isHeuristicMatchForName(String)} for more information.
     *
     * <div class="section">Conformance to the <code>equals(Object)</code> method contract</div>
     * {@link ComparisonMode#STRICT} is the only mode compliant with the {@link Object#equals(Object)} contract.
     * For all other modes, the comparison is not guaranteed to be <cite>symmetric</cite> neither
     * <cite>transitive</cite>. See {@link LenientComparable#equals(Object, ComparisonMode) LenientComparable}
     * for more information.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
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
            case ALLOW_VARIANT:
            case DEBUG: {
                return implementsSameInterface(object);
            }
            default: {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.UnknownEnumValue_2, ComparisonMode.class, mode));
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
     * <div class="section">Implementation note</div>
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
     * <div class="section">Overriding</div>
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

    /**
     * Formats the inner part of the <cite>Well Known Text</cite> (WKT) representation for this object.
     * The default implementation writes the following elements:
     *
     * <ul>
     *   <li>The object {@linkplain #getName() name}.</li>
     * </ul>
     *
     * Keywords and metadata (scope, extent, identifier and remarks) shall not be formatted here.
     * For example if this formattable element is for a {@code GeodeticCRS[…]} element,
     * then subclasses shall write the content starting at the insertion point shown below:
     *
     * <table class="compact" summary="WKT and Java code example.">
     * <tr>
     *   <th>WKT example</th>
     *   <th>Java code example</th>
     * </tr><tr><td>
     * {@preformat text
     *   GeodeticCRS["WGS 84", ID["EPSG", 4326]]
     *                       ↑
     *               (insertion point)
     * }
     * </td><td>
     * {@preformat java
     *     super.formatTo(formatter);
     *     // ... write the elements at the insertion point ...
     *     return "GeodeticCRS";
     * }
     * </td></tr></table>
     *
     * <div class="section">Formatting non-standard WKT</div>
     * If the implementation can not represent this object without violating some WKT constraints,
     * it can uses its own (non-standard) keywords but shall declare that it did so by invoking one
     * of the {@link Formatter#setInvalidWKT(IdentifiedObject, Exception) Formatter.setInvalidWKT(…)}
     * methods.
     *
     * <p>Alternatively, the implementation may also have no WKT keyword for this object.
     * In such case, this method shall return {@code null}.</p>
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return The {@linkplain org.apache.sis.io.wkt.KeywordCase#CAMEL_CASE CamelCase} keyword
     *         for the WKT element, or {@code null} if unknown.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#21">WKT 2 specification §7</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, ElementKind.forType(getClass()));
        return null;
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractIdentifiedObject() {
        deprecated = false;
    }

    /**
     * The {@code gml:id}, which is mandatory. The current implementation searches for the first identifier,
     * regardless its authority. If no identifier is found, then the name or aliases are used. If none of the
     * above is found (which should not occur for valid objects), then this method returns {@code null}.
     *
     * <p>If an identifier or a name has been found, this method returns the concatenation of the following
     * elements separated by hyphens:</p>
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
    @XmlSchemaType(name = "ID")
    @XmlAttribute(name = "id", namespace = Namespaces.GML, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    final String getID() {
        // Implementation is provided in the NameIterator class for reducing the size of
        // AbstractIdentifiedObject.class file in the common case where XML is not needed.
        return NameIterator.getID(Context.current(), this, name, alias, identifiers);
    }

    /**
     * Invoked by JAXB at unmarhalling time for specifying the value of the {@code gml:id} attribute.
     * That GML identifier is not actually stored in this {@code AbstractIdentifiedObject}
     * since we rather generate it dynamically from the ISO 19111 identifiers. But we still
     * need to declare that identifier to our unmarshaller context, in case it is referenced
     * from elsewhere in the XML document.
     */
    private void setID(final String id) {
        final Context context = Context.current();
        if (!Context.setObjectForID(context, this, id)) {
            Context.warningOccured(context, getClass(), "setID", Errors.class, Errors.Keys.DuplicatedIdentifier_1, id);
        }
    }

    /**
     * Returns a single element from the {@code Set<Identifier>} collection, or {@code null} if none.
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
    @XmlElement(required = true)
    final Code getIdentifier() {
        return Code.forIdentifiedObject(getClass(), identifiers);
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the identifier.
     */
    private void setIdentifier(final Code identifier) {
        if (identifiers == null) {
            if (identifier != null) {
                final ReferenceIdentifier id = identifier.getIdentifier();
                if (id != null) {
                    identifiers = Collections.singleton(id);
                }
            }
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractIdentifiedObject.class, "setIdentifier", "identifier");
        }
    }

    /**
     * Returns the {@link #name} and all aliases which are also instance of {@link Identifier}.
     * The later happen often in SIS implementation since many aliases are instance of {@link NamedIdentifier}.
     *
     * <p>The returned collection is <cite>live</cite>: adding elements in that collection will modify this
     * {@code AbstractIdentifiedObject} instance. This is needed for unmarshalling with JAXB and should not
     * be used in other context.</p>
     *
     * <div class="section">Why there is no <code>setNames(…)</code> method</div>
     * Some JAXB implementations never invoke setter method for collections. Instead they invoke the getter and
     * add directly the identifiers in the returned collection. Whether JAXB will perform or not a final call to
     * {@code setNames(…)} is JAXB-implementation dependent (JDK7 does but JDK6 and JDK8 early access do not).
     * It seems a more portable approach (at least for JAXB reference implementations) to design our class
     * without setter method, in order to have the same behavior on all supported JDK versions.
     *
     * @see <a href="https://java.net/jira/browse/JAXB-488">JAXB-488</a>
     */
    @XmlElement(name = "name", required = true)
    final Collection<ReferenceIdentifier> getNames() {
        return new Names();
    }

    /**
     * A writable view over the {@linkplain AbstractIdentifiedObject#getName() name} of the enclosing object followed
     * by all {@linkplain AbstractIdentifiedObject#getAlias() aliases} which are instance of {@link Identifier}.
     * Used by JAXB only at (un)marshalling time because GML merges the name and aliases in a single {@code <gml:name>}
     * property.
     *
     * <div class="section">Why we do not use {@code Identifier[]} array instead</div>
     * It would be easier to define a {@code getNames()} method returning all identifiers in an array, and let JAXB
     * invoke {@code setNames(Identifier[])} at unmarshalling time.  But methods expecting an array in argument are
     * invoked by JAXB only after the full element has been unmarshalled. For some {@code AbstractIdentifiedObject}
     * subclasses, this is too late. For example {@code DefaultOperationMethod} may need to know the operation name
     * before to parse the parameters.
     */
    private final class Names extends AbstractCollection<ReferenceIdentifier> {
        /**
         * Invoked by JAXB before to write in the collection at unmarshalling time.
         * Do nothing since our object is already empty.
         */
        @Override
        public void clear() {
        }

        /**
         * Returns the number of name and aliases that are instance of {@link Identifier}.
         */
        @Override
        public int size() {
            return NameIterator.count(AbstractIdentifiedObject.this);
        }

        /**
         * Returns an iterator over the name and aliases that are instance of {@link Identifier}.
         */
        @Override
        public Iterator<ReferenceIdentifier> iterator() {
            return new NameIterator(AbstractIdentifiedObject.this);
        }

        /**
         * Invoked by JAXB at unmarshalling time for each identifier. The first identifier will be taken
         * as the name and all other identifiers (if any) as aliases.
         *
         * <p>Some (but not all) JAXB implementations never invoke setter method for collections.
         * Instead they invoke {@link AbstractIdentifiedObject#getNames()} and add directly the identifiers
         * in the returned collection. Consequently this method must writes directly in the enclosing object.
         * See <a href="https://java.net/jira/browse/JAXB-488">JAXB-488</a> for more information.</p>
         */
        @Override
        public boolean add(final ReferenceIdentifier id) {
            if (NameIterator.isUnnamed(name)) {
                name = id;
            } else {
                /*
                 * Our Code and RS_Identifier implementations should always create NamedIdentifier instance,
                 * so the 'instanceof' check should not be necessary. But we do a paranoiac check anyway.
                 */
                final GenericName n = id instanceof GenericName ? (GenericName) id : new NamedIdentifier(id);
                if (alias == null) {
                    alias = Collections.singleton(n);
                } else {
                    /*
                     * This implementation is inefficient since each addition copies the array, but we rarely
                     * have more than two aliases.  This implementation is okay for a small number of aliases
                     * and ensures that the enclosing AbstractIdentifiedObject is unmodifiable except by this
                     * add(…) method.
                     *
                     * Note about alternative approaches
                     * ---------------------------------
                     * An alternative approach could be to use an ArrayList and replace it by an unmodifiable
                     * list only after unmarshalling (using an afterUnmarshal(Unmarshaller, Object) method),
                     * but we want to avoid Unmarshaller dependency (for reducing classes loading for users
                     * who are not interrested in XML) and it may actually be less efficient for the vast
                     * majority of cases where there is less than 3 aliases.
                     */
                    final int size = alias.size();
                    final GenericName[] names = alias.toArray(new GenericName[size + 1]);
                    names[size] = n;
                    alias = UnmodifiableArrayList.wrap(names);
                }
            }
            return true;
        }
    }

    /**
     * Invoked by JAXB for setting the remarks.
     *
     * @see #getRemarks()
     */
    private void setRemarks(final InternationalString value) {
        if (remarks == null) {
            remarks = value;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractIdentifiedObject.class, "setRemarks", "remarks");
        }
    }
}
