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
package org.apache.sis.referencing.operation;

import java.util.List;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Specific to the geoapi-4.0 branch:
import org.apache.sis.referencing.legacy.DefaultImageCRS;


/**
 * Information about the operation from a source component to a target component in {@code CompoundCRS} instances.
 * An instance of {@code SubOperationInfo} is created for each target CRS component. This class allows to collect
 * information about all operation steps before to start the creation of pass-through operations. This separation
 * is useful for applying reordering.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see CoordinateOperationFinder#createOperationStep(CoordinateReferenceSystem, List, CoordinateReferenceSystem, List)
 */
final class SubOperationInfo {
    /**
     * Types of target CRS, together with the type of CRS that may be used as the source for that target.
     * For each array {@code COMPATIBLE_TYPES[i]}, the first element (i.e. {@code COMPATIBLE_TYPES[i][0]})
     * is the target CRS and the whole array (including the first element) gives the valid source CRS type,
     * in preference order.
     *
     * <h4>Example</h4>
     * If a target CRS is of type {@link VerticalCRS}, then the source CRS may be another {@code VerticalCRS}
     * or a {@link GeodeticCRS}. The geodetic CRS is possible because it may be three-dimensional.
     *
     * <h4>Exclusions</h4>
     * {@link ProjectedCRS} and {@link DerivedCRS} are not in this list because we rather use their base CRS
     * as the criterion for determining their type.
     */
    private static final Class<?>[][] COMPATIBLE_TYPES = {
        {GeodeticCRS.class},
        {VerticalCRS.class, GeodeticCRS.class},
        {TemporalCRS.class},
        {ParametricCRS.class},
        {EngineeringCRS.class},
        {DefaultImageCRS.class}
    };

    /**
     * Returns the class of the given CRS after unwrapping derived and projected CRS.
     * The returned type is for use with {@link #COMPATIBLE_TYPES}.
     */
    private static Class<?> type(SingleCRS crs) {
        while (crs instanceof DerivedCRS) {
            crs = ((DerivedCRS) crs).getBaseCRS();
        }
        return crs.getClass();
    }

    /**
     * The coordinate operation between a component of the source CRS and a component of the target CRS.
     * If {@code null}, then {@link CoordinateOperationContext#getConstantCoordinates()} shall return a
     * non-null value, otherwise a {@link MissingSourceDimensionsException} will be thrown.
     */
    final CoordinateOperation operation;

    /**
     * The constant values to store in target coordinates, or {@code null} if none.
     * This property is usually null, but may be non-null if no component of the source <abbr>CRS</abbr>
     * has been found for a component of the target <abbr>CRS</abbr>, and fallbacks were specified with
     * {@link CoordinateOperationContext#getConstantCoordinates()}.
     *
     * <p>The array length is the number of dimensions in the <em>component</em> of the target <abbr>CRS</abbr>
     * for which this {@code SubOperationInfo} is constructed. These coordinates cannot be <abbr>NaN</abbr>.</p>
     *
     * @see CoordinateOperationContext#getConstantCoordinates()
     */
    private double[] constantCoordinates;

    /**
     * The first dimension (inclusive) and the last dimension (exclusive) where the
     * {@link SingleCRS} starts/ends in the full (usually compound) <abbr>CRS</abbr>.
     *
     * @see #sourceToSelected(int, SubOperationInfo[])
     */
    private final int sourceLowerDimension, sourceUpperDimension,
                      targetLowerDimension, targetUpperDimension;

    /**
     * Index of this instance in the array of {@code SubOperationInfo} instances,
     * before the reordering applied by {@link #getSourceCRS(SubOperationInfo[])}.
     */
    final int targetComponentIndex;

    /**
     * The component of the target <abbr>CRS</abbr> which is managed by this {@code SubOperationInfo}.
     * This is the component at the index specified by {@link #targetComponentIndex}.
     */
    final SingleCRS targetComponent;

