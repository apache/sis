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

import org.apache.sis.maths.Similarity3D;
import org.apache.sis.maths.Vector;

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
        n0.getTransform().setTranslation(Vector.of(1.0,2.0,3.0));
        n1.getTransform().setTranslation(Vector.of(4.0,5.0,6.0));
        n2.getTransform().setTranslation(Vector.of(10.0,20.0,30.0));
        n3.getTransform().setTranslation(Vector.of(100.0,200.0,300.0));

        root.getChildren().add(n0);
        root.getChildren().add(n1);
        n1.getChildren().add(n2);
        n1.getChildren().add(n3);

        Similarity3D t = SceneUtilities.computeRootOtNodeTransform(n0);
        assertEquals(Vector.of(1.0,2.0,3.0), t.getTranslation());

        t = SceneUtilities.computeRootOtNodeTransform(n1);
        assertEquals(Vector.of(4.0,5.0,6.0), t.getTranslation());

        t = SceneUtilities.computeRootOtNodeTransform(n2);
        assertEquals(Vector.of(14.0,25.0,36.0), t.getTranslation());

        t = SceneUtilities.computeRootOtNodeTransform(n3);
        assertEquals(Vector.of(104.0,205.0,306.0), t.getTranslation());
    }
}
