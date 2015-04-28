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
package org.apache.sis.internal.profile.fra;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;


/**
 * AFNOR extension to ISO {@link Constraints}.
 * This extension adds a {@link #getCitations()} property citing the documents that specify the constraints.
 * In the 2013 revision of ISO 19115, this property is available as {@link #getReferences()}.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 *
 * {@preformat xml
 *   <complexType name="FRA_Constraints_Type">
 *     <complexContent>
 *       <extension base="{http://www.isotc211.org/2005/gmd}MD_Constraints_Type">
 *         <sequence>
 *           <element name="citation" type="{http://www.isotc211.org/2005/gmd}CI_Citation_PropertyType" maxOccurs="unbounded" minOccurs="0"/>
 *         </sequence>
 *       </extension>
 *     </complexContent>
 *   </complexType>
 * }
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.4
 * @module
 */
@XmlType(name = "FRA_Constraints_Type")
@XmlRootElement(name= "FRA_Constraints")
public class Constraints extends DefaultConstraints {
    /**
     * For serialization purpose.
     */
    private static final long serialVersionUID = -5558935205709762055L;

    /**
     * The documents that specifies the nature of the constraints.
     */
    private Collection<Citation> citations;

    /**
     * Constructs an initially empty constraints.
     */
    public Constraints() {
    }

    /**
     * Constructs an instance initialized to a copy of the given object.
     * This constructor does <strong>not</strong> copy the FRA-specific properties.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public Constraints(final org.opengis.metadata.constraint.Constraints object) {
        super(object);
    }

    /**
     * Returns the documents that specifies the nature of the constraints.
     *
     * @return Citations to the current documents.
     */
    @XmlElement(name = "citation")
    public Collection<Citation> getCitations() {
        return citations = nonNullCollection(citations, Citation.class);
    }

    /**
     * Sets the documents that specifies the nature of the constraints.
     *
     * @param newValues Citation to the new documents.
     */
    public void setCitations(final Collection<? extends Citation> newValues) {
        citations = writeCollection(newValues, citations, Citation.class);
    }
}
