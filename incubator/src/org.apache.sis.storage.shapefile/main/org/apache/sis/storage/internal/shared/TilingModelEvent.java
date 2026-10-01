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
import org.opengis.util.GenericName;

/**
 * Coverage store structure change event.
 *
 * @author Johann Sorel (Geomatys)
 */
public class TilingModelEvent extends ModelEvent {

    public static enum Type{
        PYRAMID_ADD,
        PYRAMID_UPDATE,
        PYRAMID_DELETE,
        MOSAIC_ADD,
        MOSAIC_UPDATE,
        MOSAIC_DELETE
    };

    private final Type type;
    private final String pyramidId;
    private final String mosaicId;


    public TilingModelEvent(Resource source, Type type, String pyramidId, String mosaicId) {
        super(source);
        this.type = type;
        this.pyramidId = pyramidId;
        this.mosaicId = mosaicId;
    }

    public Type getType() {
        return type;
    }

    public String getPyramidId() {
        return pyramidId;
    }

    public String getMosaicId() {
        return mosaicId;
    }

    @Override
    public TilingModelEvent copy(final Resource source){
        return new TilingModelEvent(source, type, pyramidId, mosaicId);
    }

    public static TilingModelEvent createPyramidAddEvent(
            final Resource source, final GenericName name, final String pyramidId){
        return new TilingModelEvent(source, Type.PYRAMID_ADD, pyramidId, null);
    }

    public static TilingModelEvent createPyramidUpdateEvent(
            final Resource source, final GenericName name, final String pyramidId){
        return new TilingModelEvent(source, Type.PYRAMID_UPDATE, pyramidId, null);
    }

    public static TilingModelEvent createPyramidDeleteEvent(
            final Resource source, final GenericName name, final String pyramidId){
        return new TilingModelEvent(source, Type.PYRAMID_DELETE, pyramidId, null);
    }

    public static TilingModelEvent createMosaicAddEvent(
            final Resource source, final GenericName name, final String pyramidId, final String mosaicId){
        return new TilingModelEvent(source, Type.PYRAMID_ADD, pyramidId, mosaicId);
    }

    public static TilingModelEvent createMosaicUpdateEvent(
            final Resource source, final GenericName name, final String pyramidId, final String mosaicId){
        return new TilingModelEvent(source, Type.PYRAMID_UPDATE, pyramidId, mosaicId);
    }

    public static TilingModelEvent createMosaicDeleteEvent(
            final Resource source, final GenericName name, final String pyramidId, final String mosaicId){
        return new TilingModelEvent(source, Type.PYRAMID_DELETE, pyramidId, mosaicId);
    }

}
