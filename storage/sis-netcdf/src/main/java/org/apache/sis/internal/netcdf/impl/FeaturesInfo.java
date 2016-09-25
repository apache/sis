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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.IntegerList;
import org.apache.sis.util.resources.Errors;
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
     * The number of instances for each feature.
     */
    private final IntegerList counts;

    /**
     * The moving feature identifiers ("mfIdRef").
     */
    private final VariableInfo identifiers;

    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     *
     * @param  cpf          the count of instances per feature.
     * @param  identifiers  the feature identifiers.
     */
    private FeaturesInfo(final Vector cpf, final VariableInfo identifiers) {
        final int n = cpf.size();
        int max = 1;
        for (int i=0; i<n; i++) {
            final int v = cpf.intValue(i);
            if (v > max) max = v;
        }
        counts = new IntegerList(n, max);
        for (int i=0; i<n; i++) {
            counts.addInt(cpf.intValue(i));
        }
        this.identifiers = identifiers;
        final Object role = identifiers.getAttributeValue(CF.CF_ROLE);
        if (role instanceof String && ((String) role).equalsIgnoreCase(CF.TRAJECTORY_ID)) {
            // TODO
        }
    }

    /**
     * Creates new discrete sampling parsers from the attribute values found in the given decoder.
     */
    static FeaturesInfo[] create(final ChannelDecoder decoder) throws IOException, DataStoreException {
        final List<FeaturesInfo> features = new ArrayList<>(3);     // Will usually contain at most one element.
search: for (final VariableInfo counts : decoder.variables) {
            if (counts.dimensions.length == 1 && counts.getDataType().isInteger) {
                final Object sampleDimName = counts.getAttributeValue(CF.SAMPLE_DIMENSION);
                if (sampleDimName instanceof String) {
                    /*
                     * Any one-dimensional integer variable having a "sample_dimension" attribute string value
                     * will be taken as an indication that we have Discrete Sampling Geometries. That variable
                     * shall be counting the number of feature instances, and another variable having the same
                     * dimension (optionally plus a character dimension) shall give the feature identifiers.
                     */
                    final Dimension featureDimension = counts.dimensions[0];
                    final VariableInfo identifiers = decoder.findVariable(featureDimension.name);
                    if (identifiers == null) {
                        decoder.listeners.warning(decoder.errors().getString(Errors.Keys.VariableNotFound_2,
                                decoder.getFilename(), featureDimension.name), null);
                        continue;
                    }
                    for (int i=0; i<identifiers.dimensions.length; i++) {
                        final boolean isValid;
                        switch (i) {
                            case 0:  isValid = (identifiers.dimensions[0] == featureDimension); break;
                            case 1:  isValid = (identifiers.getDataType() == DataType.CHAR); break;
                            default: isValid = false; break;                    // Too many dimensions
                        }
                        if (!isValid) {
                            decoder.listeners.warning(decoder.errors().getString(
                                    Errors.Keys.UnexpectedDimensionForVariable_4,
                                    decoder.getFilename(), identifiers.getName(),
                                    featureDimension.getName(), identifiers.dimensions[i].name), null);
                            continue search;
                        }
                    }
                    /*
                     * The "sample_dimension" attribute value shall be the name of a dimension.
                     * Those dimensions are associated to other variables (not the count one),
                     * but the 'findDimension' method below searches in all dimensions.
                     */
                    final Dimension sampleDim = counts.findDimension((String) sampleDimName);
                    if (sampleDim == null) {
                        decoder.listeners.warning(decoder.errors().getString(Errors.Keys.DimensionNotFound_3,
                                decoder.getFilename(), counts.getName(), sampleDimName), null);
                        continue;
                    }
                    features.add(new FeaturesInfo(counts.read(), identifiers));
                }
            }
        }
        return features.toArray(new FeaturesInfo[features.size()]);
    }
}
