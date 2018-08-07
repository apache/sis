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

import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.AttributeType;
import org.apache.sis.internal.jaxb.gco.CharSequenceAdapter;
import org.apache.sis.util.ArgumentChecks;


/**
 * Bridges between deprecated {@link FeatureType} / {@link AttributeType} and {@link CharSequence}.
 * {@code FeatureType} and {@code AttributeType} were used in ISO 19115:2003, but have been replaced
 * by {@link CharSequence} in ISO 19115:2014. The corresponding GeoAPI 3.0 interfaces are empty since
 * they were placeholder for future work. We use this {@code LegacyFeatureType} as a temporary bridge,
 * to be removed with GeoAPI 4.0.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @deprecated To be removed after migration to GeoAPI 4.0.
 */
@Deprecated
public final class LegacyFeatureType implements FeatureType, AttributeType, CharSequence {
    /**
     * The adapter doing most of the actual work of converting {@code FeatureType} or {@code AttributeType}
     * to {@code <gco:CharacterSequence>} elements.
     */
    static final CharSequenceAdapter ADAPTER = new CharSequenceAdapter();

    /**
     * The value to wrap as a {@code FeatureType} or {@code AttributeType}.
     */
    private final CharSequence value;

    /**
     * Creates a new type for the given value, which must be non-null.
     *
     * @param  value  the text to wrap in a legacy feature type.
     */
    public LegacyFeatureType(final CharSequence value) {
        ArgumentChecks.ensureNonNull("value", value);
        this.value = value;
    }

    /**
     * Wraps the given {@code FeatureType} or {@code AttributeType} as a {@code CharSequence}.
     */
    static CharSequence wrap(final Object value) {
        return (value == null || value instanceof CharSequence)
                ? (CharSequence) value : new LegacyFeatureType(value.toString());
    }

    /**
     * Delegates to the value given at construction time.
     */
    @Override public int          length()                        {return value.length();}
    @Override public char         charAt(int index)               {return value.charAt(index);}
    @Override public CharSequence subSequence(int start, int end) {return value.subSequence(start, end);}
    @Override public String       toString()                      {return value.toString();}
}
