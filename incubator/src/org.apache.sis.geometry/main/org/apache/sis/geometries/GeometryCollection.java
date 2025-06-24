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
package org.apache.sis.geometries;

import java.util.Set;
import org.apache.sis.geometry.GeneralEnvelope;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;

/**
 * A GeometryCollection is a geometric object that is a collection of some number of geometric objects.
 * All the elements in a GeometryCollection shall be in the same Spatial Reference System.
 * This is also the Spatial Reference System for the GeometryCollection.
 * GeometryCollection places no other constraints on its elements.
 *
 * Subclasses of GeometryCollection may restrict membership based on dimension and may also place
 * other constraints on the degree of spatial overlap between elements.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Collection", specification=ISO_19107) // section 6.4.31
public interface GeometryCollection<T extends Geometry> extends Geometry {

    public static final String TYPE = "GEOMETRYCOLLECTION";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @UML(identifier="elementType", specification=ISO_19107) // section 6.4.31.2
    default Set<GeometryType> getElementType() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isEmpty() {
        return getNumGeometries() == 0;
    }

    /**
     * Returns the number of geometries in this GeometryCollection.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.3.2
     * @return the number of geometries in this GeometryCollection.
     */
    @UML(identifier="numElement", specification=ISO_19107) // section 6.4.31.3
    int getNumGeometries();

    /**
     * Returns the Nth geometry in this GeometryCollection.
     *
     * Difference with ISO 19107 :
     * - for add as defined by ISO 19107, use GeometryOperations union operator.
     * - for remove as defined by ISO 19107, use GeometryOperations difference operator.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.3.2
     * @param n geometry index.
     * @return the Nth geometr in this GeometryCollection.
     */
    @UML(identifier="element", specification=ISO_19107) // section 6.4.32
    T getGeometryN(int n);

    @Override
    public default Envelope getEnvelope() {
        GeneralEnvelope e = null;
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            T sn = getGeometryN(i);
            Envelope envelope = sn.getEnvelope();
            if (envelope != null) {
                if (e == null) {
                    e = new GeneralEnvelope(envelope);
                } else {
                    e.add(envelope);
                }
            }
        }
        if (e == null) {
            e = new GeneralEnvelope(getCoordinateReferenceSystem());
            e.setToNaN();
        }
        return e;
    }

    @Override
    public default AttributesType getAttributesType() {
        if (getNumGeometries() == 0) return null;
        return getGeometryN(0).getAttributesType();
    }

}
