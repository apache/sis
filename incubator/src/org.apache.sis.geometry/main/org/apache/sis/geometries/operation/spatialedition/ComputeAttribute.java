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
package org.apache.sis.geometries.operation.spatialedition;

import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.operation.Operation;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.Tuple;
import java.util.function.Function;

/**
 * Create a new attribute or update an existing one.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComputeAttribute extends Operation<ComputeAttribute> {

    /**
     * Function to generate attribute value
     */
    public final Function<Point,Tuple> valueGenerator;
    /**
     * New attribute name
     */
    public final String attributeName;
    /**
     * New attribute system
     */
    public final SampleSystem attributeSystem;
    /**
     * New attribute type
     */
    public final DataType attributeType;
    /**
     * Result geometry, this is often the same geometry as input
     * but may be a new one if adding or modifying the input was not possible.
     */
    public Geometry result;

    /**
     *
     * @param geom geometry to modify
     * @param attributeName new attribute name
     * @param attributeSystem new attribute system
     * @param attributeType new attribute type
     * @param valueGenerator function to generate attribute value
     */
    public ComputeAttribute(Geometry geom, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Tuple> valueGenerator) {
        super(geom);
        this.attributeName = attributeName;
        this.attributeSystem = attributeSystem;
        this.attributeType = attributeType;
        this.valueGenerator = valueGenerator;
    }

}
