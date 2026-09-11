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
package org.apache.sis.storage.internal.shared;

import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.StoreEvent;

/**
 * Event raised when a resource has been added or removed from an Aggregation.
 *
 * @author Johann Sorel (Geomatys)
 */
public class AggregationEvent extends StoreEvent {

    public static final int TYPE_ADD = 1;
    public static final int TYPE_REMOVE = 2;

    private final int type;
    private final Resource[] changes;

    /**
     *
     * @param source parent resource of the modified resources
     * @param type ADD or REMOVE
     * @param changes the resources added or removed.
     */
    public AggregationEvent(Resource source, int type, Resource ... changes) {
        super(source);
        this.type = type;
        this.changes = changes;
    }

    /**
     * @return ADD or REMOVE
     */
    public int getType() {
        return type;
    }

    /**
     * @return the resources added or removed.
     */
    public Resource[] getChanges() {
        return changes;
    }

}
