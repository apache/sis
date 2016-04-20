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
package org.apache.sis.referencing.crs;

import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.referencing.crs.AbstractCRS.isBaseCRS;

// Branch-dependent imports
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultParametricDatum;


/**
 * A 1-dimensional coordinate reference system which uses parameter values or functions.
 * Parametric CRS can be used for physical properties or functions that vary monotonically with height.
 * A typical example is the pressure in meteorological applications.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultParametricDatum Parametric}.<br>
 * <b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultParametricCS Parametric}.
 * </p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.referencing.datum.DefaultParametricDatum
 * @see org.apache.sis.referencing.cs.DefaultParametricCS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createParametricCRS(String)
 */
@XmlType(name = "ParametricCRSType", propOrder = {
    "coordinateSystem",
    "datum"
})
@XmlRootElement(name = "ParametricCRS")
public class DefaultParametricCRS extends AbstractCRS implements ParametricCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4013698133331342649L;

    /**
     * The datum.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDatum(ParametricDatum)}</p>
     *
     * @see #getDatum()
     */
    private DefaultParametricDatum datum;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
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
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter types may be changed to
     * {@code org.opengis.referencing.datum.ParametricDatum} and {@code org.opengis.referencing.cs.ParametricCS}
     * Those change are pending GeoAPI revision.</div>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param datum The datum.
     * @param cs The coordinate system.
     */
    public DefaultParametricCRS(final Map<String,?> properties,
                                final DefaultParametricDatum datum,
                                final DefaultParametricCS cs)
    {
        super(properties, cs);
        ensureNonNull("datum", datum);
        this.datum = datum;
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter type may be changed
     * to {@code org.opengis.referencing.crs.ParametricCRS}. This change is pending GeoAPI revision.</div>
     *
     * @param crs The coordinate reference system to copy.
     */
    protected DefaultParametricCRS(final DefaultParametricCRS crs) {
        super(crs);
        datum = crs.getDatum();
    }

    /**
     * Returns the datum.
     *
     * @return The datum.
     */
    @Override
    @XmlElement(name = "parametricDatum", required = true)
    public DefaultParametricDatum getDatum() {
        return datum;
    }

    /**
     * Returns the coordinate system.
     *
     * @return The coordinate system.
     */
    @Override
    @XmlElement(name = "parametricCS", required = true)
    public DefaultParametricCS getCoordinateSystem() {
        return (DefaultParametricCS) super.getCoordinateSystem();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultParametricCRS forConvention(final AxesConvention convention) {
        return (DefaultParametricCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new DefaultParametricCRS(properties, datum, (DefaultParametricCS) cs);
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code ParametricCRS[…]} element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code ParametricCRS} is defined in the WKT 2 specification only.</div>
     *
     * @return {@code "ParametricCRS"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#83">WKT 2 specification</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return isBaseCRS(formatter) ? WKTKeywords.BaseParamCRS : WKTKeywords.ParametricCRS;
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
    private DefaultParametricCRS() {
        /*
         * The datum and the coordinate system are mandatory for SIS working. We do not verify their presence
         * here because the verification would have to be done in an 'afterMarshal(…)' method and throwing an
         * exception in that method causes the whole unmarshalling to fail.  But the SC_CRS adapter does some
         * verifications.
         */
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getDatum()
     */
    private void setDatum(final DefaultParametricDatum value) {
        if (datum == null) {
            datum = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultParametricCRS.class, "setDatum", "parametricDatum");
        }
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCoordinateSystem()
     */
    private void setCoordinateSystem(final DefaultParametricCS cs) {
        setCoordinateSystem("parametricCS", cs);
    }
}
