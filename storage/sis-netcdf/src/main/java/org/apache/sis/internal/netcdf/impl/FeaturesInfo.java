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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.function.Consumer;
import java.io.IOException;
import org.apache.sis.math.Vector;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.netcdf.DataType;
import org.apache.sis.internal.netcdf.DiscreteSampling;
import org.apache.sis.internal.netcdf.Resources;
import org.apache.sis.internal.feature.MovingFeature;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.util.collection.BackingStoreException;
import ucar.nc2.constants.CF;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;


/**
 * Implementations of the discrete sampling features decoder. This implementation shall be able to decode at least the
 * netCDF files encoded as specified in the OGC 16-114 (OGC Moving Features Encoding Extension: netCDF) specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
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
     * The variable that contains <var>x</var> and <var>y</var> coordinate values (typically longitudes and latitudes).
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
    private final FeatureType type;

    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     *
     * @param  decoder      the source of the features to create.
     * @param  counts       the count of instances per feature.
     * @param  identifiers  the feature identifiers.
     * @param  time         the variable that contains time.
     * @param  coordinates  the variable that contains <var>x</var> and <var>y</var> coordinate values.
     * @param  properties   the variables that contain custom properties.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    @SuppressWarnings("rawtypes")                               // Because of generic array creation.
    private FeaturesInfo(final ChannelDecoder decoder,
            final Vector counts, final VariableInfo identifiers, final VariableInfo time,
            final Collection<VariableInfo> coordinates, final Collection<VariableInfo> properties)
    {
        super(decoder.geomlib, decoder.listeners);
        this.counts      = counts;
        this.identifiers = identifiers;
        this.coordinates = coordinates.toArray(new VariableInfo[coordinates.size()]);
        this.properties  = properties .toArray(new VariableInfo[properties .size()]);
        this.time        = time;
        /*
         * Creates a description of the features to be read.
         */
        final Map<String,Object> info = new HashMap<>(4);
        final PropertyType[] pt = new PropertyType[this.properties.length + 2];
        AttributeType[] characteristics = null;
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
                    characteristics = new AttributeType[] {MovingFeature.TIME};
                    break;
                }
                default: {
                    // TODO: use more accurate Number subtype for value class.
                    variable        = this.properties[i-2];
                    valueClass      = (variable.meaning(0) != null) ? String.class : Number.class;
                    minOccurs       = 0;
                    maxOccurs       = Integer.MAX_VALUE;
                    break;
                }
            }
            info.put(DefaultAttributeType.NAME_KEY, (variable != null) ? variable.getName() : "trajectory");
            // TODO: add description.
            pt[i] = new DefaultAttributeType<>(info, valueClass, minOccurs, maxOccurs, null, characteristics);
        }
        String name = "Features";       // TODO: find a better name.
        info.put(DefaultAttributeType.NAME_KEY, decoder.nameFactory.createLocalName(decoder.namespace, name));
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
     *
     * @throws IllegalArgumentException if the geometric object library is not available.
     * @throws ArithmeticException if the size of a variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
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
                    final DimensionInfo featureDimension = counts.dimensions[0];
                    final DimensionInfo sampleDimension = decoder.findDimension((String) sampleDimName);
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
                        features.add(new FeaturesInfo(decoder, counts.read(), identifiers, time, coordinates.values(), properties));
                    }
                }
            }
        }
        return features.toArray(new FeaturesInfo[features.size()]);
    }

    /**
     * Returns the type of all features to be read by this {@code FeaturesInfo}.
     */
    @Override
    public FeatureType getType() {
        return type;
    }

    /**
     * Returns the number of features in this set.
     *
     * @return the number of features.
     */
    @Override
    protected Integer getFeatureCount() {
        return counts.size();
    }

    /**
     * Returns the stream of features.
     *
     * @param  parallel  ignored, since current version does not support parallelism.
     */
    @Override
    public Stream<Feature> features(boolean parallel) {
        return StreamSupport.stream(new Iter(), false);
    }

    /**
     * Implementation of the iterator returned by {@link #features(boolean)}.
     */
    private final class Iter implements Spliterator<Feature> {
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

        /**
         * Executes the given action only on the next feature, if any.
         *
         * @throws ArithmeticException if the size of a variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
         * @throws BackingStoreException if an {@link IOException} or {@link DataStoreException} occurred.
         *
         * @todo current reading process implies lot of seeks, which is inefficient.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super Feature> action) {
            final int length = counts.intValue(index);
            final GridExtent extent = new GridExtent(null, new long[] {position},
                            new long[] {Math.addExact(position, length)}, false);
            final int[] step = {1};
            final Vector   id, t;
            final Vector[] coords = new Vector[coordinates.length];
            final Object[] props  = new Object[properties.length];
            try {
                id = identifiers.read();                    // Efficiency should be okay because of cached value.
                t = time.read(extent, step);
                for (int i=0; i<coordinates.length; i++) {
                    coords[i] = coordinates[i].read(extent, step);
                }
                for (int i=0; i<properties.length; i++) {
                    final VariableInfo p = properties[i];
                    final Vector data = p.read(extent, step);
                    if (p.isEnumeration()) {
                        final String[] meanings = new String[data.size()];
                        for (int j=0; j<meanings.length; j++) {
                            String m = p.meaning(data.intValue(j));
                            meanings[j] = (m != null) ? m : "";
                        }
                        props[i] = Arrays.asList(meanings);
                    } else {
                        props[i] = data;
                    }
                }
            } catch (IOException | DataStoreException e) {
                throw new BackingStoreException(canNotReadFile(), e);
            }
            final Feature feature = type.newInstance();
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
            feature.setPropertyValue("trajectory", factory.createPolyline(dimension, Vector.create(tmp)));
            action.accept(feature);
            position = Math.addExact(position, length);
            return ++index < counts.size();
        }

        /**
         * Current implementation can not split this iterator.
         */
        @Override
        public Spliterator<Feature> trySplit() {
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
         * The iteration is assumed {@link #ORDERED} in the declaration order in the netCDF file.
         * The iteration is {@link #NONNULL} (i.e. {@link #tryAdvance(Consumer)} is not allowed
         * to return null value) and {@link #IMMUTABLE} (i.e. we do not support modification of
         * the netCDF file while an iteration is in progress).
         *
         * @return characteristics of iteration over the features in the netCDF file.
         */
        @Override
        public int characteristics() {
            return ORDERED | NONNULL | IMMUTABLE | SIZED;
        }
    }
}
