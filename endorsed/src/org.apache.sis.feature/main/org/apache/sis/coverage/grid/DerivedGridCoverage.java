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

import java.util.List;
import java.util.Locale;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.image.DataType;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A grid coverage which is derived from a single source coverage.
 * The default implementations of methods in this class assume that this
 * derived coverage uses the same sample dimensions as the source coverage.
 * If it is not the case, then some methods may need to be overridden.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class DerivedGridCoverage extends GridCoverage {
    /**
     * The source grid coverage.
     */
    protected final GridCoverage source;

    /**
     * Constructs a new grid coverage which is derived from the given source.
     * The new grid coverage share the same sample dimensions as the source.
     *
     * @param  source  the source from which to copy the sample dimensions.
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     */
    DerivedGridCoverage(final GridCoverage source, final GridGeometry domain) {
        super(source, domain);
        this.source = source;
    }

    /**
     * Constructs a new grid coverage which is derived from the given source.
     * The new grid coverage share the same grid geometry as the source.
     * Subclasses which use this constructor may need to override the following methods:
     * {@link #getBandType()}, {@link #evaluator()}.
     *
     * @param  source  the source from which to copy the grid geometry.
     * @param  ranges  sample dimensions for each image band.
     */
    DerivedGridCoverage(final GridCoverage source, final List<? extends SampleDimension> ranges) {
        super(source.getGridGeometry(), ranges);
        this.source = source;
    }

    /**
     * Returns {@code true} if this coverage should not be replaced by its source.
     *
     * @see GridCoverageProcessor.Optimization#REPLACE_SOURCE
     */
    boolean isNotRepleacable() {
        return false;
    }

    /**
     * Returns the data type identifying the primitive type used for storing sample values in each band.
     * The default implementation returns the type of the source.
     */
    @Override
    DataType getBandType() {
        return source.getBandType();
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * That function accepts {@link DirectPosition} in arbitrary Coordinate Reference System;
     * conversions to grid indices are applied by the {@linkplain #source} as needed.
     *
     * <h4>Differences with usual behavior</h4>
     * The evaluator returned by the default implementation has two methods with a behavior different
     * than the intuitively expected ones. However, those differences are allowed by methods contract.
     *
     * <ul>
     *   <li>{@link GridCoverage.Evaluator#getCoverage()} returns an instance which is not {@code this}.</li>
     *   <li>The results returned by {@link GridCoverage.Evaluator#toGridCoordinates(DirectPosition)}
     *       are coordinates in a grid potentially inconsistent with {@link #getGridGeometry()}.</li>
     * </ul>
     *
     * Those differences are allowed because otherwise, this method would be forced to use a wrapper at
     * least for transforming {@link GridCoverage.Evaluator#toGridCoordinates(DirectPosition)} results.
     * It would add an indirection level for all others (more important) method calls.
     */
    @Override
    public Evaluator evaluator() {
        return source.evaluator();
    }

    /**
     * Returns a tree representation of some elements of this grid coverage.
     * This method create the tree documented in parent class,
     * augmented with a short summary of the source.
     *
     * @param  locale   the locale to use for textual labels.
     * @param  bitmask  combination of {@link GridGeometry} flags.
     * @return a tree representation of the specified elements.
     */
    @Debug
    @Override
    public TreeTable toTree(final Locale locale, final int bitmask) {
        final TreeTable tree = super.toTree(locale, bitmask);
        final TreeTable.Node branch = tree.getRoot().newChild();
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        final TableColumn<CharSequence> column = TableColumn.VALUE_AS_TEXT;
        branch.setValue(column, vocabulary.getString(Vocabulary.Keys.Source));
        branch.newChild().setValue(column, summary(source));
        return tree;
    }

    /**
     * Returns a short (single-line) string representation of the given coverage.
     * This is used for listing sources.
     */
    private static String summary(final GridCoverage source) {
        final StringBuilder b = new StringBuilder(Classes.getShortClassName(source));
        final GridExtent extent = source.gridGeometry.extent;
        if (extent != null) {
            b.append('[');
            final int dimension = extent.getDimension();
            for (int i=0; i<dimension; i++) {
                if (i != 0) b.append(" × ");
                // Do not use `extent.getSize(i)` for avoiding potential ArithmeticException.
                b.append(GridExtent.toSizeString(extent.getHigh(i) - extent.getLow(i) + 1));
            }
            b.append(']');
        }
        return b.toString();
    }
}
