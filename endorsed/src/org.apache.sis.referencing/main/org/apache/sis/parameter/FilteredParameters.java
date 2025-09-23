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

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;


/**
 * Wraps the given group of parameters, but hiding some parameters.
 * This is used for hiding contextual parameters such as "semi_major".
 * Hidden parameters will still be provided if explicitly requested.
 * This filtered list is unmodifiable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("CloneableImplementsClone")
final class FilteredParameters extends UnmodifiableParameterValueGroup {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4880548875706032438L;

    /**
     * The filtered parameter values.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final GeneralParameterValue[] filtered;

    /**
     * Creates a filtered view of given parameters.
     */
    private FilteredParameters(final UnmodifiableParameterValueGroup source, final GeneralParameterValue[] filtered) {
        super(source);
        this.filtered = filtered;
    }

    /**
     * Creates a filtered view of given parameters.
     * This method takes a snapshot of descriptor list using the given filter.
     * No reference to that filter is kept after this method execution.
     *
     * @param  source  the group of parameters where values are actually stored.
     * @param  filter    filter for deciding whether to keep a parameter.
     * @return the filtered parameters. May be {@code source} itself.
     */
    static UnmodifiableParameterValueGroup create(final UnmodifiableParameterValueGroup source,
            final Predicate<? super GeneralParameterDescriptor> filter)
    {
        if (source != null && filter != null) {
            final List<GeneralParameterValue> sources = source.values();
            final GeneralParameterValue[] filtered = new GeneralParameterValue[sources.size()];
            int count = 0;
            for (final GeneralParameterValue value : sources) {
                if (filter.test(value.getDescriptor())) {
                    filtered[count++] = value;
                }
            }
            if (count != filtered.length) {
                return new FilteredParameters(source, Arrays.copyOf(filtered, count));
            }
        }
        return source;                // Nothing to filter.
    }

    /**
     * Returns a filtered view over the parameter value.
     */
    @Override
    public List<GeneralParameterValue> values() {
        return UnmodifiableArrayList.wrap(filtered);
    }

    /**
     * Compares the specified object with this parameter for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode) && mode == ComparisonMode.STRICT) {
            final FilteredParameters other = (FilteredParameters) object;
            return Arrays.equals(filtered, other.filtered);
        }
        return false;
    }

    /**
     * Returns a hash value for this parameter.
     */
    @Override
    public int hashCode() {
        return super.hashCode() ^ filtered.length;
    }
}
