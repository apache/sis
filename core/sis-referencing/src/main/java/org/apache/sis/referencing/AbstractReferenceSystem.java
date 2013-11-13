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
import javax.xml.bind.annotation.XmlElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.iso.Types;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.collection.Containers.property;

// Related to JDK7
import java.util.Objects;


/**
 * Description of a spatial and temporal reference system used by a dataset.
 * This class inherits the {@linkplain #getName() name}, {@linkplain #getAlias() aliases},
 * {@linkplain #getIdentifiers() identifiers} and {@linkplain #getRemarks() remarks} from
 * the parent class, and adds the following information:
 *
 * <ul>
 *   <li>a {@linkplain #getDomainOfValidity() domain of validity}, the area for which the reference system is valid,</li>
 *   <li>a {@linkplain #getScope() scope}, which describes the domain of usage or limitation of usage.
 * </ul>
 *
 * {@section Instantiation}
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@Immutable
public class AbstractReferenceSystem extends AbstractIdentifiedObject implements ReferenceSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3337659819553899435L;

    /**
     * Area for which the (coordinate) reference system is valid.
     *
     * @see #getDomainOfValidity()
     */
    private final Extent domainOfValidity;

    /**
     * Description of domain of usage, or limitations of usage,
     * for which this (coordinate) reference system object is valid.
     *
     * @see #getScope()
     */
    @XmlElement(required = true)
    private final InternationalString scope;

    /**
     * Constructs a new reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param object The reference system to copy.
     */
    public AbstractReferenceSystem(final ReferenceSystem object) {
        super(object);
        domainOfValidity = object.getDomainOfValidity();
        scope            = object.getScope();
    }

    /**
     * Constructs a reference system from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link String} or {@link InternationalString}</td>
     *     <td>{@link #getScope}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
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
     * @param properties The properties to be given to this object.
     */
    public AbstractReferenceSystem(final Map<String,?> properties) {
        super(properties);
        domainOfValidity = property(properties, DOMAIN_OF_VALIDITY_KEY, Extent.class);
        scope = Types.toInternationalString(properties, SCOPE_KEY);
    }

    /**
     * Returns the region or timeframe in which this reference system is valid,
     * or {@code null} if unspecified.
     *
     * @return Area or region or timeframe in which this (coordinate) reference system is valid, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @Override
    public Extent getDomainOfValidity() {
        return domainOfValidity;
    }

    /**
     * Returns the domain or limitations of usage, or {@code null} if unspecified.
     *
     * @return Description of domain of usage, or limitations of usage, for which this
     *         (coordinate) reference system object is valid, or {@code null}.
     */
    @Override
    public InternationalString getScope() {
        return scope;
    }

    /**
     * Compares this reference system with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties are
     * compared including the {@linkplain #getDomainOfValidity() domain of validity} and
     * the {@linkplain #getScope() scope}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final AbstractReferenceSystem that = (AbstractReferenceSystem) object;
                    return Objects.equals(domainOfValidity, that.domainOfValidity) &&
                           Objects.equals(scope,            that.scope);
                }
                case BY_CONTRACT: {
                    if (!(object instanceof ReferenceSystem)) break;
                    final ReferenceSystem that = (ReferenceSystem) object;
                    return deepEquals(getDomainOfValidity(), that.getDomainOfValidity(), mode) &&
                           deepEquals(getScope(),            that.getScope(), mode);
                }
                default: {
                    // Domain of validity and scope are metadata, so they can be ignored.
                    return (object instanceof ReferenceSystem);
                }
            }
        }
        return false;
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     * If the given argument is {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}, then the
     * {@linkplain #getDomainOfValidity() domain of validity} and the {@linkplain #getScope() scope}
     * properties are ignored, in addition to other ignored properties documented in the
     * {@linkplain AbstractIdentifiedObject#hashCode(ComparisonMode) super-class}.
     *
     * @return The hash code value for the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        /*
         * The "^ (int) serialVersionUID" is an arbitrary change applied to the hash code value in order to
         * differentiate this ReferenceSystem implementation from implementations of other GeoAPI interfaces.
         */
        int code = super.hashCode(mode) ^ (int) serialVersionUID;
        switch (mode) {
            case STRICT: {
                code ^= Objects.hash(domainOfValidity, scope);
                break;
            }
            case BY_CONTRACT: {
                code ^= Objects.hash(getDomainOfValidity(), getScope());
                break;
            }
        }
        return code;
    }
}
