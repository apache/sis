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
import java.io.IOException;
import org.apache.sis.math.Vector;
import org.apache.sis.internal.netcdf.DataType;
import org.apache.sis.internal.netcdf.DiscreteSampling;
import org.apache.sis.internal.netcdf.Resources;
import org.apache.sis.storage.DataStoreException;
import ucar.nc2.constants.CF;


/**
 * Implementations of the discrete sampling features decoder. This implementation shall be able to decode at least the
 * NetCDF files encoded as specified in the OGC 16-114 (OGC Moving Features Encoding Extension: NetCDF) specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class FeaturesInfo extends DiscreteSampling {
    /**
     * The number of instances for each feature.
     */
    private final Vector counts;

    /**
     * The moving feature identifiers ("mfIdRef").
     */
    private final VariableInfo identifiers;

    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     *
     * @param  counts       the count of instances per feature.
     * @param  identifiers  the feature identifiers.
     */
    private FeaturesInfo(final Vector counts, final VariableInfo identifiers) {
        this.counts      = counts;
        this.identifiers = identifiers;
    }

    /**
     * Returns {@code true} if the given attribute value is one of the {@code cf_role} attribute values
     * supported by this implementation.
     */
    private static boolean isSupportedRole(final Object role) {
        return (role instanceof String) && ((String) role).equalsIgnoreCase(CF.TRAJECTORY_ID);
    }

    /**
     * Creates new discrete sampling parsers from the attribute values found in the given decoder.
     */
    static FeaturesInfo[] create(final ChannelDecoder decoder) throws IOException, DataStoreException {
        final List<FeaturesInfo> features = new ArrayList<>(3);     // Will usually contain at most one element.
search: for (final VariableInfo counts : decoder.variables) {
            /*
             * Any one-dimensional integer variable having a "sample_dimension" attribute string value
             * will be taken as an indication that we have Discrete Sampling Geometries. That variable
             * shall be counting the number of feature instances, and another variable having the same
             * dimension (optionally plus a character dimension) shall give the feature identifiers.
             * Example:
             *
             *     dimensions:
             *         identifiers = 100;
             *         points = UNLIMITED;
             *     variables:
             *         int identifiers(identifiers);
             *             identifiers:cf_role = "trajectory_id";
             *         int counts(identifiers);
             *             counts:sample_dimension = "points";
             */
            if (counts.dimensions.length == 1 && counts.getDataType().isInteger) {
                final Object sampleDimName = counts.getAttributeValue(CF.SAMPLE_DIMENSION);
                if (sampleDimName instanceof String) {
                    final Dimension featureDimension = counts.dimensions[0];
                    final Dimension sampleDimension = decoder.findDimension((String) sampleDimName);
                    if (sampleDimension == null) {
                        decoder.listeners.warning(decoder.resources().getString(Resources.Keys.DimensionNotFound_3,
                                decoder.getFilename(), counts.getName(), sampleDimName), null);
                        continue;
                    }
                    /*
                     * We should have another variable of the same name than the feature dimension name
                     * ("identifiers" in above example). That variable should have a "cf_role" attribute
                     * set to one of the values known to current implementation.  If we do not find such
                     * variable, search among other variables before to give up. That second search is not
                     * part of CF convention and will be accepted only if there is no ambiguity.
                     */
                    VariableInfo identifiers = decoder.findVariable(featureDimension.name);
                    if (identifiers == null || !isSupportedRole(identifiers.getAttributeValue(CF.CF_ROLE))) {
                        VariableInfo replacement = null;
                        for (final VariableInfo alt : decoder.variables) {
                            if (alt.dimensions.length != 0 && alt.dimensions[0] == featureDimension
                                    && isSupportedRole(alt.getAttributeValue(CF.CF_ROLE)))
                            {
                                if (replacement != null) {
                                    replacement = null;
                                    break;                  // Ambiguity found: consider that we found no replacement.
                                }
                                replacement = alt;
                            }
                        }
                        if (replacement != null) {
                            identifiers = replacement;
                        }
                        if (identifiers == null) {
                            decoder.listeners.warning(decoder.resources().getString(Resources.Keys.VariableNotFound_2,
                                    decoder.getFilename(), featureDimension.name), null);
                            continue;
                        }
                    }
                    /*
                     * At this point we found a variable that should be the feature identifiers.
                     * Verify that the variable dimensions are valid.
                     */
                    for (int i=0; i<identifiers.dimensions.length; i++) {
                        final boolean isValid;
                        switch (i) {
                            case 0:  isValid = (identifiers.dimensions[0] == featureDimension); break;
                            case 1:  isValid = (identifiers.getDataType() == DataType.CHAR); break;
                            default: isValid = false; break;                    // Too many dimensions
                        }
                        if (!isValid) {
                            decoder.listeners.warning(decoder.resources().getString(
                                    Resources.Keys.UnexpectedDimensionForVariable_4,
                                    decoder.getFilename(), identifiers.getName(),
                                    featureDimension.getName(), identifiers.dimensions[i].name), null);
                            continue search;
                        }
                    }
                    /*
                     * At this point, all information have been verified as valid.
                     */
                    features.add(new FeaturesInfo(counts.read().compress(0), identifiers));
                }
            }
        }
        return features.toArray(new FeaturesInfo[features.size()]);
    }
}
