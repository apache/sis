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
package org.apache.sis.referencing.factory.sql;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Strings;


/**
 * Collect information needed for evaluating the pertinence of an object.
 * The criteria are, in order:
 *
 * <ol>
 *   <li>Superseded objects are last.</li>
 *   <li>Largest domain of validity (after intersection with <abbr>AOI</abbr> are first.</li>
 * </ol>
 *
 * This class defines a {@link #compareTo(ObjectPertinence)} method which is inconsistent
 * with {@link #equals(Object)}, but this is okay for the purpose of this internal class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ObjectPertinence implements Comparable<ObjectPertinence> {
    /**
     * Code of the object for which the pertinence is evaluated.
     */
    private final int code;

    /**
     * An estimation of the surface of the domain of validity as a negative number, or NaN if none.
     * The units of measurement do not matter here. The only requirement is that larger areas are
     * represented by <em>smaller</em> numbers, in order to have them sorted first.
     * The {@link Double#NaN} values will be sorted last.
     */
    private final double area;

    /**
     * The objects to use instead of the object identified by the code given at construction time.
     * This list is non-empty when the object is superseded by more recent objects.
     * This list is empty if there is no supersession.
     */
    final List<Integer> replacedBy;

    /**
     * Creates a new set of information for the object identified by the given code.
     *
     * @param  code     the authority code of the object for which to collect information.
     * @param  extents  authority codes of the extents of the object identified by {@code code}.
     * @param  factory  the factory to use for getting extent objects.
     * @throws FactoryException if an error occurred while fetching extents.
     */
    ObjectPertinence(final int code, final List<String> extents, final GeodeticAuthorityFactory factory)
            throws FactoryException
    {
        this.code = code;
        GeographicBoundingBox bbox = null;
        for (final String extentCode : extents) {
            final Extent extent = factory.createExtent(extentCode);
            bbox = Extents.union(bbox, Extents.getGeographicBoundingBox(extent));
        }
        area = -Extents.area(bbox);
        replacedBy = new ArrayList<>();
    }

    /**
     * Returns the code of the object for which the pertinence is evaluated.
     */
    final String code() {
        return Integer.toString(code);
    }

    /**
     * Determines the ordering based on the extent.
     * This method does not take supersession in account.
     * This method is inconsistent with {@link #equals(Object)},
     * but this is okay for the purpose of this internal class.
     */
    @Override
    public int compareTo(final ObjectPertinence other) {
        return Double.compare(area, other.area);    // Reminder: we want NaN to be sorted last.
    }

    /**
     * Sorts in-place the elements that are in the given array.
     *
     * @param  elements  the elements to sort.
     * @return {@code true} if the array changed as a result of this method call.
     */
    static boolean sort(final ObjectPertinence[] elements) {
        boolean changed = false;
        for (int i=1; i<elements.length; i++) {
            if (elements[i-1].compareTo(elements[i]) > 0) {
                Arrays.sort(elements);
                changed = true;
                break;
            }
        }
        int iteration = 0;
        boolean redo;
        do {
            redo = false;
            for (int i=0; i<elements.length; i++) {
                for (final Integer replacement : elements[i].replacedBy) {
                    for (int j=i+1; j<elements.length; j++) {
                        final ObjectPertinence candidate = elements[j];
                        if (candidate.code == replacement) {
                            /*
                             * Found an element to move in front of the superseded ones.
                             */
                            System.arraycopy(elements, i, elements, i+1, j-i);
                            elements[i++] = candidate;
                            redo = changed = true;
                        }
                    }
                }
            }
        } while (redo && ++iteration < Formulas.MAXIMUM_ITERATIONS);
        return changed;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(),
                "code", code,
                "area", Math.abs((float) (area / 1E+6)),    // Square kilometers
                "replacedBy", CollectionsExt.first(replacedBy));
    }
}
