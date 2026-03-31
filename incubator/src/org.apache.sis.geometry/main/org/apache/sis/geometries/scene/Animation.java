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

import java.util.Objects;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Animation {

    public static final String TARGET_TRANSLATION = "translation";
    public static final String TARGET_ROTATION = "rotation";
    public static final String TARGET_SCALE = "scale";
    public static final String TARGET_WEIGHTS = "weights";

    private String target;
    private AnimationSampler sampler;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public AnimationSampler getSampler() {
        return sampler;
    }

    public void setSampler(AnimationSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.target);
        hash = 53 * hash + Objects.hashCode(this.sampler);
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
        final Animation other = (Animation) obj;
        if (!Objects.equals(this.target, other.target)) {
            return false;
        }
        if (!Objects.equals(this.sampler, other.sampler)) {
            return false;
        }
        return true;
    }

}
