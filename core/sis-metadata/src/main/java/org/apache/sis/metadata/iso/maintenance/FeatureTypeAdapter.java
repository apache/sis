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
package org.apache.sis.metadata.iso.maintenance;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.feature.type.FeatureType;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;


/**
 * For (un)marshalling deprecated {@link FeatureType} as a character string,
 * as expected by ISO 19115-3:2016. This is a temporary bridge to be removed
 * after the GeoAPI interfaces has been upgraded to ISO 19115-1:2014 model.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class FeatureTypeAdapter extends XmlAdapter<GO_CharacterString, FeatureType> {
    /**
     * Wrap the given value from {@link DefaultScopeDescription} to the elements
     * defined by ISO 19115-3:2016 schema.
     */
    @Override
    public FeatureType unmarshal(GO_CharacterString value) {
        return new LegacyFeatureType(LegacyFeatureType.ADAPTER.unmarshal(value));
    }

    /**
     * Unwrap the elements defined by ISO 19115-3:2016 schema to the value used by
     * {@link DefaultScopeDescription}.
     */
    @Override
    public GO_CharacterString marshal(FeatureType value) {
        return LegacyFeatureType.ADAPTER.marshal(LegacyFeatureType.wrap(value));
    }
}
