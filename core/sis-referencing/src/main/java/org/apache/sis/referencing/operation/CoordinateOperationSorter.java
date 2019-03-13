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

import java.util.Arrays;
import java.util.List;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.CRS;


/**
 * Used for sorting coordinate operation in preference order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class CoordinateOperationSorter implements Comparable<CoordinateOperationSorter> {
    /**
     * The coordinate operation to sort with other operations.
     */
    private final CoordinateOperation operation;

    /**
     * Intersection between the domain of validity of the operation and the area of interest,
     * as a negative value for sorting widest areas first and unknown areas ({@link Double#NaN}) last.
     */
    private final double area;

    /**
     * Accuracy of the coordinate operation.
     */
    private final double accuracy;

    /**
     * Creates a new sorter for the given coordinate operation.
     *
     * @param operation       the coordinate operation to sort with other operations.
     * @param areaOfInterest  the geographic area of interest, or {@code null} if unspecified.
     */
    private CoordinateOperationSorter(final CoordinateOperation operation, final GeographicBoundingBox areaOfInterest) {
        this.operation = operation;
        area = -Extents.area(Extents.intersection(areaOfInterest,
                             Extents.getGeographicBoundingBox(operation.getDomainOfValidity())));
        accuracy = CRS.getLinearAccuracy(operation);
    }

    /**
     * Returns -1 if this element should be sorted before the given element.
     */
    @Override
    public int compareTo(final CoordinateOperationSorter other) {
        int c = Double.compare(area, other.area);
        if (c == 0) {
            c = Double.compare(accuracy, other.accuracy);
        }
        return c;
    }

    /**
     * Sorts in-place the given list of operations.
     *
     * @param  operations      the operation to sort.
     * @param  areaOfInterest  the geographic area of interest, or {@code null} if unspecified.
     */
    static void sort(final List<CoordinateOperation> operations, final GeographicBoundingBox areaOfInterest) {
        if (operations.size() > 1) {
            final CoordinateOperationSorter[] s = new CoordinateOperationSorter[operations.size()];
            for (int i=0; i<s.length; i++) {
                s[i] = new CoordinateOperationSorter(operations.get(i), areaOfInterest);
            }
            Arrays.sort(s);
            operations.clear();
            for (int i=0; i<s.length; i++) {
                operations.add(s[i].operation);
            }
        }
    }
}
