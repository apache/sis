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
package org.apache.sis.parameter;

import java.util.Map;
import org.apache.sis.measure.Range;


/**
 * A descriptor with a maximum occurrence of 2.
 * This is illegal according ISO 19111 but nevertheless supported by Apache SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@SuppressWarnings("serial")
final class MultiOccurrenceDescriptor<T> extends DefaultParameterDescriptor<T> {
    /**
     * Creates a new descriptor with the given name and default value.
     */
    MultiOccurrenceDescriptor(final Map<String,?> properties, final Class<T> valueClass,
            final Range<?> valueDomain, final T[] validValues, final T defaultValue, final boolean required)
    {
        super(properties, valueClass, valueDomain, validValues, defaultValue, required);
    }

    /**
     * We are cheating here:  {@code maximumOccurs} should always be 1 for {@code ParameterValue}.
     * However, the SIS implementation should be robust enough to accept other values.
     * We use this class test that.
     */
    @Override
    public int getMaximumOccurs() {
        return 2;
    }
}
