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
 * Coordinate System (CS) definitions as the set of coordinate system axes that spans the coordinate space.
 * An explanation for this package is provided in the {@linkplain org.opengis.referencing.cs OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <p>The root class in this package is {@link org.apache.sis.referencing.cs.AbstractCS}.
 * Various subclasses are defined for various kinds of mathematical rules that determine
 * how coordinates are associated to quantities such as angles and distances.
 * Those SIS subclasses provide additional methods that are not part of OGC/ISO specifications:</p>
 * <ul>
 *   <li>{@link org.apache.sis.referencing.cs.AbstractCS#forConvention AbstractCS.forConvention(AxesConvention)}</li>
 * </ul>
 *
 * This package provides also a {@link org.apache.sis.referencing.cs.CoordinateSystems} utility class
 * with static methods for estimating an angle between two axes, determining the change of axis directions
 * and units between two coordinate systems, or filtering axes.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlSchema(location = "http://schemas.opengis.net/gml/3.2.1/coordinateSystems.xsd",
           elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns =
{
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CS_AxisDirection.class),
    @XmlJavaTypeAdapter(CS_CoordinateSystemAxis.class),
    @XmlJavaTypeAdapter(CS_RangeMeaning.class),
    @XmlJavaTypeAdapter(UnitAdapter.ForCS.class)
})
package org.apache.sis.referencing.cs;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.UnitAdapter;
import org.apache.sis.internal.jaxb.referencing.*;
