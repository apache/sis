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
package org.apache.sis.geometries.math;

/**
 * A similarity is the equivalent of a affine transform but preserving angles by avoiding
 * shearing value not rotations.
 * 3 different elements are stored.
 * - rotation matrix
 * - translation vector
 * - scale vector
 * <p>
 * The purpose of similary is to store elements separately, avoiding innacuracy and progressive
 * distortion when opearations are accumulated.
 * <p>
 * A good description of the problem can be found here :
 * https://www.gamedeveloper.com/programming/in-depth-matrices-rotation-scale-and-drifting
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Similarity {

    /**
     * Test if this transform is identity.
     * <ul>
     *  <li>Scale must be all at 1</li>
     *  <li>Translation must be all at 0</li>
     *  <li>Rotation must be an identity matrix</li>
     * </ul>
     *
     * @return true if transform is identity.
     */
    boolean isIdentity();

    /**
     * Set this transformation to identity.
     */
    void toIdentity();

}