    /**
     * Creates a new instance wrapping the given coordinate operation or constant coordinates.
     * The constant coordinates are fetched after construction if they appear needed.
     *
     * @see #fetchConstantsForMissingSourceDimensions(CoordinateOperationContext)
     */
    private SubOperationInfo(final CoordinateOperation operation,
                             final int sourceLowerDimension, final int sourceUpperDimension,
                             final int targetLowerDimension, final int targetUpperDimension,
                             final int targetComponentIndex, final SingleCRS targetComponent)
    {
        this.operation            = operation;
        this.sourceLowerDimension = sourceLowerDimension;
        this.sourceUpperDimension = sourceUpperDimension;
        this.targetLowerDimension = targetLowerDimension;
        this.targetUpperDimension = targetUpperDimension;
        this.targetComponentIndex = targetComponentIndex;
        this.targetComponent      = targetComponent;
    }

    /**
     * Searches in given list of source components for operations capable to transform coordinates to each target CRS.
     * There is one {@code SubOperationInfo} instance per target <abbr>CRS</abbr> because we need to satisfy all target
     * dimensions. However, it is okay to ignore some components or dimensions of the source <abbr>CRS</abbr>.
     *
     * @param  caller     the object which is inferring a coordinate operation.
     * @param  sourceCRS  the source source <abbr>CRS</abbr>, used for error message.
     * @param  sources    all components of the source <abbr>CRS</abbr>.
     * @param  targets    all components of the target <abbr>CRS</abbr>.
     * @return information about each coordinate operation from a source <abbr>CRS</abbr> to a target <abbr>CRS</abbr>.
     * @throws FactoryException if an error occurred while grabbing a coordinate operation.
     * @throws MissingSourceDimensionsException if a target axis cannot be mapped to a source axis or a constant.
     * @throws TransformException if an error occurred while computing a coordinate value for unmatched dimension.
     * @throws NoninvertibleTransformException if the default coordinate value for a unmatched dimension is NaN.
     */
    static SubOperationInfo[] createSteps(final CoordinateOperationFinder caller,
                                          final CoordinateReferenceSystem sourceCRS,
                                          final List<? extends SingleCRS> sources,
                                          final List<? extends SingleCRS> targets)
            throws FactoryException, TransformException
    {
        final var infos = new SubOperationInfo[targets.size()];
        final var sourceComponentIsUsed = new boolean[sources.size()];
        /*
         * Each target CRS must be associated to exactly one source CRS
         * (or to zero source CRS if the missing source CRS can be replaced by constant coordinate values).
         * Each source CRS must be associated to zero or one target CRS, not necessarily in the same order.
         * Therefore, the iteration over source components will be repeated for each target CRS, combined
         * with `sourceComponentIsUsed` flags for using each source CRS at most once.
         */
        int targetLowerDimension;
        int targetUpperDimension = 0;
        int targetComponentIndex = 0;
        while (targetComponentIndex < infos.length) {
            final SingleCRS targetComponent = targets.get(targetComponentIndex);
            targetLowerDimension  = targetUpperDimension;
            targetUpperDimension += targetComponent.getCoordinateSystem().getDimension();

            final Class<?> targetType = type(targetComponent);
            OperationNotFoundException failure = null;
            CoordinateOperation operation = null;
            /*
             * For each target CRS, search for a source CRS which has not yet been used.
             * Some sources may be left unused after this method completion. Check only
             * sources that may be compatible according the `COMPATIBLE_TYPES` array.
             */
            int sourceLowerDimension = 0;
            int sourceUpperDimension = 0;
            int sourceComponentIndex = 0;
searchSrc:  while (sourceComponentIndex < sourceComponentIsUsed.length) {
                final SingleCRS sourceComponent = sources.get(sourceComponentIndex);
                sourceUpperDimension += sourceComponent.getCoordinateSystem().getDimension();
                if (!sourceComponentIsUsed[sourceComponentIndex]) {
                    final Class<?> sourceType = type(sourceComponent);
                    for (final Class<?>[] compatibleTypes : COMPATIBLE_TYPES) {
                        if (compatibleTypes[0].isAssignableFrom(targetType)) {
                            for (final Class<?> compatibleType : compatibleTypes) {
                                if (compatibleType.isAssignableFrom(sourceType)) try {
                                    operation = caller.createOperation(sourceComponent, targetComponent);
                                    sourceComponentIsUsed[sourceComponentIndex] = true;
                                    break searchSrc;
                                } catch (OperationNotFoundException exception) {
                                    if (failure == null) {
                                        failure = exception;
                                    } else {
                                        failure.addSuppressed(exception);
                                    }
                                }
                            }
                        }
                    }
                }
                // Following variable must be updated before to stop the loop. Therefore, it cannot be at loop beginning.
                sourceLowerDimension = sourceUpperDimension;
                sourceComponentIndex++;
            }
            if (failure != null) {
                if (operation == null) {
                    throw failure;
                }
                caller.recoverableException("createOperationStep", failure);
            }
            final var info = new SubOperationInfo(operation,
                    sourceLowerDimension, sourceUpperDimension,
                    targetLowerDimension, targetUpperDimension,
                    targetComponentIndex, targetComponent);
            /*
             * If we enter in the following block, we have not been able to find a source for the target component.
             * Usually, we don't know how to get coordinate values, so `MissingSourceDimensionsException` is thrown.
             * However, in some contexts (e.g. when searching for an operation between two `GridGeometry` instances)
             * it is possible to assign a constant value to the target coordinates. Those values cannot be guessed
             * by `org.apache.sis.referencing`, they must be provided by the user.
             */
            if (operation == null) {
                final CoordinateSystemAxis missing = info.fetchConstantsForMissingSourceDimensions(caller.context);
                if (info.constantCoordinates == null) {
                    final MissingSourceDimensionsException e;
                    if (missing != null) {
                        e = new MissingSourceDimensionsException(caller.resources().getString(
                                Resources.Keys.ConstantCoordinateValueRequired_1, caller.label(missing)));
                        e.addMissing(missing.getDirection());
                    } else {
                        e = new MissingSourceDimensionsException(caller.notFoundMessage(sourceCRS, targetComponent));
                        e.addMissing(sourceCRS.getCoordinateSystem(), targetComponent.getCoordinateSystem());
                    }
                    throw e;
                }
            }
            infos[targetComponentIndex++] = info;
        }
        return infos;
    }

