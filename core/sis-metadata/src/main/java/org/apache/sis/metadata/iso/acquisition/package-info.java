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
 * Information about the measuring instruments, the platform carrying them, and the mission to which the data contributes.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.acquisition OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <div class="section">Overview</div>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation Acquisition information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultObjective              Objective}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatformPass           PlatformPass}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEvent                  Event}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequirement            Requirement}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequestedDate          Requested date}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlan                   Plan}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultOperation              Operation}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform               Platform}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultInstrument             Instrument}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEnvironmentalRecord    Environmental record}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.ObjectiveType Objective type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Trigger       Trigger}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Context       Context}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Sequence      Sequence}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Priority      Priority}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.GeometryType  Geometry type}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.acquisition.OperationType Operation type}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                             {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation Acquisition information}<br>
 * {@code  ├─}                 {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultObjective              Objective}<br>
 * {@code  │   ├─}             {@linkplain org.opengis.metadata.acquisition.ObjectiveType                        Objective type} «code list»<br>
 * {@code  │   ├─}             {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatformPass           Platform pass}<br>
 * {@code  │   │   └─}         {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEvent                  Event}<br>
 * {@code  │   │       ├─}     {@linkplain org.opengis.metadata.acquisition.Trigger                              Trigger} «code list»<br>
 * {@code  │   │       ├─}     {@linkplain org.opengis.metadata.acquisition.Context                              Context} «code list»<br>
 * {@code  │   │       └─}     {@linkplain org.opengis.metadata.acquisition.Sequence                             Sequence} «code list»<br>
 * {@code  │   ├─}             {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequirement            Requirement}<br>
 * {@code  │   │   ├─}         {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequestedDate          Requested date}<br>
 * {@code  │   │   └─}         {@linkplain org.opengis.metadata.acquisition.Priority                             Priority} «code list»<br>
 * {@code  │   └─}             {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlan                   Plan}<br>
 * {@code  │       ├─}         {@linkplain org.opengis.metadata.acquisition.GeometryType                         Geometry type} «code list»<br>
 * {@code  │       └─}         {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultOperation              Operation}<br>
 * {@code  │           ├─}     {@linkplain org.opengis.metadata.acquisition.OperationType                        Operation type} «code list»<br>
 * {@code  │           └─}     {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform               Platform}<br>
 * {@code  │               └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultInstrument             Instrument}<br>
 * {@code  └─}                 {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEnvironmentalRecord    Environmental record}<br>
 * </td></tr></table>
 *
 * <div class="section">Null values, nil objects and collections</div>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element can not be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <cite>live</cite>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlSchema(location=Schemas.METADATA_XSD, elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.GMI, xmlns = {
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(GM_Object.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_ProgressCode.class),
    @XmlJavaTypeAdapter(MI_ContextCode.class),
    @XmlJavaTypeAdapter(MI_EnvironmentalRecord.class),
    @XmlJavaTypeAdapter(MI_Event.class),
    @XmlJavaTypeAdapter(MI_GeometryTypeCode.class),
    @XmlJavaTypeAdapter(MI_Instrument.class),
    @XmlJavaTypeAdapter(MI_Objective.class),
    @XmlJavaTypeAdapter(MI_ObjectiveTypeCode.class),
    @XmlJavaTypeAdapter(MI_Operation.class),
    @XmlJavaTypeAdapter(MI_OperationTypeCode.class),
    @XmlJavaTypeAdapter(MI_Plan.class),
    @XmlJavaTypeAdapter(MI_Platform.class),
    @XmlJavaTypeAdapter(MI_PlatformPass.class),
    @XmlJavaTypeAdapter(MI_PriorityCode.class),
    @XmlJavaTypeAdapter(MI_RequestedDate.class),
    @XmlJavaTypeAdapter(MI_Requirement.class),
    @XmlJavaTypeAdapter(MI_SequenceCode.class),
    @XmlJavaTypeAdapter(MI_TriggerCode.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.acquisition;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
import org.apache.sis.internal.jaxb.geometry.GM_Object;
