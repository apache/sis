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
package org.apache.sis.style.sld1;

import java.util.List;


/**
 * A list of constraints restricting the data shown for a layer.
 * The constraints are either on feature types ({@link LayerFeatureConstraints})
 * or on coverages ({@link LayerCoverageConstraints}).
 *
 * @author  Johann Sorel (Geomatys)
 *
 * @param <C>  the type of constraints in the list.
 */
public abstract class LayerConstraints<C> extends SLDElement {
    /**
     * Creates a new list of constraints.
     */
    LayerConstraints() {
    }

    /**
     * Returns the constraints restricting the data shown for a layer.
     * A layer constraints element should contain at least one constraint.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return the constraints, as a live collection.
     */
    public abstract List<C> constraints();

    /**
     * Returns a deep clone of this object.
     *
     * @return deep clone of all constraints.
     */
    @Override
    @SuppressWarnings("unchecked")
    public LayerConstraints<C> clone() {
        return (LayerConstraints<C>) super.clone();
    }
}
