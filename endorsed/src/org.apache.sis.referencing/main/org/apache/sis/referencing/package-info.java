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
 * Base classes for reference systems used for general positioning.
 * An explanation for this package is provided in the {@linkplain org.opengis.referencing OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <p>The most commonly used kinds of Reference Systems in Apache SIS are the <i>Coordinate Reference Systems</i>
 * (CRS), which handle coordinates of arbitrary dimensions. The SIS implementations can handle 2D and 3D coordinates,
 * as well as 4D, 5D, <i>etc</i>. Another less-frequently used kind of Reference System uses labels instead, as in
 * postal address. This package is the root for both kinds, with an emphasis on the one for coordinates.
 * The two kinds of referencing system are implemented in the following packages:</p>
 * <ul>
 *   <li>{@link org.apache.sis.referencing.crs} for <cite>referencing by coordinates</cite> (ISO 19111)</li>
 *   <li>{@link org.apache.sis.referencing.gazetteer} for <cite>referencing by geographic identifiers</cite>
 *       (ISO 19112), together with the linking from geographic identifiers to coordinates.</li>
 * </ul>
 *
 * <h2>Fetching geodetic object instances</h2>
 * Geodetic objects can be instantiated either
 * {@linkplain org.apache.sis.referencing.factory.GeodeticObjectFactory directly by specifying all information to a factory method or constructor}, or
 * {@linkplain org.apache.sis.referencing.factory.GeodeticAuthorityFactory indirectly by specifying the identifier of an entry in a database}.
 * In particular, the <a href="https://epsg.org/">EPSG</a> database provides definitions for many geodetic objects,
 * and Apache SIS provides convenience shortcuts for some of them in the
 * {@link org.apache.sis.referencing.CommonCRS} enumerations. Other convenience methods are
 * {@link org.apache.sis.referencing.CRS#forCode(String)},
 * {@link org.apache.sis.referencing.CRS#fromWKT(String)} and
 * {@link org.apache.sis.referencing.CRS#fromXML(String)}
 *
 * <h2>Usage example</h2>
 * The following example projects a (<var>latitude</var>, <var>longitude</var>) coordinate to
 * a <cite>Universal Transverse Mercator</cite> projection in the zone of the coordinate:
 *
 * {@snippet lang="java" :
 *     GeographicCRS source = CommonCRS.WGS84.geographic();
 *     ProjectedCRS  target = CommonCRS.WGS84.UTM(20, 30);                        // 20°N 30°E   (watch out axis order!)
 *     CoordinateOperation operation = CRS.findOperation(source, target, null);
 *     if (CRS.getLinearAccuracy(operation) > 100) {
 *         // If the accuracy is coarser than 100 metres (or any other threshold at application choice)
 *         // maybe the operation is not suitable. Decide here what to do (throw an exception, etc).
 *     }
 *     MathTransform mt = operation.getMathTransform();
 *     DirectPosition position = new DirectPosition2D(20, 30);                    // 20°N 30°E   (watch out axis order!)
 *     position = mt.transform(position, position);
 *     System.out.println(position);
 *     }
 *
 * <h2>The EPSG database</h2>
 * The EPSG geodetic parameter dataset is a structured database required to:
 *
 * <ul>
 *   <li>define {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems}
 *       (CRS) such that coordinates describe positions unambiguously;</li>
 *   <li>define {@linkplain org.apache.sis.referencing.operation.AbstractCoordinateOperation Coordinate Operations}
 *       that allow coordinates to be changed from one CRS to another CRS.</li>
 * </ul>
 *
 * Various programmatic elements in Apache SIS have a relationship with EPSG entries, including:
 *
 * <ul>
 *   <li>classes or methods implementing a specific coordinate operation method;</li>
 *   <li>enumeration constants representing some specific CRS;</li>
 *   <li>fields containing parameter values.</li>
 * </ul>
 *
 * Relationship with EPSG has two components documented in the javadoc: the object type and the EPSG code.
 * The <var>type</var> specifies which {@link org.opengis.referencing.AuthorityFactory} method to invoke, while
 * the <var>code</var> specifies the argument value to give to that method in order to get the EPSG object.
 * For example, the {@link org.apache.sis.referencing.CommonCRS#WGS84} documentation said that object
 * of type <i>geodetic reference frame</i> is associated to code {@code EPSG:6326}.
 * This means that the EPSG object could be obtained by the following code:
 *
 * {@snippet lang="java" :
 *     DatumAuthorityFactory factory = ...;             // TODO: document how to obtain an EPSG factory.
 *     GeodeticDatum datum = factory.createGeodeticDatum("6326");
 *     }
 *
 * The EPSG objects can also be inspected online on the <a href="https://epsg.org/">EPSG repository</a> web site.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.5
 * @since   0.4
 */
@XmlSchema(location = "http://schemas.opengis.net/gml/3.2.1/referenceSystems.xsd",
           elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns =
{
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(RS_Identifier.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class)
})
package org.apache.sis.referencing;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gco.*;
import org.apache.sis.xml.bind.metadata.*;
import org.apache.sis.xml.bind.referencing.RS_Identifier;
