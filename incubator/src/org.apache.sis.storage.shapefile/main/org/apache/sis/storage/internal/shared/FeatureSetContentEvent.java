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

import java.util.Set;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.storage.Resource;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.ResourceId;

/**
 * FeatureSet content event.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FeatureSetContentEvent extends ContentEvent {

    public static enum Type{
        ADD,
        UPDATE,
        DELETE
    };

    private final Type type;
    private Filter ids;

    public FeatureSetContentEvent(final Resource source, final Type type, final Filter<Feature> identifiers) {
        super(source);
        this.type = type;
        this.ids = identifiers;
    }

    public FeatureSetContentEvent(final Resource source, final Type type, final Set<ResourceId> ids){
        this(source, type, resourceId(ids));
    }

    public static Filter<Feature> resourceId(final Set<ResourceId> ids) {
        if (ids == null) {
            return null;
        }
        switch (ids.size()) {
            case 0:  return Filter.exclude();
            case 1:  return ids.iterator().next();
            default: return DefaultFilterFactory.forFeatures().or((Set) ids);
        }
    }

    /**
     * Get the event type, can be Add, Update or Delete.
     * @return Type of the event , never null.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the modified feature ids related to this event.
     * This object may be null if the ids could not be retrieved.
     * @return ResourceId or null
     */
    public Filter<Feature> getIds() {
        return ids;
    }

    public FeatureSetContentEvent copy(final Resource source){
        return new FeatureSetContentEvent(source, type, ids);
    }

    public static FeatureSetContentEvent createAddEvent(final Resource source, final Filter<Feature> ids){
        return new FeatureSetContentEvent(source, Type.ADD, ids);
    }

    public static FeatureSetContentEvent createUpdateEvent(final Resource source, final Filter<Feature> ids){
        return new FeatureSetContentEvent(source, Type.UPDATE, ids);
    }

    public static FeatureSetContentEvent createDeleteEvent(final Resource source, final Filter<Feature> ids){
        return new FeatureSetContentEvent(source, Type.DELETE, ids);
    }

}
