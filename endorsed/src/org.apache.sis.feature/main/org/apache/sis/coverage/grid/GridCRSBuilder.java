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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.DefiningConversion;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.CoordinateOperationContext;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.internal.shared.OperationMethodExt;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Characters;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Builder for coordinate reference system which is derived from the coverage <abbr>CRS</abbr>
 * by the inverse of the "grid to <abbr>CRS</abbr>" transform. Those <abbr>CRS</abbr> describe
 * coordinates associated to the grid extent. This class provides two factory methods:
 *
 * <ul>
 *   <li>{@link #forCoverage()}</li>
 *   <li>{@link #forExtentAlone(Matrix, DimensionNameType[])}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class GridCRSBuilder extends ReferencingFactoryContainer {
    /**
     * Name of the parameter specifying the grid geometry.
     * In principle, this parameter should be mandatory. However, it is defined as optional for now
     * for avoiding the need to separate grid geometries when the <abbr>CRS</abbr> is compound.
     */
    private static final String GRID_PARAM = "Grid geometry";

    /**
     * Name of the parameter specifying which part (center or corner)
     * of the cell is associated with the coverage data attributes.
     */
    private static final String ANCHOR_PARAM = "Pixel in cell";

    /**
     * Description of the "<abbr>CRS</abbr> to grid indices" operation method.
     */
    private static final class Method extends DefaultOperationMethod implements OperationMethodExt {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -404891574462494877L;

        /** Copy of {@link org.apache.sis.referencing.operation.DefaultConcatenatedOperation#TRANSFORM_KEY}. */
        private static final String TRANSFORM_KEY = "transform";

        /** The unique instance. */
        static final Method INSTANCE;
        static {
            final ParameterBuilder b = new ParameterBuilder().setRequired(true);
            final ParameterDescriptor<?> anchor = b.addName(ANCHOR_PARAM).create(PixelInCell.class, PixelInCell.CELL_CENTER);
            final ParameterDescriptor<?> grid   = b.addName(GRID_PARAM).setRequired(false).create(GridGeometry.class, null);
            INSTANCE = new Method(b.addName("CRS to grid indices").createGroup(grid, anchor));
        }

        /** Creates the unique instance. */
        private Method(final ParameterDescriptorGroup params) {
            super(Map.of(IdentifiedObject.NAME_KEY, params.getName()), params);
        }

        /**
         * If the given <abbr>CRS</abbr> has been built by this method, returns the grid geometry.
         * Otherwise, returns {@code null}. The {@code anchor} argument is used for verifying that
         * the two operations use the same "pixel in cell" configuration.
         */
        private static GridGeometry grid(final CoordinateReferenceSystem crs, final EnumSet<PixelInCell> anchor) {
            if (crs instanceof DerivedCRS) {
                final Conversion conversion = ((DerivedCRS) crs).getConversionFromBase();
                if (conversion.getMethod() instanceof Method) {
                    final ParameterValueGroup values = conversion.getParameterValues();
                    if (anchor.isEmpty() == anchor.add((PixelInCell) values.parameter(ANCHOR_PARAM).getValue())) {
                        return (GridGeometry) values.parameter(GRID_PARAM).getValue();
                    }
                }
            }
            return null;
        }

        /**
         * If the given pair of <abbr>CRS</abbr>s is derived from grid geometries, finds a transform between them.
         * Compared to the default transform, the returned transform may handle the anti-meridian crossing.
         */
        @Override
        public boolean completeOperationMetadata(final CoordinateOperationContext context,
                                                 final CoordinateReferenceSystem  sourceCRS,
                                                 final CoordinateReferenceSystem  targetCRS,
                                                 final Map<String, Object> properties)
        {
            // The `instanceof` check is important for preventing never ending recursive invocations.
            if (!(context instanceof CoordinateOperationFinder || properties.containsKey(TRANSFORM_KEY))) {
                final EnumSet<PixelInCell> anchor = EnumSet.noneOf(PixelInCell.class);
                final GridGeometry source = grid(sourceCRS, anchor);
                if (source != null) {
                    final GridGeometry target = grid(targetCRS, anchor);
                    if (target != null) try {
                        // The set should always have exactly one element. If not, it would be a bug in `grid(…)`.
                        properties.put(TRANSFORM_KEY, source.createTransformTo(target, Containers.peekIfSingleton(anchor)));
                        return true;
                    } catch (TransformException e) {
                        throw new BackingStoreException(e);
                    }
                }
            }
            return false;
        }
    }

    /**
     * Scope of usage of the derived or engineering <abbr>CRS</abbr> created by this class.
     * This is a localized text saying "Conversions from coverage <abbr>CRS</abbr> to grid cell indices."
     */
    private static final InternationalString SCOPE = Resources.formatInternational(Resources.Keys.CrsToGridConversion);

    /**
     * The grid geometry for which to create a derived or compound <abbr>CRS</abbr> for cell indices.
     * This is kept constant after initialization, i.e. this field is not updated when descending in
     * <abbr>CRS</abbr> components. May be {@code null} if the <abbr>CRS</abbr> is built only from a
     * grid extent.
     */
    private GridGeometry fullGrid;

    /**
     * The cell part (center or corner) to map.
     */
    private final PixelInCell anchor;

    /**
     * A helper tool for separating the "<abbr>CRS</abbr> to grid" transform for each component.
     * This is {@code null} if the grid geometry has no <abbr>CRS</abbr> or no transform.
     */
    private TransformSeparator separator;

    /**
     * Properties to pass to the constructors of <abbr>CRS</abbr> components. Populated with metadata
     * of potential interest except {@value IdentifiedObject#NAME_KEY}, which must be added before usage.
     *
     * @see #properties(Object)
     */
    private final Map<String, Object> properties;

    /**
     * Locale to use for axis names and error messages, or {@code null} for default.
     * This is static and final for now because we do not yet provide a public API
     * for this option, but this policy may change in the future.
     */
    private static final Locale LOCALE = null;

    /**
     * Creates a new helper class for building a grid coordinate reference system.
     *
     * @param  anchor  the cell part to map (center or corner).
     */
    GridCRSBuilder(final PixelInCell anchor) {
        this.anchor = anchor;
        properties  = new HashMap<>(8);
        if (LOCALE != null) {
            properties.put(DefiningConversion.LOCALE_KEY, LOCALE);
        }
    }

    /**
     * Returns an error message for an illegal "grid to <abbr>CRS</abbr>" transform.
     */
    private static String illegalGridToCRS() {
        return Resources.forLocale(LOCALE).getString(Resources.Keys.IllegalGridGeometryComponent_1, "gridToCRS");
    }

    /**
     * Creates a derived or engineering <abbr>CRS</abbr> for the grid extent of a grid coverage.
     * Derived <abbr>CRS</abbr> are preferred as they allow conversions to geospatial <abbr>CRS</abbr>.
     * May return a compound <abbr>CRS</abbr> if the grid geometry has, for example, a temporal component.
     *
     * @param  grid     grid geometry of the coverage.
     * @param  derived  whether to force {@link DerivedCRS} instances.
     * @param  name     name of the derived or engineering <abbr>CRS</abbr> to create.
     * @return a derived, engineering or compound <abbr>CRS</abbr> for cell indices associated to the grid extent.
     * @throws InvalidGeodeticParameterException if characteristics of the grid geometry disallow this operation.
     * @throws FactoryException if another error occurred during the use of a referencing factory.
     */
    final CoordinateReferenceSystem forCoverage(final GridGeometry grid, final boolean derived, final Identifier name)
            throws FactoryException
    {
        properties.put(DefiningConversion.NORMALIZED_KEY, Boolean.FALSE);
        properties.put(ObjectDomain.SCOPE_KEY, SCOPE);
        grid.getGeographicExtent().ifPresent((domain) -> {
            properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, new DefaultExtent(null, domain, null, null));
        });
        fullGrid = grid;
        if (derived || grid.isDefined(GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) try {
            separator = new TransformSeparator(grid.getGridToCRS(anchor).inverse());
            return forComponent(name, grid.getCoordinateReferenceSystem(), 0, 0);
        } catch (NoninvertibleTransformException e) {
            throw new InvalidGeodeticParameterException(illegalGridToCRS(), e);
        }
        /*
         * Case where the grid geometry has no CRS or no "grid to CRS" transform.
         * We cannot create a derived CRS. Fallback on an engineering CRS with no
         * relationship to any other CRS.
         */
        final int dimension = grid.getDimension();
        final DimensionNameType[] dimensionNames;
        if (grid.isDefined(GridGeometry.EXTENT)) {
            dimensionNames = Arrays.copyOf(grid.getExtent().getAxisTypes(), dimension);
        } else {
            dimensionNames = new DimensionNameType[dimension];
        }
        final CoordinateSystem cs = createCS(dimension, dimensionNames, directions(dimensionNames), 1, true);
        final EngineeringDatum datum = getDatumFactory().createEngineeringDatum(properties(name));
        return getCRSFactory().createEngineeringCRS(properties(datum.getName()), datum, cs);
    }

    /**
     * Creates a derived <abbr>CRS</abbr> with a conversion from the real world <abbr>CRS</abbr> to grid indices.
     * This method may invoke itself recursively for separating a compound <abbr>CRS</abbr> into its components.
     *
     * <p>After return, {@link #separator} contains information about the transform for this component.
     * Caller can get the dimensions that have been used. Caller shall invoke {@code transform.clear()}
     * before to invoke this method again.</p>
     *
     * @param  name     name of the derived or compound <abbr>CRS</abbr> to create.
     * @param  baseCRS  real world <abbr>CRS</abbr> or component of that <abbr>CRS</abbr>.
     * @param  srcDim   dimension of the first axis of {@code baseCRS} relatively to the full real world <abbr>CRS</abbr>.
     * @param  tgtDim   dimension of the first axis of the return value relatively to the full derived <abbr>CRS</abbr>.
     * @return <abbr>CRS</abbr> for cell indices derived from the <abbr>CRS</abbr> of the given grid geometry.
     * @throws FactoryException if an error occurred during the use of a referencing factory.
     */
    private CoordinateReferenceSystem forComponent(final Object name, final CoordinateReferenceSystem baseCRS, int srcDim, int tgtDim)
            throws FactoryException
    {
        final int dimension = baseCRS.getCoordinateSystem().getDimension();
        /*
         * If the given CRS is a compound CRS (e.g. horizontal + vertical + temporal),
         * invoke this method recursively for each component and assemble the result.
         * We must keep in mind that the resulting CRS components are not necessarily
         * in same order as the components of the real world CRS.
         */
        if (baseCRS instanceof CompoundCRS) {
            // At first, elements are duplicated in the `components` array for each axis.
            final var components = new CoordinateReferenceSystem[dimension];
            for (final CoordinateReferenceSystem crs : ((CompoundCRS) baseCRS).getComponents()) {
                final CoordinateReferenceSystem derived = forComponent(name(crs), crs, srcDim, tgtDim);
                for (int i : separator.getTargetDimensions()) {
                    components[i] = derived;
                }
                separator.clear();
                srcDim += crs.getCoordinateSystem().getDimension();
                tgtDim += derived.getCoordinateSystem().getDimension();
            }
            // Deduplicate components with the restriction that same components must be consecutive.
            int count = 1;
            for (int i=1; i<dimension; i++) {
                final CoordinateReferenceSystem crs = components[i];
                if (crs != components[i-1]) {
                    for (int j = count; --j >= 0;) {
                        if (components[j] == crs) {
                            throw new InvalidGeodeticParameterException(illegalGridToCRS());
                        }
                    }
                    components[count++] = crs;
                }
            }
            return getCRSFactory().createCompoundCRS(properties(name), ArraysExt.resize(components, count));
        }
        /*
         * Case of a single (non-compound) CRS. The separator contains the "CRS to grid" transform.
         * Therefore, the source dimensions are in the base CRS (the real world CRS) and the target
         * dimensions are those of the derived CRS to create.
         */
        separator.addSourceDimensionRange(srcDim, srcDim + dimension);
        final MathTransform crsToGrid = separator.separate();
        final int[] dispatch = separator.getTargetDimensions();
        final var dimensionNames = new DimensionNameType[dispatch.length];
        /*
         * Get the directions of the axes of the coverage coordinate system, but in the order of grid dimensions.
         * The direction array may contain null elements if directions could not be inferred for some dimensions.
         * Design note: we perform this check for each single component instead of computing the derivative once
         * in the `forCoverage(…)` method because we need an affine transform. When the transform for the whole
         * grid is not affine, very often the transform for some single components is still affine.
         */
        AxisDirection[] directions;
toGrid: try {
            final Matrix derivative;
            if (fullGrid.isDefined(GridGeometry.EXTENT)) {
                final GridExtent extent = fullGrid.getExtent();
                Arrays.setAll(dimensionNames, (i) -> extent.getAxisType(dispatch[i]).orElse(null));
                derivative = crsToGrid.derivative(new DirectPositionView.Double(extent.getPointOfInterest(anchor), srcDim, dimension));
            } else try {
                derivative = crsToGrid.derivative(null);
            } catch (NullPointerException e) {
                Logging.ignorableException(GridExtent.LOGGER, GridGeometry.class, "createGridCRS", e);
                directions = directions(dimensionNames);
                break toGrid;
            }
            // Last arguments are `null` because `dimensionNames` is already in the desired order.
            directions = CoordinateSystems.getSimpleAxisDirections(baseCRS.getCoordinateSystem());
            directions = reorder(directions, derivative, null, null);
            for (int i=0; i < dimensionNames.length; i++) {
                if (dimensionNames[i] == null && directions[i] != null) {
                    final DimensionNameType type = GridExtent.DIMENSION_NAMES.get(directions[i]);
                    if (type != null && !ArraysExt.contains(dimensionNames, type)) {
                        dimensionNames[i] = type;
                    }
                }
            }
        } catch (TransformException e) {
            // `GridGeometry.createGridCRS(…)` is the public API that invoked this method.
            Logging.recoverableException(GridExtent.LOGGER, GridGeometry.class, "createGridCRS", e);
            directions = directions(dimensionNames);
        }
        /*
         * Create the coordinate system, then the conversion, and finally the derived CRS.
         * The `GRID_PARAM` parameter should be mandatory, but for now it is not clear that
         * it is worth to pay the cost of creating sub-grids.
         */
        final Method method = Method.INSTANCE;
        final ParameterValueGroup params = method.getParameters().createValue();
        if (srcDim == 0 && dimension == fullGrid.getDimension()) {
            params.parameter(GRID_PARAM).setValue(fullGrid);
        }
        params.parameter(ANCHOR_PARAM).setValue(anchor);
        final var conversion = new DefiningConversion(properties(method.getName()), method, crsToGrid, params);
        final CoordinateSystem cs = createCS(dispatch.length, dimensionNames, directions, tgtDim + 1, true);
        return getCRSFactory().createDerivedCRS(properties(name), baseCRS, conversion, cs);
    }

    /**
     * Returns the properties map for the construction of a <abbr>CRS</abbr> or operation of the given name.
     */
    private Map<String,?> properties(final Object name) {
        properties.put(IdentifiedObject.NAME_KEY, name);
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Returns a default name for the component of a grid <abbr>CRS</abbr>.
     *
     * @param  baseCRS  the real world <abbr>CRS</abbr> from which to derive a grid <abbr>CRS</abbr>.
     * @return default name for a grid <abbr>CRS</abbr> derived from the given real world <abbr>CRS</abbr>.
     */
    private static String name(final CoordinateReferenceSystem baseCRS) {
        String name = IdentifiedObjects.getSimpleNameOrIdentifier(baseCRS);
        if (name == null) {
            name = IdentifiedObjects.getDisplayName(baseCRS, LOCALE);
        }
        return "Grid based on " + name;
    }

    /**
     * Returns the coordinate reference system that we use as a template for object names.
     * This template uses generic terms such as "Cell indices" for the <abbr>CRS</abbr> name
     * and "Unknown grid" for the datum.
     */
    private static EngineeringCRS template() {
        return CommonCRS.Engineering.GRID.crs();
    }

    /**
     * Creates the coordinate system for the derived or engineering <abbr>CRS</abbr> of a grid.
     *
     * @param  dimension       number of dimensions of the coordinate system to create.
     * @param  dimensionNames  names of grid dimension. Shall not be null but may contain null elements.
     * @param  directions      directions of the axes of the coordinate system to create. May contain null elements.
     * @param  labelOffset     offset to add to the dimension for producing a default axis name or abbreviation.
     * @return coordinate system for the grid extent, or {@code null} if it cannot be inferred.
     * @throws FactoryException if an error occurred during the use of {@link CSFactory}.
     */
    private CoordinateSystem createCS(final int dimension, final DimensionNameType[] dimensionNames,
            final AxisDirection[] directions, final int labelOffset, final boolean cartesian)
            throws FactoryException
    {
        final CSFactory csFactory = getCSFactory();
        boolean hasVertical = false;
        boolean hasTime     = false;
        boolean hasOther    = false;
        final var axes = new CoordinateSystemAxis[dimension];
        for (int j=0; j<dimension; j++) {
            String abbreviation = null;
            final DimensionNameType type = dimensionNames[j];
            boolean pixel = false;
            if (type != null) {
                if (type == DimensionNameType.COLUMN || type == DimensionNameType.SAMPLE) {
                    abbreviation = "x"; pixel = true;
                } else if (type == DimensionNameType.ROW || type == DimensionNameType.LINE) {
                    abbreviation = "y"; pixel = true;
                } else if (type == DimensionNameType.VERTICAL) {
                    abbreviation = "z"; hasVertical = true;
                } else if (type == DimensionNameType.TIME) {
                    abbreviation = "t"; hasTime = true;
                } else {
                    hasOther = true;
                }
            }
            if (abbreviation != null) {
                for (int i = j; --i >= 0;) {
                    final CoordinateSystemAxis previous = axes[i];
                    if (abbreviation.equals(previous.getAbbreviation())) {
                        abbreviation = null;
                        break;
                    }
                }
            }
            if (abbreviation == null) {
                final var b = new StringBuilder(4).append('x').append(labelOffset + j);
                for (int i = b.length(); --i >= 1;) {
                    b.setCharAt(i, Characters.toSubScript(b.charAt(i)));
                }
                abbreviation = b.toString();
            }
            /*
             * Try to infer the axis name from the grid dimension name type, otherwise create
             * a default name in a way similar to the abbreviation (with indices in subscripts).
             */
            String name = Types.toString(Types.getCodeTitle(type), LOCALE);
            if (name == null) {
                name = Vocabulary.forLocale(LOCALE).getString(Vocabulary.Keys.Dimension_1, labelOffset + j);
            }
            AxisDirection direction = directions[j];
            if (direction == null) {
                direction = AxisDirection.UNSPECIFIED;
            }
            axes[j] = csFactory.createCoordinateSystemAxis(properties(name), abbreviation, direction, pixel ? Units.PIXEL : Units.UNITY);
        }
        /*
         * Create a coordinate system of affine type if all axes seem spatial.
         * If no specialized type seems to fit, use an unspecified ("abstract")
         * coordinate system type in last resort.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Map<String,?> properties = properties(template().getCoordinateSystem().getName());
        final CoordinateSystemAxis axis = axes[0];
        switch (dimension) {
            case 1:  {
                if (hasVertical) {
                    return csFactory.createVerticalCS(properties, axis);
                } else if (hasTime) {
                    return csFactory.createTimeCS(properties, axis);
                } else if (hasOther) {
                    break;
                } else {
                    return csFactory.createLinearCS(properties, axis);
                }
            }
            case 2: {
                if (hasVertical | hasTime | hasOther) break;
                return cartesian
                        ? csFactory.createCartesianCS(properties, axis, axes[1])
                        : csFactory.createAffineCS   (properties, axis, axes[1]);
            }
            case 3: {
                if (hasVertical | hasTime | hasOther) break;
                return cartesian
                        ? csFactory.createCartesianCS(properties, axis, axes[1], axes[2])
                        : csFactory.createAffineCS   (properties, axis, axes[1], axes[2]);
            }
        }
        return new AbstractCS(properties, axes);
    }

    /**
     * Returns the default axis directions for grid dimensions of the given name.
     *
     * @param  types  grid dimension names.
     * @return default axis directions. May contain null elements.
     */
    private static AxisDirection[] directions(final DimensionNameType[] types) {
        final var directions = new AxisDirection[types.length];
        for (int i=0; i<types.length; i++) {
            final DimensionNameType type = types[i];
            if (type != null) {
                final AxisDirection direction = GridExtent.AXIS_DIRECTIONS.get(type);
                if (!ArraysExt.contains(directions, direction)) {
                    directions[i] = direction;
                }
            }
        }
        return directions;
    }

    /**
     * Adjusts the order of code list values for any change of order applied by the given transform.
     * For example, if {@code directions} contains the axis directions in the coverage <abbr>CRS</abbr>
     * and if {@code derivative} is the derivative of the <abbr>CRS</abbr> to grid transform, then this
     * method returns the axis directions of the grid. Values that cannot be mapped are set to null.
     *
     * @param  directions  the directions to reorder. Shall not be null but may contain null elements.
     * @param  derivative  derivative of the transform from source to target <abbr>CRS</abbr>.
     * @param  source      an optional array to reorder together with {@code directions}.
     * @param  target      where to store the result of {@code source} reordering, or {@code null}.
     * @return the reordered axis directions. May contain {@code null} elements.
     */
    private static AxisDirection[] reorder(final AxisDirection[] directions, final Matrix derivative,
                                           final DimensionNameType[] source, final DimensionNameType[] target)
    {
        final var ordered = new AxisDirection[derivative.getNumRow()];
        for (int j=0; j<ordered.length; j++) {
            boolean found = false;
            for (int i=0; i<directions.length; i++) {
                final double m = derivative.getElement(j, i);
                if (m != 0) {
                    if (found) {
                        ordered[j] = null;
                        if (target != null) {
                            target[j] = null;
                        }
                        break;
                    }
                    found = true;
                    AxisDirection selected = directions[i];
                    if (selected != null && m < 0) {
                        selected = AxisDirections.opposite(selected);
                    }
                    ordered[j] = selected;
                    if (target != null && i < source.length) {
                        target[j] = source[i];
                    }
                }
            }
        }
        return ordered;
    }

    /**
     * Builds the coordinate reference system of the result of transforming a {@link GridExtent}.
     * This is used only in the rare cases where we need to represent an extent as an envelope.
     * This class converts {@link DimensionNameType} codes into axis names, abbreviations and directions.
     * It is the converse of {@link GridExtent#typeFromAxes(CoordinateReferenceSystem, int)}.
     *
     * <p>The <abbr>CRS</abbr> type is always engineering. In particular, the <abbr>CRS</abbr> cannot be temporal
     * because we do not know the temporal datum origin and because index unit is not a temporal unit.</p>
     *
     * @param  derivative  derivative of the transform converting grid cell indices to envelope coordinates.
     * @param  types       the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @return <abbr>CRS</abbr> for the grid, or empty if it cannot be built.
     * @throws FactoryException if an error occurred during the use of a referencing factory.
     *
     * @see GridExtent#toEnvelope(MathTransform)
     * @see GridExtent#typeFromAxes(CoordinateReferenceSystem, int)
     */
    final Optional<EngineeringCRS> forExtentAlone(final Matrix derivative, final DimensionNameType[] types)
            throws FactoryException
    {
        final int dimension = derivative.getNumRow();
        final var dimensionNames = new DimensionNameType[dimension];
        AxisDirection[] directions = directions(ArraysExt.resize(types, dimension));
        directions = reorder(directions, derivative, types, dimensionNames);
        final CoordinateSystem cs = createCS(dimension, dimensionNames, directions, 1, false);
        if (cs == null) {
            return Optional.empty();
        }
        final EngineeringCRS template = template();
        return Optional.of(getCRSFactory().createEngineeringCRS(properties(template.getName()), template.getDatum(), cs));
    }
}
