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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers;
import org.apache.sis.util.ArraysExt;


/**
 * Holds the ordinates for geometry/area/zone/point within some reference system.
 *
+ * synonym : ISO-19170 : 8.2.4.3.  Cell address
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Code {

    private final ReferenceSystem rs;
    private final Object[] ordinates;

    public Code(ReferenceSystem rs) {
        this.rs = rs;
        this.ordinates = new Object[ReferenceSystems.getDimension(rs)];
    }

    public Code(ReferenceSystem rs, Object ... ordinates) {
        this.rs = rs;
        this.ordinates = ordinates;
    }

    /**
     * The reference system (RS) in which the location tuple is given.
     * May be {@code null} if this particular {@code Location} is included in a larger object
     * with such a reference to a {@linkplain ReferenceSystem reference system}.
     * In this case, the reference system is implicitly assumed to take on the value
     * of the containing object's <abbr>RS</abbr>.
     *
     * @return the reference system (RS), or {@code null}.
     */
    public ReferenceSystem getReferenceSystem() {
        return rs;
    }

    /**
     * The length of ordinate sequence (the number of entries). This is determined by the
     * {@linkplain #getReferenceSystem() reference system}.
     *
     * @return the dimensionality of this location.
     */
    public int getDimension() {
        return ordinates.length;
    }

    /**
     * A <b>copy</b> of the ordinates stored as an array of object values.
     * Changes to the returned array will not affect this {@code Location}.
     * The array length shall be equal to the {@linkplain #getDimension() dimension}.
     *
     * @return a copy of the ordinates. Changes in the returned array will not be reflected back
     *         in this {@code Location} object.
     */
    public Object[] getOrdinates() {
        return ordinates.clone();
    }

    /**
     * Returns the ordinate at the specified dimension.
     *
     * @param  dimension  the dimension in the range 0 to {@linkplain #getDimension dimension}−1.
     * @return the ordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() number of dimensions}.
     */
    public Object getOrdinate(int dimension) {
        return ordinates[dimension];
    }

    /**
     * Set the ordinate at the specified dimension.
     *
     * @param dimension  the dimension in the range 0 to {@linkplain #getDimension dimension}−1.
     * @param value ordinate value
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() number of dimensions}.
     */
    public void setOrdinate(int dimension, Object value) {
        this.ordinates[dimension] = value;
    }

    /**
     * Set all ordinates.
     *
     * @param values ordinate values
     * @throws MismatchedDimensionException if dimension do not match
     */
    public void setOrdinates(Object... values) throws MismatchedDimensionException {
        if (values.length != ordinates.length) throw new MismatchedDimensionException();
        System.arraycopy(values, 0, ordinates, 0, values.length);
    }

    public DirectPosition toDirectPosition() throws TransformException, FactoryException {
        double[] position = new double[0];
        int offset = 0;
        for (final ReferenceSystem srs : ReferenceSystems.getSingleComponents(rs, true)) {
            if (srs instanceof CoordinateReferenceSystem crs) {
                final double[] coordinates = new double[crs.getCoordinateSystem().getDimension()];
                for (int i = 0; i < coordinates.length; i++) {
                    coordinates[i] = ((Number) ordinates[offset++]).doubleValue();
                }
                position = ArraysExt.concatenate(position, coordinates);
            } else if (srs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                final Zone zone = dggrs.getGridSystem().getHierarchy().getZone(ordinates[offset++]);
                position = ArraysExt.concatenate(position, zone.getPosition().getCoordinates());
            } else {
                throw new UnsupportedOperationException("todo");
            }
        }
        final GeneralDirectPosition dp = new GeneralDirectPosition(position);
        dp.setCoordinateReferenceSystem(ReferenceSystems.getLeaningCRS(rs));
        return dp;
    }

    /**
     * Returns one line per single component of the reference system, with its ordinates and its name.
     * Incomplete codes (unset ordinates, no or unknown reference system) are formatted too.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Code");
        final List<ReferenceSystem> components = (rs != null) ? ReferenceSystems.getSingleComponents(rs, true) : List.of();
        int offset = 0;
        for (final ReferenceSystem component : components) {
            if (offset >= ordinates.length) break;
            final int end;
            if (component instanceof CoordinateReferenceSystem crs) {
                end = Math.min(offset + crs.getCoordinateSystem().getDimension(), ordinates.length);
            } else if (component instanceof ReferencingByIdentifiers) {
                end = offset + 1;
            } else {
                end = ordinates.length;     // Unknown number of dimensions, take all remaining ordinates.
            }
            sb.append("\n ");
            for (int i = offset; i < end; i++) {
                sb.append(' ').append(format(component, ordinates[i]));
            }
            sb.append(' ').append(IdentifiedObjects.getName(component, null));
            offset = end;
        }
        if (offset < ordinates.length) {
            sb.append("\n ");
            for (int i = offset; i < ordinates.length; i++) {
                sb.append(' ').append(ordinates[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ordinate as an instant if the component is temporal, or the ordinate unchanged otherwise.
     */
    private static Object format(final ReferenceSystem component, final Object ordinate) {
        if (component instanceof TemporalCRS tcrs && ordinate instanceof Number n) {
            return DefaultTemporalCRS.castOrCopy(tcrs).toInstant(n.doubleValue());
        }
        return ordinate;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + Objects.hashCode(this.rs);
        hash = 61 * hash + Arrays.deepHashCode(this.ordinates);
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
        final Code other = (Code) obj;
        if (!Objects.equals(this.rs, other.rs)) {
            return false;
        }
        return Arrays.deepEquals(this.ordinates, other.ordinates);
    }

}
