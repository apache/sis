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
import java.util.Arrays;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.io.IOException;
import java.time.Instant;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.provider.Equirectangular;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.TemporalUtilities;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;


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
 *   <li>Invoke {@link #build(Decoder)} on each builder prepared in above step.</li>
 *   <li>Assemble the CRS components created in above step in a {@code CompoundCRS}.</li>
 * </ol>
 *
 * The builder type is inferred from axes. The axes are identified by their abbreviations,
 * which is a {@linkplain Axis#abbreviation controlled vocabulary} for this implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
abstract class CRSBuilder<D extends Datum, CS extends CoordinateSystem> {
    /**
     * The type of datum as a GeoAPI sub-interface of {@link Datum}.
     * Used for verifying the type of cached datum at {@link #datumIndex}.
     */
    private final Class<D> datumType;

    /**
     * Name of the datum on which the CRS is presumed to be based, or {@code ""}. This is used
     * for building a datum name like <cite>"Unknown datum presumably based on GRS 1980"</cite>.
     */
    private final String datumBase;

    /**
     * Index of the cached datum in a {@code Datum[]} array, from 0 inclusive to {@value #DATUM_CACHE_SIZE} exclusive.
     * The datum type at that index must be an instance of {@link #datumType}. We cache only the datum because they do
     * not depend on the netCDF file content in the common case where the CRS is not explicitly specified.
     */
    private final byte datumIndex;

    /**
     * Specify the range of valid number of dimensions, inclusive.
     * The {@link #dimension} value shall be in that range.
     */
    private final byte minDim, maxDim;

    /**
     * Number of valid elements in the {@link #axes} array. The count should not be larger than 3,
     * even if the netCDF file has more axes, because each {@code CRSBuilder} is only for a subset.
     */
    private byte dimension;

    /**
     * The axes to use for creating the coordinate reference system.
     * They are information about netCDF axes, not yet ISO 19111 axes.
     * The axis are listed in "natural" order (reverse of netCDF order).
     * Only the {@link #dimension} first elements are valid.
     */
    private Axis[] axes;

    /**
     * The datum created by {@link #createDatum(DatumFactory, Map)}.
     */
    protected D datum;

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
    private CRSBuilder(final Class<D> datumType, final String datumBase, final byte datumIndex, final byte minDim, final byte maxDim) {
        this.datumType  = datumType;
        this.datumBase  = datumBase;
        this.datumIndex = datumIndex;
        this.minDim     = minDim;
        this.maxDim     = maxDim;
        this.axes       = new Axis[3];
    }

    /**
     * Dispatches the given axis to a {@code CRSBuilder} appropriate for the axis type. The axis type is determined
     * from {@link Axis#abbreviation}, taken as a controlled vocabulary. If no suitable {@code CRSBuilder} is found
     * in the given list, then a new one will be created and added to the list.
     *
     * @param  components  the list of builder where to dispatch the axis. May be modified by this method.
     * @param  axis        the axis to add to a builder in the given list.
     * @throws DataStoreContentException if the given axis can not be added in a builder.
     */
    @SuppressWarnings("fallthrough")
    public static void dispatch(final List<CRSBuilder<?,?>> components, final Axis axis) throws DataStoreContentException {
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
         * If a builder of 'addTo' class already exists, add the axis in the existing builder.
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
previous:   for (int i=components.size(); --i >= 0;) {
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
     * @throws DataStoreContentException if the given axis can not be added in this builder.
     */
    private void add(final Axis axis) throws DataStoreContentException {
        if (dimension == Byte.MAX_VALUE) {
            throw new DataStoreContentException(getFirstAxis().coordinates.errors()
                    .getString(Errors.Keys.ExcessiveListSize_2, "axes", (short) (Byte.MAX_VALUE + 1)));
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
     */
    public final SingleCRS build(final Decoder decoder) throws FactoryException, DataStoreException, IOException {
        if (dimension < minDim || dimension > maxDim) {
            final Variable axis = getFirstAxis().coordinates;
            throw new DataStoreContentException(axis.resources().getString(Resources.Keys.UnexpectedAxisCount_4,
                    axis.getFilename(), getClass().getSimpleName(), dimension, NamedElement.listNames(axes, dimension, ", ")));
        }
        /*
         * If the subclass can offer coordinate system and datum candidates based on a brief inspection of axes,
         * set the datum, CS and CRS field values to those candidate. Those values do not need to be exact; they
         * will be overwritten later if they do not match the netCDF file content.
         */
        datum = datumType.cast(decoder.datumCache[datumIndex]);         // Should be before 'setPredefinedComponents' call.
        setPredefinedComponents(decoder);
        /*
         * If `setPredefinedComponents(decoder)` offers a datum, we will used it as-is. Otherwise create the datum now.
         * Datum are often not defined in netCDF files, so the above `setPredefinedComponents` method call may have set
         * EPSG::6019 — "Not specified (based on GRS 1980 ellipsoid)". If not, we build a similar name.
         */
        if (datum == null) {
            // Not localized because stored as a String, possibly exported in WKT or GML, and 'datumBase' is in English.
            createDatum(decoder.getDatumFactory(), properties("Unknown datum presumably based upon ".concat(datumBase)));
        }
        decoder.datumCache[datumIndex] = datum;
        /*
         * Verify if a pre-defined coordinate system can be used. This is often the case, for example
         * the EPSG::6424 coordinate system can be used for (longitude, latitude) axes in degrees.
         * Using a pre-defined CS allows us to get more complete definitions (minimum and maximum values, etc.).
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
         * If 'setPredefinedComponents(decoder)' did not proposed a coordinate system, or if it proposed a CS
         * but its axes do not match the axes in the netCDF file, then create a new coordinate system here.
         */
        if (referenceSystem == null) {
            final Map<String,?> properties;
            if (coordinateSystem == null) {
                // Fallback if the coordinate system is not predefined.
                final StringJoiner joiner = new StringJoiner(" ");
                final CSFactory csFactory = decoder.getCSFactory();
                final CoordinateSystemAxis[] iso = new CoordinateSystemAxis[dimension];
                for (int i=0; i<iso.length; i++) {
                    final Axis axis = axes[i];
                    joiner.add(axis.getName());
                    iso[i] = axis.toISO(csFactory, i);
                }
                createCS(csFactory, properties(joiner.toString()), iso);
                properties = properties(coordinateSystem.getName());
            } else {
                properties = properties(NamedElement.listNames(axes, dimension, " "));
            }
            createCRS(decoder.getCRSFactory(), properties);
        }
        /*
         * Creates the coordinate reference system using current value of 'datum' and 'coordinateSystem' fields.
         * The coordinate system initially have a [-180 … +180]° longitude range. If the actual coordinate values
         * are outside that range, switch the longitude range to [0 … 360]°.
         */
        final CoordinateSystem cs = referenceSystem.getCoordinateSystem();
        for (int i=cs.getDimension(); --i >= 0;) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            if (RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
                final Vector coordinates = axes[i].read();                          // Typically a cached vector.
                final int length = coordinates.size();
                if (length != 0) {
                    final double first = coordinates.doubleValue(0);
                    final double last  = coordinates.doubleValue(length - 1);
                    if (Math.min(first, last) >= 0 && Math.max(first, last) > axis.getMaximumValue()) {
                        referenceSystem = (SingleCRS) AbstractCRS.castOrCopy(referenceSystem).forConvention(AxesConvention.POSITIVE_RANGE);
                        break;
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
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Returns the EPSG code of a possible coordinate system from EPSG database. This method proceed by brief
     * inspection of axis directions and units; there is no guarantees that the coordinate system returned by
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
        final AxisDirection[] directions = new AxisDirection[dimension];
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
     * can propose a CRS candidate instead than only a CS candidate.</p>
     */
    abstract void setPredefinedComponents(Decoder decoder) throws FactoryException;

    /**
     * Creates the datum for the coordinate reference system to build. The datum are generally not specified in netCDF files.
     * To make that clearer, this method builds datum with names like <cite>"Unknown datum presumably based on GRS 1980"</cite>.
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
        Geodetic(final byte minDim) {
            super(GeodeticDatum.class, "GRS 1980", (byte) 0, minDim, (byte) 3);
        }

        /**
         * Initializes this builder before {@link #build(Decoder)} execution.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            defaultCRS = decoder.convention().defaultHorizontalCRS(false);
        }

        /**
         * Creates a {@link GeodeticDatum} for <cite>"Unknown datum presumably based on GRS 1980"</cite>.
         * This method is invoked only if {@link #setPredefinedComponents(Decoder)} failed to create a datum.
         */
        @Override final void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            final GeodeticDatum template = defaultCRS.datum();
            datum = factory.createGeodeticDatum(properties, template.getEllipsoid(), template.getPrimeMeridian());
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
                isLongitudeFirst = AxisDirection.EAST.equals(axis.direction);
                if (isLongitudeFirst || AxisDirection.NORTH.equals(axis.direction)) {
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
            super((byte) 3);
        }

        /**
         * Possibly sets {@link #datum} and {@link #coordinateSystem} to predefined objects
         * matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            super.setPredefinedComponents(decoder);
            if (isPredefinedCS(Units.DEGREE)) {
                GeocentricCRS crs = defaultCRS.spherical();
                if (isLongitudeFirst) {
                    crs = DefaultGeocentricCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
                }
                referenceSystem  = crs;
                coordinateSystem = (SphericalCS) crs.getCoordinateSystem();
                datum            = crs.getDatum();
            } else {
                datum = defaultCRS.datum();
            }
        }

        /**
         * Creates the three-dimensional {@link SphericalCS} from given axes. This method is invoked only
         * if {@link #setPredefinedComponents(Decoder)} failed to assign a CS or if {@link #build(Decoder)}
         * found that the {@link #coordinateSystem} does not have compatible axes.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            coordinateSystem = factory.createSphericalCS(properties, axes[0], axes[1], axes[2]);
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         * This method is invoked under conditions similar to the ones of above {@code createCS(…)} method.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem = factory.createGeocentricCRS(properties, datum, coordinateSystem);
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
            super((byte) 2);
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
            } else {
                datum = defaultCRS.datum();
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
         * {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system or if {@link #build(Decoder)}
         * found that the {@link #coordinateSystem} does not have compatible axes.
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
            referenceSystem = factory.createGeographicCRS(properties, datum, coordinateSystem);
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
         * Defining conversion for "Not specified (presumed Plate Carrée)". This conversion use spherical formulas.
         * Consequently it should be used with {@link #sphericalDatum} instead of {@link #defaultCRS}.
         */
        private static final Conversion UNKNOWN_PROJECTION;
        static {
            final CoordinateOperationFactory factory = DefaultFactories.forBuildin(CoordinateOperationFactory.class);
            try {
                final OperationMethod method = factory.getOperationMethod(Equirectangular.NAME);
                UNKNOWN_PROJECTION = factory.createDefiningConversion(
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
            super((byte) 2);
        }

        /**
         * Possibly sets {@link #datum}, {@link #coordinateSystem} and {@link #referenceSystem}
         * to predefined objects matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) throws FactoryException {
            super.setPredefinedComponents(decoder);
            sphericalDatum = decoder.convention().defaultHorizontalCRS(true);
            datum = sphericalDatum.datum();
            if (isPredefinedCS(Units.METRE)) {
                coordinateSystem = decoder.getStandardProjectedCS();
            }
        }

        /**
         * Creates the two- or three-dimensional {@link CartesianCS} from given axes. This method is invoked only if
         * {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system or if {@link #build(Decoder)}
         * found that the {@link #coordinateSystem} does not have compatible axes.
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
            if (!baseCRS.getDatum().equals(datum)) {
                baseCRS = factory.createGeographicCRS(properties, datum, baseCRS.getCoordinateSystem());
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
         * Creates a new builder (invoked by lambda function).
         */
        public Vertical() {
            super(VerticalDatum.class, "Mean Sea Level", (byte) 1, (byte) 1, (byte) 1);
        }

        /**
         * Possibly sets {@link #coordinateSystem} to a predefined CS matching the axes defined in the netCDF file.
         */
        @Override void setPredefinedComponents(final Decoder decoder) {
            final Axis axis = getFirstAxis();
            final Unit<?> unit = axis.getUnit();
            final CommonCRS.Vertical predefined;
            if (Units.METRE.equals(unit)) {
                if (AxisDirection.UP.equals(axis.direction)) {
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
         * Creates a {@link VerticalDatum} for <cite>"Unknown datum based on Mean Sea Level"</cite>.
         */
        @Override void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            datum = factory.createVerticalDatum(properties, VerticalDatumType.GEOIDAL);
        }

        /**
         * Creates the one-dimensional {@link VerticalCS} from given axes. This method is invoked
         * only if {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system
         * or if {@link #build(Decoder)} found that the axis or direction are not compatible.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            coordinateSystem = factory.createVerticalCS(properties, axes[0]);
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem =  factory.createVerticalCRS(properties, datum, coordinateSystem);
        }
    }




    /**
     * Temporal CRS with (t) axis. Its datum need to be built
     * in a special way since it contains the time origin.
     */
    private static final class Temporal extends CRSBuilder<TemporalDatum, TimeCS> {
        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Temporal() {
            super(TemporalDatum.class, "", (byte) 2, (byte) 1, (byte) 1);
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
         * Creates a {@link TemporalDatum} for <cite>"Unknown datum based on …"</cite>.
         */
        @Override void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            final Axis axis = getFirstAxis();
            axis.getUnit();                                     // Force epoch parsing if not already done.
            Instant epoch = axis.coordinates.epoch;
            final CommonCRS.Temporal c = CommonCRS.Temporal.forEpoch(epoch);
            if (c != null) {
                datum = c.datum();
            } else {
                properties = properties("Time since " + epoch);
                datum = factory.createTemporalDatum(properties, TemporalUtilities.toDate(epoch));
            }
        }

        /**
         * Creates the one-dimensional {@link TimeCS} from given axes. This method is invoked only
         * if {@link #setPredefinedComponents(Decoder)} failed to assign a coordinate system or if
         * {@link #build(Decoder)} found that the axis or direction are not compatible.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            coordinateSystem = factory.createTimeCS(properties, axes[0]);
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            properties = properties(getFirstAxis().coordinates.getUnitsString());
            referenceSystem =  factory.createTemporalCRS(properties, datum, coordinateSystem);
        }
    }




    /**
     * Unknown CRS with (x,y,z) axes.
     */
    private static final class Engineering extends CRSBuilder<EngineeringDatum, CoordinateSystem> {
        /**
         * Creates a new builder (invoked by lambda function).
         */
        public Engineering() {
            super(EngineeringDatum.class, "affine coordinate system", (byte) 3, (byte) 2, (byte) 3);
        }

        /**
         * No-op since we have no predefined engineering CRS.
         */
        @Override void setPredefinedComponents(final Decoder decoder) {
        }

        /**
         * Creates a {@link VerticalDatum} for <cite>"Unknown datum based on affine coordinate system"</cite>.
         */
        @Override void createDatum(DatumFactory factory, Map<String,?> properties) throws FactoryException {
            datum = factory.createEngineeringDatum(properties);
        }

        /**
         * Creates two- or three-dimensional coordinate system (usually {@link AffineCS}) from given axes.
         */
        @Override void createCS(CSFactory factory, Map<String,?> properties, CoordinateSystemAxis[] axes) throws FactoryException {
            try {
                if (axes.length > 2) {
                    coordinateSystem = factory.createAffineCS(properties, axes[0], axes[1], axes[2]);
                } else {
                    coordinateSystem = factory.createAffineCS(properties, axes[0], axes[1]);
                }
            } catch (InvalidGeodeticParameterException e) {
                /*
                 * Unknown Coordinate System type, for example because of unexpected units of measurement for a
                 * Cartesian or affine coordinate system.  The fallback object created below is not abstract in
                 * the Java sense, but in the sense that we don't have more specific information on the CS type.
                 */
                coordinateSystem = new AbstractCS(properties, axes);
                recoverableException(e);
            }
        }

        /**
         * Creates the coordinate reference system from datum and coordinate system computed in previous steps.
         */
        @Override void createCRS(CRSFactory factory, Map<String,?> properties) throws FactoryException {
            referenceSystem =  factory.createEngineeringCRS(properties, datum, coordinateSystem);
        }
    }

    /**
     * Maximal {@link #datumIndex} value +1. The maximal value can be seen in the call to {@code super(…)} constructor
     * in the last inner class defined above.
     */
    static final int DATUM_CACHE_SIZE = 4;
}