    /**
     * Initializes the {@link #constantCoordinates} field to the constant coordinate values for the operation step
     * managed by this {@code SubOperationInfo}. If no constants have been specified, {@link #constantCoordinates}
     * field stay null and the reason for the failure is returned. That reason is an axis missing in the source
     * <abbr>CRS</abbr>, or {@code null} if that axis is unknown.
     *
     * @param  context  options supplied by the user, or {@code null}.
     * @return the coordinate axis that could not be resolved, or {@code null} if none or unknown.
     */
    private CoordinateSystemAxis fetchConstantsForMissingSourceDimensions(final CoordinateOperationContext context) {
        if (context != null) {
            final DirectPosition coordinates = context.getConstantCoordinates();
            if (coordinates != null) {
                /*
                 * Finds the index of the first coordinate to use among the constant coordinates.
                 * The default CRS of `coordinates` is the full target CRS (with all dimensions).
                 * If a different CRS is specified, search the index of this target CRS component.
                 * No coordinate transformation is perfomed in this method, only selection.
                 */
                int indexOfConstant = targetLowerDimension;     // Value for the default CRS.
                final CoordinateReferenceSystem crs = coordinates.getCoordinateReferenceSystem();
locate:         if (crs != null) {
                    indexOfConstant = 0;
                    for (SingleCRS component : CRS.getSingleComponents(crs)) {
                        if (CRS.equivalent(targetComponent, component)) break locate;
                        indexOfConstant += component.getCoordinateSystem().getDimension();
                    }
                    return null;
                }
                final int d = coordinates.getDimension();
                final var c = new double[targetUpperDimension - targetLowerDimension];
                for (int i=0; i<c.length; i++) {
                    if (indexOfConstant >= d || Double.isNaN(c[i] = coordinates.getCoordinate(indexOfConstant++))) {
                        return targetComponent.getCoordinateSystem().getAxis(i);
                    }
                }
                constantCoordinates = c;
            }
        }
        return null;
    }

