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
package org.apache.sis.internal.netcdf;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.function.Consumer;
import java.util.OptionalLong;
import java.io.IOException;
import org.opengis.metadata.acquisition.GeometryType;
import org.apache.sis.math.Vector;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.feature.MovingFeatures;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.Characters;
import ucar.nc2.constants.CF;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Implementations of the discrete sampling features decoder. This implementation shall be able to decode at least the
 * netCDF files encoded as specified in the OGC 16-114 (OGC Moving Features Encoding Extension: netCDF) specification.
 * This implementation is used as a fallback when the subclass does not provide a more specialized class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
final class FeatureSet extends DiscreteSampling {
    /**
     * Value of {@code "featureType"} global attribute for netCDF files that this class can handle.
     * Also used as property name for the geometry object.
     */
    static final String TRAJECTORY = "trajectory";

    /**
     * The number of instances for each feature, or {@code null} if none. If non-null, then the number of features
     * is the length of this vector and each {@link Feature} instance has multi-valued properties with a number of
     * elements given by this count.
     *
     * If null, then the number of features is determined by the length of other variables.
     *
     * @see #getFeatureCount()
     */
    private final Vector counts;

    /**
     * The singleton properties (for which there is only one value per feature instance), or an empty array if none.
     * If non-empty, it typically contains a single variable which is the moving feature identifiers ("mfIdRef").
     * If {@link #counts} is non-null, then the length of {@code identifiers[i]} variables shall be the same than
     * the length of the {@link #counts} vector.
     */
    private final Variable[] identifiers;

    /**
     * Whether the {@link #coordinates} array contains a temporal variable.
     * If {@code true}, then the time variable shall be last.
     */
    private final boolean hasTime;

    /**
     * The variables for <var>x</var>, <var>y</var> and potentially <var>z</var> or <var>t</var> coordinate values.
     * The <var>x</var> and <var>y</var> coordinates are typically longitudes and latitudes, but not necessarily.
     * If temporal coordinates exist, the time variable must be last and {@link #hasTime} shall be {@code true}.
     * All variables in this array shall have the same length. If {@link #counts} is non-null, then the length
     * of {@code coordinates[i]} variables shall be the sum of all {@link #counts} values.
     */
    private final Variable[] coordinates;

    /**
     * Any time-varying properties other than coordinate values, or an empty array if none.
     * All variables in this array shall have the same length than {@link #coordinates} variables.
     */
    private final Variable[] properties;

    /**
     * The type of all features to be read by this {@code FeatureSet}.
     */
    private final FeatureType type;

    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     * All arrays given to this method are stored by direct reference (they are not cloned).
     *
     * <p>The {@code name} argument can be anything. A not-too-bad choice (when nothing better is available)
     * is the name of the first dimension of {@code coordinates} and {@code properties} variables. All those
     * variables should have that first dimension in common, because {@code create(…)} uses that criterion.</p>
     *
     * @param  decoder      the source of the features to create.
     * @param  name         name to give to the feature type.
     * @param  counts       the count of instances per feature, or {@code null} if none.
     * @param  identifiers  the feature identifiers, possibly with other singleton properties.
     * @param  coordinates  <var>x</var>, <var>y</var> and potentially <var>z</var> or <var>t</var> coordinate values.
     * @param  hasTime      whether the {@code coordinates} array contains a temporal variable.
     * @param  properties   the variables that contain custom time-varying properties.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    private FeatureSet(final Decoder decoder, String name, final Vector counts, final Variable[] identifiers,
                       final Variable[] coordinates, final boolean hasTime, final Variable[] properties)
    {
        super(decoder.geomlib, decoder.listeners);
        this.counts      = counts;
        this.identifiers = identifiers;
        this.coordinates = coordinates;
        this.properties  = properties;
        this.hasTime     = hasTime;
        /*
         * Creates a description of the features to be read with following properties:
         *
         *    - Identifier and other properties having a single value per feature instance.
         *    - Trajectory as a geometric object, potentially with a time characteristic.
         *    - Time-varying properties (i.e. properties having a value per instant).
         */
        final FeatureTypeBuilder builder = new FeatureTypeBuilder(decoder.nameFactory, decoder.geomlib, decoder.listeners.getLocale());
        for (final Variable v : identifiers) {
            final Class<?> type = v.getDataType().getClass(v.getNumDimensions() > 1);
            describe(v, builder.addAttribute(type), false);
        }
        if (coordinates.length > (hasTime ? 1 : 0)) {
            final AttributeTypeBuilder<?> geometry = builder.addAttribute(
                    counts != null ? GeometryType.LINEAR : GeometryType.POINT);
            geometry.setName(TRAJECTORY).addRole(AttributeRole.DEFAULT_GEOMETRY);
            if (hasTime) {
                geometry.addCharacteristic(MovingFeatures.TIME);
            }
        }
        for (final Variable v : properties) {
            /*
             * Use `Number` type instead than a more specialized subclass because values
             * will be stored in `Vector` objects and that class implements `List<Number>`.
             */
            final Class<?> type = v.isEnumeration() ? String.class : Number.class;
            describe(v, builder.addAttribute(type).setMaximumOccurs(Integer.MAX_VALUE), hasTime);
        }
        /*
         * By default, `name` is a netCDF dimension name (see method javadoc), usually all lower-cases.
         * Make the first letter upper-case for consistency with SIS convention used for feature types.
         */
        name = Strings.toUpperCase(name, Characters.Filter.UNICODE_IDENTIFIER, false);
        type = builder.setName(name).build();
    }

