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
package org.apache.sis.internal.map;

import org.apache.sis.storage.Resource;
import org.opengis.style.Style;

/**
 * A map layer is an association of a {@link Resource} and a {@link Style}.
 *
 * <p>
 * Map layers are the key elements of a map, they defined the relationship
 * between datas and a symbology. The visual result of a layer should be similar
 * for any rendering engine. The result may be different because of different
 * rendering strategies for label placements, 2D or 3D but the fundamentals
 * representation of each Feature or Coverage should be unchanged.
 * </p>
 *
 * <p>
 * NOTE : this class is a first draft subject to modifications.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public final class MapLayer {

    private Resource resource;
    private Style style;

    /**
     * Returns layer base resource.
     *
     * <p>
     * The resource should be a DataSet but may still be a Aggregate.
     * The behavior in such case depends on the rendering engine.
     * </p>
     *
     * @return rendered resource
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Set layer resource.
     *
     * <p>
     * The resource should never be null, still the null case is tolerate
     * to indicate the layer should have exist but is unavailable for an indeterminate
     * reason. This case may happen for processed or distant services resources.
     * </p>
     *
     * @param resource , may be null
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Returns the layer style.
     *
     * <p>
     * If the style is undefined, the behavior is left to the rendering engine.
     * It is expected that a default style should be used.
     * </p>
     *
     * @return layer style, may be null
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Set layer style.
     *
     * @param style layer style, can be null
     */
    public void setStyle(Style style) {
        this.style = style;
    }

}
