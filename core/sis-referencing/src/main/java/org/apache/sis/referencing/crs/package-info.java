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

/**
 * Coordinate Reference System (CRS) definitions as coordinate systems related to the earth through datum.
 * An explanation for this package is provided in the {@linkplain org.opengis.referencing.crs OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <p>The root class for this package is {@link org.apache.sis.referencing.crs.AbstractCRS}.
 * Coordinate Reference System (CRS) can have various number of dimensions, but some restrictions
 * apply depending on the CRS type:</p>
 *
 * <table class="sis">
 *   <caption>Common Coordinate Reference System types</caption>
 *   <tr><th>Dimension</th> <th>CRS type examples</th> <th>Remarks</th></tr>
 *   <tr>
 *     <td>3</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic},
 *         {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS Geocentric}</td>
 *     <td>ISO 19111 uses the same class, {@code GeodeticCRS}, for those two cases.</td>
 *   </tr><tr>
 *     <td>2</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic},
 *         {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected}</td>
 *     <td>{@code GeographicCRS} can also be 3D.</td>
 *   </tr><tr>
 *     <td>1</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultVerticalCRS Vertical},
 *         {@linkplain org.apache.sis.referencing.crs.DefaultTemporalCRS Temporal}.</td>
 *     <td></td>
 *   </tr><tr>
 *     <td>Any</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultCompoundCRS Compound}</td>
 *     <td>Often used for adding a time axis to the above CRS.</td>
 *   </tr>
 * </table>
 *
 * <div class="section">Apache SIS extensions</div>
 * Some SIS implementations provide additional methods that are not part of OGC/ISO specifications:
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.crs.AbstractCRS#forConvention AbstractCRS.forConvention(AxesConvention)}</li>
 *   <li>{@link org.apache.sis.referencing.crs.DefaultTemporalCRS#toDate(double)}</li>
 *   <li>{@link org.apache.sis.referencing.crs.DefaultTemporalCRS#toValue DefaultTemporalCRS.toValue(Date)}</li>
 * </ul>
 *
 * In addition Apache SIS provides two distinct classes for geographic and geocentric CRS where OGC/ISO defines
 * a single {@code GeodeticCRS} type. OGC/ISO distinguishes the geographic/geocentric cases according the type
 * of the coordinate system associated to that CRS:
 *
 * <ul>
 *   <li>A geodetic CRS associated to an {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal CS}
 *       is geographic.</li>
 *   <li>A geodetic CRS associated to a {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical} or
 *       {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS} is geocentric.</li>
 * </ul>
 *
 * SIS keeps the geographic and geocentric CRS as distinct types since such distinction is in wide use.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlSchema(location = "http://schemas.opengis.net/gml/3.2.1/coordinateReferenceSystems.xsd",
           elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns =
{
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    /*
     * Do NOT declare the following adapters in this package-info:
     *
     *   - CS_CoordinateSystem
     *   - SC_SingleCRS
     *   - SC_CRS
     *
     * Because the above types are the base type of many other types,
     * adding the above adapters is a cause of confusion for JAXB.
     *
     * Note: be careful with CS_AffineCS and CS_CartesianCS relationship.
     */
    @XmlJavaTypeAdapter(CD_GeodeticDatum.class),
    @XmlJavaTypeAdapter(CD_EngineeringDatum.class),
    @XmlJavaTypeAdapter(CD_ImageDatum.class),
    @XmlJavaTypeAdapter(CD_ParametricDatum.class),
    @XmlJavaTypeAdapter(CD_TemporalDatum.class),
    @XmlJavaTypeAdapter(CD_VerticalDatum.class),
    @XmlJavaTypeAdapter(CS_CartesianCS.class),      // Must be before CS_AffineCS.
    @XmlJavaTypeAdapter(CS_AffineCS.class),
    @XmlJavaTypeAdapter(CS_CylindricalCS.class),
    @XmlJavaTypeAdapter(CS_EllipsoidalCS.class),
    @XmlJavaTypeAdapter(CS_LinearCS.class),
    @XmlJavaTypeAdapter(CS_ParametricCS.class),
    @XmlJavaTypeAdapter(CS_PolarCS.class),
    @XmlJavaTypeAdapter(CS_SphericalCS.class),
    @XmlJavaTypeAdapter(CS_TimeCS.class),
    @XmlJavaTypeAdapter(CS_UserDefinedCS.class),
    @XmlJavaTypeAdapter(CS_VerticalCS.class),
    @XmlJavaTypeAdapter(CC_Conversion.class),
    @XmlJavaTypeAdapter(SC_GeographicCRS.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class)
})
package org.apache.sis.referencing.crs;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.referencing.*;
