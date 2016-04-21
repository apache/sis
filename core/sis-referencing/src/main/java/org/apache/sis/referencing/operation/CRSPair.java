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

import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A pair of source-destination {@link CoordinateReferenceSystem} objects.
 * Used as key in hash map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class CRSPair {
    /**
     * The source and target CRS.
     */
    final CoordinateReferenceSystem sourceCRS, targetCRS;

    /**
     * Creates a {@code CRSPair} for the specified source and target CRS.
     */
    CRSPair(final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS)
    {
        this.sourceCRS = sourceCRS;
        this.targetCRS = targetCRS;
    }

    /**
     * Returns the hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(sourceCRS) * 31 + Objects.hashCode(targetCRS);
    }

    /**
     * Compares this pair to the specified object for equality.
     *
     * {@note We perform the CRS comparison using strict equality, not using
     *        <code>equalsIgnoreMetadata</code>, because metadata matter since
     *        they are attributes of the <code>CoordinateOperation</code>
     *        object to be created.}
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof CRSPair) {
            final CRSPair that = (CRSPair) object;
            return Objects.equals(this.sourceCRS, that.sourceCRS) &&
                   Objects.equals(this.targetCRS, that.targetCRS);
        }
        return false;
    }

    /**
     * Returns the name of the GeoAPI interface implemented by the specified object. In the GeographicCRS
     * or EllipsoidalCS cases, the trailing CRS or CS suffix is replaced by the number of dimensions
     * (e.g. "Geographic3D").
     */
    static String label(final IdentifiedObject object) {
        if (object == null) {
            return null;
        }
        Class<? extends IdentifiedObject> type;
        if (object instanceof AbstractIdentifiedObject) {
            type = ((AbstractIdentifiedObject) object).getInterface();
        } else {
            type = Classes.getLeafInterfaces(object.getClass(), IdentifiedObject.class)[0];
        }
        String suffix, label = Classes.getShortName(type);
        if (label.endsWith((suffix = "CRS")) || label.endsWith(suffix = "CS")) {
            Object cs = object;
            if (object instanceof CoordinateReferenceSystem) {
                cs = ((CoordinateReferenceSystem) object).getCoordinateSystem();
            }
            if (cs instanceof EllipsoidalCS) {
                final StringBuilder sb = new StringBuilder(label);
                sb.setLength(label.length() - suffix.length());
                label = sb.append(((CoordinateSystem) cs).getDimension()).append('D').toString();
            }
        }
        String name = IdentifiedObjects.getName(object, null);
        if (name != null) {
            int i = 30;                                         // Arbitrary length threshold.
            if (name.length() >= i) {
                while (i > 15) {                                // Arbitrary minimal length.
                    final int c = name.codePointBefore(i);
                    if (Character.isSpaceChar(c)) break;
                    i -= Character.charCount(c);
                }
                name = CharSequences.trimWhitespaces(name, 0, i).toString() + '…';
            }
            label = label + "[“" + name + "”]";
        }
        return label;
    }

    /**
     * Return a string representation of this key.
     */
    @Override
    public String toString() {
        return label(sourceCRS) + " → " + label(targetCRS);
    }
}
