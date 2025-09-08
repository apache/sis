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


/**
 * Defines the origin of a parametric coordinate reference system.
 *
 * <h2>Creating new parametric datum instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 2
 * and 3 below), or indirectly by specifying the identifier of an entry in a database (choices 1 below).
 *
 * <ol>
 *   <li>Create a {@code ParametricDatum} from an identifier in a database by invoking
 *       {@code DatumAuthorityFactory.createParametricDatum(String)}.</li>
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
 * @version 1.5
 *
 * @see org.apache.sis.referencing.cs.DefaultParametricCS
 * @see org.apache.sis.referencing.crs.DefaultParametricCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createParametricDatum(String)
 *
 * @since 0.7
 */
@XmlType(name = "ParametricDatumType")
@XmlRootElement(name = "ParametricDatum")
public class DefaultParametricDatum extends AbstractDatum {
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
     *     <td>"domains"</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getAnchorDefinition()}</td>
     *   </tr><tr>
     *     <td>{@code "anchorEpoch"}</td>
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
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter type may be changed
     * to {@code org.opengis.referencing.datum.ParametricDatum}. This change is pending GeoAPI revision.</div>
     *
     * @param  datum  the datum to copy.
     */
    protected DefaultParametricDatum(final DefaultParametricDatum datum) {
        super(datum);
    }

    /**
     * Formats this datum as a <cite>Well Known Text</cite> {@code ParametricDatum[…]} element.
     *
     * <h4>Compatibility note</h4>
     * {@code ParametricDatum} is defined in the <abbr>WKT</abbr> 2 specification only.
     * Apache <abbr>SIS</abbr> accepts this type as members of datum ensembles,
     * but this is not valid <abbr>WKT</abbr> according <abbr>ISO</abbr> 19162:2019.
     *
     * @return {@code "PDatum"} or {@code "ParametricDatum"}.
     *         May also be {@code "Member"} if this datum is inside a <abbr>WKT</abbr> {@code Ensemble[…]} element.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final String name = super.formatTo(formatter);
        if (name != null) {
            // Member of a datum ensemble, but ISO 19162:2019 allows that for geodetic and vertical datum only.
            formatter.setInvalidWKT(this, null);
            return name;
        }
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
