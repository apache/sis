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
package org.apache.sis.storage.netcdf.base;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Spliterator;
import java.util.OptionalLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.function.Consumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.DoubleBuffer;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.metadata.acquisition.GeometryType;
import org.apache.sis.system.Configuration;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.feature.internal.shared.MovingFeatures;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.util.Characters;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.math.Vector;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Attribute;


/**
 * Implementations of the discrete sampling features decoder. This implementation shall be able to decode at least the
 * netCDF files encoded as specified in the OGC 16-114 (OGC Moving Features Encoding Extension: netCDF) specification.
 * This implementation is used as a fallback when the subclass does not provide a more specialized class.
 *
 * <h4>Limitations</h4>
 * Current implementation may perform many seek operations during traversal of feature instances.
 * It may be inefficient unless the {@link Decoder} uses a {@code ChannelDataInput} backed by a direct buffer.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FeatureSet extends DiscreteSampling {
    /**
     * Value of {@code "featureType"} global attribute for netCDF files that this class can handle.
     * Also used as property name for the geometry object.
     *
     * @see #isTrajectory
     */
    static final String TRAJECTORY = "trajectory";

    /**
     * Number of features to get in one read operation. We do not read features one-by-one because it may be slow.
     * We do not read all features at once neither because it consumes a lot of memory if the netCDF file is large.
     * This value is a compromise between reducing I/O operations and reducing memory consumption.
     */
    @Configuration
    private static final int PAGE_SIZE = 512;

    /**
     * The number of instances for each feature, or {@code null} if none. If non-null, then the number of features
     * is the length of this vector and each {@link Feature} instance has multi-valued properties with a number of
     * elements given by this count. If null, the number of features is determined by the length of other variables.
     *
     * @see #getFeatureCount()
     */
    private final Vector counts;

    /**
     * The singleton properties (for which there is only one value per feature instance), or an empty array if none.
     * In the case of trajectories, this array usually contains a single variable for the moving feature identifiers
     * ("mfIdRef"). If {@link #counts} is non-null, then the length of all {@code properties} variables shall be the
     * same as the length of the {@link #counts} vector.
     */
    private final Variable[] properties;

    /**
     * The time-varying properties (for which there is many values per feature instance), or an empty array if none.
     * The length of all {@code dynamicProperties} variables shall be the sum of all {@link #counts} values.
     * If {@link #counts} is {@code null}, then this array is empty.
     */
    private final Variable[] dynamicProperties;

    /**
     * Number of variables storing the coordinates of all geometries (trajectories or points). Those variables are
     * stored at the beginning of either the {@link #properties} array or the {@link #dynamicProperties} array.
     * The array where to find coordinates is determined by {@link #isTrajectory}:
     *
     * <ul>
     *   <li>If {@code isTrajectory = false}, coordinates are at the beginning of {@link #properties}.</li>
     *   <li>If {@code isTrajectory = true},  coordinates are at the beginning of {@link #dynamicProperties}.</li>
     * </ul>
     *
     * The coordinates are <var>x</var>, <var>y</var> and potentially <var>z</var> or <var>t</var>, in that order.
     * The <var>x</var> and <var>y</var> coordinates are typically longitudes and latitudes, but not necessarily.
     * If temporal coordinates exist, the time variable must be last and {@link #hasTime} shall be {@code true}.
     * Ordering is defined by the {@link AxisType} enumeration.
     *
     * <p>Note that referencing dimension is not necessarily equal to geometry dimension,
     * because temporal coordinates are not stored in the geometry object.</p>
     *
     * @see AxisType
     * @see #getReferencingDimension(boolean)
     * @see Iter#geometryDimension
     */
    private final int referencingDimension;

    /**
     * The kind of geometry described by coordinates. Current implementation supports only two types,
     * identified by the {@code false} and {@code true} values respectively:
     *
     * <ul>
     *   <li>{@link GeometryType#POINT}  with coordinates stored in {@link #properties}.</li>
     *   <li>{@link GeometryType#LINEAR} with coordinates stored in {@link #dynamicProperties}.</li>
     * </ul>
     *
     * If there are no coordinates ({@link #referencingDimension} = 0), then this field shall be {@code true}.
     * This is a convenience for the way we compute an {@code isEmpty} flag in {@code tryAdvance(Consumer)}.
     * This policy may change in any future version.
     *
     * @see #TRAJECTORY
     * @see #getReferencingDimension(boolean)
     */
    private final boolean isTrajectory;

    /**
     * Whether coordinates include a temporal variable.
     * If {@code true}, then the time variable shall be last.
     * If {@code false}, then {@link #referencingDimension} is equal to geometry dimension.
     */
    private final boolean hasTime;

    /**
     * The temporal component of the coordinate reference system (CRS), or {@code null} if none.
     * Note that this field may be {@code null} even if {@link #hasTime} is {@code true},
     * if the CRS cannot be expressed as a {@link TemporalCRS}.
     */
    private final DefaultTemporalCRS timeCRS;

    /**
     * The type of all features to be read by this {@code FeatureSet}.
     */
    private final FeatureType type;

    /**
     * Creates a new discrete sampling parser for features identified by the given variable.
     * All arrays given to this method are stored by direct reference (they are not cloned).
     *
     * <p>The {@code name} argument can be anything. A not-too-bad choice (when nothing better is available) is
     * the name of the first dimension of {@code dynamicProperties} (preferred) or {@code properties} (fallback)
     * variables. Variables in the same array should have that first dimension in common because {@code create(…)}
     * uses that criterion.</p>
     *
     * @param  decoder               the source of the features to create.
     * @param  name                  name to give to the feature type.
     * @param  counts                the count of instances per feature, or {@code null} if none.
     * @param  properties            variables providing a single value per feature instance (e.g. "mfIdRef").
     * @param  dynamicProperties     variables that contain time-varying properties other than coordinates.
     * @param  selectedAxes          variables storing the coordinates of all geometries (trajectories or points).
     * @param  isTrajectory          whether coordinates are stored in {@code properties} or {@code dynamicProperties}.
     * @param  hasTime               whether coordinates include a temporal variable.
     * @param  lock                  the lock to use in {@code synchronized(lock)} statements.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    private FeatureSet(final Decoder decoder, String name, final Vector counts, final Variable[] properties,
                       final Variable[] dynamicProperties, final Map<AxisType,Variable> selectedAxes,
                       final boolean isTrajectory, final boolean hasTime, final DataStore lock)
            throws DataStoreException, IOException
    {
        super(decoder.geomlib, decoder.listeners, lock);
        this.counts               = counts;
        this.properties           = properties;
        this.dynamicProperties    = dynamicProperties;
        this.referencingDimension = selectedAxes.size();
        this.isTrajectory         = isTrajectory | (referencingDimension == 0);
        this.hasTime              = hasTime;
        /*
         * We will create a description of the features to be read with following properties:
         *
         *    - Identifier and other properties having a single value per feature instance.
         *    - Trajectory as a geometric object, potentially with a time characteristic.
         *    - Time-varying properties (i.e. properties having a value per instant).
         */
        final var builder = new FeatureTypeBuilder(decoder.nameFactory, decoder.geomlib, decoder.listeners.getLocale());
        /*
         * Identifier and other static properties (one value per feature instance).
         */
        for (int i = getReferencingDimension(false); i < properties.length; i++) {
            final Variable v = properties[i];
            final Class<?> t;
            if (v.getEnumeration() != null) {
                t = String.class;
            } else {
                t = v.getDataType().getClass(v.getNumDimensions() > 1);
            }
            describe(v, builder.addAttribute(t));
        }
        /*
         * Geometry object as a single point or a trajectory, associated with:
         *   - A Coordinate Reference System (CRS) characteristic.
         *   - A "datetimes" characteristic if a time axis exists.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        DefaultTemporalCRS timeCRS = null;
        if (referencingDimension != 0) {
            final AttributeTypeBuilder<?> geometry;
            geometry = builder.addAttribute(isTrajectory ? GeometryType.LINEAR : GeometryType.POINT);
            geometry.setName(TRAJECTORY).addRole(AttributeRole.DEFAULT_GEOMETRY);
            try {
                final SingleCRS[] time = new SingleCRS[1];
                geometry.setCRS(CRSBuilder.assemble(decoder, selectedAxes.values(), time));
                if (time[0] instanceof TemporalCRS) {
                    timeCRS = DefaultTemporalCRS.castOrCopy((TemporalCRS) time[0]);
                }
            } catch (FactoryException ex) {
                decoder.listeners.warning(decoder.resources().getString(Resources.Keys.CanNotCreateCRS_3,
                                          decoder.getFilename(), name, ex.getLocalizedMessage()), ex);
            }
            if (hasTime) {
                geometry.addCharacteristic(MovingFeatures.characteristic(timeCRS != null));
            }
        }
        this.timeCRS = timeCRS;
        /*
         * Dynamic properties (many values by feature instances).
         * Use `Number` type instead of a more specialized subclass because values
         * will be stored in `Vector` objects and that class implements `List<Number>`.
         */
        for (int i = getReferencingDimension(true); i < dynamicProperties.length; i++) {
            final Variable v = dynamicProperties[i];
            final Class<?> t = (v.getEnumeration() != null || v.isString()) ? String.class : Number.class;
            describe(v, builder.addAttribute(t).setMaximumOccurs(Integer.MAX_VALUE));
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
     * If the variable has a {@code "cf_role"} attribute set to {@code "trajectory_id"},
     * then the attribute will also be declared as an identifier.
     *
     * @param  variable   the variable from which to get metadata.
     * @param  attribute  the attribute to configure with variable metadata.
     */
    private static void describe(final Variable variable, final AttributeTypeBuilder<?> attribute) {
        final String name = variable.getName();
        attribute.setName(name);
        final String desc = variable.getDescription();
        if (desc != null && !desc.equals(name)) {
            attribute.setDefinition(desc);
        }
        final Unit<?> unit = variable.getUnit();
        if (unit != null) {
            attribute.setUnit(unit);
        }
        if (CF.TRAJECTORY_ID.equalsIgnoreCase(variable.getAttributeAsString(CF.CF_ROLE))) {
            attribute.addRole(AttributeRole.IDENTIFIER_COMPONENT);
        }
    }

    /**
     * Creates new discrete sampling parsers from the attribute values found in the given decoder.
     * This method shall be invoked in a method synchronized on {@code lock}.
     *
     * @param  decoder  the source of the features to create.
     * @param  lock     the lock to use in {@code synchronized(lock)} statements.
     * @throws IllegalArgumentException if the geometric object library is not available.
     * @throws ArithmeticException if the size of a variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    static FeatureSet[] create(final Decoder decoder, final DataStore lock) throws IOException, DataStoreException {
        assert Thread.holdsLock(lock);
        final var features = new ArrayList<FeatureSet>(3);      // Will usually contain at most one element.
        final var done = new HashMap<Dimension,Boolean>();      // Whether a dimension has already been used.
        for (final Variable v : decoder.getVariables()) {
            if (v.getRole() != VariableRole.FEATURE_PROPERTY) {
                continue;
            }
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
            if (v.getNumDimensions() == 1 && v.getDataType().isInteger) {
                final String sampleDimName = v.getAttributeAsString(CF.SAMPLE_DIMENSION);
                if (sampleDimName != null) {
                    // At this point, the variable is assumed to be `counts`.
                    final Dimension featureDimension = v.getGridDimensions().get(0);
                    final Dimension sampleDimension = decoder.findDimension(sampleDimName);
                    if (sampleDimension != null) {
                        addFeatureSet(features, decoder, v, featureDimension, sampleDimension, lock);
                        done.put(sampleDimension, Boolean.TRUE);
                    } else {
                        decoder.listeners.warning(decoder.resources().getString(Resources.Keys.DimensionNotFound_3,
                                                  decoder.getFilename(), v.getName(), sampleDimName));
                    }
                    done.put(featureDimension, Boolean.TRUE);           // Overwrite `false` value with `true`.
                    continue;
                }
            }
            done.putIfAbsent(v.getGridDimensions().get(0), Boolean.FALSE);
        }
        /*
         * Above loop handled all features which seem to be trajectories (i.e. having a `counts` variable allowing
         * each feature instance to contain an arbitrary number of points). If there is other feature variables not
         * handled by above loop (i.e. feature properties without `counts` variable), the features are assumed to be
         * "simple features" with only points instead of trajectories.
         */
        for (final Map.Entry<Dimension,Boolean> entry : done.entrySet()) {
            if (!entry.getValue()) {
                final Dimension dimension = entry.getKey();
                addFeatureSet(features, decoder, null, dimension, dimension, lock);
            }
        }
        return features.toArray(FeatureSet[]::new);
    }

    /**
     * Searches all variables having the expected feature dimension or sample dimension.
     * Those variable contains the actual data. For example if the sample dimension name
     * is "points", then we may have:
     *
     * <pre class="text">
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
     *     short myCustomProperty(points);</pre>
     *
     * @param  features          where to add the {@code FeatureSet} instance.
     * @param  decoder           the source of the features to create.
     * @param  counts            the count of instances per feature, or {@code null} if none.
     * @param  featureDimension  dimension of properties having a single value per feature instance.
     * @param  sampleDimension   dimension of properties having multiple values per feature instance.
     * @param  lock              the lock to use in {@code synchronized(lock)} statements.
     */
    private static void addFeatureSet(final List<FeatureSet> features, final Decoder decoder,
            final Variable counts, final Dimension featureDimension, final Dimension sampleDimension,
            final DataStore lock) throws IOException, DataStoreException
    {
        final String featureName = featureDimension.getName();
        if (featureName == null) {
            // May happen with HDF5 file read using UCAR library.
            return;
        }
        final boolean        isTrajectory      = !sampleDimension.equals(featureDimension);
        final List<Variable> properties        = new ArrayList<>();
        final List<Variable> dynamicProperties = isTrajectory ? new ArrayList<>() : Collections.emptyList();
        final EnumMap<AxisType,Variable> coordinates = new EnumMap<>(AxisType.class);
        final EnumMap<AxisType,Variable> trajectory  = new EnumMap<>(AxisType.class);
        for (final Variable data : decoder.getVariables()) {
            if (data.equals(counts)) {
                continue;
            }
            /*
             * We should have another variable of the same name as the feature dimension name.
             * In SIS implementation, this variable is optional. But if present, it should have
             * the expected dimension. According CF convention that variable should also have a
             * "cf_role" attribute set to "trajectory_id", but this is not required by SIS.
             */
            final boolean dynamic;
            if (featureName.equalsIgnoreCase(data.getName())) {
                if (isScalarOrString(data, featureDimension, decoder)) {
                    properties.add(data);
                }
                continue;
            } else if (isScalarOrString(data, featureDimension, null)) {
                properties.add(data);
                dynamic = false;
            } else if (isTrajectory && isScalarOrString(data, sampleDimension, null)) {
                dynamicProperties.add(data);
                dynamic = true;
            } else {
                continue;
            }
            /*
             * Check if the property that we just added is a coordinate system axis.
             * We handle separately the axes having coordinates provided by static and dynamic properties.
             * We will decide at the end of this loop which one of those two groups to use.
             */
            final AxisType axisType = AxisType.valueOf(data, true);
            if (axisType != null) {
                final Variable previous = (dynamic ? trajectory : coordinates).putIfAbsent(axisType, data);
                if (previous != null) {
                    // Duplicated axis type. Keep the first axis in declaration order.
                    decoder.listeners.warning(decoder.resources().getString(Resources.Keys.DuplicatedAxisType_4,
                                              decoder.getFilename(), axisType, previous.getName(), data.getName()));
                }
            }
        }
        /*
         * Choose whether coordinates are taken in static or dynamic properties. Current implementation does not
         * support mixing both modes (e.g. X and Y coordinates as static properties and T as dynamic property).
         * The variables are reordered for making sure that X, Y, Z, T are first and in that order.
         */
        final var r = new Reorder();
        features.add(new FeatureSet(decoder, featureName,
                     (counts != null) ? counts.read() : null,
                     r.toArray(properties, coordinates, false),
                     r.toArray(dynamicProperties, trajectory, true),
                     r.selectedAxes, r.isTrajectory, r.hasTime, lock));         // Those arguments must be last.
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
            case Variable.STRING_DIMENSION: {
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
     * Returns the content of a property list as an array, potentially with coordinates moved at the array beginning.
     * At most one call to the {@link #toArray(List, EnumMap, boolean)} will reorder the properties; other calls will
     * return properties in the order they appear in the list.
     */
    private static final class Reorder {
        /**
         * Variables storing the coordinates of all geometries (trajectories or points).
         * Those variables are taken either from static properties or from dynamic properties.
         */
        Map<AxisType,Variable> selectedAxes;

        /**
         * The kind of geometry described by coordinates.
         * This is the value to assign to {@link FeatureSet#isTrajectory}.
         */
        boolean isTrajectory;

        /**
         * Whether coordinates include a temporal variable.
         * This is the value to assign to {@link FeatureSet#hasTime}.
         */
        boolean hasTime;

        /**
         * Creates an initially empty builder of variable arrays.
         */
        Reorder() {
            selectedAxes = Collections.emptyMap();
        }

        /**
         * Returns the content of given property list as an array, potentially with coordinate variables first.
         *
         * @param  properties   the list to return as an array, not necessarily with elements in same order.
         * @param  coordinates  {@code properties} variables to consider as coordinate values.
         * @param  dynamic      value to assign to {@link #isTrajectory} if coordinate axes have been found.
         */
        @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
        Variable[] toArray(final List<Variable> properties, final EnumMap<AxisType,Variable> coordinates, final boolean dynamic) {
            Variable[] array = new Variable[properties.size()];
            if (selectedAxes.isEmpty() && coordinates.containsKey(AxisType.X) && coordinates.containsKey(AxisType.Y)) {
                isTrajectory  = dynamic;
                selectedAxes  = coordinates;
                hasTime       = coordinates.containsKey(AxisType.T);
                array         = coordinates.values().toArray(array);     // Put coordinates at array beginning.
                final int dim = coordinates.size();
                int n = dim;
skip:           for (final Variable v : properties) {
                    for (int i=dim; --i >= 0;) {
                        if (array[i] == v) continue skip;               // Skip already added coordinates.
                    }
                    array[n++] = v;                                     // Add property after coordinates.
                }
                assert n == array.length;
            } else {
                array = properties.toArray(array);
            }
            return array;
        }
    }

    /**
     * Returns the number of variables that are storing coordinate values.
     * If present, those variables are always at the lowest array indices.
     *
     * @param  dynamic  {@code true} for searching in dynamic properties, or {@code false} for static properties.
     * @return number of coordinate variables in the specified group of properties.
     */
    final int getReferencingDimension(final boolean dynamic) {
        return (isTrajectory ^ dynamic) ? 0 : referencingDimension;
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
    public OptionalLong getFeatureCount() {
        if (counts != null) {
            return OptionalLong.of(counts.size());
        }
        if (properties.length != 0) {
            final long length = properties[0].getGridDimensions().get(0).length();
            if (length >= 0) {
                return OptionalLong.of(length);
            }
        }
        return OptionalLong.empty();
    }

    /**
     * Returns {@code true} if the given value is null, an empty string or a NaN value.
     */
    static boolean isEmpty(final Object value) {
        return (value == null) || "".equals(value) ||
               (value instanceof Float  && ((Float)  value).isNaN()) ||
               (value instanceof Double && ((Double) value).isNaN());
    }

    /**
     * Prepares indices of a sub-region to read in a vector of the given number of dimensions.
     * This is a helper method for {@link Iter}, defined here because we cannot put static methods
     * in a non-static inner class.
     *
     * @param  dimensions  dimensions of the vector to read. Can be {@code null} if {@code numDim} is 1.
     * @param  numDim      number of dimensions in the vector to read: {@code dimensions.size()}.
     * @param  position    position of the first value to read in the netCDF variables.
     * @param  length      number of property values to read.
     */
    static GridExtent extent(final List<Dimension> dimensions, int numDim, final long position, final int length) {
        final long[] lower = new long[numDim];
        final long[] upper = new long[numDim];
        lower[--numDim] = position;
        upper[  numDim] = Math.addExact(position, length);
        for (int i=0; i<numDim; i++) {
            upper[i] = dimensions.get(numDim-i).length();
        }
        return new GridExtent(null, lower, upper, false);
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
         * Expected number of feature instances.
         */
        private final int size;

        /**
         * Index of the next feature to read.
         */
        private int featureIndex;

        /**
         * Index of the first feature for which the {@link #propertyValues} contains data.
         * Call to {@code propertyValues[p].get(i)} provides value of property <var>p</var>
         * for the feature at index {@code currentLowerIndex + i}.
         */
        private int currentLowerIndex;

        /**
         * Index after the last feature for which the {@link #propertyValues} contains data.
         * This is {@link #currentLowerIndex} + {@code propertyValues[any].size()}.
         */
        private int currentUpperIndex;

        /**
         * Index of the first {@link #propertyValues} list which is not a coordinate vector.
         * Lists before that index will be stored in the geometry instead of as feature property.
         */
        private final int propertyIndexOffset;

        /**
         * Values of all simple properties (having a single value per feature instance).
         * The list size should not exceed {@value FeatureSet#PAGE_SIZE} elements.
         *
         * @see FeatureSet#properties
         */
        private final List<?>[] propertyValues;

        /**
         * Names of feature properties where to store {@link #propertyValues}.
         */
        private final String[] propertyNames;

        /**
         * Index where to start reading dynamic property values for the next feature.
         * This is the sum of the length of data in all previous features.
         */
        private long dynamicPropertyPosition;

        /**
         * Dimension of geometry objects.
         * This is {@link #referencingDimension} minus the temporal dimension if any.
         */
        private final int geometryDimension;

        /**
         * Creates a new iterator. This constructor reads immediately data for the first
         * {@value #PAGE_SIZE} feature instances as a way to detect problem early.
         */
        Iter() throws IOException, DataStoreException {
            size = (int) Math.min(getFeatureCount().orElse(0), Integer.MAX_VALUE);
            geometryDimension = referencingDimension - (hasTime ? 1 : 0);
            propertyIndexOffset = getReferencingDimension(false);
            int n = properties.length;
            propertyValues = new List<?>[n];                            // Including coordinate vectors.
            propertyNames  = new String[n -= propertyIndexOffset];      // Excluding coordinate vectors.
            for (int i=0; i<n; i++) {
                propertyNames[i] = properties[i + propertyIndexOffset].getName();
            }
            readNextPage();     // See constructor javadoc.
        }

        /**
         * Reads static property values for the next {@value #PAGE_SIZE} feature instances.
         * Reading starts at the feature at index given by {@link #currentLowerIndex}.
         */
        private void readNextPage() throws IOException, DataStoreException {
            final int length = Math.min(size - currentLowerIndex, PAGE_SIZE);
            read(properties, propertyIndexOffset, currentLowerIndex, length, propertyValues);
            currentUpperIndex = currentLowerIndex + length;
        }

        /**
         * Executes the given action only on the next feature, if any.
         *
         * <h4>Limitations</h4>
         * Current implementation may perform a lot of seek operations, which may be inefficient
         * unless the {@link Decoder} uses a {@code ChannelDataInput} backed by a direct buffer.
         *
         * @throws ArithmeticException if the size of a variable exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
         * @throws BackingStoreException if an {@link IOException} or {@link DataStoreException} occurred.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super Feature> action) {
            final Feature feature = type.newInstance();
            final Vector[] coordinateValues;
            int offset, length;
            try {
                /*
                 * Read the properties having one value per feature instance, for example the feature identifier.
                 * Those values are read by chunks, for example 512 values read in advance for each property.
                 * Features where all values are null, empty string or NaN are skipped.
                 */
                List<?>[] values = propertyValues;
                boolean isEmpty;
                do {
                    if (featureIndex >= currentUpperIndex) {
                        if (featureIndex >= size) {
                            return false;
                        }
                        currentLowerIndex = featureIndex;
                        readNextPage();
                    }
                    offset  = featureIndex - currentLowerIndex;
                    length  = (counts != null) ? counts.intValue(featureIndex) : 1;
                    isEmpty = (length == 0);
                    for (int i=0; i < propertyNames.length; i++) {
                        final Object value = values[i + propertyIndexOffset].get(offset);
                        if (!isEmpty(value)) {
                            isEmpty = false;
                            feature.setPropertyValue(propertyNames[i], value);
                        }
                    }
                    featureIndex++;
                } while (isEmpty);
                /*
                 * At this point we found that there is some data we can put in a feature instance.
                 * Above loop has set the static properties (those having one value per feature).
                 * The block below sets the dynamic properties (those having time-varying values).
                 */
                final int n = dynamicProperties.length;
                if (n != 0) {
                    final List<?>[] target = new List<?>[n];
                    int i = getReferencingDimension(true);
                    read(dynamicProperties, i, dynamicPropertyPosition, length, target);
                    for (/* i = first property after coordinate vectors */; i<n; i++) {
                        feature.setPropertyValue(dynamicProperties[i].getName(), target[i]);
                    }
                    if (isTrajectory) {
                        values = target;
                    }
                }
                /*
                 * The coordinate vectors are provided by the first variables in either `properties` or
                 * `dynamicProperties`, depending on whether the geometries are points or trajectories.
                 * Above code has set `values` to the correct source of coordinate data.
                 *
                 * The following `System.arraycopy(…)` call writes `List<?>` references into a `Vector[]` array,
                 * which seems unsafe. But it should not cause an ArrayStoreException because the elements that
                 * we copy should be `Vector` instances, even if the remaining `values` elements are not.
                 */
                coordinateValues = new Vector[referencingDimension];
                System.arraycopy(values, 0, coordinateValues, 0, coordinateValues.length);
            } catch (IOException e) {
                throw new UncheckedIOException(canNotReadFile(), e);
            } catch (DataStoreException e) {
                throw new BackingStoreException(canNotReadFile(), e);
            }
            /*
             * Create the geometry, which may be a trajectory or a single point. Some geometry libraries
             * (e.g. Java2D) provide different implementations for single-precision or double-precision.
             * The use of single-precision when possible can help to reduce memory usage.
             */
            boolean isEmpty = isTrajectory;                     // Skip `isEmptyOrNaN()` test for points.
            boolean isFloat = factory.supports(Capability.SINGLE_PRECISION);
            for (final Vector vc : coordinateValues) {
                if (isEmpty) isEmpty = vc.isEmptyOrNaN();
                if (isFloat) isFloat = vc.isSinglePrecision();
            }
makeGeom:   if (!isEmpty) {
                final Object geometry;
                if (isTrajectory) {
                    /*
                     * Case when the geometry can have an arbitrary number of points.
                     * Coordinates are taken from `dynamicProperties` variable, which
                     * are read every time that a feature instance is created.
                     */
                    final int n = Math.multiplyExact(length, geometryDimension);
                    final double[] c = new double[n];
                    for (int i=0; i<n; i++) {
                        c[i] = coordinateValues[i % geometryDimension].doubleValue(i / geometryDimension);
                    }
                    final DoubleBuffer vc = DoubleBuffer.wrap(c);
                    geometry = factory.createPolyline(false, isFloat, Dimensions.forCount(geometryDimension, false), vc);
                } else {
                    /*
                     * Case when the geometry is a single point. Note that the X and Y coordinates
                     * are guaranteed to be present because of the check done by `Reorder.toArray(…)`.
                     */
                    if (isFloat) {
                        final float x = coordinateValues[0].floatValue(offset);
                        final float y = coordinateValues[0].floatValue(offset);
                        if (Float.isNaN(x) && Float.isNaN(y)) break makeGeom;
                        geometry = factory.createPoint(x, y);
                    } else {
                        final double x = coordinateValues[0].doubleValue(offset);
                        final double y = coordinateValues[1].doubleValue(offset);
                        if (Double.isNaN(x) && Double.isNaN(y)) break makeGeom;
                        geometry = factory.createPoint(x, y);
                    }
                }
                feature.setPropertyValue(TRAJECTORY, geometry);
            }
            /*
             * Add time characteristic on the geometry. Actually this characteristic
             * could be applied to all dynamic properties, but that would be redundancies.
             * The time vector is the first vector after the geometry dimensions.
             */
            if (hasTime) {
                MovingFeatures.setTimes((Attribute<?>) feature.getProperty(TRAJECTORY),
                                        coordinateValues[geometryDimension], timeCRS);
            }
            action.accept(feature);
            dynamicPropertyPosition += length;         // Check for ArithmeticException is already done by `extent(…)` call.
            return true;
        }

        /**
         * Reads property values starting at the given position.
         * The same sub-region is read for all variables.
         *
         * @param  variables   the variables to read.
         * @param  refdim      number of referencing dimensions in {@code variables}.
         * @param  position    position of the first value to read in the netCDF variables.
         * @param  length      number of property values to read.
         * @param  target      where to store the results of read operations.
         */
        private void read(final Variable[] variables, final int refdim, final long position, final int length,
                          final List<?>[] target) throws IOException, DataStoreException
        {
            final GridExtent extent = extent(null, 1, position, length);
            List<Dimension> textDimensions = null;
            GridExtent textExtent = null;
            synchronized (getSynchronizationLock()) {
                for (int i=0; i < variables.length; i++) {
                    final Variable p = variables[i];
                    List<?> value;
                    if (p.getNumDimensions() > 1) {
                        final List<Dimension> dimensions = p.getGridDimensions();
                        if (textExtent == null || !dimensions.equals(textDimensions)) {
                            textExtent = extent(dimensions, dimensions.size(), position, length);
                            textDimensions = dimensions;
                        }
                        value = p.readAnyType(textExtent, null);
                    } else if (i >= refdim) {
                        value = p.readAnyType(extent, null);        // May be `Vector` or `List<String>`.
                    } else {
                        value = p.read(extent, null);               // Force the type to `Vector`.
                    }
                    final Map<Long,String> enumeration = p.getEnumeration();
                    if (enumeration != null && value instanceof Vector) {
                        final var data = (Vector) value;
                        final var meanings = new String[data.size()];
                        for (int j=0; j<meanings.length; j++) {
                            String m = enumeration.get(data.longValue(j));
                            meanings[j] = (m != null) ? m : "";
                        }
                        value = Arrays.asList(meanings);
                    }
                    target[i] = value;
                }
            }
        }

        /**
         * Current implementation cannot split this iterator.
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
            return size - featureIndex;
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
