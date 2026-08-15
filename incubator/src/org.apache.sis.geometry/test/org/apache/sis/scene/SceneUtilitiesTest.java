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
package org.apache.sis.scene;

import org.apache.sis.geometries.math.Similarity3D;
import org.apache.sis.geometries.math.Vector3D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SceneUtilitiesTest {

    @Test
    public void testComputeRootToNode() {

        final SceneNode root = new SceneNode();
        final SceneNode n0 = new SceneNode();
        final SceneNode n1 = new SceneNode();
        final SceneNode n2 = new SceneNode();
        final SceneNode n3 = new SceneNode();
        n0.getTransform().setTranslation(new Vector3D.Double(1,2,3));
        n1.getTransform().setTranslation(new Vector3D.Double(4,5,6));
        n2.getTransform().setTranslation(new Vector3D.Double(10,20,30));
        n3.getTransform().setTranslation(new Vector3D.Double(100,200,300));

        root.getChildren().add(n0);
        root.getChildren().add(n1);
        n1.getChildren().add(n2);
        n1.getChildren().add(n3);

        Similarity3D t = SceneUtilities.computeRootOtNodeTransform(n0);
        assertEquals(new Vector3D.Double(1,2,3), t.getTranslation());

        t = SceneUtilities.computeRootOtNodeTransform(n1);
        assertEquals(new Vector3D.Double(4,5,6), t.getTranslation());

        t = SceneUtilities.computeRootOtNodeTransform(n2);
        assertEquals(new Vector3D.Double(14,25,36), t.getTranslation());

        t = SceneUtilities.computeRootOtNodeTransform(n3);
        assertEquals(new Vector3D.Double(104,205,306), t.getTranslation());
    }
}