    /**
     * Sets the attribute name, and potentially its definition, from the given variable.
     * If the variable has a {@code "cf_role"} attribute set {@code "trajectory_id"},
     * then the attribute will also be declared as an identifier.
     *
     * @param  variable   the variable from which to get metadata.
     * @param  attribute  the attribute to configure with variable metadata.
     * @param  hasTime    whether to add a "time" characteristic on the attribute.
     */
    private static void describe(final Variable variable, final AttributeTypeBuilder<?> attribute, final boolean hasTime) {
        final String name = variable.getName();
        attribute.setName(name);
        final String desc = variable.getDescription();
        if (desc != null && !desc.equals(name)) {
            attribute.setDefinition(desc);
        }
        if (hasTime) {
            attribute.addCharacteristic(MovingFeatures.TIME);
        }
        if (CF.TRAJECTORY_ID.equalsIgnoreCase(variable.getAttributeAsString(CF.CF_ROLE))) {
            attribute.addRole(AttributeRole.IDENTIFIER_COMPONENT);
        }
    }

    /**
     * Creates new discrete sampling parsers from the attribute values found in the given decoder.
     *
     * @param  decoder  the source of the features to create.
     * @throws IllegalArgumentException if the geometric object library is not available.
     * @throws ArithmeticException if the size of a variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    static FeatureSet[] create(final Decoder decoder) throws IOException, DataStoreException {
        final List<FeatureSet> features = new ArrayList<>(3);     // Will usually contain at most one element.
search: for (final Variable counts : decoder.getVariables()) {
            /*
             * Any one-dimensional integer variable having a "sample_dimension" attribute string value
             * will be taken as an indication that we have Discrete Sampling Geometries. That variable
             * shall be counting the number of feature instances, and another variable having the same
             * dimension (optionally plus a character dimension) should give the feature identifiers.
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
            if (counts.getNumDimensions() == 1 && counts.getDataType().isInteger) {
                final String sampleDimName = counts.getAttributeAsString(CF.SAMPLE_DIMENSION);
                if (sampleDimName != null) {
                    final Dimension sampleDimension = decoder.findDimension(sampleDimName);
                    if (sampleDimension != null) {
                        addFeatureSet(features, decoder, counts, counts.getGridDimensions().get(0), sampleDimension);
                    } else {
                        decoder.listeners.warning(decoder.resources().getString(Resources.Keys.DimensionNotFound_3,
                                                  decoder.getFilename(), counts.getName(), sampleDimName));
                    }
                }
            }
        }
        return features.toArray(new FeatureSet[features.size()]);
    }

    /**
     * Searches all variables having the expected feature dimension or sample dimension.
     * Those variable contains the actual data. For example if the sample dimension name
     * is "points", then we may have:
     *
     * {@preformat text
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
     * }
     *
     * @param  features          where to add the {@code FeatureSet} instance.
     * @param  decoder           the source of the features to create.
     * @param  counts            the count of instances per feature, or {@code null} if none.
     * @param  featureDimension  dimension of properties having a single value per feature instance.
     * @param  sampleDimension   dimension of properties having multiple values per feature instance.
     * @return whether a {@code FeatureSet} has been added to the {@code features} collection.
     */
    private static boolean addFeatureSet(final List<FeatureSet> features, final Decoder decoder, final Variable counts,
            final Dimension featureDimension, final Dimension sampleDimension) throws IOException, DataStoreException
    {
        final String                 featureName = featureDimension.getName();
        final boolean                isPointSet  = sampleDimension.equals(featureDimension);
        final List<Variable>         singletons  = isPointSet ? Collections.emptyList() : new ArrayList<>();
        final List<Variable>         properties  = new ArrayList<>();
        final Map<AxisType,Variable> coordinates = new EnumMap<>(AxisType.class);
        for (final Variable data : decoder.getVariables()) {
            if (data.equals(counts)) {
                continue;
            }
            /*
             * We should have another variable of the same name than the feature dimension name.
             * In SIS implementation, this variable is optional. But if present, it should have
             * the expected dimension. According CF convention that variable should also have a
             * "cf_role" attribute set to "trajectory_id", but this is not required by SIS.
             */
            if (featureName.equalsIgnoreCase(data.getName())) {
                if (isScalarOrString(data, featureDimension, decoder)) {
                    (isPointSet ? properties : singletons).add(data);
                }
            } else if (!isPointSet && isScalarOrString(data, featureDimension, null)) {
                /*
                 * Feature property other than identifiers. Should rarely happen.
                 */
                singletons.add(data);
            } else if (isScalarOrString(data, sampleDimension, null)) {
                /*
                 * All other sample property (i.e. property having a value for each temporal value).
                 * If `isPointSet` is true, then sample properties are actually feature properties.
                 */
                final AxisType axisType = AxisType.valueOf(data);
                if (axisType != null) {
                    final Variable previous = coordinates.putIfAbsent(axisType, data);
                    if (previous != null) {
                        // Duplicated axis type. Keep the first axis in declaration order.
                        decoder.listeners.warning(decoder.resources().getString(Resources.Keys.DuplicatedAxisType_4,
                                                  decoder.getFilename(), axisType, previous.getName(), data.getName()));
                    }
                } else {
                    properties.add(data);
                }
            }
        }
        return features.add(new FeatureSet(decoder, featureName,
                            (counts != null) ? counts.read() : null,
                            toArray(singletons),
                            toArray(coordinates.values()), coordinates.containsKey(AxisType.T),
                            toArray(properties)));

    }

