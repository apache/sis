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
package org.apache.sis.referencing.datum;

import java.util.Map;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.io.wkt.Formatter;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.ParametricDatum;


/**
 * Defines the origin of a parametric coordinate reference system.
 *
 * <h2>Creating new parametric datum instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 2
 * and 3 below), or indirectly by specifying the identifier of an entry in a database (choices 1 below).
 *
 * <ol>
 *   <li>Create a {@code ParametricDatum} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createParametricDatum(String)}.</li>
 *   <li>Create a {@code ParametricDatum} by invoking the {@code DatumFactory.createParametricDatum(…)} method,
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code DefaultParametricDatum} by invoking the
 *       {@linkplain #DefaultParametricDatum(Map) constructor}.</li>
 * </ol>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 *
 * @see org.apache.sis.referencing.cs.DefaultParametricCS
 * @see org.apache.sis.referencing.crs.DefaultParametricCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createParametricDatum(String)
 *
 * @since 0.7
 */
@XmlType(name = "ParametricDatumType")
@XmlRootElement(name = "ParametricDatum")
public class DefaultParametricDatum extends AbstractDatum implements ParametricDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4951496470378888681L;

    /**
     * Creates a parametric datum from the given properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
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
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_DEFINITION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getAnchorDefinition()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_EPOCH_KEY}</td>
     *     <td>{@link java.time.temporal.Temporal}</td>
     *     <td>{@link #getAnchorEpoch()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createParametricDatum(Map)
     */
    public DefaultParametricDatum(final Map<String,?> properties) {
        super(properties);
    }

    /**
     * Creates a new datum with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  datum  the datum to copy.
     *
     * @see #castOrCopy(ParametricDatum)
     */
    protected DefaultParametricDatum(final ParametricDatum datum) {
        super(datum);
    }

    /**
     * Returns a SIS datum implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultParametricDatum castOrCopy(final ParametricDatum object) {
        return (object == null) || (object instanceof DefaultParametricDatum) ?
                (DefaultParametricDatum) object : new DefaultParametricDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParametricDatum.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code TemporalDatum}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code ParametricDatum.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ParametricDatum> getInterface() {
        return ParametricDatum.class;
    }

    /**
     * Formats this datum as a <i>Well Known Text</i> {@code ParametricDatum[…]} element.
     *
     * <h4>Compatibility note</h4>
     * {@code ParametricDatum} is defined in the WKT 2 specification only.
     *
     * @return {@code "ParametricDatum"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return formatter.shortOrLong(WKTKeywords.PDatum, WKTKeywords.ParametricDatum);
    }


    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new datum in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultParametricDatum() {
    }
}
