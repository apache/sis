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
package org.apache.sis.profile.fra;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.jaxb.metadata.ReferenceSystemMetadata;


/**
 * AFNOR extension to ISO {@link ReferenceSystem}.
 * The following schema fragment specifies the expected content contained within this class.
 *
 * {@preformat xml
 *   <complexType name="FRA_IndirectReferenceSystem_Type">
 *     <complexContent>
 *       <extension base="{http://www.isotc211.org/2005/gmd}MD_ReferenceSystem_Type">
 *       </extension>
 *     </complexContent>
 *   </complexType>
 * }
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 0.4 (derived from geotk-3.00)
 * @since   0.4
 * @module
 */
@XmlType(name = "FRA_IndirectReferenceSystem_Type")
@XmlRootElement(name= "FRA_IndirectReferenceSystem")
public class FRA_IndirectReferenceSystem extends ReferenceSystemMetadata {
    /**
     * For serialization purpose.
     */
    private static final long serialVersionUID = 177802130150613930L;

    /**
     * Empty constructor for JAXB.
     */
    private FRA_IndirectReferenceSystem() {
    }

    /**
     * Creates a new reference system from the given one.
     *
     * @param crs The reference system to partially copy.
     */
    public FRA_IndirectReferenceSystem(final ReferenceSystem crs) {
        super(crs);
    }

    /**
     * Creates a new reference system from the given code.
     *
     * @param identifier The reference system identifier.
     */
    public FRA_IndirectReferenceSystem(final ReferenceIdentifier identifier) {
        super(identifier);
    }

    /**
     * Creates a new reference system from the specified code and authority.
     *
     * @param authority
     *          Organization or party responsible for definition and maintenance of the code space or code.
     * @param codespace
     *          Name or identifier of the person or organization responsible for namespace.
     *          This is often an abbreviation of the authority name.
     * @param code
     *          Identifier code or name, optionally from a controlled list or pattern defined by a code space.
     */
    public FRA_IndirectReferenceSystem(final Citation authority, final String codespace, final String code) {
        super(new ImmutableIdentifier(authority, codespace, code));
    }
}
