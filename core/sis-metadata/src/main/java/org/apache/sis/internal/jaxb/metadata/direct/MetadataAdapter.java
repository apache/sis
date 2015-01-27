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

import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * Base class for adapters from GeoAPI interfaces to their SIS implementation.
 *
 * @param <BoundType> The GeoAPI interface being adapted.
 * @param <ValueType> The SIS class implementing the interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class MetadataAdapter<BoundType, ValueType extends BoundType>
        extends XmlAdapter<ValueType,BoundType>
{
    /**
     * Empty constructor for subclasses only.
     */
    protected MetadataAdapter() {
    }

    /**
     * Returns the given object unchanged, to be marshalled directly.
     *
     * @param  value The metadata value.
     * @return The value to marshal (which is the same).
     */
    @Override
    public final BoundType unmarshal(final ValueType value) {
        return value;
    }
}
