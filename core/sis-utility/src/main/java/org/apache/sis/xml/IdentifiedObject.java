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
package org.apache.sis.xml;

import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;


/**
 * The interface for all SIS objects having identifiers. Identifiers are {@link String} in
 * a namespace identified by a {@link Citation}. The namespace can be some organization like
 * <a href="http://www.epsg.org">EPSG</a> for Coordinate Reference System objects, or a
 * well-known acronym like ISBN for <cite>International Standard Book Number</cite>.
 *
 * <p>When an identified object is marshalled in a ISO 19139 compliant XML document, some identifiers
 * are handled in a special way: they appear as {@code gml:id}, {@code gco:uuid} or {@code xlink:href}
 * attributes of the XML element. Those identifiers can be specified using the {@link IdentifierSpace}
 * enum values as below:</p>
 *
 * {@preformat java
 *     IdentifiedObject object = ...;
 *     object.getIdentifierMap().put(IdentifierSpace.ID, "myID");
 * }
 *
 * <div class="section">Relationship with GeoAPI</div>
 * Identifiers exist also in some (not all) GeoAPI objects. Some GeoAPI objects
 * ({@link org.opengis.metadata.acquisition.Instrument}, {@link org.opengis.metadata.acquisition.Platform},
 * {@link org.opengis.metadata.acquisition.Operation}, {@link org.opengis.metadata.lineage.Processing},
 * <i>etc.</i>) have an explicit single identifier attribute, while other GeoAPI objects
 * ({@link org.opengis.metadata.citation.Citation}, {@link org.opengis.metadata.acquisition.Objective},
 * referencing {@link org.opengis.referencing.IdentifiedObject}, <i>etc.</i>) allow an arbitrary
 * number of identifiers. However GeoAPI does not define explicit methods for handling the {@code id},
 * {@code uuid} or {@code href} attributes, since they are specific to XML marshalling (they do not
 * appear in OGC/ISO abstract specifications). This {@code IdentifiedObject} interface provides a
 * way to handle those identifiers.
 *
 * <p>Note that GeoAPI defines a similar interface, also named {@link org.opengis.referencing.IdentifiedObject}.
 * However that GeoAPI interface is not of general use, since it contains methods like
 * {@link org.opengis.referencing.IdentifiedObject#toWKT() toWKT()} that are specific to referencing
 * or geometric objects. In addition, the GeoAPI interface defines some attributes
 * ({@linkplain org.opengis.referencing.IdentifiedObject#getName() name},
 * {@linkplain org.opengis.referencing.IdentifiedObject#getAlias() alias},
 * {@linkplain org.opengis.referencing.IdentifiedObject#getRemarks() remarks}) that are not needed
 * for the purpose of handling XML {@code id}, {@code uuid} or {@code href} attributes.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see IdentifierSpace
 * @see org.apache.sis.metadata.iso.ISOMetadata
 * @see ReferenceResolver#newIdentifiedObject(MarshalContext, Class, Identifier[])
 */
public interface IdentifiedObject {
    /**
     * Returns all identifiers associated to this object. Each {@linkplain Identifier#getCode()
     * identifier code} shall be unique in the {@linkplain Identifier#getAuthority() identifier
     * authority} name space. Examples of namespace are:
     *
     * <ul>
     *   <li>{@linkplain org.apache.sis.metadata.iso.citation.Citations#EPSG EPSG} codes</li>
     *   <li><cite>Universal Product Code</cite> (UPC)</li>
     *   <li><cite>National Stock Number</cite> (NSN)</li>
     *   <li><cite>International Standard Book Number</cite>
     *       ({@linkplain org.apache.sis.metadata.iso.citation.Citations#ISBN ISBN})</li>
     *   <li><cite>International Standard Serial Number</cite>
     *       ({@linkplain org.apache.sis.metadata.iso.citation.Citations#ISSN ISSN})</li>
     *   <li><cite>Universally Unique Identifier</cite> ({@linkplain java.util.UUID})</li>
     *   <li>XML {@linkplain IdentifierSpace#ID ID} attribute</li>
     *   <li>{@linkplain XLink} ({@code href}, {@code role}, {@code arcrole}, {@code title},
     *       {@code show} and {@code actuate} attributes)</li>
     * </ul>
     *
     * Note that XML ID attribute are actually unique only in the scope of the XML document
     * being processed.
     *
     * @return All identifiers associated to this object, or an empty collection if none.
     *
     * @see org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers()
     * @see org.apache.sis.metadata.iso.acquisition.DefaultObjective#getIdentifiers()
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#getIdentifiers()
     */
    Collection<? extends Identifier> getIdentifiers();

    /**
     * A map view of the {@linkplain #getIdentifiers() identifiers} collection
     * as (<var>authority</var>, <var>code</var>) entries.
     * Each {@linkplain java.util.Map.Entry map entry} is associated
     * to an element from the above identifier collection in which the
     * {@linkplain java.util.Map.Entry#getKey() key} is the
     * {@linkplain Identifier#getAuthority() identifier authority} and the
     * {@linkplain java.util.Map.Entry#getValue() value} is the
     * {@linkplain Identifier#getCode() identifier code}.
     *
     * <p>There is usually a one-to-one relationship between the map entries and the identifier
     * elements, but not always:</p>
     *
     * <ul>
     *   <li>The map view may contain less entries, because the map interface allows only one
     *   entry per authority. If the {@linkplain #getIdentifiers() identifier collection} contains
     *   many identifiers for the same authority, then only the first occurrence is visible through
     *   this {@code Map} view.</li>
     *
     *   <li>The map view may also contain more entries than the {@linkplain #getIdentifiers()
     *   identifier collection}. For example the {@link org.opengis.metadata.citation.Citation}
     *   interface defines separated attributes for ISBN, ISSN and other identifiers. This map
     *   view may choose to unify all those attributes in a single view.</li>
     * </ul>
     *
     * The map supports {@link IdentifierMap#put(Object, Object) put} operations
     * if and only if this {@code IdentifiedObject} is modifiable.
     *
     * @return The identifiers as a map of (<var>authority</var>, <var>code</var>) entries,
     *         or an empty map if none.
     */
    IdentifierMap getIdentifierMap();
}
