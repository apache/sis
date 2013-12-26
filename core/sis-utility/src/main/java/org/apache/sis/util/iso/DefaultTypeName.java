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
package org.apache.sis.util.iso;

import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;


/**
 * The name of an attribute type associated to a {@linkplain DefaultMemberName member name}.
 * {@code DefaultTypeName} can be instantiated by any of the following methods:
 *
 * <ul>
 *   <li>{@link DefaultNameFactory#createTypeName(NameSpace, CharSequence)}</li>
 * </ul>
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace} and {@link CharSequence}
 * arguments given to the constructor are also immutable. Subclasses shall make sure that any overridden methods
 * remain safe to call from multiple threads and do not change any public {@code TypeName} state.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@XmlRootElement(name = "TypeName")
public class DefaultTypeName extends DefaultLocalName implements TypeName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7182126541436753582L;

    /**
     * Empty constructor to be used by JAXB only. Despite its "final" declaration,
     * the {@link #name} field will be set by JAXB during unmarshalling.
     */
    private DefaultTypeName() {
    }

    /**
     * Constructs a type name from the given character sequence. The argument are given unchanged
     * to the {@linkplain DefaultLocalName#DefaultLocalName(NameSpace,CharSequence) super-class
     * constructor}.
     *
     * @param scope The scope of this name, or {@code null} for a global scope.
     * @param name  The local name (never {@code null}).
     */
    protected DefaultTypeName(final NameSpace scope, final CharSequence name) {
        super(scope, name);
    }
}
