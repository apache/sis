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
import org.apache.sis.util.ArgumentChecks;


/**
 * A sampler defines how textures are to be interpolated and clipped/repeated when applied on a surface.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Sampler {

    public static enum Wrap {
        CLAMP_TO_EDGE,
        MIRRORED_REPEAT,
        REPEAT
    }

    public static enum MagFilter {
        /** system defined */
        AUTO,
        NEAREST,
        LINEAR,
    }

    public static enum MinFilter {
        /** system defined */
        AUTO,
        NEAREST,
        LINEAR,
        NEAREST_MIPMAP_NEAREST,
        LINEAR_MIPMAP_NEAREST,
        NEAREST_MIPMAP_LINEAR,
        LINEAR_MIPMAP_LINEAR
    }

    /**
     * Magnification filter.
     */
    private MagFilter magFilter = MagFilter.AUTO;
    /**
     * Minification filter.
     */
    private MinFilter minFilter = MinFilter.AUTO;
    /**
     * s wrapping mode.
     * default value : REPEAT
     */
    private Wrap wrapS = Wrap.REPEAT;
    /**
     * t wrapping mode.
     * default value : REPEAT
     */
    private Wrap wrapT = Wrap.REPEAT;

    public Sampler() {
    }

    /**
     * Magnification filter.
     */
    public MagFilter getMagFilter() {
        return magFilter;
    }

    /**
     * Magnification filter.
     */
    public void setMagFilter(MagFilter magFilter) {
        ArgumentChecks.ensureNonNull("mag Filter", magFilter);
        this.magFilter = magFilter;
    }

    /**
     * Minification filter.
     */
    public MinFilter getMinFilter() {
        return minFilter;
    }

    /**
     * Minification filter.
     */
    public void setMinFilter(MinFilter minFilter) {
        ArgumentChecks.ensureNonNull("min Filter", minFilter);
        this.minFilter = minFilter;
    }

    /**
     * s wrapping mode.
     */
    public Wrap getWrapS() {
        return wrapS;
    }

    /**
     * s wrapping mode.
     */
    public void setWrapS(Wrap wrapS) {
        ArgumentChecks.ensureNonNull("wrap s", wrapS);
        this.wrapS = wrapS;
    }

    /**
     * t wrapping mode.
     */
    public Wrap getWrapT() {
        return wrapT;
    }

    /**
     * t wrapping mode.
     */
    public void setWrapT(Wrap wrapT) {
        ArgumentChecks.ensureNonNull("wrap t", wrapT);
        this.wrapT = wrapT;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.magFilter);
        hash = 97 * hash + Objects.hashCode(this.minFilter);
        hash = 97 * hash + Objects.hashCode(this.wrapS);
        hash = 97 * hash + Objects.hashCode(this.wrapT);
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
        final Sampler other = (Sampler) obj;
        if (this.magFilter != other.magFilter) {
            return false;
        }
        if (this.minFilter != other.minFilter) {
            return false;
        }
        if (this.wrapS != other.wrapS) {
            return false;
        }
        if (this.wrapT != other.wrapT) {
            return false;
        }
        return true;
    }

}
