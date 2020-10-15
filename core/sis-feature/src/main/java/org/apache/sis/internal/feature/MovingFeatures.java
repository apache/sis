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
package org.apache.sis.internal.feature;

import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import org.opengis.util.LocalName;
import org.apache.sis.util.iso.Names;
import org.apache.sis.math.Vector;
import org.apache.sis.feature.DefaultAttributeType;

// Branch-dependent imports
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;


/**
 * Utility methods for instantiating features where the geometry is a trajectory
 * and some property values may change with time.
 *
 * This class is <strong>not</strong> thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
public class MovingFeatures {
    /**
     * Definition of characteristics containing a list of time instants in chronological order, without duplicates.
     */
    public static final AttributeType<Instant> TIME;
    static {
        final LocalName scope = Names.createLocalName("OGC", null, "MF");
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(DefaultAttributeType.NAME_KEY, Names.createScopedName(scope, null, "datetimes"));
        TIME = new DefaultAttributeType<>(properties, Instant.class, 0, Integer.MAX_VALUE, null);
    }

    /**
     * Caches of list of instants, used for sharing existing instances.
     * We do this sharing because it is common to have many properties
     * having the same time characteristics.
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
     * Set the time characteristic on the given attribute.
     *
     * @param  dest    the attribute on which to set time characteristic.
     * @param  millis  times in milliseconds since the epoch.
     */
    public final void setTime(final Attribute<?> dest, final long[] millis) {
        final Attribute<Instant> c = TIME.newInstance();
        c.setValues(cache.computeIfAbsent(InstantList.vectorize(millis), InstantList::new));
        dest.characteristics().values().add(c);
    }
}
