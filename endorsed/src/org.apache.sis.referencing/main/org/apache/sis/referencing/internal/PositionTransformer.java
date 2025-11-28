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
package org.apache.sis.referencing.internal;

import java.util.Objects;
import java.util.NoSuchElementException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.MultiRegisterOperations;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.GeneralDirectPosition;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.RegisterOperations;
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * A direct position capable to transform another position from its arbitrary CRS to the CRS of this position.
 * This class caches the last transform used in order to improve performance when
 * {@linkplain CoordinateOperation#getSourceCRS() source} and
 * {@linkplain CoordinateOperation#getTargetCRS() target} CRS do not change often.
 * Using this class is faster than invoking <code>{@linkplain RegisterOperations#findCoordinateOperations
 * RegisterOperations.findCoordinateOperations}(lastCRS, targetCRS)</code> for every points.
 *
 * <ul class="verbose">
 *   <li><b>Note 1:</b>
 *   This class is advantageous on a performance point of view only if the same instance of
 *   {@code PositionTransformer} is used for transforming many points between arbitrary CRS
 *   and this {@linkplain #getCoordinateReferenceSystem() position CRS}.</li>
 *
 *   <li><b>Note 2:</b>
 *   This convenience class is useful when the source and target CRS are <em>not likely</em> to change often.
 *   If you are sure that the source and target CRS will not change at all for a given set of positions,
 *   then using {@link CoordinateOperation} directly gives better performances. This is because this class
 *   checks if the CRS changed before every transformations, which may be costly.</li>
 * </ul>
 *
 * This class should not appear in a public API. It is used as a helper private field in more complex classes.
 * For example, suppose that {@code MyClass} needs to perform its internal working in some particular CRS,
 * but we want robust API accepting whatever CRS the client uses. {@code MyClass} can be written as below:
 *
 * {@snippet lang="java" :
 *     public class MyClass {
 *         private static final CoordinateReferenceSystem PUBLIC_CRS = ...
 *         private static final CoordinateReferenceSystem INTERNAL_CRS = ...
 *
 *         private final PositionTransformer myPosition =
 *                 new PositionTransformer(PUBLIC_CRS, INTERNAL_CRS, null);
 *
 *         public void setPosition(DirectPosition position) throws TransformException {
 *             // The position CRS is usually PUBLIC_CRS, but code below will work even if it is not.
 *             myPosition.transform(position);
 *         }
 *
 *         public DirectPosition getPosition() throws TransformException {
 *             return myPosition.inverseTransform(PUBLIC_CRS);
 *         }
 *     }
 *     }
 *
 * This class is not thread-safe.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})         // Not intended to be serialized and nothing to clone.
public final class PositionTransformer extends GeneralDirectPosition {
    /**
     * The factory to use for creating new coordinate operation.
     */
    private final RegisterOperations factory;

    /**
     * The default CRS to assume when {@link #transform(DirectPosition)} has been invoked without associated CRS.
     * This is often the same as the {@linkplain #getCoordinateReferenceSystem() target CRS}, but not necessarily.
     */
    public final CoordinateReferenceSystem defaultCRS;

    /**
     * The last CRS of a position given to {@link #transform(DirectPosition)}, or {@code null}.
     * This is used as the source CRS of the coordinate operation. The {@code targetCRS} will
     * be the {@linkplain #getCoordinateReferenceSystem() CRS associated with this position}.
     *
     * @see #setSourceCRS(CoordinateReferenceSystem)
     */
    private transient CoordinateReferenceSystem lastCRS;

    /**
     * The forward and inverse transforms. Will be created only when first needed.
     * Those fields are left to {@code null} value if the transform is identity.
     *
     * @see #setSourceCRS(CoordinateReferenceSystem)
     * @see #inverse()
     */
    private transient MathTransform forward, inverse;

    /**
     * Creates a new position which will contain the result of coordinate transformations to the given CRS.
     * The {@linkplain #getCoordinateReferenceSystem() CRS associated with this position} will be initially
     * set to {@code targetCRS}.
     *
     * @param  defaultCRS  the CRS to take as the source when <code>{@link #transform transform}(position)</code>
     *         is invoked with a position without associated CRS. If {@code null}, default to {@code targetCRS}.
     * @param  targetCRS  the {@linkplain #getCoordinateReferenceSystem() CRS associated with this position}. Will be the target
     *         of {@linkplain #transform coordinate transformations} until the next call to {@link #setCoordinateReferenceSystem
     *         setCoordinateReferenceSystem(…)} or {@link #setLocation(DirectPosition) setLocation}. Cannot be null.
     * @param  factory  the factory to use for creating coordinate operations, or {@code null} for the default.
     */
    public PositionTransformer(final CoordinateReferenceSystem defaultCRS, final CoordinateReferenceSystem targetCRS,
            final RegisterOperations factory)
    {
        super(targetCRS);
        this.defaultCRS = (defaultCRS != null) ? defaultCRS : targetCRS;
        this.factory    = (factory != null) ? factory : MultiRegisterOperations.provider();
    }

    /**
     * Sets the coordinate reference system in which the coordinate is given.
     * The given CRS will be used as:
     *
     * <ul>
     *   <li>the {@linkplain CoordinateOperation#getTargetCRS() target CRS} for every call to {@link #transform(DirectPosition)},</li>
     *   <li>the {@linkplain CoordinateOperation#getSourceCRS() source CRS} for every call to {@link #inverseTransform()}.</li>
     * </ul>
     *
     * @param  targetCRS  the new CRS for this direct position.
     * @throws MismatchedDimensionException if the specified CRS does not have the expected number of dimensions.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem targetCRS) throws MismatchedDimensionException {
        super.setCoordinateReferenceSystem(Objects.requireNonNull(targetCRS));
        forward = null;
        inverse = null;
    }

    /**
     * Sets the {@link #lastCRS} field and creates the associated {@link #forward} transform.
     * This method does not create yet the {@link #inverse} transform, since it may not be needed.
     */
    private void setSourceCRS(final CoordinateReferenceSystem crs) throws TransformException {
        final CoordinateReferenceSystem targetCRS = getCoordinateReferenceSystem();
        final CoordinateOperation operation;
        try {
            operation = factory.findCoordinateOperations(crs, targetCRS).iterator().next();
        } catch (FactoryException | NoSuchElementException exception) {
            throw new TransformException(exception.getLocalizedMessage(), exception);
        }
        /*
         * Note: `lastCRS` should be set last, when we are sure that all other fields
         * are set to their correct values. This is in order to keep this instance in
         * a consistent state in case an exception is thrown.
         */
        forward = operation.getMathTransform();
        inverse = null;
        lastCRS = crs;
        if (forward.isIdentity()) {
            forward = null;
        }
    }

    /**
     * Transforms the given position from the CRS of this position to the default CRS.
     * The result is stored in the given array.
     *
     * @param  point  the coordinates of the point to transform in-place.
     * @throws TransformException if a coordinate transformation was required and failed.
     */
    public void transform(final double[] point) throws TransformException {
        if (point != null) {
            if (lastCRS != defaultCRS) {
                setSourceCRS(defaultCRS);
            }
            if (forward != null) {
                forward.transform(point, 0, point, 0, 1);
            }
        }
    }

    /**
     * Transforms a given position from its CRS to the CRS of this {@code PositionTransformer}.
     * If the CRS associated to the given position is {@code null}, then that CRS is assumed to
     * be the default CRS specified at construction time. Otherwise if that CRS is not equal to
     * the {@linkplain #getCoordinateReferenceSystem() CRS associated with this position}, then
     * a coordinates transformations are applied. The result may be stored in this instance.
     *
     * @param  position  a position using an arbitrary CRS, or {@code null}. This object will not be modified.
     * @return the transformed position, either {@code this} or the given position (which may be {@code null}).
     * @throws TransformException if a coordinate transformation was required and failed.
     */
    public DirectPosition transform(final DirectPosition position) throws TransformException {
        if (position != null) {
            CoordinateReferenceSystem userCRS = position.getCoordinateReferenceSystem();
            if (userCRS == null) {
                userCRS = defaultCRS;
            }
            /*
             * A projection may be required. Check if it is the same one as the one used last time this method
             * has been invoked. If the specified position uses a new CRS, then get the transformation and save
             * it in case the next call to this method would use again the same transformation.
             */
            if (!CRS.equivalent(lastCRS, userCRS)) {
                setSourceCRS(userCRS);
            }
            if (forward != null) {
                return forward.transform(position, this);
            }
        }
        return position;
    }

    /**
     * Returns the inverse transform, computed when first needed.
     */
    private MathTransform inverse() throws TransformException {
        if (inverse == null) {
            if (!CRS.equivalent(lastCRS, defaultCRS)) {
                setSourceCRS(defaultCRS);
            }
            inverse = (forward != null) ? forward.inverse() : MathTransforms.identity(getDimension());
        }
        return inverse;
    }

    /**
     * Returns a new point with the same coordinates as this one, but transformed to the default CRS.
     * This method never returns {@code this}, so the returned point does not need to be cloned.
     *
     * @return the same position as {@code this}, but transformed to the default CRS.
     * @throws TransformException if a coordinate transformation was required and failed.
     */
    public DirectPosition inverseTransform() throws TransformException {
        return inverse().transform(this, new GeneralDirectPosition(defaultCRS));
    }

    /**
     * Transforms this point to the default CRS and stores the result in the given array, and returns the derivative.
     * The {@code target} array length should be {@code CRS.getDimensionOrZero(defaultCRS)}.
     *
     * @param  target  where to store the transformed coordinates.
     * @return the derivative (Jacobian matrix) at the location of this point.
     * @throws TransformException if a coordinate transformation was required and failed.
     */
    public Matrix inverseTransform(final double[] target) throws TransformException {
        return MathTransforms.derivativeAndTransform(inverse(), coordinates, 0, target, 0);
    }
}
