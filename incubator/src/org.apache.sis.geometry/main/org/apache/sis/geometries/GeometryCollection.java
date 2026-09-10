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
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.ProductCurve;
import org.apache.sis.geometries.internal.shared.DefaultGeometryCollection;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.solid.MultiPolyhedron;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometry.GeneralEnvelope;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;


/**
 * A typed collection of geometric objects which behaves as the set union of its elements.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the elements are in the same spatial reference system, which is also the reference
 *       system of the collection.</li>
 *   <li>Every element is an instance of one of the {@linkplain #getElementType() allowed types},
 *       or of a subtype of one of them.</li>
 *   <li>The topological dimension of the collection is the largest topological dimension among
 *       its elements.</li>
 *   <li>No other constraint is placed on the elements, but subtypes may restrict membership based
 *       on dimension and may constrain the degree of spatial overlap between elements.</li>
 * </ul>
 *
 * <p>Note: because a collection behaves as the union of its elements and not as a set of sets, an
 * implementation which needs a genuine set of sets should use a plain array of geometries instead
 * of this interface.</p>
 *
 * @param  <T>  type of the elements of this collection.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.3
 * @see ISO 19107:2019 - 6.4.31
 */
@UML(identifier="Collection", specification=ISO_19107)
public sealed interface GeometryCollection<T extends Geometry> extends Geometry
        permits MultiPoint,
                MultiCurve,
                MultiSurface,
                MultiPolyhedron,
                ProductCurve,
                DefaultGeometryCollection,
                MultiMeshPrimitive
{

    /**
     * Well-known text keyword of this geometry type.
     */
    public static final String TYPE = "GEOMETRYCOLLECTION";

    /**
     * Returns {@value #TYPE}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Geometry types allowed in this collection, usually fixed at construction time
     * according to the purpose of this particular collection.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Every element of this collection is an instance of one of the returned types,
     *       or of a subtype of one of them.</li>
     *   <li>An implementation may make this set read-only when the purpose of the collection
     *       restricts the allowed types.</li>
     * </ul>
     *
     * @return geometry types allowed in this collection.
     *
     * @see ISO 19107:2019 - 6.4.31.2
     */
    @UML(identifier="elementType", specification=ISO_19107)
    default Set<GeometryType> getElementType() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether this collection has no element.
     *
     * @see ISO 19107:2019 - 6.4.4.2
     */
    @Override
    default boolean isEmpty() {
        return getNumGeometries() == 0;
    }

    /**
     * Returns the number of geometries in this GeometryCollection.
     * It can only change by adding or removing elements.
     *
     * @return the number of geometries in this GeometryCollection.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.3.2
     * @see ISO 19107:2019 - 6.4.31.3
     */
    @UML(identifier="numElement", specification=ISO_19107)
    int getNumGeometries();

    /**
     * Returns the Nth geometry in this GeometryCollection.
     *
     * <p>Difference with ISO 19107:</p>
     * <ul>
     *   <li>for {@code add} as defined by ISO 19107, use the union operator of
     *       {@link org.apache.sis.geometries.operation.GeometryProcessor};</li>
     *   <li>for {@code remove} as defined by ISO 19107, use the difference operator of
     *       {@link org.apache.sis.geometries.operation.GeometryProcessor}.</li>
     * </ul>
     *
     * @param n geometry index.
     * @return the Nth geometr in this GeometryCollection.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.3.2
     * @see ISO 19107:2019 - 6.4.32, 6.4.32.2, 6.4.32.3
     */
    @UML(identifier="element", specification=ISO_19107)
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
