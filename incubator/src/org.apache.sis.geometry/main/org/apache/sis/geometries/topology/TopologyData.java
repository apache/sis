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
package org.apache.sis.geometries.topology;

import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.util.GenericName;
import org.apache.sis.geometries.Geometry;


/**
 * Data for building a topology from its underlying geometry, such that all the geometry interiors
 * are disjoint and every geometry boundary is represented by other geometries.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.1.5
 */
@UML(identifier="TopologyData", specification=ISO_19107)
public interface TopologyData {

    /**
     * Identifier by which other data of the same construction refer to this one.
     * Its namespace component is unique within its maximal complex and its topological dimension.
     *
     * @return identifier of this data.
     *
     * @see ISO 19107:2019 - 10.1.5.2
     */
    @UML(identifier="id", specification=ISO_19107)
    GenericName getId();

    /**
     * Data of the elements on the boundary of this element.
     *
     * @return data of the boundary elements, empty if none.
     *
     * @see ISO 19107:2019 - 10.1.5.3
     */
    @UML(identifier="boundaryData", specification=ISO_19107)
    List<? extends TopologyData> getBoundaryData();

    /**
     * Geometry of the interior of this element. Its dimension usually matches the topological dimension,
     * but not necessarily when the topology describes connections, as in a Poincaré dual graph.
     *
     * <p>Difference with ISO 19107: the standard type is {@code GeometryData},
     * which does not exist in this module.</p>
     *
     * @return geometry of the interior of this element.
     *
     * @see ISO 19107:2019 - 10.1.5.4
     */
    @UML(identifier="geometryData", specification=ISO_19107)
    Geometry getGeometryData();

}
