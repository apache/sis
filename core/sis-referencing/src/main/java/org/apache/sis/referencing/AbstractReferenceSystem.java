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
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.metadata.EX_Extent;
import org.apache.sis.internal.metadata.MetadataUtilities;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.collection.Containers.property;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Description of a spatial and temporal reference system used by a dataset.
 * Reference systems do not necessarily use coordinates. For example a reference system could use postal codes.
 * The specialized case of referencing by coordinates is handled by the
 * {@link org.apache.sis.referencing.crs.AbstractCRS} subclass.
 *
 * <p>This class inherits the {@linkplain #getName() name}, {@linkplain #getAlias() aliases},
 * {@linkplain #getIdentifiers() identifiers} and {@linkplain #getRemarks() remarks} from
 * the parent class, and adds the following information:</p>
 *
 * <ul>
 *   <li>a {@linkplain #getDomainOfValidity() domain of validity}, the area for which the reference system is valid,</li>
 *   <li>a {@linkplain #getScope() scope}, which describes the domain of usage or limitation of usage.
 * </ul>
 *
 * <div class="section">Instantiation</div>
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code ReferenceSystem} instances created using
 * only SIS factories and static constants can be shared by many objects and passed between threads without
 * synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlTransient
public class AbstractReferenceSystem extends AbstractIdentifiedObject implements ReferenceSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3337659819553899435L;

    /**
     * Area for which the (coordinate) reference system is valid.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDomainOfValidity(Extent)}</p>
     *
     * @see #getDomainOfValidity()
     */
    private Extent domainOfValidity;

    /**
     * Description of domain of usage, or limitations of usage,
     * for which this (coordinate) reference system object is valid.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setScope(InternationalString)}</p>
     *
     * @see #getScope()
     */
    private InternationalString scope;

    /**
     * Constructs a reference system from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
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
     *     <td>{@link #getScope()}</td>
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
     * Constructs a new reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param object The reference system to copy.
     */
    protected AbstractReferenceSystem(final ReferenceSystem object) {
        super(object);
        domainOfValidity = object.getDomainOfValidity();
        scope            = object.getScope();
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code ReferenceSystem.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The GeoAPI interface implemented by this class.
     */
    @Override
    public Class<? extends ReferenceSystem> getInterface() {
        return ReferenceSystem.class;
    }

    /**
     * Returns the region or timeframe in which this reference system is valid, or {@code null} if unspecified.
     *
     * @return Area or region or timeframe in which this (coordinate) reference system is valid, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @Override
    @XmlElement(name = "domainOfValidity")
    // For an unknown reason, JAXB does not take the adapter declared in package-info for this particular property.
    @Workaround(library = "JDK", version = "1.8")
    @XmlJavaTypeAdapter(EX_Extent.class)
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
    @XmlElement(name ="scope", required = true)
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
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final AbstractReferenceSystem that = (AbstractReferenceSystem) object;
                return Objects.equals(domainOfValidity, that.domainOfValidity) &&
                       Objects.equals(scope,            that.scope);
            }
            case BY_CONTRACT: {
                final ReferenceSystem that = (ReferenceSystem) object;
                return deepEquals(getDomainOfValidity(), that.getDomainOfValidity(), mode) &&
                       deepEquals(getScope(),            that.getScope(), mode);
            }
            default: {
                // Domain of validity and scope are metadata, so they can be ignored.
                return true;
            }
        }
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(domainOfValidity, scope);
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
    AbstractReferenceSystem() {
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getDomainOfValidity()
     */
    private void setDomainOfValidity(final Extent value) {
        if (domainOfValidity == null) {
            domainOfValidity = value;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractReferenceSystem.class, "setDomainOfValidity", "domainOfValidity");
        }
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getScope()
     */
    private void setScope(final InternationalString value) {
        if (scope == null) {
            scope = value;
        } else {
            MetadataUtilities.propertyAlreadySet(AbstractReferenceSystem.class, "setScope", "scope");
        }
    }
}
