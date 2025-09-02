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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.io.IOException;
import java.time.Instant;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.privy.EllipsoidalHeightCombiner;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.provider.Equirectangular;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;


/**
 * Temporary object for building a coordinate reference system from the variables in a netCDF file.
 * This class proceeds by inspecting the coordinate system axes. This is a different approach than
 * {@link GridMapping}, which parses Well Known Text or EPSG codes declared in variable attributes.
 *
 * <p>Different instances are required for the geographic, vertical and temporal components of a CRS,
 * or if a netCDF file uses different CRS for different variables. This builder is used as below:</p>
 *
 * <ol>
 *   <li>Invoke {@link #dispatch(List, Axis)} for all axes in a grid.
 *       Builders for CRS components will added in the given list.</li>
 *   <li>Invoke {@link #build(Decoder, boolean)} on each builder prepared in above step.</li>
 *   <li>Assemble the CRS components created in above step in a {@code CompoundCRS}.</li>
 * </ol>
 *
 * The builder type is inferred from axes. The axes are identified by their abbreviations,
 * which is a {@linkplain Axis#abbreviation controlled vocabulary} for this implementation.
 *
 * <h2>Exception handling</h2>
 * {@link FactoryException} is handled as a warning by {@linkplain the caller Grid#getCoordinateReferenceSystem},
 * while {@link DataStoreException} is handled as a fatal error. Warnings are stored in {@link #warnings} field.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class CRSBuilder<D extends Datum, CS extends CoordinateSystem> {
    /**
     * An arbitrary limit on the number of dimensions, for catching what may be malformed data.
     * We rarely have more than 4 dimensions.
     */
    private static final int MAXDIM = 1000;

    /**
     * The type of datum as a GeoAPI sub-interface of {@link Datum}.
     * Used for verifying the type of cached datum at {@link #datumIndex}.
     */
    private final Class<D> datumType;

    /**
     * Name of the datum on which the CRS is presumed to be based, or {@code ""}. This is used
     * for building a datum name like <q>Unknown datum presumably based on GRS 1980</q>.
     */
    private final String datumBase;

    /**
     * Index of the cached datum in a {@code Datum[]} array, from 0 inclusive to {@value #DATUM_CACHE_SIZE} exclusive.
     * The datum type at that index must be an instance of {@link #datumType}. We cache only the datum because they do
     * not depend on the netCDF file content in the common case where the CRS is not explicitly specified.
     */
    private final int datumIndex;

    /**
     * Specify the range of valid number of dimensions, inclusive.
     * The {@link #dimension} value shall be in that range.
     */
    private final int minDim, maxDim;

    /**
     * Number of valid elements in the {@link #axes} array. The count should not be larger than 3,
     * even if the netCDF file has more axes, because each {@code CRSBuilder} is only for a subset.
     */
    private int dimension;

    /**
     * The axes to use for creating the coordinate reference system.
     * They are information about netCDF axes, not yet ISO 19111 axes.
     * The axis are listed in "natural" order (reverse of netCDF order).
     * Only the {@link #dimension} first elements are valid.
     */
    private Axis[] axes;

    /**
     * The datum created by {@link #createDatum(DatumFactory, Map)}.
     * At least one of {@code datum} and {@link #datumEnsemble} shall be initialized.
     */
    protected D datum;

    /**
     * The datum ensemble created by {@link #createDatum(DatumFactory, Map)}.
     * At least one of {@link #datum} and {@code datumEnsemble} shall be initialized.
     */
    protected DatumEnsemble<D> datumEnsemble;

    /**
     * The coordinate system created by {@link #createCS(CSFactory, Map, CoordinateSystemAxis[])}.
     */
    protected CS coordinateSystem;

    /**
     * The coordinate reference system that may have been create by {@link #setPredefinedComponents(Decoder)}.
     */
    protected SingleCRS referenceSystem;

    /**
     * Non-fatal exceptions that may occur while building the coordinate reference system.
     * The same exception may be repeated many time, in which case we will report only the
     * first one.
     *
     * @see #recoverableException(FactoryException)
     */
    private FactoryException warnings;

    /**
     * Creates a new CRS builder based on datum of the given type.
     * This constructor is invoked indirectly by {@link #dispatch(List, Axis)}.
     *
     * @param  datumType   the type of datum as a GeoAPI sub-interface of {@link Datum}.
     * @param  datumBase   name of the datum on which the CRS is presumed to be based, or {@code ""}.
     * @param  datumIndex  index of the cached datum in a {@code Datum[]} array.
     * @param  minDim      minimum number of dimensions (usually 1, 2 or 3).
     * @param  maxDim      maximum number of dimensions (usually 1, 2 or 3).
     */
    private CRSBuilder(final Class<D> datumType, final String datumBase, final int datumIndex, final int minDim, final int maxDim) {
        this.datumType  = datumType;
        this.datumBase  = datumBase;
        this.datumIndex = datumIndex;
        this.minDim     = minDim;
        this.maxDim     = maxDim;
        this.axes       = new Axis[3];
    }

    /**
     * Infers a new CRS for a {@link Grid}.
     *
     * <h4>CRS replacements</h4>
     * The {@code linearizations} argument allows to replace some CRSs inferred by this method by hard-coded CRSs.
     * This is non-empty only when reading a netCDF file for a specific profile, i.e. a file decoded with a subclass
     * of {@link Convention}. The CRS to be replaced is inferred from the axis directions.
     *
     * @param  decoder           the decoder of the netCDF from which the CRS are constructed.
     * @param  grid              the grid for which the CRS are constructed.
     * @param  linearizations    contains CRS to use instead of CRS inferred by this method, or null or empty if none.
     * @param  reorderGridToCRS  an affine transform doing a final step in a "grid to CRS" transform for ordering axes.
     *         Not used by this method, but may be modified for taking in account axis order changes caused by replacements
     *         defined in {@code linearizations}. Ignored (can be null) if {@code linearizations} is null.
     * @return coordinate reference system from the given axes, or {@code null}.
     */
    public static CoordinateReferenceSystem assemble(final Decoder decoder, final Grid grid,
            final List<GridCacheValue> linearizations, final Matrix reorderGridToCRS)
            throws DataStoreException, FactoryException, IOException
    {
        final var builders = new ArrayList<CRSBuilder<?,?>>(4);
        for (final Axis axis : grid.getAxes(decoder)) {
            dispatch(builders, axis);
        }
        final var components = new SingleCRS[builders.size()];
        for (int i=0; i < components.length; i++) {
            components[i] = builders.get(i).build(decoder, true);
        }
        /*
         * If there is hard-coded CRS implied by `Convention.linearizers()`, use it now.
         * We do not verify the datum; we assume that the linearizer that built the CRS
         * was consistent with `Convention.defaultHorizontalCRS(false)`.
         */
        if ((linearizations != null) && !linearizations.isEmpty()) {
            Linearizer.replaceInCompoundCRS(components, linearizations, reorderGridToCRS);
        }
        switch (components.length) {
            case 0: return null;
            case 1: return components[0];
        }
        return new EllipsoidalHeightCombiner(decoder).createCompoundCRS(properties(grid.getName()), components);
    }

    /**
     * Infers a new horizontal and vertical CRS for a {@link FeatureSet}.
     * The CRS returned by this method does not include a temporal component.
     * Instead the temporal component, if found, is stored in the {@code time} array.
     * Note that the temporal component is not necessarily a {@link org.opengis.referencing.crs.TemporalCRS} instance;
     * it can also be an {@link org.opengis.referencing.crs.EngineeringCRS} instance if the datum epoch is unknown.
     *
     * @param  decoder  the decoder of the netCDF from which the CRS are constructed.
     * @param  axes     the axes to use for creating a CRS.
     * @param  time     an array of length 1 where to store the temporal CRS.
     * @return coordinate reference system from the given axes, or {@code null}.
     */
    static CoordinateReferenceSystem assemble(final Decoder decoder, final Iterable<Variable> axes, final SingleCRS[] time)
            throws DataStoreException, FactoryException, IOException
    {
        final var builders = new ArrayList<CRSBuilder<?,?>>(4);
        for (final Variable axis : axes) {
            dispatch(builders, new Axis(axis));
        }
        final var components = new SingleCRS[builders.size()];
        int n = 0;
        for (final CRSBuilder<?, ?> cb : builders) {
            final SingleCRS c = cb.build(decoder, false);
            if (cb instanceof Temporal) {
                time[0] = c;
            } else {
                components[n++] = c;
            }
        }
        switch (n) {
            case 0: return null;
            case 1: return components[0];
        }
        return new EllipsoidalHeightCombiner(decoder).createCompoundCRS(ArraysExt.resize(components, n));
    }

    /**
     * Dispatches the given axis to a {@code CRSBuilder} appropriate for the axis type. The axis type is determined
     * from {@link Axis#abbreviation}, taken as a controlled vocabulary. If no suitable {@code CRSBuilder} is found
     * in the given list, then a new one will be created and added to the list.
     *
     * @param  components  the list of builder where to dispatch the axis. May be modified by this method.
     * @param  axis        the axis to add to a builder in the given list.
     * @throws DataStoreContentException if the given axis cannot be added in a builder.
     */
    @SuppressWarnings("fallthrough")
    private static void dispatch(final List<CRSBuilder<?,?>> components, final Axis axis) throws DataStoreContentException {
        final Class<? extends CRSBuilder<?,?>> addTo;
        final Supplier<CRSBuilder<?,?>> constructor;
        int alternative = -1;
        switch (axis.abbreviation) {
            case 'h': for (int i=components.size(); --i >= 0;) {        // Can apply to either Geographic or Projected.
                          if (components.get(i) instanceof Projected) {
                              alternative = i;
                              break;
                          }
                      }                    // Fallthrough
            case 'λ': case 'φ':            addTo =  Geographic.class; constructor =  Geographic::new; break;
            case 'θ': case 'Ω': case 'r':  addTo =   Spherical.class; constructor =   Spherical::new; break;
            case 'E': case 'N':            addTo =   Projected.class; constructor =   Projected::new; break;
            case 'H': case 'D':            addTo =    Vertical.class; constructor =    Vertical::new; break;
            case 't':                      addTo =    Temporal.class; constructor =    Temporal::new; break;
            default:                       addTo = Engineering.class; constructor = Engineering::new; break;
        }
        /*
         * If a builder of `addTo` class already exists, add the axis in the existing builder.
         * We should have at most one builder of each class. But if we nevertheless have more,
         * add to the most recently used builder. If there is no builder, create a new one.
         */
        for (int i=components.size(); --i >= 0;) {
            final CRSBuilder<?,?> builder = components.get(i);
            if (addTo.isInstance(builder) || i == alternative) {
                builder.add(axis);
                return;
            }
        }
        final CRSBuilder<?,?> builder = constructor.get();
        /*
         * Before to add the axis to a newly created builder, verify if we wrongly associated
         * the ellipsoidal height to Geographic builder before. The issue is that ellipsoidal
         * height can be associated to either Geographic or Projected CRS.  If we do not have
         * more information, our first bet is Geographic. If our bet appears to be wrong, the
         * block below fixes it.
         */
        if (addTo == Projected.class) {
previous:   for (int i = components.size(); --i >= 0;) {
                final CRSBuilder<?,?> replace = components.get(i);
                for (final Axis a : replace.axes) {
                    if (a.abbreviation != 'h') {
                        continue previous;                  // Not a lonely ellipsoidal height in a Geographic CRS.
                    }
                }
                for (final Axis a : replace.axes) {         // Should have exactly one element, but we are paranoiac.
                    builder.add(a);
                }
                components.remove(i);
                break;
            }
        }
        builder.add(axis);
        components.add(builder);            // Add only after we ensured that the builder contains at least one axis.
    }

    /**
     * Adds an axis for the coordinate reference system to build. Adding more than 3 axes is usually an error,
     * but this method nevertheless stores those extraneous axis references for building an error message later.
     *
     * @param  axis  the axis to add.
     * @throws DataStoreContentException if the given axis cannot be added in this builder.
     */
    private void add(final Axis axis) throws DataStoreContentException {
        if (dimension > MAXDIM) {
            throw new DataStoreContentException(getFirstAxis().coordinates.errors()
                    .getString(Errors.Keys.ExcessiveListSize_2, "axes", dimension));
        }
        if (dimension >= axes.length) {
            axes = Arrays.copyOf(axes, dimension * 2);        // Should not happen (see method javadoc).
        }
        axes[dimension++] = axis;
    }

    /**
     * Returns whether the coordinate system has at least 3 axes.
     */
    final boolean is3D() {
        return dimension >= 3;
    }

    /**
     * Returns the first axis. This method is invoked for coordinate reference systems that are known
     * to contain only one axis, for example temporal coordinate systems.
     */
    final Axis getFirstAxis() {
        return axes[0];
    }

    /**
     * Creates the coordinate reference system.
     * This method can be invoked after all axes have been dispatched.
     *
     * @param  decoder  the decoder of the netCDF from which the CRS are constructed.
     * @param  grid     {@code true} if building a CRS for a grid, or {@code false} for features.
     */
    private SingleCRS build(final Decoder decoder, final boolean grid)
            throws FactoryException, DataStoreException, IOException
    {
        if (dimension > maxDim) {
            /*
             * Reminder: FactoryException is handled as a warning by the `Grid` caller.
             * By contrast, `DataStoreContentException` would be treated as a fatal error.
             */
            final Variable axis = getFirstAxis().coordinates;
            throw new FactoryException(axis.decoder.resources().getString(Resources.Keys.UnexpectedAxisCount_4,
                    axis.getFilename(), getClass().getSimpleName(), dimension, NamedElement.listNames(axes, dimension, ", ")));
        }
        /*
         * If the subclass can offer coordinate system and datum candidates based on a brief inspection of axes,
         * set the datum, CS and CRS field values to those candidate. Those values do not need to be exact; they
         * will be overwritten later if they do not match the netCDF file content.
         */
        datum = datumType.cast(decoder.datumCache[datumIndex]);         // Should be before `setPredefinedComponents` call.
        setPredefinedComponents(decoder);
        /*
         * If `setPredefinedComponents(decoder)` offers a datum, we will used it as-is. Otherwise create the datum now.
         * Datum are often not defined in netCDF files, so the above `setPredefinedComponents` method call may have set
         * EPSG::6019 — "Not specified (based on GRS 1980 ellipsoid)". If not, we build a similar name.
         */
        if (datum == null) {
            // Not localized because stored as a String, possibly exported in WKT or GML, and `datumBase` is in English.
            createDatum(decoder.getDatumFactory(), properties("Unknown datum presumably based upon ".concat(datumBase)));
        }
        decoder.datumCache[datumIndex] = datum;
        /*
         * We cannot go further if the number of dimensions is not valid for the coordinate system to build.
         * This error may happen for example when the CRS type is geographic, but only the latitude axis has
         * been declared (without longitude axis). It may happen for example with a (latitude, time) system.
         * In such case, we can build an engineering CRS has a replacement.
         */
        if (dimension < minDim) {
            final CRSBuilder<EngineeringDatum, ?> eng = new CRSBuilder.Engineering();
            System.arraycopy(axes, 0, eng.axes, 0, dimension);
            eng.dimension = dimension;
            eng.datum = decoder.getDatumFactory().createEngineeringDatum(
                    IdentifiedObjects.getProperties(datum, Datum.IDENTIFIERS_KEY));
            eng.createFromDatum(decoder, grid);
            return eng.referenceSystem;
        }
        /*
         * Verify if a predefined coordinate system can be used. This is often the case, for example
         * the EPSG::6424 coordinate system can be used for (longitude, latitude) axes in degrees.
         * Using a predefined CS allows us to get more complete definitions (minimum and maximum values, etc.).
         */
        if (coordinateSystem != null) {
            for (int i=dimension; --i >= 0;) {
                final Axis expected = axes[i];
                if (expected == null || !expected.isSameUnitAndDirection(coordinateSystem.getAxis(i))) {
                    coordinateSystem = null;
                    referenceSystem  = null;
                    break;
                }
            }
        }
        /*
         * If `setPredefinedComponents(decoder)` did not proposed a coordinate system, or if it proposed a CS
         * but its axes do not match the axes in the netCDF file, then create a new coordinate system here.
         */
        if (referenceSystem == null) {
            createFromDatum(decoder, grid);
        }
        /*
         * Creates the coordinate reference system using current value of `datum` and `coordinateSystem` fields.
         * The coordinate system initially have a [-180 … +180]° longitude range. If the actual coordinate values
         * are outside that range, switch the longitude range to [0 … 360]°.
         */
        if (grid) {
            final CoordinateSystem cs = referenceSystem.getCoordinateSystem();
            for (int i=cs.getDimension(); --i >= 0;) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (axis.getRangeMeaning() == RangeMeaning.WRAPAROUND) {
                    final NumberRange<?> range = axes[i].read().range();                // Vector is cached.
                    if (range != null) {
                        // Note: minimum/maximum are not necessarily first and last values in the vector.
                        if (range.getMinDouble() >= 0 && range.getMaxDouble() > axis.getMaximumValue()) {
                            referenceSystem = (SingleCRS) AbstractCRS.castOrCopy(referenceSystem)
                                                .forConvention(AxesConvention.POSITIVE_RANGE);
                            coordinateSystem = null;
                            break;
                        }
                    }
                }
            }
        }
        if (warnings != null) {
            decoder.listeners.warning(Level.FINE, null, warnings);
        }
        return referenceSystem;
    }

    /**
     * Unconditionally creates a coordinate reference system, overwriting current {@link #referenceSystem} value.
     * The {@link #datum} field must be initialized before to invoke this method.
     */
    private void createFromDatum(final Decoder decoder, final boolean grid)
            throws FactoryException, DataStoreException, IOException
    {
        final Map<String,?> properties;
        if (coordinateSystem == null) {
            // Fallback if the coordinate system is not predefined.
            final StringJoiner joiner = new StringJoiner(" ");
            final CSFactory csFactory = decoder.getCSFactory();
            final var iso = new CoordinateSystemAxis[dimension];
            for (int i=0; i<iso.length; i++) {
                final Axis axis = axes[i];
                joiner.add(axis.getName());
                iso[i] = axis.toISO(csFactory, i, grid);
            }
            createCS(csFactory, properties(joiner.toString()), iso);
            properties = properties(coordinateSystem.getName());
        } else {
            properties = properties(NamedElement.listNames(axes, dimension, " "));
        }
        createCRS(decoder.getCRSFactory(), properties);
    }

    /**
     * Reports a non-fatal exception that may occur during {@link #setPredefinedComponents(Decoder)}.
     * In order to avoid repeating the same warning many times, this method collects the warnings
     * together and reports them in a single log record after we finished creating the CRS.
     *
     * <p>The expected exception types are:</p>
     * <ul>
     *   <li>{@link NoSuchAuthorityCodeException}</li>
     *   <li>{@link InvalidGeodeticParameterException}</li>
     * </ul>
     */
    final void recoverableException(final FactoryException e) {
        if (warnings == null) warnings = e;
        else warnings.addSuppressed(e);
    }

    /**
     * Returns the properties to give to factory {@code create} methods.
     *
     * @param  name  name of the geodetic object (datum, coordinate system, …) to create.
     */
    private static Map<String,?> properties(final Object name) {
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Returns the EPSG code of a possible coordinate system from EPSG database. This method proceed by brief
     * inspection of axis directions and units; there is no guarantee that the coordinate system returned by
     * this method match the axes defined in the netCDF file. It is caller's responsibility to verify.
     * This is a helper method for {@link #setPredefinedComponents(Decoder)} implementations.
     *
     * @param  defaultUnit  the unit to use if unit definition is missing in the netCDF file.
     * @return EPSG code of a CS candidate, or {@code null} if none.
     *
     * @see Geodetic#isPredefinedCS(Unit)
     */
    final Integer epsgCandidateCS(final Unit<?> defaultUnit) {
        Unit<?> unit = getFirstAxis().getUnit();
        if (unit == null) unit = defaultUnit;
        final var directions = new AxisDirection[dimension];
        for (int i=0; i<directions.length; i++) {
            directions[i] = axes[i].direction;
        }
        return CoordinateSystems.getEpsgCode(unit, directions);
    }

    /**
     * If a brief inspection of unit and direction of the {@linkplain #getFirstAxis() first axis} suggests
     * that a predefined coordinate system could be used, sets the {@link #coordinateSystem} field to that CS.
     * The coordinate system does not need to be a full match since all axes will be verified by the caller.
     * This method is invoked before to fallback on {@link #createCS(CSFactory, Map, CoordinateSystemAxis[])}.
     *
     * <p>This method may opportunistically set the {@link #datum} and {@link #referenceSystem} fields if it
     * can propose a CRS candidate instead of only a CS candidate.</p>
     */
    abstract void setPredefinedComponents(Decoder decoder) throws FactoryException;

    /**
     * Creates the datum for the coordinate reference system to build. The datum are generally not specified in netCDF files.
     * To make that clearer, this method builds datum with names like <q>Unknown datum presumably based on GRS 1980</q>.
     * The newly created datum is assigned to the {@link #datum} field.
     *
     * @param  factory     the factory to use for creating the datum.
     * @param  properties  contains the name of the datum to create.
     */
    abstract void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException;

    /**
     * Creates the coordinate system from the given axes. This method is invoked only after we
     * verified that the number of axes is inside the {@link #minDim} … {@link #maxDim} range.
     * The newly created coordinate system is assigned to the {@link #coordinateSystem} field.
     *
     * @param  factory     the factory to use for creating the coordinate system.
     * @param  properties  contains the name of the coordinate system to create.
     * @param  axes        the axes of the coordinate system.
     */
    abstract void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException;

    /**
     * Creates the coordinate reference system from the values in {@link #datum} and {@link #coordinateSystem} fields.
     * This method is invoked only if {@link #referenceSystem} is still {@code null} or its axes do not correspond to
     * the expected axis. The newly created reference system is assigned to the {@link #referenceSystem} field.
     *
     * @param  factory     the factory to use for creating the coordinate reference system.
     * @param  properties  contains the name of the coordinate reference system to create.
     */
    abstract void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException;




    /**
     * Base classes of {@link Spherical}, {@link Geographic} and {@link Projected} builders.
     * They all have in common to be based on a {@link GeodeticDatum}.
     */
    private abstract static class Geodetic<CS extends CoordinateSystem> extends CRSBuilder<GeodeticDatum, CS> {
        /**
         * Index for the cache of datum in the {@link Decoder#datumCache} array.
         */
        static final int CACHE_INDEX = 0;

        /**
         * The coordinate reference system which is presumed the basis of datum on netCDF files.
         */
        protected CommonCRS defaultCRS;

        /**
         * Whether the coordinate system has longitude before latitude.
         * This flag is set as a side-effect of {@link #isPredefinedCS(Unit)} method call.
         */
        protected boolean isLongitudeFirst;

        /**
         * For subclasses constructors.
         *
         * @param  minDim  minimum number of dimensions (2 or 3).
         */
        Geodetic(final int minDim) {
            super(GeodeticDatum.class, "GRS 1980", CACHE_INDEX, minDim, 3);
        }

        /**
         * Initializes this builder before {@link #build(Decoder, boolean)} execution.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            defaultCRS = decoder.convention().defaultHorizontalCRS(false);
        }

        /**
         * Creates a {@link GeodeticDatum} for <q>Unknown datum presumably based on GRS 1980</q>.
         * This method is invoked only if {@link #setPredefinedComponents(Decoder)} failed to create a datum.
         */
        @Override final void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            datum = factory.createGeodeticDatum(properties, defaultCRS.ellipsoid(), defaultCRS.primeMeridian());
        }

        /**
         * Sets the datum from the enumeration value of a predefined CRS.
         * The predefined CRS is {@link #defaultCRS} or a spherical CRS.
         */
        protected final void setDatum(final CommonCRS crs) {
            datum = crs.datum(false);
            if (datum == null) {
                datumEnsemble = crs.datumEnsemble();
            }
        }

        /**
         * Returns {@code true} if the coordinate system may be one of the predefined CS. A returns value of {@code true}
         * is not a guarantee that the coordinate system in the netCDF file matches the predefined CS; it only tells that
         * this is reasonable chances to be the case based on a brief inspection of the first coordinate system axis.
         * If {@code true}, then {@link #isLongitudeFirst} will have been set to an indication of axis order.
         *
         * @param  expected  the expected unit of measurement of the first axis.
         *
         * @see #epsgCandidateCS(Unit)
         */
        final boolean isPredefinedCS(final Unit<?> expected) {
            final Axis axis = getFirstAxis();
            final Unit<?> unit = axis.getUnit();
            if (unit == null || expected.equals(unit)) {
                isLongitudeFirst = (axis.direction == AxisDirection.EAST);
                if (isLongitudeFirst || (axis.direction == AxisDirection.NORTH)) {
                    return true;
                }
            }
            return false;
        }
    }




    /**
     * Builder for geocentric CRS with (θ,Ω,r) axes.
     */
    private static final class Spherical extends Geodetic<SphericalCS> {
        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Spherical() {
            super(3);
        }

        /**
         * Possibly sets {@link #datum} and {@link #coordinateSystem} to predefined objects
         * matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            super.setPredefinedComponents(decoder);
            if (isPredefinedCS(Units.DEGREE)) {
                GeodeticCRS crs = defaultCRS.spherical();
                if (isLongitudeFirst) {
                    crs = DefaultGeocentricCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
                }
                referenceSystem  = crs;
                coordinateSystem = (SphericalCS) crs.getCoordinateSystem();
                datum            = crs.getDatum();
                datumEnsemble    = crs.getDatumEnsemble();
            } else {
                setDatum(defaultCRS);
            }
        }

        /**
         * Creates the two- or three-dimensional {@link SphericalCS} from given axes. This method is invoked only
         * if {@link #setPredefinedComponents(Decoder)} failed to assign a CS or if {@link #build(Decoder, boolean)}
         * found that the {@link #coordinateSystem} does not have compatible axes.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            if (axes.length > 2) {
                coordinateSystem = factory.createSphericalCS(properties, axes[0], axes[1], axes[2]);
            } else {
                coordinateSystem = factory.createSphericalCS(properties, axes[0], axes[1]);
            }
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         * This method is invoked under conditions similar to the ones of above {@code createCS(…)} method.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem = factory.createGeodeticCRS(properties, datum, datumEnsemble, coordinateSystem);
        }
    }




    /**
     * Geographic CRS with (λ,φ,h) axes.
     * The height, if present, is ellipsoidal height.
     */
    private static final class Geographic extends Geodetic<EllipsoidalCS> {
        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Geographic() {
            super(2);
        }

        /**
         * Possibly sets {@link #datum}, {@link #coordinateSystem} and {@link #referenceSystem}
         * to predefined objects matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            super.setPredefinedComponents(decoder);
            if (isPredefinedCS(Units.DEGREE)) {
                GeographicCRS crs;
                if (is3D()) {
                    crs = defaultCRS.geographic3D();
                    if (isLongitudeFirst) {
                        crs = DefaultGeographicCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
                    }
                } else if (isLongitudeFirst) {
                    crs = defaultCRS.normalizedGeographic();
                } else {
                    crs = defaultCRS.geographic();
                }
                referenceSystem  = crs;
                coordinateSystem = crs.getCoordinateSystem();
                datum            = crs.getDatum();
                datumEnsemble    = crs.getDatumEnsemble();
            } else {
                setDatum(defaultCRS);
                final Integer epsg = epsgCandidateCS(Units.DEGREE);
                if (epsg != null) try {
                    coordinateSystem = decoder.getCSAuthorityFactory().createEllipsoidalCS(epsg.toString());
                } catch (NoSuchAuthorityCodeException e) {
                    recoverableException(e);
                }
            }
        }

        /**
         * Creates the two- or three-dimensional {@link EllipsoidalCS} from given axes. This method is invoked only if
         * {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system or if {@link #build(Decoder,
         * boolean)} found that the {@link #coordinateSystem} does not have compatible axes.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            if (axes.length > 2) {
                coordinateSystem = factory.createEllipsoidalCS(properties, axes[0], axes[1], axes[2]);
            } else {
                coordinateSystem = factory.createEllipsoidalCS(properties, axes[0], axes[1]);
            }
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         * This method is invoked under conditions similar to the ones of above {@code createCS(…)} method.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem = factory.createGeographicCRS(properties, datum, datumEnsemble, coordinateSystem);
        }
    }




    /**
     * Projected CRS with (E,N,h) axes. There is not enough information in a netCDF files for creating the right
     * map projection, unless {@code "grid_mapping"} attributes are specified. If insufficient information, this
     * class creates an unknown map projection based on Plate Carrée. Note that this map projection may be replaced
     * by {@link GridMapping#crs} at a later stage.
     */
    private static final class Projected extends Geodetic<CartesianCS> {
        /**
         * The spherical variant of {@link #defaultCRS}.
         */
        private CommonCRS sphericalDatum;

        /**
         * Defining conversion for "Not specified (presumed Plate Carrée)". This conversion uses spherical formulas.
         * Consequently, it should be used with {@link #sphericalDatum} instead of {@link #defaultCRS}.
         */
        private static final Conversion UNKNOWN_PROJECTION;
        static {
            try {
                OperationMethod method = DefaultMathTransformFactory.provider().getOperationMethod(Equirectangular.NAME);
                UNKNOWN_PROJECTION = DefaultCoordinateOperationFactory.provider().createDefiningConversion(
                        properties("Not specified (presumed Plate Carrée)"),
                        method, method.getParameters().createValue());
            } catch (FactoryException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Projected() {
            super(2);
        }

        /**
         * Possibly sets {@link #datum}, {@link #coordinateSystem} and {@link #referenceSystem}
         * to predefined objects matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            super.setPredefinedComponents(decoder);
            sphericalDatum = decoder.convention().defaultHorizontalCRS(true);
            setDatum(sphericalDatum);
            if (isPredefinedCS(Units.METRE)) {
                coordinateSystem = decoder.getStandardProjectedCS();
            }
        }

        /**
         * Creates the two- or three-dimensional {@link CartesianCS} from given axes. This method is invoked only if
         * {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system or if {@link #build(Decoder,
         * boolean)} found that the {@link #coordinateSystem} does not have compatible axes.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            if (axes.length > 2) {
                coordinateSystem = factory.createCartesianCS(properties, axes[0], axes[1], axes[2]);
            } else {
                coordinateSystem = factory.createCartesianCS(properties, axes[0], axes[1]);
            }
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         * The datum for this method is based on a sphere.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            final boolean is3D = (coordinateSystem.getDimension() >= 3);
            GeographicCRS baseCRS = is3D ? sphericalDatum.geographic3D() : sphericalDatum.geographic();
            if (!Utilities.equalsIgnoreMetadata(baseCRS.getDatum(), datum) &&
                !Utilities.equalsIgnoreMetadata(baseCRS.getDatumEnsemble(), datumEnsemble))
            {
                baseCRS = factory.createGeographicCRS(properties, datum, datumEnsemble, baseCRS.getCoordinateSystem());
            }
            referenceSystem = factory.createProjectedCRS(properties, baseCRS, UNKNOWN_PROJECTION, coordinateSystem);
        }
    }




    /**
     * Vertical CRS with (H) or (D) axis.
     * Used for mean sea level (not for ellipsoidal height).
     */
    private static final class Vertical extends CRSBuilder<VerticalDatum, VerticalCS> {
        /**
         * Index for the cache of datum in the {@link Decoder#datumCache} array.
         */
        static final int CACHE_INDEX = Geodetic.CACHE_INDEX + 1;

        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Vertical() {
            super(VerticalDatum.class, "Mean Sea Level", CACHE_INDEX, 1, 1);
        }

        /**
         * Possibly sets {@link #coordinateSystem} to a predefined CS matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) {
            final Axis axis = getFirstAxis();
            final Unit<?> unit = axis.getUnit();
            final CommonCRS.Vertical predefined;
            if (Units.METRE.equals(unit)) {
                if (axis.direction == AxisDirection.UP) {
                    predefined = CommonCRS.Vertical.MEAN_SEA_LEVEL;
                } else {
                    predefined = CommonCRS.Vertical.DEPTH;
                }
            } else if (Units.HECTOPASCAL.equals(unit)) {
                predefined = CommonCRS.Vertical.BAROMETRIC;
            } else {
                return;
            }
            coordinateSystem = predefined.crs().getCoordinateSystem();
        }

        /**
         * Creates a {@link VerticalDatum} for <q>Unknown datum based on Mean Sea Level</q>.
         */
        @SuppressWarnings("deprecation")
        @Override void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            datum = factory.createVerticalDatum(properties, RealizationMethod.GEOID);
        }

        /**
         * Creates the one-dimensional {@link VerticalCS} from given axes. This method is invoked
         * only if {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system
         * or if {@link #build(Decoder, boolean)} found that the axis or direction are not compatible.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            coordinateSystem = factory.createVerticalCS(properties, axes[0]);
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem =  factory.createVerticalCRS(properties, datum, datumEnsemble, coordinateSystem);
        }
    }




    /**
     * Temporal CRS with (t) axis. Its datum need to be built
     * in a special way since it contains the time origin.
     */
    private static final class Temporal extends CRSBuilder<TemporalDatum, TimeCS> {
        /**
         * Index for the cache of datum in the {@link Decoder#datumCache} array.
         */
        static final int CACHE_INDEX = Vertical.CACHE_INDEX + 1;

        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Temporal() {
            super(TemporalDatum.class, "", CACHE_INDEX, 1, 1);
        }

        /**
         * Possibly sets {@link #coordinateSystem} to a predefined CS matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) {
            final Axis axis = getFirstAxis();
            final Unit<?> unit = axis.getUnit();
            final CommonCRS.Temporal predefined;
            if (Units.DAY.equals(unit)) {
                predefined = CommonCRS.Temporal.JULIAN;
            } else if (Units.SECOND.equals(unit)) {
                predefined = CommonCRS.Temporal.UNIX;
            } else if (Units.MILLISECOND.equals(unit)) {
                predefined = CommonCRS.Temporal.JAVA;
            } else {
                return;
            }
            coordinateSystem = predefined.crs().getCoordinateSystem();
        }

        /**
         * Creates a {@link TemporalDatum} for <q>Unknown datum based on …</q>.
         * This method may left the datum to {@code null} if the epoch is unknown.
         * In such case, {@link #createCRS createCRS(…)} will create an engineering CRS instead.
         */
        @Override void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            final Axis axis = getFirstAxis();
            axis.getUnit();                                     // Force epoch parsing if not already done.
            final Instant epoch = axis.coordinates.epoch;
            if (epoch != null) {
                final CommonCRS.Temporal c = CommonCRS.Temporal.forEpoch(epoch);
                if (c != null) {
                    datum = c.datum();
                } else {
                    properties = properties("Time since " + epoch);
                    datum = factory.createTemporalDatum(properties, epoch);
                }
            }
        }

        /**
         * Creates the one-dimensional {@link TimeCS} from given axes. This method is invoked only
         * if {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system or if
         * {@link #build(Decoder, boolean)} found that the axis or direction are not compatible.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            coordinateSystem = factory.createTimeCS(properties, axes[0]);
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         * It should be a temporal CRS. But if the temporal datum cannot be created because epoch was unknown,
         * this method fallbacks on an engineering CRS.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            properties = properties(getFirstAxis().coordinates.getUnitsString());
            if (datum != null) {
                referenceSystem = factory.createTemporalCRS(properties, datum, datumEnsemble, coordinateSystem);
            } else {
                referenceSystem = factory.createEngineeringCRS(properties, CommonCRS.Engineering.TIME.datum(), coordinateSystem);
            }
        }
    }




    /**
     * Unknown CRS with (x,y,z) axes.
     */
    private static final class Engineering extends CRSBuilder<EngineeringDatum, CoordinateSystem> {
        /**
         * Index for the cache of datum in the {@link Decoder#datumCache} array.
         */
        static final int CACHE_INDEX = Temporal.CACHE_INDEX + 1;

        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Engineering() {
            super(EngineeringDatum.class, "affine coordinate system", CACHE_INDEX, 1, 3);
        }

        /**
         * No-op since we have no predefined engineering CRS.
         */
        @Override void setPredefinedComponents(final Decoder decoder) {
        }

        /**
         * Creates a {@link VerticalDatum} for <q>Unknown datum based on affine coordinate system</q>.
         */
        @Override void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            datum = factory.createEngineeringDatum(properties);
        }

        /**
         * Creates two- or three-dimensional coordinate system (usually {@link AffineCS}) from given axes.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            try {
                switch (axes.length) {
                    case 0:  break;     // Should never happen but we are paranoiac.
                    case 1:  coordinateSystem = factory.createParametricCS(properties, axes[0]); return;
                    case 2:  coordinateSystem = factory.createAffineCS(properties, axes[0], axes[1]); return;
                    default: coordinateSystem = factory.createAffineCS(properties, axes[0], axes[1], axes[2]); return;
                }
            } catch (InvalidGeodeticParameterException e) {
                /*
                 * Unknown Coordinate System type, for example because of unexpected units of measurement for a
                 * Cartesian or affine coordinate system.  The fallback object created below is not abstract in
                 * the Java sense, but in the sense that we don't have more specific information on the CS type.
                 */
                recoverableException(e);
            }
            coordinateSystem = new AbstractCS(properties, axes);
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem =  factory.createEngineeringCRS(properties, datum, datumEnsemble, coordinateSystem);
        }
    }

    /**
     * Maximal {@link #datumIndex} value +1. The maximal value can be seen in the call to {@code super(…)} constructor
     * in the last inner class defined above.
     */
    static final int DATUM_CACHE_SIZE = Engineering.CACHE_INDEX + 1;
}