    /**
     * Returns the content of given collection as an array.
     */
    private static Variable[] toArray(final Collection<Variable> variables) {
        return variables.toArray(new Variable[variables.size()]);
    }

    /**
     * Returns {@code true} if the given variable starts with the given dimension.
     * If the variable is an array of character, then it can have 2 dimensions.
     * Otherwise it shall have exactly one dimension.
     *
     * @param  data              the data for which to check the dimensions.
     * @param  featureDimension  the dimension that we expect as the first dimension.
     * @param  decoder           decoder where to report warnings, or {@code null} for silent mode.
     */
    @SuppressWarnings("fallthrough")
    private static boolean isScalarOrString(final Variable data, final Dimension featureDimension, final Decoder decoder) {
        List<Dimension> dimensions = null;
        final int unexpectedDimension;
        switch (data.getNumDimensions()) {
            default: {                              // Too many dimensions
                unexpectedDimension = 2;
                break;
            }
            case 2: {
                if (data.getDataType() != DataType.CHAR) {
                    unexpectedDimension = 1;
                    break;
                }
                // Fall through for checking the first dimension.
            }
            case 1: {
                dimensions = data.getGridDimensions();
                if (featureDimension.equals(dimensions.get(0))) {
                    return true;
                }
                unexpectedDimension = 0;
                break;
            }
            case 0: {                               // Should not happen.
                return false;
            }
        }
        if (decoder != null) {
            if (dimensions == null) {
                dimensions = data.getGridDimensions();
            }
            decoder.listeners.warning(decoder.resources().getString(
                    Resources.Keys.UnexpectedDimensionForVariable_4,
                    decoder.getFilename(), data.getName(),
                    featureDimension.getName(), dimensions.get(unexpectedDimension).getName()));
        }
        return false;
    }

