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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.io.wkt.Formatter;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Defines the origin of an engineering coordinate reference system.
 * An engineering datum is used in a region around that origin.
 * This origin can be fixed with respect to the earth (such as a defined point at a construction site),
 * or be a defined point on a moving vehicle (such as on a ship or satellite).
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if all
 * components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.crs.DefaultEngineeringCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createEngineeringDatum(String)
 *
 * @since 0.4
 */
@XmlType(name = "EngineeringDatumType")
@XmlRootElement(name = "EngineeringDatum")
public class DefaultEngineeringDatum extends AbstractDatum implements EngineeringDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1498304918725248637L;

    /**
     * Creates an engineering datum from the given properties. The properties map is given
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
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
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
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createEngineeringDatum(Map)
     */
    public DefaultEngineeringDatum(final Map<String,?> properties) {
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
     * @see #castOrCopy(EngineeringDatum)
     */
    protected DefaultEngineeringDatum(final EngineeringDatum datum) {
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
    public static DefaultEngineeringDatum castOrCopy(final EngineeringDatum object) {
        return (object == null) || (object instanceof DefaultEngineeringDatum) ?
                (DefaultEngineeringDatum) object : new DefaultEngineeringDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code EngineeringDatum.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code EngineeringDatum}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code EngineeringDatum.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends EngineeringDatum> getInterface() {
        return EngineeringDatum.class;
    }

    /**
     * Formats this datum as a <i>Well Known Text</i> {@code EngineeringDatum[…]} element.
     *
     * <h4>Compatibility note</h4>
     * Apache <abbr>SIS</abbr> accepts this type as members of datum ensembles,
     * but this is not valid <abbr>WKT</abbr> according <abbr>ISO</abbr> 19162:2019.
     *
     * @return {@code "EDatum"} or {@code "EngineeringDatum"} (WKT 2), or {@code "Local_Datum"} (WKT 1).
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
            /*
             * Datum type was provided for all kind of datum in the legacy OGC 01-009 specification.
             * Datum types became provided only for vertical datum in the ISO 19111:2003 specification,
             * then removed completely in the ISO 19111:2007 revision. We are supposed to format them
             * in WKT 1, but do not have any indication about what the values should be.
             */
            formatter.append(0);
            return WKTKeywords.Local_Datum;
        }
        return formatter.shortOrLong(WKTKeywords.EDatum, WKTKeywords.EngineeringDatum);
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
    private DefaultEngineeringDatum() {
    }
}
