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

import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.IOException;
import org.apache.sis.math.Vector;
import org.apache.sis.internal.netcdf.DataType;
import org.apache.sis.internal.netcdf.DiscreteSampling;
import org.apache.sis.internal.netcdf.Resources;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.MovingFeature;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import ucar.nc2.constants.CF;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Spliterator;
import org.apache.sis.internal.jdk8.Stream;
import org.apache.sis.internal.jdk8.StreamSupport;
import org.apache.sis.internal.jdk8.Consumer;
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.AbstractIdentifiedType;


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
     * The amount of identifiers shall be the same than the length of the {@link #counts} vector.
     */
    private final VariableInfo identifiers;

    /**
     * The variable that contains time.
     */
    private final VariableInfo time;

    /**
     * The variable that contains <var>x</var> and <var>y</var> ordinate values (typically longitudes and latitudes).
     * All variables in this array shall have the same length, and that length shall be the same than {@link #time}.
     */
    private final VariableInfo[] coordinates;

    /**
     * Any custom properties.
     */
    private final VariableInfo[] properties;

    /**
     * The type of all features to be read by this {@code FeaturesInfo}.
     */
    private final DefaultFeatureType type;

    private final Geometries<?> factory = Geometries.implementation(null);          // TODO: shall be given by the store.

    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     *
     * @param  counts       the count of instances per feature.
     * @param  identifiers  the feature identifiers.
     */
    @SuppressWarnings("rawtypes")                               // Because of generic array creation.
    private FeaturesInfo(final Vector counts, final VariableInfo identifiers, final VariableInfo time,
            final Collection<VariableInfo> coordinates, final Collection<VariableInfo> properties)
    {
        this.counts      = counts;
        this.identifiers = identifiers;
        this.coordinates = coordinates.toArray(new VariableInfo[coordinates.size()]);
        this.properties  = properties .toArray(new VariableInfo[properties .size()]);
        this.time        = time;
        /*
         * Creates a description of the features to be read.
         */
        final Map<String,Object> info = new HashMap<>(4);
        final AbstractIdentifiedType[] pt = new AbstractIdentifiedType[this.properties.length + 2];
        DefaultAttributeType[] characteristics = null;
        for (int i=0; i<pt.length; i++) {
            final VariableInfo variable;
            final Class<?> valueClass;
            int minOccurs = 1;
            int maxOccurs = 1;
            switch (i) {
                case 0: {
                    variable        = identifiers;
                    valueClass      = Integer.class;
                    break;
                }
                case 1: {
                    variable        = null;
                    valueClass      = factory.polylineClass;
                    characteristics = new DefaultAttributeType[] {MovingFeature.TIME};
                    break;
                }
                default: {
                    variable        = this.properties[i-2];
                    valueClass      = Number.class;           // TODO: use more accurate value class.
                    minOccurs       = 0;
                    maxOccurs       = Integer.MAX_VALUE;
                    break;
                }
            }
            info.put(DefaultAttributeType.NAME_KEY, (variable != null) ? variable.getName() : "trajectory");
            // TODO: add description.
            pt[i] = new DefaultAttributeType<>(info, valueClass, minOccurs, maxOccurs, null, characteristics);
        }
        info.put(DefaultAttributeType.NAME_KEY, "Feature");     // TODO: find a better name.
        type = new DefaultFeatureType(info, false, null, pt);
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
                     * At this point, all information have been verified as valid. Now search all variables having
                     * the expected sample dimension. Those variable contains the actual data. For example if the
                     * sample dimension name is "points", then we may have:
                     *
                     *     double longitude(points);
                     *         longitude:axis = "X";
                     *         longitude:standard_name = "longitude";
                     *         longitude:units = "degrees_east";
                     *     double latitude(points);
                     *         latitude:axis = "Y";
                     *         latitude:standard_name = "latitude";
                     *         latitude:units = "degrees_north";
                     *     double time(points);
                     *         time:axis = "T";
                     *         time:standard_name = "time";
                     *         time:units = "minutes since 2014-11-29 00:00:00";
                     *     short myCustomProperty(points);
                     */
                    final Map<String,VariableInfo> coordinates = new LinkedHashMap<>();
                    final List<VariableInfo> properties  = new ArrayList<>();
                    for (final VariableInfo data : decoder.variables) {
                        if (data.dimensions.length == 1 && data.dimensions[0] == sampleDimension) {
                            final Object axisType = data.getAttributeValue(CF.AXIS);
                            if (axisType == null) {
                                properties.add(data);
                            } else if (coordinates.put(axisType.toString(), data) != null) {
                                continue search;    // Two axes of the same type: abort.
                            }
                        }
                    }
                    final VariableInfo time = coordinates.remove("T");
                    if (time != null) {
                        features.add(new FeaturesInfo(counts.read().compress(0), identifiers, time, coordinates.values(), properties));
                    }
                }
            }
        }
        return features.toArray(new FeaturesInfo[features.size()]);
    }

    /**
     * Returns the stream of features.
     */
    @Override
    public Stream<AbstractFeature> features() {
        return StreamSupport.stream(new Iter(), false);
    }

    /**
     * Implementation of the iterator returned by {@link #features()}.
     */
    private final class Iter implements Spliterator<AbstractFeature> {
        /**
         * Index of the next feature to read.
         */
        private int index;

        /**
         * Position in the data vectors of the next feature to read.
         * This is the sum of the length of data in all previous features.
         */
        private int position;

        /**
         * Creates a new iterator.
         */
        Iter() {
        }

        @Override
        public void forEachRemaining(Consumer<? super AbstractFeature> action) {
            while (tryAdvance(action));
        }

        /**
         * Executes the given action only on the next feature, if any.
         *
         * @todo current reading process implies lot of seeks, which is inefficient.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super AbstractFeature> action) {
            final int   length = counts.intValue(index);
            final int[] lower  = {position};
            final int[] upper  = {position + length};
            final int[] step   = {1};
            final Vector   id, t;
            final Vector[] coords = new Vector[coordinates.length];
            final Vector[] props  = new Vector[properties.length];
            try {
                id = identifiers.read();                    // Efficiency should be okay because of cached value.
                t = time.read(lower, upper, step);
                for (int i=0; i<coordinates.length; i++) {
                    coords[i] = coordinates[i].read(lower, upper, step);
                }
                for (int i=0; i<properties.length; i++) {
                    props[i] = properties[i].read(lower, upper, step);
                }
            } catch (IOException | DataStoreException e) {
                throw new BackingStoreException(canNotReadFile(), e);
            }
            final AbstractFeature feature = type.newInstance();
            feature.setPropertyValue(identifiers.getName(), id.intValue(index));
            for (int i=0; i<properties.length; i++) {
                feature.setPropertyValue(properties[i].getName(), props[i]);
                // TODO: set time characteristic.
            }
            // TODO: temporary hack - to be replaced by support in Vector.
            final int dimension = coordinates.length;
            final double[] tmp = new double[length * dimension];
            for (int i=0; i<tmp.length; i++) {
                tmp[i] = coords[i % dimension].doubleValue(i / dimension);
            }
            feature.setPropertyValue("trajectory", factory.createPolyline(dimension, Vector.create(tmp, false)));
            action.accept(feature);
            position = JDK8.addExact(position, length);
            return ++index < counts.size();
        }

        /**
         * Current implementation can not split this iterator.
         */
        @Override
        public Spliterator<AbstractFeature> trySplit() {
            return null;
        }

        /**
         * Returns the number of features.
         */
        @Override
        public long estimateSize() {
            return counts.size();
        }

        /**
         * Returns the characteristics of the iteration over feature instances.
         * The iteration is assumed {@link #ORDERED} in the declaration order in the NetCDF file.
         * The iteration is {@link #NONNULL} (i.e. {@link #tryAdvance(Consumer)} is not allowed
         * to return null value) and {@link #IMMUTABLE} (i.e. we do not support modification of
         * the NetCDF file while an iteration is in progress).
         *
         * @return characteristics of iteration over the features in the NetCDF file.
         */
        @Override
        public int characteristics() {
            return ORDERED | NONNULL | IMMUTABLE | SIZED;
        }
    }

    /**
     * Returns the error message for a file that can not be read.
     */
    final String canNotReadFile() {
        return null;    // TODO
    }
}
