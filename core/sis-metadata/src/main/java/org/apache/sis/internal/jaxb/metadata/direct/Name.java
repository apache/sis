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
package org.apache.sis.internal.jaxb.metadata.direct;

import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.iso.Names;


/**
 * A name to be marshalled instead than the {@link org.apache.sis.util.iso} object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see org.apache.sis.internal.jaxb.gco.GO_GenericName
 *
 * @todo This class could be shared for other kind if names (not just scoped names).
 */
@XmlRootElement(name = "ScopedName", namespace = Namespaces.GCO)
public final class Name {
    /**
     * The scoped name.
     */
    @XmlValue
    private String name;

    /**
     * The code space, or {@code null} if none.
     */
    @XmlAttribute
    private String codeSpace;

    /**
     * Sets the value from the given name.
     *
     * @param name The name to marshal.
     */
    public void set(final GenericName name) {
        this.name = name.toString();
        final NameSpace scope = name.scope();
        if (scope != null && !scope.isGlobal()) {
            codeSpace = scope.name().toString();
        }
    }

    /**
     * Returns the name from the current value.
     *
     * @return The unmarshalled name.
     */
    public GenericName get() {
        return Names.parseGenericName(codeSpace, ":", name);
    }
}
