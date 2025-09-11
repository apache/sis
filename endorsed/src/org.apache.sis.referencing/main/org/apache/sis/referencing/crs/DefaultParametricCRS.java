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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.referencing.datum.DefaultParametricDatum;

// Specific to the main branch:
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;


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
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.datum.DefaultParametricDatum
 * @see org.apache.sis.referencing.cs.DefaultParametricCS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createParametricCRS(String)
 *
 * @since 0.7
 */
@XmlType(name = "ParametricCRSType", propOrder = {
    "coordinateSystem",
    "datum"
})
@XmlRootElement(name = "ParametricCRS")
public class DefaultParametricCRS extends AbstractSingleCRS<DefaultParametricDatum> implements ParametricCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5443671973122639841L;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The properties given in argument follow the same rules as for the
     * {@linkplain org.apache.sis.referencing.AbstractReferenceSystem#AbstractReferenceSystem(Map)
     * super-class constructor}. The following table is a reminder of main (not all) properties:
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
     *   </tr>
     * </table>
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter types may be changed to
     * {@code org.opengis.referencing.datum.ParametricDatum} and {@code org.opengis.referencing.cs.ParametricCS}
     * Those change are pending GeoAPI revision.</div>
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system.
     *
     * @since 1.5
     */
    public DefaultParametricCRS(final Map<String,?> properties,
                                final DefaultParametricDatum datum,
                                final DefaultDatumEnsemble<DefaultParametricDatum> ensemble,
                                final DefaultParametricCS cs)
    {
        super(properties, DefaultParametricDatum.class, datum, ensemble, cs);
        checkDimension(1, 1, cs);
    }

    /**
     * @deprecated A {@code DefaultDatumEnsemble} argument has been added.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public DefaultParametricCRS(final Map<String,?> properties,
                                final DefaultParametricDatum datum,
                                final DefaultParametricCS cs)
    {
        this(properties, datum, null, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    private DefaultParametricCRS(final DefaultParametricCRS original, final AbstractCS cs) {
        super(original, null, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the parameter type may be changed
     * to {@code org.opengis.referencing.crs.ParametricCRS}. This change is pending GeoAPI revision.</div>
     *
     * @param  crs  the coordinate reference system to copy.
     */
    protected DefaultParametricCRS(final DefaultParametricCRS crs) {
        super(crs);
    }

    /**
     * Returns the reference surface used as the origin of this <abbr>CRS</abbr>.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     *
     * @return the parametric datum, or {@code null} if this <abbr>CRS</abbr> is related to
     *         an object identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     */
    @Override
    @XmlElement(name = "parametricDatum", required = true)
    public DefaultParametricDatum getDatum() {
        return super.getDatum();
    }

    /**
     * Returns a collection of datums which, for low accuracy requirements,
     * may be considered to be insignificantly different from each other.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a single {@linkplain #getDatum() datum}.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may
     * be changed to the {@code org.opengis.referencing.datum.DatumEnsemble} interface.
     * This change is pending GeoAPI revision.</div>
     *
     * @return the datum ensemble, or {@code null} if this <abbr>CRS</abbr> is related
     *         to an object identified only by a single {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DefaultDatumEnsemble<DefaultParametricDatum> getDatumEnsemble() {
        return super.getDatumEnsemble();
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
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
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultParametricCRS(this, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code ParametricCRS[…]} element.
     *
     * <h4>Compatibility note</h4>
     * {@code ParametricCRS} is defined in the WKT 2 specification only.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "ParametricCRS"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return isBaseCRS(formatter) ? WKTKeywords.BaseParamCRS : WKTKeywords.ParametricCRS;
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
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
        setDatum("parametricDatum", value);
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
