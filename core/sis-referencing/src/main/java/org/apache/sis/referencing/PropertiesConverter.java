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

import java.util.Map;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.internal.converter.SurjectiveConverter;

import static org.opengis.metadata.Identifier.CODE_KEY;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.REMARKS_KEY;


/**
 * Converts {@link AbstractIdentifiedObject} entries to {@link NamedIdentifier} entries.
 * This converter:
 *
 * <ul>
 *   <li>Exclude remarks, because they will be taken by {@code AbstractIdentifiedObject}
 *       and we don't want to duplicate them in {@code NamedIdentifier}.</li>
 *   <li>Optionally rename de {@code NAME_KEY} as {@code CODE_KEY} in order to allow
 *       {@code NamedIdentifier} to inherit the name given to {@code AbstractIdentifiedObject}
 *       if no code were explicitely given.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class PropertiesConverter extends SurjectiveConverter<String,String> {
    /**
     * The converter to use if the map already contains a {@code CODE_KEY}.
     */
    private static final PropertiesConverter CODE_INCLUDED = new PropertiesConverter(false);

    /**
     * The converter to use if the does not contain a {@code CODE_KEY},
     * in which case it needs to be inferred from the {@code NAME_KEY}.
     */
    private static final PropertiesConverter CODE_INFERED = new PropertiesConverter(true);

    /**
     * {@code true} for renaming {@code NAME_KEY} as {@code CODE_KEY},
     * or {@code false} for excluding the {@code NAME_KEY} instead.
     */
    private final boolean rename;

    /**
     * Constructor for static constants only.
     */
    private PropertiesConverter(final boolean rename) {
        this.rename = rename;
    }

    /**
     * Converts the given map.
     */
    @SuppressWarnings("unchecked")
    static Map<String,?> convert(final Map<String,?> properties) {
        return ObjectConverters.derivedKeys((Map<String,Object>) properties,
                properties.containsKey(CODE_KEY) ? CODE_INCLUDED : CODE_INFERED, Object.class);
    }

    /**
     * Returns the type of keys before conversion.
     */
    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    /**
     * Returns the type of keys after conversion.
     */
    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    /**
     * Converts a key as documented in class javadoc.
     */
    @Override
    public String apply(final String key) {
        if (key != null) {
            if (key.equals(NAME_KEY)) {
                return rename ? CODE_KEY : null;
            }
            if (key.startsWith(REMARKS_KEY)) {
                return null; // Instructs the map to exclude all remarks.
            }
        }
        return key;
    }
}
