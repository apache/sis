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
package org.apache.sis.internal.jaxb.gml;

import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;


/**
 * JAXB adapter for {@link GMLCodeList}, in order to integrate the value in an element
 * complying with OGC/ISO standard.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
final class CodeListUID {
    /**
     * The code space of the {@link #value} as an URI, or {@code null}.
     */
    @XmlAttribute
    String codeSpace;

    /**
     * The code list identifier.
     */
    @XmlValue
    String value;

    /**
     * Empty constructor for JAXB only.
     */
    private CodeListUID() {
    }

    /**
     * Creates a new adapter for the given value.
     */
    CodeListUID(final String codeSpace, final CodeList<?> code) {
       this.codeSpace = codeSpace;
       value = Types.getCodeName(code);
    }
}