    /**
     * Returns the source CRS of given operations. This method modifies the given array in-place by moving all
     * sourceless operations last. Then an array is returned with the source CRS of only ordinary operations.
     * Each CRS at index <var>i</var> in the returned array is the component from {@link #sourceLowerDimension}
     * inclusive to {@link #sourceUpperDimension} exclusive in the complete (usually compound) source CRS analyzed
     * by {@link CoordinateOperationFinder}.
     *
     * @param  selected  all operations from source to target {@link CompoundCRS}.
     * @return source CRS of all ordinary operations (excluding operations producing constant values).
     */
    static CoordinateReferenceSystem[] getSourceCRS(final SubOperationInfo[] selected) {
        int n = selected.length;
        final int last = n - 1;
        for (int i=0; i<n; i++) {
            final SubOperationInfo component = selected[i];
            if (component.operation == null) {
                System.arraycopy(selected, i+1, selected, i, last - i);
                selected[last] = component;
                n--;
                i--;
            }
        }
        final var stepComponents = new CoordinateReferenceSystem[n];
        for (int i=0; i<n; i++) {
            stepComponents[i] = selected[i].operation.getSourceCRS();
        }
        return stepComponents;
    }

    /**
     * Returns the index of the last non-identity operation. This is used as a slight optimization for deciding when
     * {@link CoordinateOperationFinder} can stop to create intermediate target {@link CompoundCRS} instances because
     * all remaining operations leave target coordinates unchanged. It may help to skip a few operations for example
     * when converting (<var>x</var>, <var>y</var>, <var>t</var>) coordinates where <var>t</var> value is unchanged.
     *
     * @param  selected  all operations from source to target {@link CompoundCRS}.
     * @return index of the last non-identity operation, inclusive.
     */
    static int indexOfFinal(final SubOperationInfo[] selected) {
        int n = selected.length;
        while (n != 0) {
            if (!selected[--n].isIdentity()) {
                break;
            }
        }
        return n;
    }

    /**
     * Returns a matrix for an affine transform moving coordinate values from their position in the source CRS to a
     * position in the order {@link #operation}s are applied. This matrix is needed because {@link #operation} may
     * select any source CRS in the list of {@link SingleCRS} given to the {@link #createSteps createSteps(…)} method;
     * the source CRS are not necessarily picked in the same order as they appear in the list.
     *
     * <h4>Example</h4>
     * If the source CRS has (<var>x</var>, <var>y</var>, <var>t</var>) coordinates and the target CRS has
     * (<var>t</var>, <var>x</var>, <var>y</var>) coordinates with some operation applied on <var>x</var>
     * and <var>y</var>, then the operations will be applied in that order:
     *
     * <ol>
     *   <li>An operation for <var>t</var>, because it is the first coordinate to appear in target CRS.</li>
     *   <li>An operation for (<var>x</var>, <var>y</var>), because those coordinates are next in target CRS.</li>
     * </ol>
     *
     * Since {@link DefaultPassThroughOperation} cannot take coordinates before the "first affected coordinate"
     * dimension and move them into the "trailing coordinates" dimension, we have to reorder coordinates before
     * to create the pass-through operations. This is done by the following matrix:
     *
     * <pre class="math">
     *   ┌   ┐   ┌         ┐┌   ┐
     *   │ t │   │ 0 0 1 0 ││ x │
     *   │ x │ = │ 1 0 0 0 ││ y │
     *   │ y │   │ 0 1 0 0 ││ t │
     *   │ 1 │   │ 0 0 0 1 ││ 1 │
     *   └   ┘   └         ┘└   ┘</pre>
     *
     * Furthermore, some dimensions may be dropped,
     * e.g. from (<var>x</var>, <var>y</var>, <var>t</var>) to (<var>x</var>, <var>y</var>).
     *
     * @param  sourceDimensions  number of dimensions in the source {@link CompoundCRS}.
     * @param  selected          all operations from source to target {@link CompoundCRS}.
     * @return mapping from source {@link CompoundCRS} to each {@link CoordinateOperation#getSourceCRS()}.
     */
    static MatrixSIS sourceToSelected(final int sourceDimensions, final SubOperationInfo[] selected) {
        int selectedDimensions = 0;
        for (final SubOperationInfo component : selected) {
            if (component.operation == null) break;
            selectedDimensions += component.sourceUpperDimension - component.sourceLowerDimension;
        }
        final MatrixSIS select = Matrices.createZero(selectedDimensions + 1, sourceDimensions + 1);
        select.setElement(selectedDimensions, sourceDimensions, 1);
        int j = 0;
        for (final SubOperationInfo component : selected) {
            if (component.operation == null) break;
            for (int i = component.sourceLowerDimension; i < component.sourceUpperDimension; i++) {
                select.setElement(j++, i, 1);
            }
        }
        return select;
    }

