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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
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
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Builder for coordinate reference system which is derived from the coverage CRS by the inverse
 * of the "grid to CRS" transform. Those CRS describe coordinates associated to the grid extent.
 * This class provides two factory methods:
 *
 * <ul>
 *   <li>{@link #forCoverage(String, GridGeometry)}</li>
 *   <li>{@link #forExtentAlone(Matrix, DimensionNameType[])}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class GridExtentCRS {
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
     * Description of "CRS to grid indices" operation method.
     */
    private static final OperationMethod METHOD;
    static {
        final ParameterBuilder b = new ParameterBuilder().setRequired(true);
        final ParameterDescriptor<?>   name   = b.addName(NAME_PARAM).create(String.class, null);
        final ParameterDescriptor<?>   anchor = b.addName(ANCHOR_PARAM).create(PixelInCell.class, PixelInCell.CELL_CENTER);
        final ParameterDescriptorGroup params = b.addName("CRS to grid indices").createGroup(name, anchor);
        METHOD = new DefaultOperationMethod(properties(params.getName()), params);
    }

    /**
     * Scope of usage of the CRS.
     * This is a localized text saying "Conversions from coverage CRS to grid cell indices."
     */
    private static final InternationalString SCOPE = Resources.formatInternational(Resources.Keys.CrsToGridConversion);

    /**
     * Name of the coordinate systems created by this class.
     */
    private static final NamedIdentifier CS_NAME = new NamedIdentifier(
            Citations.SIS, Vocabulary.formatInternational(Vocabulary.Keys.GridExtent));

    /**
     * Do not allow instantiation of this class.
     */
    private GridExtentCRS() {
    }

    /**
     * Creates a derived CRS for the grid extent of a grid coverage.
     *
     * <h4>Limitation</h4>
     * If the CRS is compound, then this method takes only the first single CRS element.
     * This is a restriction imposed by {@link DerivedCRS} API.
     * As a result, the returned CRS may cover only the 2 or 3 first grid dimensions.
     *
     * @param  name    name of the CRS to create.
     * @param  gg      grid geometry of the coverage.
     * @param  anchor  the cell part to map (center or corner).
     * @param  locale  locale to use for axis names, or {@code null} for default.
     * @return a derived CRS for coordinates (cell indices) associated to the grid extent.
     * @throws FactoryException if an error occurred during the use of {@link CSFactory} or {@link CRSFactory}.
     */
    static DerivedCRS forCoverage(final String name, final GridGeometry gg, final PixelInCell anchor, final Locale locale)
            throws FactoryException, NoninvertibleTransformException
    {
        /*
         * Get the first `SingleCRS` instance (see "limitations" in method javadoc).
         */
        CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem();
        boolean reduce = false;
        while (!(crs instanceof SingleCRS)) {
            if (!(crs instanceof CompoundCRS)) {
                throw unsupported(locale, crs);
            }
            crs = ((CompoundCRS) crs).getComponents().get(0);
            reduce = true;
        }
        /*
         * If we took only a subset of CRS dimensions, take the same subset
         * of "grid to CRS" dimensions and list of grid axes.
         */
        MathTransform gridToCRS = gg.getGridToCRS(anchor);
        DimensionNameType[] types = gg.getExtent().getAxisTypes();
        if (reduce) {
            final TransformSeparator s = new TransformSeparator(gridToCRS);
            s.addTargetDimensionRange(0, crs.getCoordinateSystem().getDimension());
            gridToCRS = s.separate();
            final int[] src = s.getSourceDimensions();
            final DimensionNameType[] allTypes = types;
            types = new DimensionNameType[src.length];
            for (int i=0; i<src.length; i++) {
                final int j = src[i];
                if (j < allTypes.length) {
                    types[i] = allTypes[j];
                }
            }
        }
        /*
         * Build the coordinate system assuming a null (identity) "grid to CRS" matrix
         * because we are building the CS for the grid, not for the transformed envelope.
         */
        final CoordinateSystem cs = createCS(gridToCRS.getSourceDimensions(), null, types, locale);
        if (cs == null) {
            throw unsupported(locale, crs);
        }
        /*
         * Put everything together: parameters, conversion and finally the derived CRS.
         */
        final var properties = new HashMap<String,Object>(8);
        properties.put(IdentifiedObject.NAME_KEY, METHOD.getName());
        properties.put(DefaultConversion.LOCALE_KEY, locale);
        properties.put(ObjectDomain.SCOPE_KEY, SCOPE);
        gg.getGeographicExtent().ifPresent((domain) -> {
            properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY,
                    new DefaultExtent(null, domain, null, null));
        });
        final ParameterValueGroup params = METHOD.getParameters().createValue();
        params.parameter(NAME_PARAM).setValue(name);
        params.parameter(ANCHOR_PARAM).setValue(anchor);
        final Conversion conversion = new DefaultConversion(properties, METHOD, gridToCRS.inverse(), params);
        properties.put(IdentifiedObject.NAME_KEY, name);
        return DefaultDerivedCRS.create(properties, (SingleCRS) crs, conversion, cs);
    }

    /**
     * Returns the exception to throw for an unsupported CRS.
     */
    private static FactoryException unsupported(final Locale locale, final CoordinateReferenceSystem crs) {
        return new FactoryException(Errors.forLocale(locale)
                .getString(Errors.Keys.UnsupportedType_1, Classes.getShortClassName(crs)));
    }

    /**
     * Creates a properties map to give to CS, CRS or datum constructors.
     */
    private static Map<String,?> properties(final Object name) {
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Creates a coordinate system axis of the given name.
     */
    private static CoordinateSystemAxis axis(final CSFactory csFactory, final String name,
            final String abbreviation, final AxisDirection direction) throws FactoryException
    {
        return csFactory.createCoordinateSystemAxis(properties(name), abbreviation, direction, Units.UNITY);
    }

    /**
     * Returns a default axis abbreviation for the given dimension.
     */
    private static String abbreviation(final int dimension) {
        final StringBuilder b = new StringBuilder(4).append('x').append(dimension);
        for (int i=b.length(); --i >= 1;) {
            b.setCharAt(i, Characters.toSuperScript(b.charAt(i)));
        }
        return b.toString();
    }

    /**
     * Creates the coordinate system for engineering CRS.
     *
     * @param  tgtDim     number of dimensions of the coordinate system to create.
     * @param  gridToCRS  matrix of the transform used for converting grid cell indices to envelope coordinates.
     *         It does not matter whether it maps pixel center or corner (translation coefficients are ignored).
     *         A {@code null} means to handle as an identity transform.
     * @param  types   the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @param  locale  locale to use for axis names, or {@code null} for default.
     * @return coordinate system for the grid extent, or {@code null} if it cannot be inferred.
     * @throws FactoryException if an error occurred during the use of {@link CSFactory}.
     */
    private static CoordinateSystem createCS(final int tgtDim, final Matrix gridToCRS,
            final DimensionNameType[] types, final Locale locale) throws FactoryException
    {
        int srcDim = types.length;      // Used only for inspecting names. No need to be accurate.
        if (gridToCRS != null) {
            srcDim = Math.min(gridToCRS.getNumCol() - 1, srcDim);
        }
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[tgtDim];
        final CSFactory csFactory = GeodeticObjectFactory.provider();
        boolean hasVertical = false;
        boolean hasTime     = false;
        boolean hasOther    = false;
        for (int i=0; i<srcDim; i++) {
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
                    for (int j=0; j<tgtDim; j++) {
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
                for (int k = tgtDim; --k >= 0;) {
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
        for (int j=0; j<tgtDim; j++) {
            if (axes[j] == null) {
                final String name = Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Dimension_1, j);
                final String abbreviation = abbreviation(j);
                axes[j] = axis(csFactory, name, abbreviation, AxisDirection.UNSPECIFIED);
            }
        }
        /*
         * Create a coordinate system of affine type if all axes seem spatial.
         * If no specialized type seems to fit, use an unspecified ("abstract")
         * coordinate system type in last resort.
         */
        final Map<String,?> properties = properties(CS_NAME);
        final CoordinateSystem cs;
        if (hasOther || (tgtDim > (hasTime ? 1 : 3))) {
            cs = new AbstractCS(properties, axes);
        } else switch (tgtDim) {
            case 1:  {
                final CoordinateSystemAxis axis = axes[0];
                if (hasVertical) {
                    cs = csFactory.createVerticalCS(properties, axis);
                } else if (hasTime) {
                    cs = csFactory.createTimeCS(properties, axis);
                } else {
                    cs = csFactory.createLinearCS(properties, axis);
                }
                break;
            }
            case 2: {
                /*
                 * A null `gridToCRS` means that we are creating a CS for the grid, which is assumed a
                 * Cartesian space. A non-null value means that we are creating a CRS for a transformed
                 * envelope, in which case the CS type is not really known.
                 */
                cs = (gridToCRS == null)
                        ? csFactory.createCartesianCS(properties, axes[0], axes[1])
                        : csFactory.createAffineCS   (properties, axes[0], axes[1]);
                break;
            }
            case 3: {
                cs = (gridToCRS == null)
                        ? csFactory.createCartesianCS(properties, axes[0], axes[1], axes[2])
                        : csFactory.createAffineCS   (properties, axes[0], axes[1], axes[2]);
                break;
            }
            default: {
                cs = null;
                break;
            }
        }
        return cs;
    }

    /**
     * Builds the engineering coordinate reference system of a {@link GridExtent}.
     * This is used only in the rare cases where we need to represent an extent as an envelope.
     * This class converts {@link DimensionNameType} codes into axis names, abbreviations and directions.
     * It is the converse of {@link GridExtent#typeFromAxes(CoordinateReferenceSystem, int)}.
     *
     * <p>The CRS type is always engineering.
     * We cannot create temporal CRS because we do not know the temporal datum origin.</p>
     *
     * @param  gridToCRS  matrix of the transform used for converting grid cell indices to envelope coordinates.
     *         It does not matter whether it maps pixel center or corner (translation coefficients are ignored).
     * @param  types   the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @param  locale  locale to use for axis names, or {@code null} for default.
     * @return CRS for the grid, or {@code null}.
     * @throws FactoryException if an error occurred during the use of {@link CSFactory} or {@link CRSFactory}.
     *
     * @see GridExtent#typeFromAxes(CoordinateReferenceSystem, int)
     */
    static EngineeringCRS forExtentAlone(final Matrix gridToCRS, final DimensionNameType[] types)
            throws FactoryException
    {
        final CoordinateSystem cs = createCS(gridToCRS.getNumRow() - 1, gridToCRS, types, null);
        if (cs == null) {
            return null;
        }
        return GeodeticObjectFactory.provider().createEngineeringCRS(
                properties(cs.getName()), CommonCRS.Engineering.GRID.datum(), cs);
    }
}
