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
 * Information about spatial, vertical, and temporal extent.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.extent OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <h2>Overview</h2>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO 19115 metadata}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.extent.DefaultExtent                Extent}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.extent.AbstractGeographicExtent     Geographic extent} «abstract»<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox Geographic bounding box}<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicDescription Geographic description}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.extent.DefaultBoundingPolygon       Bounding polygon}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.extent.DefaultVerticalExtent        Vertical extent}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.extent.DefaultTemporalExtent        Temporal extent}<br>
 * {@code      └─} {@linkplain org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent Spatial temporal extent}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *             {@linkplain org.apache.sis.metadata.iso.extent.DefaultExtent                Extent}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.extent.AbstractGeographicExtent     Geographic extent} «abstract»<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.extent.DefaultVerticalExtent        Vertical extent}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.extent.DefaultTemporalExtent        Temporal extent}<br>
 *             {@linkplain org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent Spatial temporal extent}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.extent.AbstractGeographicExtent     Geographic extent} «abstract»<br>
 *             {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox Geographic bounding box}<br>
 *             {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicDescription Geographic description}<br>
 *             {@linkplain org.apache.sis.metadata.iso.extent.DefaultBoundingPolygon       Bounding polygon}<br>
 * </td></tr></table>
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link org.apache.sis.metadata.iso.extent.Extents}
 *     <ul>
 *       <li>{@link org.apache.sis.metadata.iso.extent.Extents#getGeographicBoundingBox
 *       getGeographicBoundingBox(Extent)}
 *       for extracting a global geographic bounding box.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.Extents#intersection
 *       intersection(GeographicBoundingBox, GeographicBoundingBox)}
 *       for computing the intersection of two geographic bounding boxes.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.Extents#area
 *       area(GeographicBoundingBox)}
 *       for estimating the area of a geographic bounding box.</li>
 *     </ul>
 *   </li>
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox}
 *     <ul>
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#setBounds(double, double, double, double)
 *       setBounds(double, double, double, double)}
 *       for setting the extent from (λ,φ) values.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#setBounds(org.opengis.geometry.Envelope)
 *       setBounds(Envelope)}
 *       for setting the extent from the given envelope.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#setBounds(org.opengis.metadata.extent.GeographicBoundingBox)
 *       setBounds(GeographicBoundingBox)}
 *       for setting the extent from another bounding box.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add
 *       add(GeographicBoundingBox)}
 *       for expanding this extent to include another bounding box.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#intersect
 *       intersect(GeographicBoundingBox)}
 *       for the intersection between the two bounding boxes.</li>
 *     </ul>
 *   </li>
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultVerticalExtent}
 *     <ul>
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultVerticalExtent#setBounds
 *       setBounds(Envelope)}
 *       for setting the vertical element from the given envelope.</li>
 *     </ul>
 *   </li>
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultTemporalExtent}
 *     <ul>
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultTemporalExtent#setBounds(java.time.temporal.Temporal, java.time.temporal.Temporal)
 *       setBounds(Temporal, Temporal)}
 *       for setting the temporal element from the start time and end time.</li>
 *
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultTemporalExtent#setBounds(org.opengis.geometry.Envelope)
 *       setBounds(Envelope)}
 *       for setting the temporal element from the given envelope.</li>
 *     </ul>
 *   </li>
 *   <li>{@link org.apache.sis.metadata.iso.extent.DefaultExtent}
 *     <ul>
 *       <li>{@link org.apache.sis.metadata.iso.extent.DefaultExtent#addElements
 *       addElements(Extent)}
 *       for adding extent elements inferred from the given envelope.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Null values, nil objects and collections</h2>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element cannot be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <em>live</em>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.6
 * @since   0.3
 */
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/gex/1.0/gex.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.GEX,
           xmlns = {
                @XmlNs(prefix = "gex", namespaceURI = Namespaces.GEX),      // Geospatial Extent
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC)       // Metadata Common Classes
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(EX_GeographicExtent.class),
    @XmlJavaTypeAdapter(EX_TemporalExtent.class),
    @XmlJavaTypeAdapter(EX_VerticalExtent.class),
    @XmlJavaTypeAdapter(GM_Object.class),
    @XmlJavaTypeAdapter(GO_Boolean.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(SC_VerticalCRS.class),
    @XmlJavaTypeAdapter(TM_Primitive.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(value=GO_Decimal.class, type=double.class)
})
package org.apache.sis.metadata.iso.extent;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.gco.*;
import org.apache.sis.xml.bind.gml.*;
import org.apache.sis.xml.bind.metadata.*;
import org.apache.sis.xml.bind.metadata.geometry.GM_Object;
