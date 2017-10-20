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
package org.apache.sis.internal.referencing;

import java.util.List;
import java.util.Objects;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.apache.sis.internal.metadata.AxisDirections;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * The default coordinate operation factory, provided in a separated class for deferring class loading
 * until first needed. Contains also utility methods related to coordinate operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public final class CoordinateOperations extends SystemListener {
    /**
     * Cached values or {@link #wrapAroundChanges wrapAroundChanges(…)}, created when first needed.
     * Indices are bit masks computed by {@link #changes changes(…)}. Since the most common "wrap around" axes
     * are longitude at dimension 0 or 1, and some measurement of time (in climatology) at dimension 2 or 3,
     * then the most likely values are (binary digits):
     *
     * {@preformat text
     *     0000    0100    1000
     *     0001    0101    1001
     *     0010    0110    1010
     * }
     *
     * The last decimal value is 10 (binary {@code 1010}); we don't need to cache more.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final List<Integer>[] CACHE = new List[11];

    /**
     * The system-wide default factory.
     */
    private static volatile DefaultCoordinateOperationFactory factory;

    /**
     * For system listener only.
     */
    private CoordinateOperations() {
        super(Modules.REFERENCING);
    }

    /**
     * Discards the factory if the classpath changed.
     */
    static {
        add(new CoordinateOperations());
    }

    /**
     * Invoked when the classpath changed.
     */
    @Override
    protected void classpathChanged() {
        factory = null;
    }

    /**
     * Returns the factory.
     *
     * @return the system-wide factory.
     */
    public static DefaultCoordinateOperationFactory factory() {
        DefaultCoordinateOperationFactory c = factory;
        if (c == null) {
            // DefaultFactories.forBuildin(…) performs the necessary synchronization.
            factory = c = DefaultFactories.forBuildin(CoordinateOperationFactory.class, DefaultCoordinateOperationFactory.class);
        }
        return c;
    }

    /**
     * Returns {@code true} if the given axis is of kind "Wrap Around".
     * Defined here because used together with {@link #wrapAroundChanges wrapAroundChanges(…)}.
     *
     * @param  axis  the axis to test.
     * @return {@code true} if the given axis has "wrap around" range meaning.
     */
    public static boolean isWrapAround(final CoordinateSystemAxis axis) {
        return RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning());
    }

    /**
     * Computes indices of target dimensions where "wrap around" may happen as a result of a coordinate operation.
     * This is usually the longitude axis when the source CRS uses the [-180 … +180]° range and the target CRS uses
     * the [0 … 360]° range, or the converse.
     *
     * @param  source  the source of the coordinate operation.
     * @param  target  the target of the coordinate operation.
     * @return list of target dimensions where "wrap around" may happen.
     */
    public static List<Integer> wrapAroundChanges(CoordinateReferenceSystem source, final CoordinateSystem target) {
        long changes = changes(source.getCoordinateSystem(), target);
        while (source instanceof GeneralDerivedCRS) {
            source = ((GeneralDerivedCRS) source).getBaseCRS();
            changes |= changes(source.getCoordinateSystem(), target);
        }
        final boolean useCache = (changes >= 0 && changes < CACHE.length);
        if (useCache) {
            /*
             * In most cases we have an existing instance. Since WrapAroundAxes are immutable, if we got a reference,
             * the object should be okay ("published" in memory model terminology) even if we did not synchronized.
             * The reference however may not be visible in the array, but we will check later in a synchronized block.
             */
            final List<Integer> existing = CACHE[(int) changes];
            if (existing != null) {
                return existing;
            }
        }
        /*
         * No existing instance found. Expand the 'changes' bit mask in an array of integers in order to create an
         * unmodifiable List<Integer>. The list is for public API; internally, Apache SIS will use toBitMask(…).
         */
        long r = changes;
        final Integer[] indices = new Integer[Long.bitCount(r)];
        for (int i=0; i<indices.length; i++) {
            final int dim = Long.numberOfTrailingZeros(r);
            indices[i] = dim;
            r &= ~(1L << dim);
        }
        final List<Integer> dimensions = JDK9.listOf(indices);
        if (useCache) {
            synchronized (CACHE) {
                final List<Integer> existing = CACHE[(int) changes];
                if (existing != null) {
                    return existing;
                }
                CACHE[(int) changes] = dimensions;
            }
        }
        return dimensions;
    }

    /**
     * Returns the packed indices of target dimensions where ordinate values may need to be wrapped around.
     * This method matches source and target coordinate system axes having {@link RangeMeaning#WRAPAROUND},
     * then verifies if the range of values changed (taking unit conversions in account). A target dimension
     * {@code i} may need to "wrap around" the coordinate values if the {@code 1 << i} bit is set.
     * If there is no change, then the value is zero.
     */
    private static long changes(final CoordinateSystem source, final CoordinateSystem target) {
        long changes = 0;
        if (source != target) {                                 // Optimization for a common case.
            /*
             * Get the dimensions of all "wrap around" axes in the source coordinate system.
             * We create this list first because we may iterate more than once on those axes
             * and we want to clear the axes that we already matched. We use a bitmask for
             * efficiency, with the bits of "wrap around" dimensions set to 1.
             */
            long isWrapAroundAxis = 0;
            for (int i=Math.min(Long.SIZE, source.getDimension()); --i >= 0;) {
                if (isWrapAround(source.getAxis(i))) {
                    isWrapAroundAxis |= (1 << i);
                }
            }
            if (isWrapAroundAxis != 0) {                        // Loop below is useless otherwise.
                /*
                 * For each "wrap around" axis in the target CRS, search a matching axis in the source CRS
                 * which is also "wrap around", is colinear and uses compatible unit of measurement. There
                 * is usually at most one "wrap around" axis, but this code is nevertheless generic enough
                 * for an arbitrary amount of axes.
                 */
                final int dim = Math.min(Long.SIZE, target.getDimension());
compare:        for (int i=0; i<dim; i++) {
                    final CoordinateSystemAxis axis = target.getAxis(i);
                    if (isWrapAround(axis)) {
                        long candidates = isWrapAroundAxis;
                        do {
                            final long mask = Long.lowestOneBit(candidates);
                            final CoordinateSystemAxis src = source.getAxis(Long.numberOfTrailingZeros(mask));
                            if (Objects.equals(AxisDirections.absolute(src .getDirection()),
                                               AxisDirections.absolute(axis.getDirection())))
                            {
                                try {
                                    final UnitConverter c  = src.getUnit().getConverterToAny(axis.getUnit());
                                    final double minimum   = axis.getMinimumValue();
                                    final double maximum   = axis.getMaximumValue();
                                    final double tolerance = (maximum - minimum) * Numerics.COMPARISON_THRESHOLD;
                                    if (!Numerics.epsilonEqual(c.convert(src.getMinimumValue()), minimum, tolerance) ||
                                        !Numerics.epsilonEqual(c.convert(src.getMaximumValue()), maximum, tolerance))
                                    {
                                        changes |= (1 << i);
                                    }
                                    isWrapAroundAxis &= ~mask;      // We are done with this source axis.
                                    if (isWrapAroundAxis == 0) {
                                        break compare;              // Useless to continue if there is no more source axis.
                                    }
                                    continue compare;               // Match next pair of wrap around axes.
                                } catch (IncommensurableException e) {
                                    // Ignore (should not happen often). We will try to match another pair of axes.
                                }
                            }
                            candidates &= ~mask;        // Unable to use that axis. Check if we can use another one.
                        } while (candidates != 0);
                    }
                }
            }
        }
        return changes;
    }
}