    /**
     * Returns the type of all features to be read by this {@code FeatureSet}.
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
    protected OptionalLong getFeatureCount() {
        if (counts != null) {
            return OptionalLong.of(counts.size());
        }
        for (int i=0; ; i++) {
            final Variable[] data;
            switch (i) {
                case 0: data = identifiers; break;
                case 1: data = coordinates; break;
                case 2: data = properties;  break;
                default: return OptionalLong.empty();
            }
            if (data.length != 0) {
                final long length = data[0].getGridDimensions().get(0).length();
                if (length >= 0) return OptionalLong.of(length);
            }
        }
    }

    /**
     * Returns the stream of features.
     *
     * @param  parallel  ignored, since current version does not support parallelism.
     */
    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        try {
            return StreamSupport.stream(new Iter(), false);
        } catch (IOException e) {
            throw new DataStoreException(canNotReadFile(), e);
        }
    }

    /**
     * Implementation of the iterator returned by {@link #features(boolean)}.
     */
    private final class Iter implements Spliterator<Feature> {
        /**
         * Expected number of features.
         */
        private final int count;

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
         * Values of all singleton properties (typically only identifiers), or an empty array if none.
         *
         * @see FeatureSet#identifiers
         */
        private final List<?>[] idValues;

        /**
         * Creates a new iterator.
         */
        Iter() throws IOException, DataStoreException {
            count = (int) Math.min(getFeatureCount().orElse(0), Integer.MAX_VALUE);
            idValues = new List<?>[identifiers.length];
            for (int i=0; i < idValues.length; i++) {
                // Efficiency should be okay because those lists are cached.
                idValues[i] = identifiers[i].readAnyType();
            }
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
            final Vector[] coordinateValues  = new Vector[coordinates.length];
            final Object[] singleProperties  = new Object[identifiers.length];
            final Object[] varyingProperties = new Object[properties .length];
            for (int i=0; i < singleProperties.length; i++) {
                singleProperties[i] = idValues[i].get(index);
            }
            final int[] step = {1};
            boolean isEmpty = true;
            int length;
            do {
                length = (counts != null) ? counts.intValue(index) : 1;
                if (length != 0) {
                    isEmpty = false;
                    final GridExtent extent = new GridExtent(null, new long[] {position},
                                    new long[] {Math.addExact(position, length)}, false);
                    try {
                        for (int i=0; i<coordinateValues.length; i++) {
                            coordinateValues[i] = coordinates[i].read(extent, step);
                        }
                        for (int i=0; i<properties.length; i++) {
                            final Variable p = properties[i];
                            final Vector data = p.read(extent, step);
                            if (p.isEnumeration()) {
                                final String[] meanings = new String[data.size()];
                                for (int j=0; j<meanings.length; j++) {
                                    String m = p.meaning(data.intValue(j));
                                    meanings[j] = (m != null) ? m : "";
                                }
                                varyingProperties[i] = Arrays.asList(meanings);
                            } else {
                                varyingProperties[i] = data;
                            }
                        }
                    } catch (IOException | DataStoreException e) {
                        throw new BackingStoreException(canNotReadFile(), e);
                    }
                }
                if (++index >= count) {
                    return false;
                }
            } while (isEmpty);
            /*
             * At this point we found that there is some data we can put in a feature instance.
             */
            final Feature feature = type.newInstance();
            for (int i=0; i < singleProperties.length; i++) {
                feature.setPropertyValue(identifiers[i].getName(), singleProperties[i]);
            }
            for (int i=0; i<properties.length; i++) {
                feature.setPropertyValue(properties[i].getName(), varyingProperties[i]);
                // TODO: set time characteristic.
            }
            // TODO: temporary hack - to be replaced by support in Vector.
            final int dimension = coordinates.length - (hasTime ? 1 : 0);
            final double[] tmp = new double[length * dimension];
            for (int i=0; i<tmp.length; i++) {
                tmp[i] = coordinateValues[i % dimension].doubleValue(i / dimension);
            }
            feature.setPropertyValue(TRAJECTORY, factory.createPolyline(false, dimension, Vector.create(tmp)));
            action.accept(feature);
            position = Math.addExact(position, length);
            return true;
        }

        /**
         * Current implementation can not split this iterator.
         */
        @Override
        public Spliterator<Feature> trySplit() {
            return null;
        }

        /**
         * Returns the remaining number of features to traverse.
         */
        @Override
        public long estimateSize() {
            return count - index;
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
