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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;


/**
 * JAXB data container for a {@code DerivedCRSType} pseudo code list.
 * SIS does not actually provide that code list (we implement the associated interfaces instead,
 * e.g. {@link org.opengis.referencing.crs.VerticalCRS} for {@code SC_DerivedCRSType.vertical}),
 * so we reconstruct in this adapter what would be marshalled if we had that code list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class SC_DerivedCRSType {
    /**
     * The code space (e.g. {@code "EPSG"}).
     */
    @XmlAttribute
    String codeSpace;

    /**
     * The derived CRS type. Can be one of the following values (from ISO 19111:2007):
     *
     * <ul>
     *   <li>{@code geodetic}</li>
     *   <li>{@code vertical}</li>
     *   <li>{@code time} (from ISO 19162)</li>
     *   <li>{@code engineering}</li>
     *   <li>{@code image}</li>
     * </ul>
     */
    @XmlValue
    String value;

    /**
     * Returns an instance from the given WKT keyword.
     * The given keyword can be one of the following values (from ISO 19162):
     *
     * <ul>
     *   <li>{@code GeodeticCRS}</li>
     *   <li>{@code VerticalCRS}</li>
     *   <li>{@code TimeCRS}</li>
     *   <li>{@code EngineeringCRS}</li>
     * </ul>
     *
     * @param  keyword The ISO 19162 (WKT 2) keyword, or {@code null}.
     * @return The pseudo code list with ISO 19111 keyword, or {@code null}.
     */
    public static SC_DerivedCRSType fromWKT(final String keyword) {
        if (keyword != null) {
            assert keyword.endsWith("CRS") : keyword;
            final StringBuilder buffer = new StringBuilder().append(keyword, 0, keyword.length() - 3);
            buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
            return new SC_DerivedCRSType(buffer.toString());
        }
        return null;
    }

    /**
     * Creates a pseudo code list for the given value.
     *
     * @param type The ISO 19111 code.
     */
    public SC_DerivedCRSType(final String type) {
        codeSpace = "EPSG";
        value = type;
    }

    /**
     * Empty constructor for JAXB only.
     */
    private SC_DerivedCRSType() {
    }
}
