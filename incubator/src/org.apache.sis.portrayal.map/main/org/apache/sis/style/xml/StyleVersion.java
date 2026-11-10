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
package org.apache.sis.style.xml;


/**
 * The version of the Styled Layer Descriptor and Symbology Encoding standards.
 * Each version also determines the versions of the Filter Encoding and GML standards
 * used for the filters and the geometries embedded in the styles.
 *
 * <table class="sis">
 *   <caption>Standards used by each version</caption>
 *   <tr><th>Version</th> <th>Styles</th> <th>Symbology</th> <th>Filters</th> <th>Geometries</th></tr>
 *   <tr><td>{@link #V1_0_0}</td> <td>SLD 1.0.0</td> <td>SLD 1.0.0</td> <td>FE 1.0.0</td> <td>GML 2</td></tr>
 *   <tr><td>{@link #V1_1_0}</td> <td>SLD 1.1.0</td> <td>SE 1.1.0</td>  <td>FE 1.1.0</td> <td>GML 3.1</td></tr>
 * </table>
 *
 * @author  Johann Sorel (Geomatys)
 */
public enum StyleVersion {
    /**
     * OGC 02-070 — Styled Layer Descriptor 1.0.0.
     * In this version, the symbology elements are defined in the SLD namespace,
     * the filters use Filter Encoding 1.0.0 and the geometries use GML 2.
     */
    V1_0_0("1.0.0"),

    /**
     * OGC 05-078r4 — Styled Layer Descriptor 1.1.0, together with
     * OGC 05-077r4 — Symbology Encoding 1.1.0.
     * In this version, the symbology elements are defined in the SE namespace,
     * the filters use Filter Encoding 1.1.0 and the geometries use GML 3.1.
     */
    V1_1_0("1.1.0");

    /**
     * The version number as written in the {@code version} attribute of XML documents.
     * For example {@code "1.1.0"}.
     */
    final String code;

    /**
     * Creates a new enumeration value.
     *
     * @param  code  the version number as written in XML documents.
     */
    private StyleVersion(final String code) {
        this.code = code;
    }

    /**
     * Returns the version number as written in the {@code version} attribute of XML documents.
     *
     * @return the version number, for example {@code "1.1.0"}.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the enumeration value for the given version number.
     *
     * @param  code  the version number, for example {@code "1.1.0"}.
     * @return the enumeration value, or {@code null} if the given version is not recognized.
     */
    static StyleVersion forCode(final String code) {
        if (code != null) {
            for (final StyleVersion v : values()) {
                if (v.code.equals(code.trim())) {
                    return v;
                }
            }
        }
        return null;
    }
}
