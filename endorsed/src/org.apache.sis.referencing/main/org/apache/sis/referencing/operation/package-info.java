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
 * Relationship between any two {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems} (CRS).
 * An explanation for this package is provided in the {@linkplain org.opengis.referencing.operation OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the Apache SIS implementation.
 *
 * <p>This package provides an ISO 19111 {@linkplain org.apache.sis.referencing.operation.AbstractCoordinateOperation
 * Coordinate Operation implementation} and support classes. The actual transform work is performed by the sub-packages,
 * but most users will not need to deal with them directly.</p>
 *
 * <h2>Apache SIS extensions</h2>
 * Some SIS implementations provide additional methods that are not part of OGC/ISO specifications:
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.operation.AbstractCoordinateOperation#getLinearAccuracy() AbstractCoordinateOperation.getLinearAccuracy()}
 *     — tries to convert the accuracy to metres,</li>
 *   <li>{@link org.apache.sis.referencing.operation.DefaultConversion#specialize DefaultConversion.specialize(…)}
 *     — changes a <i>defining conversion</i> into a complete conversion.</li>
 * </ul>
 *
 * <h2><i>Early binding</i> versus <i>late binding</i> implementations</h2>
 * There is sometimes multiple ways of transforming coordinates for a given pair of source and target CRS.
 * For example, the {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters}
 * may vary depending on the area of interest, like in the transformations from NAD27 to WGS84.
 * Even for a fixed set of Bursa-Wolf parameter, there is various ways to use them (<cite>Molodensky</cite>,
 * <cite>Abridged Molodensky</cite>, <cite>Geocentric translation</cite>, <i>etc.</i>).
 *
 * <p>EPSG identifies two approaches for addressing this multiplicity problem.
 * Quoting the GIGS guideline:</p>
 *
 * <blockquote>
 * <ul class="verbose">
 *   <li><b>Early binding:</b>
 *     A priori association of a coordinate transformation with a geodetic CRS.
 *     The association is usually made at start-up of the session or project, as that is defined in the software,
 *     but always before any data is associated with the ‘CRS’. In general the ‘coordinate transformation’ specified
 *     uses the ‘CRS’ of the data as the source ‘CRS’ and WGS 84 as the target ‘CRS’.</li>
 *
 *   <li><b>Late binding:</b>
 *     Association at run time of a coordinate transformation with a CRS.
 *     Late binding allows the user to select the appropriate transformation upon import of ‘geospatial data’
 *     or merge of two geospatial datasets. This means that, in cases where there are multiple existing transformations,
 *     the user can choose the appropriate one, possibly aided by additional information.</li>
 * </ul>
 * <p style="text-align:right; font-size:small"><b>Source:</b>
 * <u>Geospatial Integrity of Geoscience Software Part 1 – GIGS guidelines.</u>
 * <i>OGP publication, Report No. 430-1, September 2011</i></p>
 * </blockquote>
 *
 * Apache SIS is a <i>late binding</i> implementation, while a little trace for <i>early binding</i>
 * exists in the form of the {@link org.apache.sis.referencing.datum.DefaultGeodeticDatum#getBursaWolfParameters()}
 * method for those who really need it. This means that when searching for a coordinate operation between a given
 * pair of CRS, Apache SIS will query {@link org.apache.sis.referencing.factory.sql.EPSGFactory} before to try to
 * {@linkplain org.apache.sis.referencing.operation.CoordinateOperationFinder infer the operation path by itelf}.
 * The {@link org.apache.sis.referencing.operation.CoordinateOperationContext} can be used for further refinements,
 * for example by specifying the area of interest.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 * @since   0.6
 */
@XmlSchema(location = "http://schemas.opengis.net/gml/3.2.1/coordinateOperations.xsd",
           elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns =
{
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(DQ_PositionalAccuracy.class),
    @XmlJavaTypeAdapter(CC_OperationMethod.class),
    @XmlJavaTypeAdapter(CC_CoordinateOperation.class),
    @XmlJavaTypeAdapter(CC_GeneralParameterValue.class),
    @XmlJavaTypeAdapter(CC_GeneralOperationParameter.class),
    @XmlJavaTypeAdapter(SC_CRS.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class)
})
package org.apache.sis.referencing.operation;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.gco.*;
import org.apache.sis.xml.bind.referencing.*;
import org.apache.sis.xml.bind.metadata.EX_Extent;
import org.apache.sis.xml.bind.metadata.CI_Citation;
import org.apache.sis.xml.bind.metadata.DQ_PositionalAccuracy;
