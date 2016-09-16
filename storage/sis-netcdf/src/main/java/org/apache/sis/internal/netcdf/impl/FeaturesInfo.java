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
package org.apache.sis.internal.netcdf.impl;

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.internal.netcdf.DiscreteSampling;
import ucar.nc2.constants.CF;


/**
 * Implementations of the discrete sampling features decoder. This implementation shall be able to decode at least the
 * NetCDF files encoded as specified in the OGC 16-114 (OGC Moving Features Encoding Extension: NetCDF) specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class FeaturesInfo extends DiscreteSampling {
    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     */
    private FeaturesInfo(final ChannelDecoder decoder, final VariableInfo identifiers) {
        // TODO
    }

    /**
     * Creates new discrete sampling parsers.
     */
    static FeaturesInfo[] create(final ChannelDecoder decoder) {
        final List<FeaturesInfo> features = new ArrayList<>();
        for (final VariableInfo v : decoder.variables) {
            for (final Object role : v.getAttributeValues(CF.CF_ROLE, false)) {
                if (role instanceof String && ((String) role).equalsIgnoreCase(CF.TRAJECTORY_ID)) {
                    features.add(new FeaturesInfo(decoder, v));
                }
            }
        }
        return features.toArray(new FeaturesInfo[features.size()]);
    }
}
