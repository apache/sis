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
package org.apache.sis.feature.internal.shared;

import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import org.apache.sis.util.iso.Names;
import org.apache.sis.math.Vector;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;

// Specific to the main branch:
import org.apache.sis.feature.AbstractAttribute;


/**
 * Utility methods for instantiating features where the geometry is a trajectory
 * and some property values may change with time.
 *
 * This class is <strong>not</strong> thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class MovingFeatures {
    /**
     * Definition of characteristics containing a list of instants, without duplicates.
     * Should be in chronological order, but this is not verified.
     */
    public static final DefaultAttributeType<Instant> TIME_AS_INSTANTS;

    /**
     * An alternative to {@link #TIME_AS_INSTANTS} used when times cannot be mapped to calendar dates.
     * This characteristic uses the same name as {@code TIME_AS_INSTANTS}. Consequently, at most one
     * of {@code TIME_AS_INSTANTS} and {@code TIME_AS_NUMBERS} can be used on the same property.
     */
    private static final DefaultAttributeType<Number> TIME_AS_NUMBERS;
    static {
        final var scope = Names.createLocalName("OGC", null, "MF");
        final var properties = Map.of(DefaultAttributeType.NAME_KEY, Names.createScopedName(scope, null, "datetimes"));
        TIME_AS_INSTANTS = new DefaultAttributeType<>(properties, Instant.class, 0, Integer.MAX_VALUE, null);
        TIME_AS_NUMBERS  = new DefaultAttributeType<>(properties,  Number.class, 0, Integer.MAX_VALUE, null);
    }

    /**
     * Returns the "datetimes" characteristic to add on an attribute type.
     * The characteristic will expect either {@link Instant} or {@link Number} values,
     * depending on whether a temporal CRS is available or not.
     *
     * @param  hasCRS  whether a temporal CRS is available.
     * @return the "datetimes" characteristic.
     */
    public static DefaultAttributeType<?> characteristic(final boolean hasCRS) {
        return hasCRS ? TIME_AS_INSTANTS : TIME_AS_NUMBERS;
    }

    /**
     * Caches of list of times or instants, used for sharing existing instances.
     * We do this sharing because it is common to have many properties having the same time characteristics.
     */
    private final Map<Vector,InstantList> cache;

    /**
     * Creates a new builder.
     *
     * @param  share  other builder that may share time vectors, or {@code null} if none.
     */
    public MovingFeatures(final MovingFeatures share) {
        cache = (share != null) ? share.cache : new HashMap<>();
    }

    /**
     * Sets the "datetimes" characteristic on the given attribute as a list of {@link Instant} instances.
     * Should be in chronological order, but this is not verified.
     *
     * @param  dest    the attribute on which to set time characteristic.
     * @param  millis  times in milliseconds since the epoch.
     */
    public final void setInstants(final AbstractAttribute<?> dest, final long[] millis) {
        final AbstractAttribute<Instant> c = TIME_AS_INSTANTS.newInstance();
        c.setValues(cache.computeIfAbsent(InstantList.vectorize(millis), InstantList::new));
        dest.characteristics().values().add(c);
    }

    /**
     * Sets the "datetimes" characteristic on the given attribute.
     * If the {@code converter} is non-null, it will be used for converting values to {@link Instant} instances.
     * Otherwise values are stored as-is as time elapsed in arbitrary units since an arbitrary epoch.
     *
     * <p>Values should be in chronological order, but this is not verified.
     * Current implementation does not cache the values, but this policy may be revisited in a future version.</p>
     *
     * @param  dest       the attribute on which to set time characteristic.
     * @param  values     times in arbitrary units since an arbitrary epoch.
     * @param  converter  the CRS to use for converting values to {@link Instant} instances, or {@code null}.
     */
    public static void setTimes(final AbstractAttribute<?> dest, final Vector values, final DefaultTemporalCRS converter) {
        final AbstractAttribute<?> ct;
        if (converter != null) {
            final Instant[] instants = new Instant[values.size()];
            for (int i=0; i<instants.length; i++) {
                instants[i] = converter.toInstant(values.doubleValue(i));
            }
            final AbstractAttribute<Instant> c = TIME_AS_INSTANTS.newInstance();
            c.setValues(UnmodifiableArrayList.wrap(instants));
            ct = c;
        } else {
            final AbstractAttribute<Number> c = TIME_AS_NUMBERS.newInstance();
            c.setValues(values);
            ct = c;
        }
        dest.characteristics().values().add(ct);
    }
}
