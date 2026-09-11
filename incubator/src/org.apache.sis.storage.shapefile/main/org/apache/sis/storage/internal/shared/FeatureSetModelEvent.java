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
import static org.apache.sis.util.ArgumentChecks.*;
import org.opengis.feature.FeatureType;

/**
 * FeatureSet management event.
 *
 * @todo work in progress
 * @author Johann Sorel (Geomatys)
 */
public class FeatureSetModelEvent extends ModelEvent {

    public static enum Type{
        ADD,
        UPDATE,
        DELETE
    };

    private final Type type;
    private final FeatureType oldType;
    private final FeatureType newType;

    private FeatureSetModelEvent(final Resource source, final Type type, final FeatureType oldtype, final FeatureType newtype){
        super(source);

        ensureNonNull("type", type);
        if(oldtype == null && newtype == null){
            throw new NullPointerException("Old and new feature type can not be both null.");
        }
        this.type = type;
        this.oldType = oldtype;
        this.newType = newtype;
    }

    /**
     * get the event type, can be Add, Update or Delete.
     * @return Type of the event , never null.
     */
    public Type getType() {
        return type;
    }

    /**
     * Retrieve the newly created feature type or
     * the updated feature type.
     * @return FeatureType or null if event is a Delete
     */
    public FeatureType getNewFeatureType() {
        return newType;
    }

    /**
     * Retrieve the deleted feature type or
     * the old updated feature type.
     *
     * @return FeatureType or null if event is an Add
     */
    public FeatureType getOldFeatureType() {
        return oldType;
    }

    @Override
    public FeatureSetModelEvent copy(Resource source) {
        return new FeatureSetModelEvent(source, type, oldType, newType);
    }

    public static FeatureSetModelEvent createAddEvent(final Resource source, final FeatureType type){
        return new FeatureSetModelEvent(source, Type.ADD, null, type);
    }

    public static FeatureSetModelEvent createUpdateEvent(final Resource source, final FeatureType oldType, final FeatureType newType){
        return new FeatureSetModelEvent(source, Type.UPDATE, oldType, newType);
    }

    public static FeatureSetModelEvent createDeleteEvent(final Resource source, final FeatureType type){
        return new FeatureSetModelEvent(source, Type.DELETE, type, null);
    }

}
