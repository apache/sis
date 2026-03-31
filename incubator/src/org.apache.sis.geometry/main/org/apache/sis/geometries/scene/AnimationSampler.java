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
import org.apache.sis.geometries.math.Array;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class AnimationSampler {

    private Array time;
    private Array Values;

    public Array getTime() {
        return time;
    }

    public void setTime(Array time) {
        this.time = time;
    }

    public Array getValues() {
        return Values;
    }

    public void setValues(Array Values) {
        this.Values = Values;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Objects.hashCode(this.time);
        hash = 19 * hash + Objects.hashCode(this.Values);
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
        final AnimationSampler other = (AnimationSampler) obj;
        if (!Objects.equals(this.time, other.time)) {
            return false;
        }
        if (!Objects.equals(this.Values, other.Values)) {
            return false;
        }
        return true;
    }

}
