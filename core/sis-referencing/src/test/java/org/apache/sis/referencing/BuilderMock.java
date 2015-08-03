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
package org.apache.sis.referencing;

import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.GenericName;


/**
 * A {@link Builder} that doesn't build anything. Such builder is useless and is defined here
 * only for testing the content of {@link Builder#properties} map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
final strictfp class BuilderMock extends Builder<BuilderMock> {
    /**
     * Creates a new builder.
     */
    BuilderMock() {
    }

    /**
     * Creates a new builder initialized to the given object.
     */
    BuilderMock(final IdentifiedObject object) {
        super(object);
    }

    /**
     * Convenience accessor for the property value assigned to {@link IdentifiedObject#NAME_KEY}.
     */
    Object getName() {
        return properties.get(IdentifiedObject.NAME_KEY);
    }

    /**
     * Convenience accessor for the property value assigned to {@link IdentifiedObject#ALIAS_KEY}.
     */
    GenericName[] getAliases() {
        return (GenericName[]) properties.get(IdentifiedObject.ALIAS_KEY);
    }

    /**
     * Convenience accessor for the property value assigned to {@link IdentifiedObject#IDENTIFIERS_KEY}.
     */
    Identifier[] getIdentifiers() {
        return (Identifier[]) properties.get(IdentifiedObject.IDENTIFIERS_KEY);
    }

    /**
     * Convenience accessor for aliases or identifiers as strings.
     *
     * @param kind 0 for identifiers, or 1 for names.
     */
    String[] getAsStrings(final int kind) {
        final Object[] values = (kind == 0) ? getIdentifiers() : getAliases();
        final String[] s = new String[values.length + kind];
        if (kind != 0) {
            s[0] = getName().toString();
        }
        for (int i=0; i<values.length; i++) {
            s[i + kind] = values[i].toString();
        }
        return s;
    }
}
