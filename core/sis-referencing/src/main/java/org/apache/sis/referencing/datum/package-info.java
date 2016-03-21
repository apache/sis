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
 * Relationship of a {@code CoordinateSystem} (an abstract mathematical entity) to the earth or other system.
 * An explanation for this package is provided in the {@linkplain org.opengis.referencing.datum OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <p>The root class in this package is {@link org.apache.sis.referencing.datum.AbstractDatum}.
 * Various subclasses are defined for various kinds of relationship to the Earth or time
 * ({@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic},
 *  {@linkplain org.apache.sis.referencing.datum.DefaultVerticalDatum vertical},
 *  {@linkplain org.apache.sis.referencing.datum.DefaultTemporalDatum temporal}),
 * or to platforms (mobile or not)
 * ({@linkplain org.apache.sis.referencing.datum.DefaultEngineeringDatum engineering},
 *  {@linkplain org.apache.sis.referencing.datum.DefaultImageDatum image}).
 * Some of those SIS subclasses provide additional methods that are not part of OGC/ISO specifications:</p>
 * <ul>
 *   <li>{@link org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()}</li>
 *   <li>{@link org.apache.sis.referencing.datum.DefaultEllipsoid#getEccentricity()}</li>
 *   <li>{@link org.apache.sis.referencing.datum.DefaultEllipsoid#orthodromicDistance(double, double, double, double)}</li>
 *   <li>{@link org.apache.sis.referencing.datum.DefaultPrimeMeridian#getGreenwichLongitude(javax.measure.unit.Unit)
 *       DefaultPrimeMeridian.getGreenwichLongitude(Unit)}</li>
 *   <li>{@link org.apache.sis.referencing.datum.DefaultGeodeticDatum#getBursaWolfParameters()}</li>
 *   <li>{@link org.apache.sis.referencing.datum.DefaultGeodeticDatum#getPositionVectorTransformation
 *       DefaultGeodeticDatum.getPositionVectorTransformation(GeodeticDatum, Extent)}</li>
 * </ul>
 *
 * <div class="section">Datum shifts</div>
 * Three classes are provided in support of coordinate transformations between different datums:
 * <ul>
 *   <li>{@link org.apache.sis.referencing.datum.BursaWolfParameters} performs an approximation
 *       based on a translation, rotation and scale of geocentric coordinates.</li>
 *   <li>{@link org.apache.sis.referencing.datum.TimeDependentBWP} is like {@code BursaWolfParameters},
 *       but varies with time for taking in account the motion of plate tectonic.</li>
 *   <li>{@link org.apache.sis.referencing.datum.DatumShiftGrid} is used for more accurate transformations
 *       than what {@code BursaWolfParameters} allows, by interpolating the geographic or geocentric translations
 *       in a grid (e.g. NADCON or NTv2) instead than apply the same transformation for every points.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlSchema(location = "http://schemas.opengis.net/gml/3.2.1/datums.xsd",
           elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns =
{
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(CD_Ellipsoid.class),
    @XmlJavaTypeAdapter(CD_PrimeMeridian.class),
    @XmlJavaTypeAdapter(CD_VerticalDatumType.class),
    @XmlJavaTypeAdapter(CD_PixelInCell.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class),
    @XmlJavaTypeAdapter(DateAdapter.class),
})
package org.apache.sis.referencing.datum;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.gml.DateAdapter;
import org.apache.sis.internal.jaxb.metadata.*;
import org.apache.sis.internal.jaxb.referencing.*;
