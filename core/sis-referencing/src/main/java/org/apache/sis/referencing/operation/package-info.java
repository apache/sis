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
 * Coordinate Operation implementation} and support classes. The actual transform work is performed by the following
 * sub-packages, but most users will not need to deal with them directly:</p>
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.operation.projection} — map projections,</li>
 *   <li>{@link org.apache.sis.referencing.operation.transform} — any transform other than map projections.</li>
 * </ul>
 *
 * <div class="section">Apache SIS extensions</div>
 * Some SIS implementations provide additional methods that are not part of OGC/ISO specifications:
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.operation.AbstractCoordinateOperation#getLinearAccuracy() AbstractCoordinateOperation.getLinearAccuracy()}
 *     — tries to convert the accuracy to metres,</li>
 *   <li>{@link org.apache.sis.referencing.operation.DefaultConversion#specialize DefaultConversion.specialize(…)}
 *     — changes a <cite>defining conversion</cite> into a complete conversion.</li>
 * </ul>
 *
 * <div class="section">Apache SIS specific behavior</div>
 * The following operations have a behavior in Apache SIS which may be different
 * than the behavior found in other softwares. Those particularities apply only when the math transform is
 * {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createParameterizedTransform
 * created directly}. Users do not need to care about them when the coordinate operation is
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#createOperation
 * inferred by Apache SIS for a given pair of CRS}.
 *
 * <ul>
 *   <li><b>Longitude rotation</b> (EPSG:9601) — the longitude offset may be specified in any units,
 *     but SIS unconditionally converts the value to degrees. Consequently the user is responsible
 *     for converting the longitude axis of source and target CRS to degrees before this operation is applied.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
@XmlSchema(elementFormDefault= XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns = {
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(DQ_PositionalAccuracy.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class)
})
package org.apache.sis.referencing.operation;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.metadata.EX_Extent;
import org.apache.sis.internal.jaxb.metadata.DQ_PositionalAccuracy;
