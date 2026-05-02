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

import org.apache.sis.geometries.math.Similarity;
import org.opengis.geometry.Envelope;


/**
 * An oriented geometry is a geometry with specific properties
 * and additional rotation,translation properties.
 * <p>
 * Oriented geometries are often the base class of named geometries which contain curves,
 * like sphere or cylinders, but alos any non basic shapes which can be defined by self-defined properties
 * like height/width/radius and a separate point and orientation.
 * <p>
 * TODO : oriented geometries includes :
 * - infinite plane
 * - finite plane, also called Quad
 * - cylinder with varying top/bottom radius
 * - cone whih is a 0 radius cylinder
 * - capsule  (cylinder with hemispherical ends), with varying top/bottom radius
 * - sphere
 * - ellipsoid
 *
 * @author Johann Sorel
 */
public interface OrientedGeometry extends Geometry {

    /**
     * Get geometry transform.
     * @return Similarity, never null
     */
    Similarity<?> getTransform();

    /**
     * Get the geometry envelope without the orientation rotation and translation
     * applied.
     * @return Envelope
     */
    Envelope getUnorientedEnvelope();

}
