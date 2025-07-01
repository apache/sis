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
package org.apache.sis.geometries.privy;

import java.util.Objects;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;


/**
 * Default triangle implementation.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultTriangle extends DefaultPolygon implements Triangle {

    /**
     * @param exterior exterior triangle ring
     */
    public DefaultTriangle(LinearRing exterior) {
        super(exterior);
        final PointSequence points = exterior.getPoints();
        final int size = points.size();
        switch (size) {
            case 0: //empty triangle
                break;
            case 4:
                if (!points.getPosition(0).equals(points.getPosition(3))) {
                    throw new IllegalArgumentException("Triangle first and last point positions must be identical");
                }   break;
            default:
                throw new IllegalArgumentException("Triangle exterior ring must be composed of 0 or 4 points");
        }
    }

    @Override
    public LinearRing getExteriorRing() {
        return (LinearRing) exterior;
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
        final DefaultTriangle other = (DefaultTriangle) obj;
        return Objects.equals(this.exterior, other.exterior);
    }

    @Override
    public int hashCode() {
        return 7 * exterior.hashCode();
    }

    @Override
    public String asText() {
        return Triangle.super.asText();
    }

}
