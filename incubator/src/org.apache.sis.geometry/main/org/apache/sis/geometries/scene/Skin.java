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
package org.apache.sis.geometries.scene;

import java.util.List;
import java.util.Objects;
import org.apache.sis.geometries.math.Matrix4D;


/**
 * Synonym : Skeleton mapping
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Skin {

    private List<Matrix4D> ibm;
    private SceneNode root;
    private List<SceneNode> joints;

    public List<Matrix4D> getInverseBindMatrices() {
        return ibm;
    }

    public void setInverseBindMatrices(List<Matrix4D> ibm) {
        this.ibm = ibm;
    }

    public SceneNode getRoot() {
        return root;
    }

    public void setRoot(SceneNode root) {
        this.root = root;
    }

    public List<SceneNode> getJoints() {
        return joints;
    }

    public void setJoints(List<SceneNode> joints) {
        this.joints = joints;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.ibm);
        hash = 97 * hash + Objects.hashCode(this.root);
        hash = 97 * hash + Objects.hashCode(this.joints);
        return hash;
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
        final Skin other = (Skin) obj;
        if (!Objects.equals(this.ibm, other.ibm)) {
            return false;
        }
        if (!Objects.equals(this.root, other.root)) {
            return false;
        }
        if (!Objects.equals(this.joints, other.joints)) {
            return false;
        }
        return true;
    }

}
