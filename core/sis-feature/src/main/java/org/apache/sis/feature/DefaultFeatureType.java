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
package org.apache.sis.feature;

import org.opengis.util.GenericName;


/**
 * Abstraction of a real-world phenomena. A {@code FeatureType} instance describes the class of all
 * {@link DefaultFeature} instances of that type.
 *
 * <div class="note"><b>Note:</b>
 * Compared to the Java language, {@code FeatureType} is equivalent to {@link Class} and
 * {@code Feature} instances are equivalent to {@link Object} instances of that class.</div>
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code FeatureType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultFeatureType} in the API
 * will be replaced by references to the {@code FeatureType} interface.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class DefaultFeatureType extends AbstractIdentifiedType {
    /**
     * Creates a feature type of the given name.
     *
     * @param name The name of this feature type.
     */
    public DefaultFeatureType(final GenericName name) {
        super(name);
    }
}
