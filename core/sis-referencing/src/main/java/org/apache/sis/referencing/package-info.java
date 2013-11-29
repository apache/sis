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
 * <p>The most commonly used kinds of Reference Systems in Apache SIS are the <cite>Coordinate Reference Systems</cite>
 * (CRS), which handle coordinates of arbitrary dimensions. The SIS implementations can handle 2D and 3D coordinates,
 * as well as 4D, 5D, <i>etc</i>. An other less-frequently used kind of Reference System uses labels instead, as in
 * postal address. This package is the root for both kinds, with an emphasis on the one for coordinates.</p>
 *
 * {@section Fetching geodetic object instances}
 * Geodetic objects can be instantiated either directly by specifying all information to a factory method
 * or constructor, or indirectly by specifying the identifier of an entry in a database. In particular,
 * the <a href="http://www.epsg.org">EPSG</a> database provides definitions for many geodetic objects,
 * and Apache SIS provides convenience shortcuts for some of them in the
 * {@link org.apache.sis.referencing.GeodeticObjects} enumerations.
 *
 * {@section The EPSG database}
 * The EPSG geodetic parameter dataset is a structured database required to:
 *
 * <ul>
 *   <li>define {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem Coordinate Reference Systems}
 *       (CRS) such that coordinates describe positions unambiguously;</li>
 *   <li>define {@linkplain org.opengis.referencing.operation.CoordinateOperation Coordinate Operations}
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
 * For example the {@link org.apache.sis.referencing.GeodeticObjects#WGS84} documentation said that object
 * of type <cite>geodetic datum</cite> is associated to code {@code EPSG:6326}.
 * This means that the EPSG object could be obtained by the following code:
 *
 * {@preformat java
 *   DatumAuthorityFactory factory = ...; // TODO: document how to obtain an EPSG factory.
 *   GeodeticDatum datum = factory.createGeodeticDatum("6326");
 * }
 *
 * The EPSG objects can also be inspected online on the <a href="http://www.epsg-registry.org/">EPSG registry</a> web site.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns = {
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(RS_Identifier.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringConverter.class)
})
package org.apache.sis.referencing;

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
import org.apache.sis.internal.jaxb.referencing.RS_Identifier;
