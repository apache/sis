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
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.util.FactoryException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.referencing.operation.matrix.Matrices;

// Branch-dependent imports
import org.apache.sis.referencing.crs.DefaultParametricCRS;


/**
 * Information about the relationship between a source component and a target component
 * in {@code CompoundCRS} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class SubOperationInfo {
    /**
     * Types of target CRS, together with the type of CRS that may be used as the source for that target.
     * For each array {@code COMPATIBLE_TYPES[i]}, the first element (i.e. {@code COMPATIBLE_TYPES[i][0]})
     * is the target CRS and the whole array (including the first element) gives the valid source CRS type,
     * if preference order.
     *
     * <div class="note"><b>Example:</b>
     * if a target CRS is of type {@link VerticalCRS}, then the source CRS may be another {@code VerticalCRS}
     * or a {@link GeodeticCRS}. The geodetic CRS is possible because it may be three-dimensional.</div>
     *
     * {@link ProjectedCRS} and {@link DerivedCRS} are not in this list because we rather use their base CRS
     * as the criterion for determining their type.
     */
    private static final Class<?>[][] COMPATIBLE_TYPES = {
        {GeodeticCRS.class},
        {VerticalCRS.class, GeodeticCRS.class},
        {TemporalCRS.class},
        {DefaultParametricCRS.class},
        {EngineeringCRS.class},
        {ImageCRS.class}
    };

    /**
     * Returns the class of the given CRS after unwrapping derived and projected CRS.
     * The returned type is for use with {@link #COMPATIBLE_TYPES}.
     */
    private static Class<?> type(SingleCRS crs) {
        while (crs instanceof GeneralDerivedCRS) {
            crs = (SingleCRS) ((GeneralDerivedCRS) crs).getBaseCRS();
        }
        return crs.getClass();
    }

    /**
     * The coordinate operation between a source component and a target component.
     */
    final CoordinateOperation operation;

    /**
     * Returns the first dimension (inclusive) where the source component CRS begins in the source compound CRS.
     */
    final int startAtDimension;

    /**
     * Returns the last dimension (exclusive) where the source component CRS ends in the source compound CRS.
     */
    final int endAtDimension;

    /**
     * Creates a new instance containing the given information.
     */
    private SubOperationInfo(final CoordinateOperation operation, final int startAtDimension, final int endAtDimension) {
        this.operation        = operation;
        this.startAtDimension = startAtDimension;
        this.endAtDimension   = endAtDimension;
    }

    /**
     * Searches in given list of source components for an operation capable to convert or transform coordinates
     * to the given target CRS. If no such operation can be found, then this method returns {@code null}.
     *
     * @param  caller       the object which is inferring a coordinate operation.
     * @param  sourceIsUsed flags for keeping trace of which source has been used.
     * @param  sources      all components of the source CRS.
     * @param  target       one component of the target CRS.
     * @return information about a coordinate operation from a source CRS to the given target CRS, or {@code null}.
     * @throws FactoryException if an error occurred while grabbing a coordinate operation.
     */
    static SubOperationInfo create(final CoordinateOperationFinder caller, final boolean[] sourceIsUsed,
            final List<? extends SingleCRS> sources, final SingleCRS target) throws FactoryException
    {
        OperationNotFoundException failure = null;
        final Class<?> targetType = type(target);
        for (final Class<?>[] sourceTypes : COMPATIBLE_TYPES) {
            if (sourceTypes[0].isAssignableFrom(targetType)) {
                for (final Class<?> sourceType : sourceTypes) {
                    int startAtDimension;
                    int endAtDimension = 0;
                    for (int i=0; i<sourceIsUsed.length; i++) {
                        final SingleCRS source = sources.get(i);
                        startAtDimension = endAtDimension;
                        endAtDimension += source.getCoordinateSystem().getDimension();
                        if (!sourceIsUsed[i] && sourceType.isAssignableFrom(type(source))) {
                            final CoordinateOperation operation;
                            try {
                                operation = caller.createOperation(source, target);
                            } catch (OperationNotFoundException exception) {
                                if (failure == null) {
                                    failure = exception;
                                } else {
                                    // failure.addSuppressed(exception) on the JDK7 branch.
                                }
                                continue;
                            }
                            /*
                             * Found an operation.  Exclude the source component from the list because each source
                             * should be used at most once by SourceComponent. Note that the same source may still
                             * be used again in another context if that source is also an interpolation CRS.
                             *
                             * EXAMPLE: consider a coordinate operation from (GeodeticCRS₁, VerticalCRS₁) source
                             * to (GeodeticCRS₂, VerticalCRS₂) target.  The source GeodeticCRS₁ should be mapped
                             * to exactly one target component (which is GeodeticCRS₂)  and  VerticalCRS₁ mapped
                             * to VerticalCRS₂.  But the operation on vertical coordinates may need GeodeticCRS₁
                             * for doing its work, so GeodeticCRS₁ is needed twice.  However when needed for the
                             * vertical coordinate operation, the GeodeticCRS₁ is used as an "interpolation CRS".
                             * Interpolation CRS are handled in other code paths; it is not the business of this
                             * SourceComponent class to care about them. From the point of view of this class,
                             * GeodeticCRS₁ is used only once.
                             */
                            sourceIsUsed[i] = true;
                            if (failure != null) {
                                Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                                        CoordinateOperationFinder.class, "decompose", failure);
                            }
                            return new SubOperationInfo(operation, startAtDimension, endAtDimension);
                        }
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return null;
    }

    /**
     * Returns the dimension from which all remaining operations are identity.
     */
    static int startOfIdentity(final SubOperationInfo[] selected) {
        int n = selected.length;
        while (n != 0) {
            if (!selected[--n].operation.getMathTransform().isIdentity()) {
                break;
            }
        }
        return n;
    }

    /**
     * Returns a matrix for an affine transform from all source coordinates to the coordinates of the
     * source components selected for participating in the coordinate operation.
     *
     * @param sourceDimensions    number of dimension of the source {@code CompoundCRS}.
     * @param selectedDimensions  number of source dimensions needed by the coordinate operations.
     * @param selected all {@code SourceComponent} instances needed for the target {@code CompoundCRS}.
     */
    static Matrix sourceToSelected(final int sourceDimensions, final int selectedDimensions, final SubOperationInfo[] selected) {
        final Matrix select = Matrices.createZero(selectedDimensions + 1, sourceDimensions + 1);
        select.setElement(selectedDimensions, sourceDimensions, 1);
        int j = 0;
        for (final SubOperationInfo component : selected) {
            for (int i=component.startAtDimension; i<component.endAtDimension; i++) {
                select.setElement(j++, i, 1);
            }
        }
        return select;
    }
}
