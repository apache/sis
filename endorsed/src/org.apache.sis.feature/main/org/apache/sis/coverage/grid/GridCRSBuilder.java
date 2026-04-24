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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Characters;
import org.apache.sis.util.resources.Vocabulary;
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
     * Name of the parameter where to store the grid coverage name.
     */
    private static final String NAME_PARAM = "Target grid name";

    /**
     * Name of the parameter specifying the way image indices are
     * associated with the coverage data attributes.
     */
    private static final String ANCHOR_PARAM = "Pixel in cell";

    /**
     * Description of "<abbr>CRS</abbr> to grid indices" operation method.
     */
    private static final OperationMethod METHOD;
    static {
        final ParameterBuilder b = new ParameterBuilder().setRequired(true);
        final ParameterDescriptor<?>   name   = b.addName(NAME_PARAM).create(Identifier.class, null);
        final ParameterDescriptor<?>   anchor = b.addName(ANCHOR_PARAM).create(PixelInCell.class, PixelInCell.CELL_CENTER);
        final ParameterDescriptorGroup params = b.addName("CRS to grid indices").createGroup(name, anchor);
        METHOD = new DefaultOperationMethod(singleton(params.getName()), params);
    }

    /**
     * Scope of usage of the <abbr>CRS</abbr>.
     * This is a localized text saying "Conversions from coverage <abbr>CRS</abbr> to grid cell indices."
     */
    private static final InternationalString SCOPE = Resources.formatInternational(Resources.Keys.CrsToGridConversion);

    /**
     * Name of the coordinate systems created by this class.
     */
    private static final NamedIdentifier CS_NAME = new NamedIdentifier(
            Citations.SIS, Vocabulary.formatInternational(Vocabulary.Keys.GridExtent));

    /**
     * The grid geometry for which to create a grid coordinate reference system.
     */
    private final GridGeometry grid;

    /**
     * A helper tools for separating the "grid to <abbr>CRS</abbr>" transform for each component,
     * or {@code null} if there is no transform.
     */
    private final TransformSeparator separator;

    /**
     * The cell part to map (center or corner).
     */
    private final PixelInCell anchor;

    /**
     * Name of the <abbr>CRS</abbr> to create.
     */
    private final Identifier finalName;

    /**
     * Properties to pass to the constructors of <abbr>CRS</abbr> components. Populated with metadata
     * of potential interest except {@value IdentifiedObject#NAME_KEY}, which must be added before usage.
     *
     * @see #properties(Object)
     */
    private final Map<String, Object> properties;

    /**
     * Locale to use for axis names and error messages, or {@code null} for default.
     */
    private final Locale locale;

    /**
     * Creates a new helper class for building a grid coordinate reference system.
     *
     * @param  grid     grid geometry of the coverage.
     * @param  anchor   the cell part to map (center or corner).
     * @param  name     name of the <abbr>CRS</abbr> to create.
     * @param  derived  whether to force {@link DerivedCRS} instances.
     * @param  locale   locale to use for axis names and error messages, or {@code null} for default.
     */
    GridCRSBuilder(final GridGeometry grid, final PixelInCell anchor,
            final Identifier name, final boolean derived, final Locale locale)
    {
        this.grid       = grid;
        this.anchor     = anchor;
        this.locale     = locale;
        this.finalName  = name;
        this.properties = new HashMap<>(8);
        properties.put(DefaultConversion.LOCALE_KEY, locale);
        properties.put(ObjectDomain.SCOPE_KEY, SCOPE);
        grid.getGeographicExtent().ifPresent((domain) -> {
            properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, new DefaultExtent(null, domain, null, null));
        });
        if (derived || grid.isDefined(GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) {
            separator = new TransformSeparator(grid.getGridToCRS(anchor));
        } else {
            separator = null;
        }
    }

    /**
     * Creates a derived or engineering <abbr>CRS</abbr> for the grid extent of a grid coverage.
     * Derived <abbr>CRS</abbr> are preferred as they allow conversions to geospatial <abbr>CRS</abbr>.
     * May return a compound <abbr>CRS</abbr> if the grid geometry has, for example, a temporal component.
     *
     * @return a derived, engineering or compound <abbr>CRS</abbr> for cell indices associated to the grid extent.
     * @throws InvalidGeodeticParameterException if characteristics of the grid geometry disallow this operation.
     * @throws FactoryException if another error occurred during the use of a referencing factory.
     */
    final CoordinateReferenceSystem forCoverage() throws FactoryException {
        if (separator != null) try {
            return forComponent(finalName, grid.getCoordinateReferenceSystem(), 0, 0);
        } catch (NoninvertibleTransformException e) {
            throw new InvalidGeodeticParameterException(illegalGridToCRS(), e);
        }
        DimensionNameType[] types = null;
        if (grid.isDefined(GridGeometry.EXTENT)) {
            types = grid.getExtent().getAxisTypes();
        }
        final int dimension = grid.getDimension();
        final CoordinateSystem cs = createGridCS(dimension, types, 0);
        return getCRSFactory().createEngineeringCRS(properties(finalName), CommonCRS.Engineering.GRID.datum(), cs);
    }

    /**
     * Creates a derived <abbr>CRS</abbr> with a conversion from the grid geometry <abbr>CRS</abbr>
     * to the grid extent <abbr>CRS</abbr>. This method can be invoked only when the grid geometry
     * has a <abbr>CRS</abbr> and a "grid to <abbr>CRS</abbr>" transform. This case is identified
     * by a non-null {@link #separator}. This method may invoke itself recursively.
     *
     * @param  name     name of the <abbr>CRS</abbr> to create, or {@code null} for a default value.
     * @param  baseCRS  <abbr>CRS</abbr> or component of the <abbr>CRS</abbr> of the grid geometry.
     * @param  srcDim   dimension of the first axis of {@code baseCRS}. Non-zero only during recursive invocation.
     * @param  tgtDim   dimension of the first axis of the result. Non-zero only during recursive invocation.
     * @return grid extent <abbr>CRS</abbr> derived from the given {@code baseCRS}.
     * @throws FactoryException if an error occurred during the use of a referencing factory.
     */
    private CoordinateReferenceSystem forComponent(Object name, final CoordinateReferenceSystem baseCRS, int srcDim, int tgtDim)
            throws FactoryException, NoninvertibleTransformException
    {
        if (name == null) {
            name = IdentifiedObjects.getSimpleNameOrIdentifier(baseCRS);
            if (name == null) {
                name = IdentifiedObjects.getDisplayName(baseCRS, locale);
            }
            name = "Grid of " + name;
        }
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
                final CoordinateReferenceSystem derived = forComponent(null, crs, srcDim, tgtDim);
                for (int i : separator.getSourceDimensions()) components[i] = derived;
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
         * Case of a single (non-compound) CRS. The separator contains the "grid to CRS" transform.
         * Therefore, the target dimensions are the CRS dimensions, which are the source dimensions
         * from the point of view of the derived CRS.
         */
        separator.addTargetDimensionRange(srcDim, srcDim + dimension);
        final MathTransform gridToCRS = separator.separate();
        final int[] src = separator.getSourceDimensions();
        final var types = new DimensionNameType[src.length];
        if (grid.isDefined(GridGeometry.EXTENT)) {
            final GridExtent extent = grid.getExtent();
            final CoordinateSystem cs = baseCRS.getCoordinateSystem();
            Arrays.setAll(types, (i) -> extent.getAxisType(src[i]).orElseGet(
                    () -> GridExtent.typeFromAxis(cs.getAxis(i)).orElse(null)));
        }
        final CoordinateSystem cs = createGridCS(src.length, types, tgtDim);
        final ParameterValueGroup params = METHOD.getParameters().createValue();
        params.parameter(NAME_PARAM).setValue(finalName);
        params.parameter(ANCHOR_PARAM).setValue(anchor);
        final var conversion = new DefaultConversion(properties(METHOD.getName()), METHOD, gridToCRS.inverse(), params);
        return getCRSFactory().createDerivedCRS(properties(name), baseCRS, conversion, cs);
    }

    /**
     * Returns an error message for an illegal "grid to <abbr>CRS</abbr>" transform.
     */
    private String illegalGridToCRS() {
        return Resources.forLocale(locale).getString(Resources.Keys.IllegalGridGeometryComponent_1, "gridToCRS");
    }

    /**
     * Returns the properties map for the construction of a <abbr>CRS</abbr> or operation of the given name.
     */
    private Map<String,?> properties(final Object name) {
        properties.put(IdentifiedObject.NAME_KEY, name);
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Creates a properties map for the construction of geodetic objects with only a name.
     */
    private static Map<String,?> singleton(final Object name) {
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Creates a coordinate system axis of the given name.
     */
    private static CoordinateSystemAxis axis(final CSFactory csFactory, final String name,
            final String abbreviation, final AxisDirection direction) throws FactoryException
    {
        return csFactory.createCoordinateSystemAxis(singleton(name), abbreviation, direction, Units.UNITY);
    }

    /**
     * Returns a default axis abbreviation for the given dimension.
     */
    private static String abbreviation(final int dimension) {
        final var b = new StringBuilder(4).append('x').append(dimension);
        for (int i = b.length(); --i >= 1;) {
            b.setCharAt(i, Characters.toSubScript(b.charAt(i)));
        }
        return b.toString();
    }

    /**
     * Creates the coordinate system for a derived or engineering <abbr>CRS</abbr> of the grid.
     *
     * @param  dimension  number of dimensions of the coordinate system to create.
     * @param  types      the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @param  offset     offset to apply on dimension index when creating a default axis name.
     * @return coordinate system for the grid extent (never {@code null}).
     * @throws FactoryException if an error occurred during the use of {@link CSFactory}.
     */
    private CoordinateSystem createGridCS(final int dimension, final DimensionNameType[] types, final int offset)
            throws FactoryException
    {
        /*
         * Build the coordinate system assuming a null (identity) "grid to CRS" matrix
         * because we are building the CS for the grid, not for the transformed extent.
         */
        final CoordinateSystem cs = createCS(dimension, null, types, offset, getCSFactory(), locale);
        if (cs == null) {
            // Should never happen.
            throw new InvalidGeodeticParameterException();
        }
        return cs;
    }

    /**
     * Creates the coordinate system for the derived or engineering <abbr>CRS</abbr> of a grid.
     * If the {@code gridToCRS} matrix is non-null, then the returned <abbr>CRS</abbr> is for
     * the result of transforming an extent by that transform (it may change the axis order).
     *
     * @param  dimension  number of dimensions of the coordinate system to create.
     * @param  gridToCRS  matrix of the transform used for converting grid cell indices to envelope coordinates.
     *                    It does not matter whether it maps pixel center or corner (translations are ignored).
     *                    A {@code null} value means to handle as an identity transform.
     * @param  types      the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @param  offset     offset to apply on dimension index when creating a default axis name.
     * @param  locale     locale to use for axis names, or {@code null} for default.
     * @return coordinate system for the grid extent, or {@code null} if it cannot be inferred.
     * @throws FactoryException if an error occurred during the use of {@link CSFactory}.
     */
    private static CoordinateSystem createCS(final int dimension, final Matrix gridToCRS,
                                             final DimensionNameType[] types, final int offset,
                                             final CSFactory csFactory, final Locale locale)
            throws FactoryException
    {
        final int numTypes = Math.min(types.length, dimension);
        final var axes = new CoordinateSystemAxis[dimension];
        boolean hasVertical = false;
        boolean hasTime     = false;
        boolean hasOther    = false;
        for (int i=0; i<numTypes; i++) {
            final DimensionNameType type = types[i];
            if (type != null) {
                /*
                 * Try to locate the CRS dimension corresponding to grid dimension j.
                 * We expect a one-to-one matching; if it is not the case, return null.
                 * Current version does not accept scale factors, but we could revisit
                 * in a future version if there is a need for it.
                 */
                int target = i;
                double scale = 0;
                if (gridToCRS != null) {
                    target = -1;
                    for (int j=0; j<dimension; j++) {
                        final double m = gridToCRS.getElement(j, i);
                        if (m != 0) {
                            if (target >= 0 || axes[j] != null || Math.abs(m) != 1) {
                                return null;
                            }
                            target = j;
                            scale  = m;
                        }
                    }
                    if (target < 0) {
                        return null;
                    }
                }
                /*
                 * This hard-coded set of axis directions is the converse of
                 * GridExtent.AXIS_DIRECTIONS map.
                 */
                String abbreviation;
                AxisDirection direction;
                if (type == DimensionNameType.COLUMN || type == DimensionNameType.SAMPLE) {
                    abbreviation = "x"; direction = AxisDirection.COLUMN_POSITIVE;
                } else if (type == DimensionNameType.ROW || type == DimensionNameType.LINE) {
                    abbreviation = "y"; direction = AxisDirection.ROW_POSITIVE;
                } else if (type == DimensionNameType.VERTICAL) {
                    abbreviation = "z"; direction = AxisDirection.UP; hasVertical = true;
                } else if (type == DimensionNameType.TIME) {
                    abbreviation = "t"; direction = AxisDirection.FUTURE; hasTime = true;
                } else {
                    abbreviation = abbreviation(target);
                    direction = AxisDirection.UNSPECIFIED;
                    hasOther = true;
                }
                /*
                 * Verify that no other axis has the same direction and abbreviation. If duplicated
                 * values are found, keep only the first occurrence in grid axis order (may not be
                 * the CRS axis order).
                 */
                for (int k = dimension; --k >= 0;) {
                    final CoordinateSystemAxis previous = axes[k];
                    if (previous != null) {
                        if (direction.equals(AxisDirections.absolute(previous.getDirection()))) {
                            direction = AxisDirection.UNSPECIFIED;
                            hasOther = true;
                        }
                        if (abbreviation.equals(previous.getAbbreviation())) {
                            abbreviation = abbreviation(target);
                        }
                    }
                }
                if (scale < 0) {
                    direction = AxisDirections.opposite(direction);
                }
                final String name = Types.toString(Types.getCodeTitle(type), locale);
                axes[target] = axis(csFactory, name, abbreviation, direction);
            }
        }
        /*
         * Search for axes that have not been created in above loop.
         * It happens when some axes have no associated `DimensionNameType` code.
         */
        for (int j=0; j<dimension; j++) {
            if (axes[j] == null) {
                final int i = offset + j;
                final String name = Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Dimension_1, i);
                final String abbreviation = abbreviation(i);
                axes[j] = axis(csFactory, name, abbreviation, AxisDirection.UNSPECIFIED);
            }
        }
        /*
         * Create a coordinate system of affine type if all axes seem spatial.
         * If no specialized type seems to fit, use an unspecified ("abstract")
         * coordinate system type in last resort.
         */
        final Map<String,?> properties = singleton(CS_NAME);
        switch (dimension) {
            case 1:  {
                final CoordinateSystemAxis axis = axes[0];
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
                /*
                 * A null `gridToCRS` means that we are creating a CS for the grid, which is assumed a
                 * Cartesian space. A non-null value means that we are creating a CRS for a transformed
                 * envelope, in which case the CS type is not really known.
                 */
                if (hasVertical | hasTime | hasOther) break;
                return (gridToCRS == null)
                        ? csFactory.createCartesianCS(properties, axes[0], axes[1])
                        : csFactory.createAffineCS   (properties, axes[0], axes[1]);
            }
            case 3: {
                if (hasVertical | hasTime | hasOther) break;
                return (gridToCRS == null)
                        ? csFactory.createCartesianCS(properties, axes[0], axes[1], axes[2])
                        : csFactory.createAffineCS   (properties, axes[0], axes[1], axes[2]);
            }
        }
        return new AbstractCS(properties, axes);
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
     * @param  gridToCRS  matrix of the transform used for converting grid cell indices to envelope coordinates.
     *                    It does not matter whether it maps pixel center or corner (translations are ignored).
     * @param  types      the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @return <abbr>CRS</abbr> for the grid, or empty if it cannot be built.
     * @throws FactoryException if an error occurred during the use of a referencing factory.
     *
     * @see GridExtent#toEnvelope(MathTransform)
     * @see GridExtent#typeFromAxes(CoordinateReferenceSystem, int)
     */
    static Optional<EngineeringCRS> forExtentAlone(final Matrix gridToCRS, final DimensionNameType[] types)
            throws FactoryException
    {
        final GeodeticObjectFactory factory = GeodeticObjectFactory.provider();
        final CoordinateSystem cs = createCS(gridToCRS.getNumRow() - 1, gridToCRS, types, 0, factory, null);
        if (cs == null) {
            return Optional.empty();
        }
        return Optional.of(factory.createEngineeringCRS(singleton(cs.getName()), CommonCRS.Engineering.GRID.datum(), cs));
    }
}
