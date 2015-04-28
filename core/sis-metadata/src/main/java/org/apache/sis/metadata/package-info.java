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
 * Root package for various metadata implementations.
 *
 * <div class="section">Foreword</div>
 * Many metadata standards exist, including <cite>Dublin core</cite>, <cite>ISO 19115</cite> and the Image I/O
 * metadata defined in {@link javax.imageio.metadata}. The SIS implementation focuses on ISO 19115 (including
 * its ISO 19115-2 extension), but the classes are designed in a way that allow the usage of different standards.
 * This genericity goal should be keep in mind in the discussion below.
 *
 * <div class="section">How Metadata are defined</div>
 * A metadata standard is defined by a set of Java interfaces belonging to a specific package and its sub-packages.
 * For example the ISO 19115 standard is defined by the <a href="http://www.geoapi.org">GeoAPI</a> interfaces
 * defined in the {@link org.opengis.metadata} package and sub-packages. That standard is identified in SIS by the
 * {@link org.apache.sis.metadata.MetadataStandard#ISO_19115} constant. Other standards are defined as well,
 * for example the {@link org.apache.sis.metadata.MetadataStandard#ISO_19123} constant stands for the standards
 * defined by the interfaces in the {@link org.opengis.coverage} package and sub-packages.
 *
 * <p>For each interface, the collection of declared getter methods defines its <cite>properties</cite>
 * (or <cite>attributes</cite>). If a {@link org.opengis.annotation.UML} annotation is attached to the getter method,
 * the identifier declared in that annotation is taken as the property name. This is typically the name defined by the
 * International Standard from which the interface is derived. Otherwise (if there is no {@code UML} annotation)
 * the property name is inferred from the method name like what the <cite>Java Beans</cite> framework does.</p>
 *
 * <p>The implementation classes, if they exist, are defined in different packages than the interfaces.
 * For example the ISO 19115 interfaces, declared in {@link org.opengis.metadata}, are implemented by
 * SIS in {@link org.apache.sis.metadata.iso}. The sub-packages hierarchy is the same, and the names
 * of implementation classes are the name of the implemented interfaces prefixed with {@code Abstract}
 * or {@code Default}.</p>
 *
 * <p><b>Notes:</b></p>
 * <ul class="verbose">
 *   <li>The {@code Abstract} prefix means that the class is abstract in the sense of the implemented standard.
 *       It it not necessarily abstract in the sense of Java. Because incomplete metadata are common in practice,
 *       sometime we wish to instantiate an "abstract" class despite the lack of knowledge about the exact sub-type.</li>
 *   <li>The properties are determined by the getter methods declared in the interfaces.
 *       Getter methods declared in the implementation classes are ignored.</li>
 *   <li>Setter methods, if any, can be declared in the implementation classes without the need for declarations
 *       in the interfaces. In other words, interfaces are assumed read-only unless a specific implementation provide
 *       setter methods.</li>
 *   <li>The implementation is not required to exist as source code. They can be generated on the fly with
 *       {@link java.lang.reflect.Proxy}. This is the approach taken by the {@link org.apache.sis.metadata.sql}
 *       package for generating metadata implementations backed by the content of a database.</li>
 * </ul>
 *
 * <div class="section">How Metadata are handled</div>
 * Metadata objects in SIS are mostly containers: they provide getter and setter methods for manipulating the values
 * associated to properties (for example the {@code title} property of a {@code Citation} object), but provide few logic.
 * The package {@link org.apache.sis.metadata.iso} and its sub-packages are the main examples of such containers.
 *
 * <p>In addition, the metadata modules provide support methods for handling the metadata objects through Java Reflection.
 * This is an approach similar to <cite>Java Beans</cite>, in that users are encouraged to use directly the API of
 * <cite>Plain Old Java</cite> objects (actually interfaces) every time their type is known at compile time,
 * and fallback on the reflection technic when the type is known only at runtime.</p>
 *
 * <p>Using Java reflection, a metadata can be viewed in many different ways:</p>
 * <ul class="verbose">
 *   <li><b>As a {@link java.util.Map}</b><br>
 *       The {@link org.apache.sis.metadata.MetadataStandard} class provides various methods returning a view
 *       of an arbitrary metadata implementation as a {@code Map}, where the key are the property names and the
 *       values are the return values, types or descriptions of getter methods. The map is writable if the
 *       underlying metadata implementation has setter methods, otherwise attempts to set a value throw an
 *       {@code UnmodifiableMetadataException}.</li>
 *
 *   <li><b>As a {@link org.apache.sis.util.collection.TreeTable}</b><br>
 *       The metadata are organized as a tree. For example the {@code Citation} metadata contains one or many
 *       {@code ResponsibleParty} elements, each of them containing a {@code Contact} element, which contains
 *       a {@code Telephone} element, <i>etc</i>. For each node, there is many information that can be displayed
 *       in columns:
 *       <ul>
 *         <li>A description of the element.</li>
 *         <li>The type of values ({@code String}, {@code double}, <i>etc</i>).</li>
 *         <li>The range of valid values (if the type is numeric),
 *             or an enumeration of valid values (if the type is a code list).</li>
 *         <li>The value stored in the element, or the default value.</li>
 *       </ul></li>
 *
 *   <li><b>As a table record in a database (using {@link org.apache.sis.metadata.sql})</b><br>
 *       It is possible to establish the following mapping between metadata and a SQL database:
 *       <ul>
 *         <li>Each metadata interface maps to a table of the same name in the database.</li>
 *         <li>Each property in the above interface maps to a column of the same name in the above table.</li>
 *         <li>Each instance of the above interface is a record in the above table.</li>
 *       </ul>
 *       Using Java reflection, it is possible to generate implementations of the metadata interfaces
 *       where each call to a getter method is translated into a SQL query for the above database.</li>
 * </ul>
 *
 * <div class="section">How Metadata are marshalled</div>
 * The ISO 19139 standard defines how ISO 19115 metadata shall be represented in XML.
 * The SIS library supports XML marshalling and unmarshalling with JAXB annotations.
 *
 * <p>Only the implementation classes defined in the {@link org.apache.sis.metadata.iso} packages and sub-packages
 * are annotated for JAXB marshalling. If a metadata is implemented by an other package (for example
 * {@link org.apache.sis.metadata.sql}), then it shall be converted to an annotated class before to be marshalled.
 * All SIS annotated classes provide a copy constructor for this purpose. A shallow copy is sufficient;
 * JAXB adapters will convert the elements on-the-fly when needed.</p>
 *
 * <p>The annotated classes can be given to a JAXB {@code Marshaller}. For best results, it shall be a marshaller
 * obtained from the {@link org.apache.sis.xml.MarshallerPool}, otherwise some XML outputs may be incomplete
 * (missing namespaces for instance). The {@link org.apache.sis.xml.XML} class provides convenience methods
 * for this purpose.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Adrian Custer (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
package org.apache.sis.metadata;
