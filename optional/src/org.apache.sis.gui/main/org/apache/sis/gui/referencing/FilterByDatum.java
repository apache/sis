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
package org.apache.sis.gui.referencing;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.function.Predicate;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Utilities;
import org.apache.sis.referencing.CRS;


/**
 * Filter of reference systems that are compatible with the data to render.
 * Instances of this class are immutable and thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FilterByDatum implements Predicate<ReferenceSystem> {
    /**
     * The types of datum allowed by the coordinate reference systems shown in the menus of combo boxes.
     * If a <abbr>CRS</abbr> has a datum of a type which is not in this array, then it will not be shown.
     * A {@code null} array means that no filtering is applied.
     *
     * <p>The current version of this class filters only by datum type because this is quick.
     * But a more advanced version should check if a path exists between <abbr>CRS</abbr>s.</p>
     */
    private final Class<? extends Datum>[] allowedDatumTypes;

    /**
     * Datum instances for which an exact match is required.
     * We handle engineering datum in a special way because they are the kind of datum created
     * when we cannot map the <abbr>CRS</abbr> of a grid coverage to a geodetic <abbr>CRS</abbr>.
     * We no such pivot, we cannot transform from one engineering <abbr>CRS</abbr> to another.
     */
    private final EngineeringDatum[] instances;

    /**
     * Creates a new filter for coordinate reference systems to show in the menu or combo box.
     * If the <abbr>CRS</abbr> of a menu item has a datum of a type which is different than the
     * datum types of all elements in the {@code refsys} array, then that menu item is hidden.
     *
     * @param  refsys  <abbr>CRS</abbr>s from which to get the datum types. Null elements are ignored.
     * @return filter, or {@code null} if none.
     */
    static FilterByDatum create(final CoordinateReferenceSystem[] refsys) {
        final var types = new HashSet<Class<? extends Datum>>();
        final var instances = new ArrayList<EngineeringDatum>();
        for (final CoordinateReferenceSystem crs : refsys) {
            for (final SingleCRS component : CRS.getSingleComponents(crs)) {
                add(component.getDatum(), types, instances);
                final var ensemble = component.getDatumEnsemble();
                if (ensemble != null) {
                    for (final Datum datum : ensemble.getMembers()) {
                        add(datum, types, instances);
                    }
                }
            }
        }
        types.remove(null);
        if (instances.isEmpty() && types.equals(Set.of(Datum.class))) {
            return null;
        }
        return new FilterByDatum(types.toArray(Class[]::new), instances.toArray(EngineeringDatum[]::new));
    }

    /**
     * Adds the given datum in the given collections.
     *
     * @param datum      the datum to add.
     * @param types      where to add the datum type.
     * @param instances  where to add the datum instance if not a duplicate.
     */
    private static void add(final Datum datum, final Set<Class<? extends Datum>> types, final List<EngineeringDatum> instances) {
        types.add(Classes.getStandardClass(datum, Datum.class));
        if (datum instanceof EngineeringDatum) {
            for (int i = instances.size(); --i >= 0;) {
                if (Utilities.equalsIgnoreMetadata(datum, instances.get(i))) {
                    return;
                }
            }
            instances.add((EngineeringDatum) datum);
        }
    }

    /**
     * Creates a new filter for the given datum types.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private FilterByDatum(final Class[] types, final EngineeringDatum[] instances) {
        allowedDatumTypes = types;
        this.instances = instances;
    }

    /**
     * Returns whether the given reference system has valid datum types.
     *
     * @param  system  the reference system to test.
     * @return whether the given reference system can be accepted.
     */
    @Override
    public boolean test(final ReferenceSystem system) {
        if (system instanceof CoordinateReferenceSystem) {
next:       for (final SingleCRS component : CRS.getSingleComponents((CoordinateReferenceSystem) system)) {
                if (!accept(component.getDatum())) {
                    final var ensemble = component.getDatumEnsemble();
                    if (ensemble != null) {
                        for (final Datum datum : ensemble.getMembers()) {
                            if (accept(datum)) {
                                continue next;
                            }
                        }
                    }
                    return false;
                }
            }
            return true;
        } else {
            // Assume that referencing by identifiers depend on geodetic datum.
            return Classes.isAssignableToAny(GeodeticDatum.class, allowedDatumTypes);
        }
    }

    /**
     * Tests whether the given datum is accepted.
     */
    private boolean accept(final Datum datum) {
        if (datum != null && Classes.isAssignableToAny(datum.getClass(), allowedDatumTypes)) {
            if (!(datum instanceof EngineeringDatum)) {
                return true;
            }
            for (final EngineeringDatum instance : instances) {
                if (Utilities.equalsIgnoreMetadata(datum, instance)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the base type of coordinate reference system for the datum accepted by this filter.
     */
    final Class<? extends CoordinateReferenceSystem> baseType() {
        if (allowedDatumTypes.length == 1) {
            final Class<? extends Datum> type = allowedDatumTypes[0];
            if    (VerticalDatum.class.isAssignableFrom(type)) return VerticalCRS.class;
            if    (TemporalDatum.class.isAssignableFrom(type)) return TemporalCRS.class;
            if (EngineeringDatum.class.isAssignableFrom(type)) return EngineeringCRS.class;
            return SingleCRS.class;
        }
        return CoordinateReferenceSystem.class;
    }
}
