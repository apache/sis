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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
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
     * The coordinate operation between a source CRS component and a target CRS component.
     * Exactly one of {@link #operation} or {@link #constants} shall be non-null.
     */
    final CoordinateOperation operation;

    /**
     * The constant values to store in target coordinates, or {@code null} if none. This array is usually null.
     * It may be non-null if no source CRS component has been found for a target CRS component.
     * Exactly one of {@link #operation} or {@link #constants} shall be non-null.
     *
     * <p>The array length is the number of dimensions in the full (usually compound) target CRS, but only the
     * coordinate values for sourceless dimensions are used. Other coordinates are ignored and can be NaN.</p>
     *
     * <h4>Example</h4>
     * If the source <abbr>CRS</abbr> has (<var>x</var>, <var>y</var>) axes and the target <abbr>CRS</abbr>
     * has (<var>x</var>, <var>y</var>, <var>t</var>) axes, then this array may be set to a non-null value
     * for specifying the <var>t</var> value.
     *
     * @see CoordinateOperationContext#getConstantCoordinates()
     */
    private final double[] constants;

    /**
     * The first dimension (inclusive) and the last dimension (exclusive) where the {@link SingleCRS} starts/ends
     * in the full (usually compound) CRS.
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
     * Whether this operation can be cached. This flag shall be {@code false} if
     * the operation depends on parameters that may vary between two executions.
     */
    final boolean canStoreInCache;

    /**
     * Creates a new instance wrapping the given coordinate operation or coordinate constants.
     * Exactly one of {@code operation} or {@code constants} shall be non-null.
     */
    private SubOperationInfo(final CoordinateOperation operation, final double[] constants,
                             final int sourceLowerDimension, final int sourceUpperDimension,
                             final int targetLowerDimension, final int targetUpperDimension,
                             final int targetComponentIndex, final boolean canStoreInCache)
    {
        this.operation            = operation;
        this.constants            = constants;
        this.sourceLowerDimension = sourceLowerDimension;
        this.sourceUpperDimension = sourceUpperDimension;
        this.targetLowerDimension = targetLowerDimension;
        this.targetUpperDimension = targetUpperDimension;
        this.targetComponentIndex = targetComponentIndex;
        this.canStoreInCache      = canStoreInCache;
        assert (operation == null) != (constants == null);
    }

    /**
     * Searches in given list of source components for operations capable to transform coordinates to each target CRS.
     * There is one {@code SubOperationInfo} per target CRS because we need to satisfy all target dimensions, while it
     * is okay to ignore some source dimensions. If an operation cannot be found, then this method returns {@code null}.
     *
     * @param  caller   the object which is inferring a coordinate operation.
     * @param  sources  all components of the source CRS.
     * @param  targets  all components of the target CRS.
     * @return information about each coordinate operation from a source CRS to a target CRS, or {@code null}.
     * @throws FactoryException if an error occurred while grabbing a coordinate operation.
     * @throws TransformException if an error occurred while computing a coordinate value for unmatched dimension.
     * @throws NoninvertibleTransformException if the default coordinate value for a unmatched dimension is NaN.
     */
    static SubOperationInfo[] createSteps(final CoordinateOperationFinder caller,
                                          final List<? extends SingleCRS> sources,
                                          final List<? extends SingleCRS> targets)
            throws FactoryException, TransformException
    {
        final var infos = new SubOperationInfo[targets.size()];
        final var sourceComponentIsUsed = new boolean[sources.size()];
        /*
         * Iterate over target CRS because all of them must have an operation.
         * One the other hand, source CRS can be used zero or one (not two) time.
         */
        int targetLowerDimension;
        int targetUpperDimension = 0;
next:   for (int targetComponentIndex = 0; targetComponentIndex < infos.length; targetComponentIndex++) {
            final SingleCRS target = targets.get(targetComponentIndex);
            targetLowerDimension  = targetUpperDimension;
            targetUpperDimension += target.getCoordinateSystem().getDimension();

            final Class<?> targetType = type(target);
            OperationNotFoundException failure = null;
            /*
             * For each target CRS, search for a source CRS which has not yet been used.
             * Some sources may be left unused after this method completion. Check only
             * sources that may be compatible according the `COMPATIBLE_TYPES` array.
             */
            int sourceLowerDimension;
            int sourceUpperDimension = 0;
            for (int sourceComponentIndex = 0; sourceComponentIndex < sourceComponentIsUsed.length; sourceComponentIndex++) {
                final SingleCRS source = sources.get(sourceComponentIndex);
                sourceLowerDimension  = sourceUpperDimension;
                sourceUpperDimension += source.getCoordinateSystem().getDimension();
                if (!sourceComponentIsUsed[sourceComponentIndex]) {
                    final Class<?> sourceType = type(source);
                    for (final Class<?>[] compatibleTypes : COMPATIBLE_TYPES) {
                        if (compatibleTypes[0].isAssignableFrom(targetType)) {
                            for (final Class<?> compatibleType : compatibleTypes) {
                                if (compatibleType.isAssignableFrom(sourceType)) {
                                    final CoordinateOperation operation;
                                    try {
                                        operation = caller.createOperation(source, target);
                                    } catch (OperationNotFoundException exception) {
                                        if (failure == null) {
                                            failure = exception;
                                        } else {
                                            failure.addSuppressed(exception);
                                        }
                                        continue;
                                    }
                                    /*
                                     * Found an operation. Exclude the source component from the list because each source
                                     * should be used at most once by `SubOperationInfo`. Note that the same source may
                                     * be used again in another context if that source is also an interpolation CRS.
                                     *
                                     * EXAMPLE: consider a coordinate operation from (GeodeticCRS₁, VerticalCRS₁) source
                                     * to (GeodeticCRS₂, VerticalCRS₂) target.  The source GeodeticCRS₁ should be mapped
                                     * to exactly one target component (which is GeodeticCRS₂)  and  VerticalCRS₁ mapped
                                     * to VerticalCRS₂.  But the operation on vertical coordinates may need GeodeticCRS₁
                                     * for doing its work, so GeodeticCRS₁ is needed twice. However, when needed for the
                                     * vertical coordinate operation, the GeodeticCRS₁ is used as an "interpolation CRS".
                                     * Interpolation CRS are handled in other code paths, it is not the business of this
                                     * `SubOperationInfo` class to care about them. From the point of view of this class,
                                     * GeodeticCRS₁ is used only once.
                                     */
                                    sourceComponentIsUsed[sourceComponentIndex] = true;
                                    infos[targetComponentIndex] = new SubOperationInfo(
                                            operation, null,
                                            sourceLowerDimension, sourceUpperDimension,
                                            targetLowerDimension, targetUpperDimension,
                                            targetComponentIndex, true);

                                    if (failure != null) {
                                        CoordinateOperationRegistry.recoverableException("decompose", failure);
                                    }
                                    continue next;
                                }
                            }
                        }
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
            /*
             * If we reach this point, we have not been able to find a source CRS that we can map to the target CRS.
             * Usually this is fatal, returning null will instruct the caller to throw `OperationNotFoundException`.
             * However, in some contexts (e.g. when searching for an operation between two `GridGeometry` instances)
             * it is possible to assign a constant value to the target coordinates. Those values cannot be guessed
             * by `org.apache.sis.referencing`; they must be provided by caller. If such constants are specified,
             * then we will try to apply them.
             */
            final double[] constants = CoordinateOperationContext.getConstantCoordinates();
            if (constants == null || constants.length < targetUpperDimension) {
                return null;
            }
            for (int i = targetLowerDimension; i < targetUpperDimension; i++) {
                if (Double.isNaN(constants[i])) {
                    throw new NoninvertibleTransformException(caller.resources()
                            .getString(Resources.Keys.ConstantCoordinateValueRequired_1, i));
                }
            }
            infos[targetComponentIndex] = new SubOperationInfo(
                    null, constants,
                    sourceUpperDimension, sourceUpperDimension,
                    targetLowerDimension, targetUpperDimension,
                    targetComponentIndex, false);
        }
        return infos;
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
     *
     * @param  selected  all operations from source to target {@link CompoundCRS}.
     * @param  srcDim    number of dimensions in the target CRS of previous operation step.
     * @param  tgtDim    number of dimensions in the full (usually compound) target CRS.
     */
    static MatrixSIS createConstantOperation(final SubOperationInfo[] selected, final int srcDim, final int tgtDim)
            throws TransformException
    {
        final boolean[] targetDimensionIsUsed = new boolean[tgtDim];
        final MatrixSIS m = Matrices.createZero(tgtDim + 1, srcDim + 1);
        m.setElement(tgtDim, srcDim, 1);
        for (final SubOperationInfo component : selected) {
            if (component.constants != null) {
                /*
                 * Component for which no operation has been found.
                 * Set all coordinate values of that component to the specified constants
                 */
                for (int j = component.targetLowerDimension; j < component.targetUpperDimension; j++) {
                    m.setElement(j, srcDim, component.constants[j]);
                    targetDimensionIsUsed[j] = true;
                }
            } else {
                /*
                 * Component for which an operation has been found, but maybe this is the inverse of the
                 * "Geographic 3D to 2D conversion" (EPSG:9659) which sets the height to `DEFAULT_HEIGHT`.
                 * If all values in the row are zero, then the coordinate value is unconditionally zero.
                 * Replace that value (usually the default ellipsoidal height) by the specified constant.
                 */
                final Matrix last = MathTransforms.getMatrix(MathTransforms.getLastStep(component.operation.getMathTransform()));
                if (last != null) {
otherRow:           for (int j = last.getNumRow() - 1; --j >= 0;) {     // Ignore the last row.
                        for (int i = last.getNumCol(); --i >= 0;) {
                            if (last.getElement(j, i) != 0) {
                                continue otherRow;
                            }
                        }
                        final double[] constants = CoordinateOperationContext.getConstantCoordinates();
                        if (constants == null) break;
                        final int k = component.targetLowerDimension + j;
                        m.setElement(j, srcDim, constants[k]);
                        targetDimensionIsUsed[k] = true;
                    }
                }
            }
        }
        /*
         * All coordinates that have not been set to a constant shall be propagated unchanged (scale factor of 1).
         */
        for (int i=0,j=0; j<tgtDim; j++) {
            if (!targetDimensionIsUsed[j]) {
                m.setElement(j, i++, 1);
            }
        }
        return m;
    }
}
