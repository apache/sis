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
package org.apache.sis.metadata;

import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.citation.Citation;


/**
 * Map of information for a given implementation class. This map is read-only.
 * All values in this map are instances of {@link PropertyInformation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see PropertyInformation
 * @see MetadataStandard#asInformationMap(Class, KeyNamePolicy)
 */
final class InformationMap extends PropertyMap<ExtendedElementInformation> {
    /**
     * The standard which define the {@link PropertyAccessor#type} interface.
     */
    private final Citation standard;

    /**
     * Creates an information map for the specified accessor.
     *
     * @param standard   the standard which define the {@code accessor.type} interface.
     * @param accessor   the accessor to use for the metadata.
     * @param keyPolicy  determines the string representation of keys in the map.
     */
    InformationMap(final Citation standard, final PropertyAccessor accessor, final KeyNamePolicy keyPolicy) {
        super(accessor, keyPolicy);
        this.standard = standard;
    }

    /**
     * Returns the information for the property at the specified index.
     */
    @Override
    final ExtendedElementInformation getReflectively(final int index) {
        return accessor.information(standard, index);
    }
}
