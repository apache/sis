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
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceSystem;

// Specific to the main branch:
import org.apache.sis.referencing.internal.Legacy;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.extent.Extent;


/**
 * Description of a spatial and temporal reference system used by a dataset.
 * Reference systems do not necessarily use coordinates. For example, a reference system could use postal codes.
 * The specialized case of referencing by coordinates is handled by the
 * {@link org.apache.sis.referencing.crs.AbstractCRS} subclass.
 *
 * <p>This class inherits the {@linkplain #getName() name}, {@linkplain #getAlias() aliases},
 * {@linkplain #getIdentifiers() identifiers}, {@linkplain #getDomains() domains}
 * and {@linkplain #getRemarks() remarks} from the parent class.</p>
 *
 * <h2>Instantiation</h2>
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code ReferenceSystem} instances created using
 * only SIS factories and static constants can be shared by many objects and passed between threads without
 * synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.4
 */
@XmlTransient
public class AbstractReferenceSystem extends AbstractIdentifiedObject implements ReferenceSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7207447363999869238L;

    /**
     * Constructs a reference system from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>"domains"</td>
     *     <td>{@link DefaultObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties  the properties to be given to this object.
     */
    public AbstractReferenceSystem(final Map<String,?> properties) {
        super(properties);
    }

    /**
     * Constructs a new reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param object  the reference system to copy.
     */
    protected AbstractReferenceSystem(final ReferenceSystem object) {
        super(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code ReferenceSystem.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the GeoAPI interface implemented by this class.
     */
    @Override
    public Class<? extends ReferenceSystem> getInterface() {
        return ReferenceSystem.class;
    }

    /**
     * Returns the region or timeframe in which this reference system is valid, or {@code null} if unspecified.
     *
     * @return area or region or timeframe in which this (coordinate) reference system is valid, or {@code null}.
     *
     * @deprecated Replaced by {@link #getDomains()} as of ISO 19111:2019.
     */
    @Override
    @Deprecated(since = "1.4")
    public Extent getDomainOfValidity() {
        return Legacy.getDomainOfValidity(getDomains());
    }

    /**
     * Returns the domain or limitations of usage, or {@code null} if unspecified.
     *
     * @return description of domain of usage, or limitations of usage, for which this
     *         (coordinate) reference system object is valid, or {@code null}.
     *
     * @deprecated Replaced by {@link #getDomains()} as of ISO 19111:2019.
     */
    @Override
    @Deprecated(since = "1.4")
    public InternationalString getScope() {
        return Legacy.getScope(getDomains());
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
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    AbstractReferenceSystem() {
    }
}
