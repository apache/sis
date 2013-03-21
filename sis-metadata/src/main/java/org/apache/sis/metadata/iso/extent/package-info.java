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
 * {@linkplain org.apache.sis.metadata.iso.extent.DefaultExtent Extent} implementation.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.content OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link org.apache.sis.metadata.iso.MetadataObjects#getGeographicBoundingBox
 *       MetadataObjects.getGeographicBoundingBox(Extent)}
 *       for extracting a global geographic bounding box.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#setBounds(double, double, double, double)
 *       DefaultGeographicBoundingBox.setBounds(double, double, double, double)}
 *       for setting the extent from (λ,φ) values.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#setBounds(org.opengis.geometry.Envelope)
 *       DefaultGeographicBoundingBox.setBounds(Envelope)}
 *       for setting the extent from the given envelope.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#setBounds(org.opengis.metadata.extent.GeographicBoundingBox)
 *       DefaultGeographicBoundingBox.setBounds(GeographicBoundingBox)}
 *       for setting the extent from an other bounding box.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add
 *       DefaultGeographicBoundingBox.add(GeographicBoundingBox)}
 *       for expanding this extent to include an other bounding box.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#intersect
 *       DefaultGeographicBoundingBox.intersect(GeographicBoundingBox)}
 *       for the intersection between the two bounding boxes.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultVerticalExtent#setBounds
 *       DefaultVerticalExtent.setBounds(Envelope)}
 *       for setting the vertical element from the given envelope.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultTemporalExtent#setBounds
 *       DefaultTemporalExtent.setBounds(Envelope)}
 *       for setting the temporal element from the given envelope.</li>
 *
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultExtent#addElements
 *       DefaultExtent.addElements(Extent)}
 *       for adding extent elements inferred from the given envelope.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(EX_GeographicBoundingBox.class),
    @XmlJavaTypeAdapter(EX_GeographicExtent.class),
    @XmlJavaTypeAdapter(EX_TemporalExtent.class),
    @XmlJavaTypeAdapter(EX_VerticalExtent.class),
    @XmlJavaTypeAdapter(GM_Object.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
//  @XmlJavaTypeAdapter(SC_VerticalCRS.class), // TODO
//  @XmlJavaTypeAdapter(TM_Primitive.class),   // TODO

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_Boolean.class), @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class),
    @XmlJavaTypeAdapter(GO_Decimal.class), @XmlJavaTypeAdapter(type=double.class,  value=GO_Decimal.class)
})
package org.apache.sis.metadata.iso.extent;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.metadata.*;
// TODO import org.apache.sis.internal.jaxb.referencing.*;
import org.apache.sis.internal.jaxb.geometry.GM_Object;
