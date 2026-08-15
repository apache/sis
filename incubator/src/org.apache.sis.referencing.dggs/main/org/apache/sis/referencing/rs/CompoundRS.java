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
package org.apache.sis.referencing.rs;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Objects;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.Identifier;
import org.apache.sis.metadata.iso.DefaultIdentifier;


/**
 * A <abbr>RS</abbr> describing the position of points through two or more independent <abbr>RS</abbr>s.
 * Two <abbr>RS</abbr>s are independent of each other if coordinate values in one cannot be converted or
 * transformed into coordinate values in the other.
 *
 * <p>This interface is a generalized version of CompoundCRS.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CompoundRS implements ReferenceSystem {

    private final Identifier name;
    private final List<ReferenceSystem> rss;
    private final CoordinateReferenceSystem leaningCrs;

    public CompoundRS(Identifier name, List<ReferenceSystem> rss) {
        if (rss.size() < 2) {
            throw new IllegalArgumentException("Provide at least two systems to create a CompoundRS.");
        }

        if (name == null) {
            final StringBuilder sb = new StringBuilder("CompundLS [");
            for (int i = 0, n = rss.size(); i < n; i++) {
                final ReferenceSystem cdt = rss.get(i);
                if (i != 0) sb.append(',');
                sb.append(cdt.getName().toString());
            }
            sb.append(']');
            name = new DefaultIdentifier(sb.toString());
        }
        this.name = name;
        this.rss = Collections.unmodifiableList(rss);

        final List<CoordinateReferenceSystem> crss = new ArrayList<>(rss.size()+1);
        for (int i = 0; i < rss.size(); i++) {
            final ReferenceSystem srs = rss.get(i);
            if (srs instanceof CoordinateReferenceSystem crs) {
                crss.add(crs);
            } else if (srs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                crss.add(dggrs.getGridSystem().getCrs());
            } else if (srs instanceof ReferencingByIdentifiers rbi) {
                crss.add(CommonCRS.WGS84.normalizedGeographic());
            } else {
                throw new UnsupportedOperationException("todo");
            }
        }
        try {
            leaningCrs = CRS.compound(crss.toArray(CoordinateReferenceSystem[]::new));
        } catch (FactoryException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Identifier getName() {
        return name;
    }

    /**
     * Returns the ordered list of <abbr>RS</abbr> components.
     * The returned list may contain nested compound <abbr>RS</abbr>.
     * For a list without nesting, as required by ISO 19111, see {@link #getSingleComponents()}.
     *
     * @return the ordered list of components of this compound <abbr>RS</abbr>.
     */
    public List<ReferenceSystem> getComponents() {
        return rss;
    }

    /**
     * Returns the ordered list of <abbr>CRS</abbr> components, none of which itself compound.
     * If this compound <abbr>CRS</abbr> contains nested compound <abbr>CRS</abbr> components,
     * then those components are flattened recursively in a sequence of {@link SingleCRS} objects.
     *
     * @return the ordered list of components of this compound <abbr>CRS</abbr>, none of which itself compound.
     *
     * @since 3.1
     */
    public List<ReferenceSystem> getSingleComponents() {
        var singles = new ArrayList<ReferenceSystem>(5);
        flatten(singles, new LinkedList<>());   // Linked list is cheap to construct and efficient with 0 or 1 element.
        return Collections.unmodifiableList(singles);
    }


    public synchronized CoordinateReferenceSystem getLeaningCrs() {
        return leaningCrs;
    }

    /**
     * Appends recursively all single components in the given list.
     *
     * @param  singles  the list where to add single components.
     * @param  safety   a safety against infinite recursive method calls.
     * @throws IllegalStateException if recursive components are detected.
     */
    private void flatten(final List<ReferenceSystem> singles, final List<Object> safety) {
        for (ReferenceSystem rs : getComponents()) {
            if (rs instanceof SingleCRS) {
                singles.add((SingleCRS) rs);
            } else  if (rs instanceof ReferencingByIdentifiers) {
                singles.add(rs);
            } else if (rs instanceof CompoundRS) {
                final CompoundRS r = (CompoundRS) rs;
                for (Object previous : safety) {
                    if (previous == this) {
                        throw new IllegalStateException("Recursive components detected.");
                    }
                }
                safety.add(this);
                singles.addAll(r.getSingleComponents());

            } else if (rs instanceof CompoundCRS) {
                final CompoundCRS r = (CompoundCRS) rs;
                //todo CompoundCRS should be a CompoundRS
                for (Object previous : safety) {
                    if (previous == this) {
                        throw new IllegalStateException("Recursive components detected.");
                    }
                }
                safety.add(this);
                singles.addAll(r.getSingleComponents());
            } else {
                throw new IllegalArgumentException("Unknown reference system type : " + rs.getName());
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CompoundRS[ " + name.toString()+" ");
        final List<ReferenceSystem> components = getComponents();
        for (int i = 0, n = components.size(); i < n; i++) {
            if (i > 0)sb.append(',');
            final ReferenceSystem rs = components.get(i);
            sb.append(rs.toString().replaceAll("\n", "    \n"));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.name);
        hash = 23 * hash + Objects.hashCode(this.rss);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompoundRS other = (CompoundRS) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.rss, other.rss);
    }

}
