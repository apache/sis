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
package org.apache.sis.referencing;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.util.Static;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Static methods working on {@linkplain CoordinateReferenceSystem Coordinate Reference Systems}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
public final class CRS extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CRS() {
    }

    /**
     * Returns a Coordinate Reference System from the given authority code.
     *
     * @todo This method is not yet implemented. It will be provided after the EPSG-backed
     *       authority factory has been ported to Apache SIS.
     *
     * @param  code The authority code.
     * @return The Coordinate Reference System for the given authority code.
     * @throws NoSuchAuthorityCodeException If there is no known CRS associated to the given code.
     * @throws FactoryException if the CRS creation failed for an other reason.
     */
    public static CoordinateReferenceSystem forCode(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ensureNonNull("code", code);
        return null;
    }

    /**
     * Returns the ordered list of single coordinate reference systems for the specified CRS.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the given CRS is null, returns an empty list.</li>
     *   <li>If the given CRS is an instance of {@link SingleCRS}, returns that instance in a singleton list.</li>
     *   <li>If the given CRS is an instance of {@link CompoundCRS}, returns a flattened list of its
     *       {@linkplain DefaultCompoundCRS#getComponents() components}. Some components may themselves be
     *       other {@code CompoundCRS} instances, in which case those compound CRS are also expanded in their
     *       list of {@code SingleCRS} components.</li>
     *   <li>Otherwise throws a {@code ClassCastException}.</li>
     * </ul>
     *
     * {@example Apache SIS allows 4-dimensional (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>)
     * coordinate reference system to be built in two different ways as shown below:
     *
     * <p><table class="compact">
     * <tr><th>Hierarchical structure</th><th>Flat list</th></tr>
     * <tr><td><blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>)<br>
     *   <code>  │   ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  │   └─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote></td><td><blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  ├─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote>
     * </td></tr></table></p>
     *
     * This method guaranteed that the returned list is a flat one as shown on the right side.
     * Note that such flat lists are the only one allowed by ISO/OGC standards for compound CRS.
     * The hierarchical structure is an Apache SIS flexibility.}
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The single coordinate reference systems, or an empty list if the given CRS is {@code null}.
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     *
     * @see DefaultCompoundCRS#getSingleComponents()
     */
    public static List<SingleCRS> getSingleComponents(final CoordinateReferenceSystem crs) {
        final List<SingleCRS> singles;
        if (crs == null) {
            singles = Collections.emptyList();
        } else if (crs instanceof CompoundCRS) {
            if (crs instanceof DefaultCompoundCRS) {
                singles = ((DefaultCompoundCRS) crs).getSingleComponents();
            } else {
                final List<CoordinateReferenceSystem> elements = ((CompoundCRS) crs).getComponents();
                singles = new ArrayList<>(elements.size());
                ReferencingUtilities.getSingleComponents(elements, singles);
            }
        } else {
            // Intentional CassCastException here if the crs is not a SingleCRS.
            singles = Collections.singletonList((SingleCRS) crs);
        }
        return singles;
    }
}
