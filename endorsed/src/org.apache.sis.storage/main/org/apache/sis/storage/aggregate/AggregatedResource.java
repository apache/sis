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
package org.apache.sis.storage.aggregate;

import org.opengis.util.GenericName;
import org.apache.sis.storage.Resource;


/**
 * The result of an aggregation computed by {@link CoverageAggregator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.3
 */
interface AggregatedResource {
    /**
     * Sets the identifier of the resource.
     * This method is invoked by {@link CoverageAggregator#build(GenericName)} for assigning an identifier
     * on the final result only. No identifier should be assigned on intermediate results (i.e. components).
     *
     * @param  identifier  new identifier of the resource.
     */
    void setIdentifier(GenericName identifier);

    /**
     * Sets the name of the resource.
     * This method is invoked by {@link GroupAggregate#simplify(CoverageAggregator)} when
     * an aggregate node is excluded and we want to inherit the name of the excluded node.
     * It should happen before the resource is published.
     *
     * @param  name  new name of the resource.
     */
    void setName(String name);

    /**
     * Returns a resource with the same data but the specified merge strategy.
     * If this resource already uses the given strategy, then returns {@code this}.
     * Otherwise returns a new resource. This resource is not modified by this method
     * call because this method can be invoked after this resource has been published.
     *
     * <h4>API design note</h4>
     * We could try to design a common API for {@link org.apache.sis.storage.RasterLoadingStrategy}
     * and {@link MergeStrategy}. But the former changes the state of the resource while the latter
     * returns a new resource. This is because {@code RasterLoadingStrategy} does not change data,
     * while {@link MergeStrategy} can change the data obtained from the resource.
     *
     * @param  strategy  the new merge strategy to apply.
     * @return resource using the specified strategy (may be {@code this}).
     *
     * @see MergeStrategy#apply(Resource)
     */
    Resource apply(MergeStrategy strategy);
}