    /**
     * Returns {@code true} if the coordinate operation wrapped by this object is an identity transform.
     */
    final boolean isIdentity() {
        return (operation != null) && operation.getMathTransform().isIdentity();
    }

    /**
     * Returns the matrix of an operation setting some coordinates to constant values.
     * This is possible only if the user as provided a non-null value for
     * {@link CoordinateOperationContext#getConstantCoordinates()}.
     *
     * @param  context   options supplied by the user, or {@code null}.
     * @param  selected  all operations from source to target {@link CompoundCRS}.
     * @param  srcDim    number of dimensions in the target CRS of previous operation step.
     * @param  tgtDim    number of dimensions in the full (usually compound) target CRS.
     */
    static MatrixSIS createConstantOperation(final CoordinateOperationContext context,
            final SubOperationInfo[] selected, final int srcDim, final int tgtDim)
            throws MissingSourceDimensionsException
    {
        final MatrixSIS matrix = Matrices.createZero(tgtDim + 1, srcDim + 1);
        matrix.setElement(tgtDim, srcDim, 1);
        for (final SubOperationInfo component : selected) {
            component.setConstantTerms(context, matrix, srcDim);
        }
        return matrix;
    }

    /**
     * Sets the coefficients of the given matrix to the constant coordinate values
     * in the dimensions managed by this {@code SubOperationInfo}.
     *
     * @param  context  options supplied by the user, or {@code null}.
     * @param  matrix   the matrix for which to set the coefficients.
     * @param  translationColumn  index of the last column of the given matrix.
     */
    private void setConstantTerms(final CoordinateOperationContext context, final MatrixSIS matrix, final int translationColumn) {
        if (constantCoordinates != null) {
            /*
             * Component for which no operation has been found.
             * Set all coordinate values of that component to the specified constant values.
             */
            for (int j = targetUpperDimension - targetLowerDimension; --j >= 0;) {
                matrix.setElement(targetLowerDimension + j, translationColumn, constantCoordinates[j]);
            }
        } else {
            /*
             * Component for which an operation has been found, but maybe this is the inverse of the
             * "Geographic 3D to 2D conversion" (EPSG:9659) which sets the height to `DEFAULT_HEIGHT`.
             * If all values in the row are zero, then the coordinate value is unconditionally zero.
             * Replace that value (usually the default ellipsoidal height) by the specified constant.
             */
            final var targetDimensionIsUsed = new boolean[targetUpperDimension - targetLowerDimension];
            final Matrix last = MathTransforms.getMatrix(MathTransforms.getLastStep(operation.getMathTransform()));
            if (last != null) {
                fetchConstantsForMissingSourceDimensions(context);
                if (constantCoordinates != null) {
otherRow:           for (int j = last.getNumRow() - 1; --j >= 0;) {     // Ignore the last row.
                        for (int i = last.getNumCol(); --i >= 0;) {
                            if (last.getElement(j, i) != 0) {
                                continue otherRow;
                            }
                        }
                        matrix.setElement(targetLowerDimension + j, translationColumn, constantCoordinates[j]);
                        targetDimensionIsUsed[j] = true;
                    }
                }
            }
            /*
             * All coordinates that have not been set to a constant
             * shall be propagated unchanged (scale factor of 1).
             */
            int i = sourceLowerDimension;
            for (int j = 0; j < targetDimensionIsUsed.length; j++) {
                if (!targetDimensionIsUsed[j]) {
                    matrix.setElement(targetLowerDimension + j, i++, 1);
                }
            }
        }
    }

    /**
     * Whether this operation can be cached. This flag shall be {@code false} if
     * the operation depends on parameters that may vary between two executions.
     */
    final boolean canStoreInCache() {
        return constantCoordinates == null;
    }
}
