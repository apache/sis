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
package org.apache.sis.storage.landsat;

import org.apache.sis.util.resources.Vocabulary;
import org.opengis.util.InternationalString;


/**
 * Group of bands.
 * All images of the same group for a given scene have the same size in pixels.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
enum BandGroupName {
    /**
     * Group for bands 1, 2, 3, 4, 5, 6, 7, 9.
     */
    REFLECTIVE(Vocabulary.Keys.Reflective, Vocabulary.formatInternational(Vocabulary.Keys.Reflectance), true),

    /**
     * Group for band 8.
     */
    PANCHROMATIC(Vocabulary.Keys.Panchromatic, REFLECTIVE.measurement, true),

    /**
     * Group for bands 10, 11.
     */
    THERMAL(Vocabulary.Keys.Thermal, Vocabulary.formatInternational(Vocabulary.Keys.Radiance), false);

    /**
     * Localized name of the group.
     */
    final InternationalString title;

    /**
     * Name of the measurement.
     */
    final InternationalString measurement;

    /**
     * Whether bands in this group measure reflectance.
     * If {@code false}, then only radiance is provided.
     */
    final boolean reflectance;

    /**
     * Creates a new enumeration value.
     */
    private BandGroupName(final short name, final InternationalString measurement, final boolean reflectance) {
        this.title = Vocabulary.formatInternational(name);
        this.measurement = measurement;
        this.reflectance = reflectance;
    }
}
