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
package org.apache.sis.profile.france;

import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.internal.profile.fra.*;
import org.apache.sis.util.Static;


/**
 * Provides implementations of French extensions defined by AFNOR.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.4
 * @module
 */
public final class FrenchProfile extends Static {
    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "fra"}.
     *
     * @see org.apache.sis.xml.Namespaces
     */
    public static final String NAMESPACE = "http://www.cnig.gouv.fr/2005/fra";

    /**
     * Do not allow instantiation of this class.
     */
    private FrenchProfile() {
    }

    /**
     * Returns the given metadata object as an AFNOR-compliant instance.
     * The current implementation recognizes the following types:
     *
     * <table class="sis">
     *   <caption>AFNOR extensions to ISO 19115</caption>
     *   <tr><th>GeoAPI type</th> <th>AFNOR XML element</th></tr>
     *   <tr><td>{@link org.opengis.metadata.identification.DataIdentification}</td> <td>{@code FRA_DataIdentification}</td>
     *   <tr><td>{@link org.opengis.metadata.constraint.Constraints}</td>            <td>{@code FRA_Constraints}</td>
     *   <tr><td>{@link org.opengis.metadata.constraint.LegalConstraints}</td>       <td>{@code FRA_LegalConstraints}</td>
     *   <tr><td>{@link org.opengis.metadata.constraint.SecurityConstraints}</td>    <td>{@code FRA_SecurityConstraints}</td>
     * </table>
     *
     * This method does not handle the {@link ReferenceSystem} type,
     * because AFNOR requires to specify whether the system is direct or indirect.
     * For reference system types, use {@link #toAFNOR(ReferenceSystem, boolean)} instead.
     *
     * @param metadata The metadata to make AFNOR-compliant, or {@code null}.
     * @return A copy of the metadata as an AFNOR-compliant object, or {@code metadata} if the metadata
     *        was {@code null}, does not have an AFNOR type, or was already of the appropriate type.
     */
    public static Object toAFNOR(Object metadata) {
        if (metadata != null) {
            if (metadata instanceof org.opengis.metadata.identification.DataIdentification) {
                if (!(metadata instanceof DataIdentification)) {
                    metadata = new DataIdentification((org.opengis.metadata.identification.DataIdentification) metadata);
                }
            } else if (metadata instanceof org.opengis.metadata.constraint.Constraints) {
                if (metadata instanceof org.opengis.metadata.constraint.LegalConstraints) {
                    if (!(metadata instanceof LegalConstraints)) {
                        metadata = new LegalConstraints((org.opengis.metadata.constraint.LegalConstraints) metadata);
                    }
                } else if (metadata instanceof org.opengis.metadata.constraint.SecurityConstraints) {
                    if (!(metadata instanceof SecurityConstraints)) {
                        metadata = new SecurityConstraints((org.opengis.metadata.constraint.SecurityConstraints) metadata);
                    }
                } else {
                    if (!(metadata instanceof Constraints)) {
                        metadata = new Constraints((org.opengis.metadata.constraint.Constraints) metadata);
                    }
                }
            }
        }
        return metadata;
    }

    /**
     * Returns the given given reference system as an AFNOR-compliant instance.
     * AFNOR requires the reference systems to be either <cite>direct</cite> or <cite>indirect</cite>.
     * Those two cases are represented by the following schema fragments:
     *
     * <p><b>Direct:</b></p>
     * {@preformat xml
     *   <complexType name="FRA_DirectReferenceSystem_Type">
     *     <complexContent>
     *       <extension base="{http://www.isotc211.org/2005/gmd}MD_ReferenceSystem_Type"/>
     *     </complexContent>
     *   </complexType>
     * }
     *
     * <p><b>Indirect:</b></p>
     * {@preformat xml
     *   <complexType name="FRA_IndirectReferenceSystem_Type">
     *     <complexContent>
     *       <extension base="{http://www.isotc211.org/2005/gmd}MD_ReferenceSystem_Type"/>
     *     </complexContent>
     *   </complexType>
     * }
     *
     * @param  rs The reference system to make AFNOR-compliant, or {@code null}.
     * @param  indirect {@code false} for {@code FRA_DirectReferenceSystem},
     *         or {@code true} for {@code FRA_IndirectReferenceSystem}.
     * @return A copy of the given reference system as an AFNOR-compliant object, or {@code rs}
     *         if the given reference system was {@code null} or already of the appropriate type.
     */
    public static ReferenceSystem toAFNOR(ReferenceSystem rs, final boolean indirect) {
        if (rs != null) {
            if (indirect) {
                if (!(rs instanceof IndirectReferenceSystem)) {
                    rs = new IndirectReferenceSystem(rs);
                }
            } else {
                if (!(rs instanceof DirectReferenceSystem)) {
                    rs = new DirectReferenceSystem(rs);
                }
            }
        }
        return rs;
    }
}
